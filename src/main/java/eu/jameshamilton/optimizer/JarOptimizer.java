package eu.jameshamilton.optimizer;

import static java.util.concurrent.CompletableFuture.runAsync;

import eu.jameshamilton.classfile.JarClassHierarchyResolver;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class JarOptimizer {

    private static final Logger logger = LogManager.getLogger(JarOptimizer.class);
    private static final String CLASS_EXTENSION = ".class";

    static void main(String[] args) throws IOException {
        var input = Path.of(args[0]);
        OptimizationStats stats = new OptimizationStats();

        if (input.getFileName().toString().endsWith(".jar")) {
            if (args.length != 2) {
                logger.error("Expected output jar name");
            }
            optimizeJar(stats, input, Path.of(args[1]));
        }

        stats.printSummary();
    }

    private static void optimizeJar(OptimizationStats stats, Path inputJar, Path outputJar)
            throws IOException {
        try (var jarFile = new JarFile(inputJar.toString())) {
            var optimizedEntries = new ConcurrentHashMap<String, byte[]>();

            var resolver =
                    new JarClassHierarchyResolver(jarFile)
                            .orElse(ClassHierarchyResolver.defaultResolver())
                            .cached(ConcurrentHashMap::new);

            var classEntries = new ArrayList<JarEntry>();
            var nonClassEntries = new ArrayList<JarEntry>();
            var errorEntries = Collections.synchronizedSet(new HashSet<JarEntry>());

            categorizeEntries(jarFile, classEntries, nonClassEntries);

            for (int pass = 1; pass <= 3; pass++) {
                stats.setPass(pass);
                logger.info("Optimizing pass {}...", pass);
                logger.info("Processing {} class files", classEntries.size());
                try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                    var futures =
                            classEntries.stream()
                                    .filter(o -> !errorEntries.contains(o))
                                    .map(
                                            entry ->
                                                    runAsync(
                                                            () ->
                                                                    optimizeClass(
                                                                            stats,
                                                                            jarFile,
                                                                            resolver,
                                                                            optimizedEntries,
                                                                            errorEntries,
                                                                            entry),
                                                            executor))
                                    .toArray(CompletableFuture[]::new);

                    CompletableFuture.allOf(futures).join();
                    stats.printPassSummary();
                }
            }

            logger.info("Writing optimized JAR...");
            writeOutputJar(jarFile, outputJar, classEntries, nonClassEntries, optimizedEntries);
        }
        logger.info("Optimized JAR written to: {}", outputJar);
    }

    private static void categorizeEntries(
            JarFile jarFile, List<JarEntry> classEntries, List<JarEntry> nonClassEntries) {
        var entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            var entry = entries.nextElement();
            if (entry.getName().endsWith(CLASS_EXTENSION)) {
                classEntries.add(entry);
            } else {
                nonClassEntries.add(entry);
            }
        }
    }

    private static void optimizeClass(
            OptimizationStats stats,
            JarFile jarFile,
            ClassHierarchyResolver resolver,
            ConcurrentHashMap<String, byte[]> optimizedEntries,
            Set<JarEntry> errorEntries,
            JarEntry entry) {
        stats.recordFileProcessingStart(entry.getName());
        try (InputStream is = new BufferedInputStream(jarFile.getInputStream(entry))) {
            byte[] originalBytes = is.readAllBytes();
            try {
                byte[] currentBytes =
                        optimizedEntries.getOrDefault(
                                entry.getName(), readEntryBytes(jarFile, entry));
                stats.recordParseSuccess(entry.getName());
                var classModelOptimizer = new ClassOptimizer(stats, resolver, currentBytes);
                Optional<byte[]> optimizedBytes =
                        classModelOptimizer.optimize(Optimization.optimizations);
                if (optimizedBytes.isEmpty()) {
                    logger.warn("Could not optimize {}", entry.getName());
                    optimizedEntries.put(entry.getName(), originalBytes);
                    errorEntries.add(entry);
                    stats.recordError(entry.getName());
                } else {
                    optimizedEntries.put(entry.getName(), optimizedBytes.get());
                }
            } catch (Exception e) {
                logger.error("Error optimizing {}: {}", entry.getName(), e.getMessage(), e);
                optimizedEntries.put(entry.getName(), originalBytes);
                errorEntries.add(entry);
                stats.recordError(entry.getName(), e);
            }
        } catch (IOException e) {
            stats.recordError(entry.getName(), e);
            errorEntries.add(entry);
            throw new CompletionException(e);
        }
    }

    private static void writeOutputJar(
            JarFile sourceJar,
            Path outputJar,
            List<JarEntry> classEntries,
            List<JarEntry> nonClassEntries,
            Map<String, byte[]> optimizedEntries)
            throws IOException {
        try (var jos =
                new JarOutputStream(
                        new BufferedOutputStream(new FileOutputStream(outputJar.toString())))) {
            for (var entry : classEntries) {
                jos.putNextEntry(new JarEntry(entry.getName()));
                jos.write(optimizedEntries.get(entry.getName()));
                jos.closeEntry();
            }
            for (var entry : nonClassEntries) {
                jos.putNextEntry(new JarEntry(entry.getName()));
                try (InputStream is = new BufferedInputStream(sourceJar.getInputStream(entry))) {
                    is.transferTo(jos);
                }
                jos.closeEntry();
            }
        }
    }

    private static byte[] readEntryBytes(JarFile jarFile, JarEntry entry) throws IOException {
        try (InputStream is = jarFile.getInputStream(entry)) {
            return is.readAllBytes();
        }
    }
}
