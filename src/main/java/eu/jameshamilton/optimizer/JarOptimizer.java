package eu.jameshamilton.optimizer;

import eu.jameshamilton.classfile.JarClassHierarchyResolver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.classfile.ClassHierarchyResolver;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

import static java.util.concurrent.CompletableFuture.runAsync;

public class JarOptimizer {

    private static final Logger logger = LogManager.getLogger(JarOptimizer.class);

    static void main(String[] args) throws IOException {
        var input = Path.of(args[0]);
        OptimizationStats stats = new OptimizationStats();

        if (input.getFileName().toString().endsWith(".jar")) {
            if (args.length != 2) {
                logger.error("Expected output jar name");
            }
            optimizeJar(stats, args[0], args[1]);
        }

        stats.printSummary();
    }

    private static void optimizeJar(OptimizationStats stats, String inputJarPath, String outputJarPath) throws IOException {
        try (var jarFile = new JarFile(inputJarPath)) {
            // Create a thread-safe collection to store optimized entries
            var optimizedEntries = new ConcurrentHashMap<String, byte[]>();

            // Create class hierarchy resolver
            var resolver = new JarClassHierarchyResolver(jarFile)
                .orElse(ClassHierarchyResolver.defaultResolver())
                .cached(ConcurrentHashMap::new);

            // Get all entries that need processing
            var classEntries = new ArrayList<JarEntry>();
            var resourceEntries = new ArrayList<JarEntry>();
            var errorEntries = Collections.synchronizedSet(new HashSet<JarEntry>());

            var entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                if (entry.getName().endsWith(".class")) {
                    classEntries.add(entry);
                } else {
                    resourceEntries.add(entry);
                }
            }

            for (int pass = 1; pass <= 3; pass++) {
                logger.info("Optimizing pass {}...", pass);
                stats.setPass(pass);
                logger.info("Processing {} class files", classEntries.size());
                // Process class files in parallel using virtual threads
                try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                    var futures = classEntries.stream()
                        .filter(o -> !errorEntries.contains(o))
                        .map(entry -> runAsync(() -> {

                        stats.recordFileProcessingStart(entry.getName());
                        try (InputStream is = new BufferedInputStream(jarFile.getInputStream(entry))) {
                            byte[] classBytes = is.readAllBytes();

                            try {
                                // Parse and optimize
                                byte[] currentBytes = optimizedEntries.getOrDefault(
                                    entry.getName(),
                                    readEntryBytes(jarFile, entry)
                                );

                                stats.recordParseSuccess(entry.getName());
                                var classModelOptimizer = new ClassOptimizer(stats, resolver, currentBytes);
                                Optional<byte[]> optimizedBytes = classModelOptimizer.optimize(Optimization.optimizations);
                                if (optimizedBytes.isEmpty()) {
                                    logger.warn("Could not optimize {}", entry.getName());
                                    // Store original bytes on error
                                    optimizedEntries.put(entry.getName(), classBytes);
                                    errorEntries.add(entry);
                                } else {
                                    optimizedEntries.put(entry.getName(), optimizedBytes.get());
                                }
                            } catch (Exception e) {
                                logger.error("Error optimizing {}: {}", entry.getName(), e.getMessage(), e);
                                // Store original bytes on error
                                optimizedEntries.put(entry.getName(), classBytes);
                                errorEntries.add(entry);
                            }
                        } catch (IOException e) {
                            stats.recordParseError(entry.getName(), e);
                            errorEntries.add(entry);
                            throw new CompletionException(e);
                        }
                    }, executor));

                    // Wait for all optimization tasks to complete
                    CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();

                    stats.printPassSummary();
                }
            }

            logger.info("Writing optimized JAR...");

            try (var jos = new JarOutputStream(new BufferedOutputStream(new FileOutputStream(outputJarPath)))) {
                for (var entry : classEntries) {
                    jos.putNextEntry(new JarEntry(entry.getName()));
                    jos.write(optimizedEntries.get(entry.getName()));
                    jos.closeEntry();
                }

                // Copy all the non-class files.
                for (var entry : resourceEntries) {
                    jos.putNextEntry(new JarEntry(entry.getName()));
                    try (InputStream is = new BufferedInputStream(jarFile.getInputStream(entry))) {
                        is.transferTo(jos);
                    }
                    jos.closeEntry();
                }
            }
        }

        logger.info("Optimized JAR written to: {}", outputJarPath);
    }


    private static byte[] readEntryBytes(JarFile jarFile, JarEntry entry) throws IOException {
        try (InputStream is = jarFile.getInputStream(entry)) {
            return is.readAllBytes();
        }
    }
}