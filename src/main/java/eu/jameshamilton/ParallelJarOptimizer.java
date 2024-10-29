package eu.jameshamilton;

import java.io.*;
import java.lang.classfile.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;
import java.util.jar.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ParallelJarOptimizer {
    private static final int BUFFER_SIZE = 8192;

    public static void optimizeJar(String inputJarPath, String outputJarPath) throws IOException {
        try (JarFile jarFile = new JarFile(inputJarPath)) {
            // Create a thread-safe collection to store optimized entries
            ConcurrentHashMap<String, byte[]> optimizedEntries = new ConcurrentHashMap<>();
            
            // Create class hierarchy resolver
            ClassHierarchyResolver resolver = new JarClassHierarchyResolver(jarFile)
                .orElse(ClassHierarchyResolver.defaultResolver())
                .cached();

            // Get all entries that need processing
            List<JarEntry> classEntries = new ArrayList<>();
            List<JarEntry> resourceEntries = new ArrayList<>();
            
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().endsWith(".class")) {
                    classEntries.add(entry);
                } else {
                    resourceEntries.add(entry);
                }
            }

            // Counter for progress tracking
            AtomicInteger processedCount = new AtomicInteger(0);
            int totalClasses = classEntries.size();

            // Process class files in parallel using virtual threads
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                List<CompletableFuture<Void>> futures = new ArrayList<>();

                for (JarEntry entry : classEntries) {
                    CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                        try {
                            // Read the class file
                            byte[] classBytes = readEntryBytes(jarFile, entry);
                            
                            // Parse and optimize
                            ClassModel classModel = ClassFile.of().parse(classBytes);
                            ClassModel optimizedModel = optimize(resolver, classModel);
                            
                            // Store optimized bytes
                            byte[] optimizedBytes = ClassFile.of().build(optimizedModel.thisClass().asSymbol(), 
                                cb -> optimizedModel.forEachElement(cb::accept));
                            
                            optimizedEntries.put(entry.getName(), optimizedBytes);
                            
                            // Update progress
                            int current = processedCount.incrementAndGet();
                            System.out.printf("Processed %d/%d classes (%.1f%%)\r", 
                                current, totalClasses, (current * 100.0) / totalClasses);
                            
                        } catch (Exception e) {
                            System.err.println("Error optimizing " + entry.getName() + ": " + e.getMessage());
                            // Store original bytes on error
                            try {
                                optimizedEntries.put(entry.getName(), readEntryBytes(jarFile, entry));
                            } catch (IOException ex) {
                                throw new CompletionException(ex);
                            }
                        }
                    }, executor);
                    
                    futures.add(future);
                }

                // Wait for all optimization tasks to complete
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            }

            System.out.println("\nWriting optimized JAR...");

            // Write the output JAR file
            try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(outputJarPath))) {
                // First write all optimized class files
                for (JarEntry entry : classEntries) {
                    writeJarEntry(jos, entry.getName(), optimizedEntries.get(entry.getName()));
                }

                // Then write all resource files
                byte[] buffer = new byte[BUFFER_SIZE];
                for (JarEntry entry : resourceEntries) {
                    try (InputStream is = jarFile.getInputStream(entry)) {
                        writeJarEntry(jos, entry.getName(), is, buffer);
                    }
                }
            }
        }
        
        System.out.println("Optimized JAR written to: " + outputJarPath);
    }

    private static byte[] readEntryBytes(JarFile jarFile, JarEntry entry) throws IOException {
        try (InputStream is = jarFile.getInputStream(entry)) {
            return is.readAllBytes();
        }
    }

    private static synchronized void writeJarEntry(JarOutputStream jos, String name, byte[] content) throws IOException {
        jos.putNextEntry(new JarEntry(name));
        jos.write(content);
        jos.closeEntry();
    }

    private static synchronized void writeJarEntry(JarOutputStream jos, String name, InputStream is, byte[] buffer) throws IOException {
        jos.putNextEntry(new JarEntry(name));
        int count;
        while ((count = is.read(buffer)) != -1) {
            jos.write(buffer, 0, count);
        }
        jos.closeEntry();
    }

    private static ClassModel optimize(ClassHierarchyResolver resolver, ClassModel classModel) {
        byte[] bytes = ClassFile.of(ClassFile.ClassHierarchyResolverOption.of(resolver))
            .transform(classModel,
                ClassTransform.transformingMethods((methodBuilder, classElement) -> {
                    if (classElement instanceof CodeAttribute codeModel) {
                        methodBuilder.withCode(codeBuilder -> optimize2(codeModel, codeBuilder));
                    } else {
                        methodBuilder.accept(classElement);
                    }
                }));
                
        return ClassFile.of().parse(bytes);
    }

    // Your existing optimize2 method here
}