package eu.jameshamilton;

public class NOPChecker {
    public static void analyzeJar(String jarPath) throws IOException {
        try (var jarFile = new JarFile(jarPath)) {
            Map<String, AtomicInteger> nopLocations = new ConcurrentHashMap<>();
            
            Collections.list(jarFile.entries()).parallelStream()
                .filter(e -> e.getName().endsWith(".class"))
                .forEach(entry -> {
                    try (InputStream is = jarFile.getInputStream(entry)) {
                        // Parse but don't transform
                        ClassModel classModel = ClassFile.of().parse(is.readAllBytes());
                        
                        // Check each method
                        classModel.methods().forEach(method -> {
                            if (method.code().isPresent()) {
                                var code = method.code().get();
                                int nopCount = 0;
                                
                                for (CodeElement elem : code.elementList()) {
                                    if (elem instanceof Instruction instr && 
                                        instr.opcode() == Opcode.NOP) {
                                        nopCount++;
                                    }
                                }
                                
                                if (nopCount > 0) {
                                    String key = String.format("%s#%s%s", 
                                        classModel.thisClass().asSymbol().displayName(),
                                        method.methodName().stringValue(),
                                        method.methodTypeSymbol());
                                    nopLocations.computeIfAbsent(key, 
                                        k -> new AtomicInteger()).addAndGet(nopCount);
                                }
                            }
                        });
                    } catch (IOException e) {
                        System.err.println("Error processing " + entry.getName() + ": " + e);
                    }
                });
                
            // Print results
            System.out.println("\nMethods containing NOPs in original bytecode:");
            System.out.println("===========================================");
            nopLocations.entrySet().stream()
                .sorted(Map.Entry.<String, AtomicInteger>comparingByValue().reversed())
                .limit(20)  // Show top 20 methods with most NOPs
                .forEach(e -> System.out.printf("%5d NOPs in %s%n", 
                    e.getValue().get(), e.getKey()));
                    
            int totalNops = nopLocations.values().stream()
                .mapToInt(AtomicInteger::get)
                .sum();
            System.out.printf("%nTotal NOPs found: %d%n", totalNops);
        }
    }
}