package eu.jameshamilton.optimizer;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

public class OptimizationStats {
    // Pass-specific optimization metrics
    private final Map<Integer, Map<String, LongAdder>> applicationCountByPass = new ConcurrentHashMap<>();
    private final Map<Integer, Map<String, LongAdder>> passSuccesses = new ConcurrentHashMap<>();

    // File-level metrics
    private final Map<Integer, LongAdder> filesProcessedByPass = new ConcurrentHashMap<>();
    private final Map<Integer, LongAdder> parseSuccessesByPass = new ConcurrentHashMap<>();
    private final Map<String, ConcurrentHashMap<String, LongAdder>> fileErrorsByType = new ConcurrentHashMap<>();

    // Class optimization metrics
    private final Map<Integer, LongAdder> optimizedClassCount = new ConcurrentHashMap<>();
    private final Map<Integer, LongAdder> totalBytesReduced = new ConcurrentHashMap<>();

    // Thread-local storage for tentative successes
    private final ThreadLocal<Map<String, Integer>> tentativeSuccesses = ThreadLocal.withInitial(HashMap::new);

    private int currentPass = 1;

    public void setPass(int pass) {
        System.out.println("Optimizing pass " + pass + "...");
        this.currentPass = pass;
    }

    private Map<String, LongAdder> getPassMap(Map<Integer, Map<String, LongAdder>> passMaps) {
        return passMaps.computeIfAbsent(currentPass, _ -> new ConcurrentHashMap<>());
    }

    public void recordFileProcessingStart(String fileName) {
        filesProcessedByPass.computeIfAbsent(currentPass, _ -> new LongAdder()).increment();
        // Clear any tentative successes at the start of processing a file
        tentativeSuccesses.get().clear();
    }

    public void recordParseSuccess(String fileName) {
        parseSuccessesByPass.computeIfAbsent(currentPass, _ -> new LongAdder()).increment();
    }

    public void recordParseError(String fileName, Exception e) {
        String errorType = e.getClass().getSimpleName();
        fileErrorsByType.computeIfAbsent(fileName, _ -> new ConcurrentHashMap<>())
            .computeIfAbsent(errorType, _ -> new LongAdder())
            .increment();
        // Clear tentative successes on error
        tentativeSuccesses.get().clear();
    }

    public void recordClassOptimized(int bytesReduced) {
        optimizedClassCount.computeIfAbsent(currentPass, _ -> new LongAdder()).increment();
        totalBytesReduced.computeIfAbsent(currentPass, _ -> new LongAdder()).add(bytesReduced);

        // Commit all tentative successes
        Map<String, Integer> successes = tentativeSuccesses.get();
        successes.forEach((name, count) -> {
            getPassMap(passSuccesses)
                .computeIfAbsent(name, _ -> new LongAdder())
                .add(count);
        });
        successes.clear();
    }

    public void recordAttempt(String optimizationName) {
        getPassMap(applicationCountByPass)
            .computeIfAbsent(optimizationName, _ -> new LongAdder())
            .increment();
    }

    public void recordTentativeSuccess(String optimizationName) {
        // Store success temporarily until the class optimization completes
        tentativeSuccesses.get().merge(optimizationName, 1, Integer::sum);
    }

    public void printSummary() {
        applicationCountByPass.forEach((pass, attemptMap) -> {
            System.out.printf("\nPass %d Optimizations:\n", pass);
            System.out.println("-------------------");

            attemptMap.forEach((name, attempts) -> {
                long removedInstructions = passSuccesses
                    .getOrDefault(pass, Map.of())
                    .getOrDefault(name, new LongAdder())
                    .sum();

                System.out.printf("%-30s: %d%n",
                    name.substring(0, Math.min(30, name.length())),
                    removedInstructions);
            });

            // Print total bytes reduced for this pass
            long bytesReduced = totalBytesReduced.getOrDefault(pass, new LongAdder()).sum();
            long optimizedClasses = optimizedClassCount.getOrDefault(pass, new LongAdder()).sum();
            if (optimizedClasses > 0) {
                System.out.printf("\nOptimized %d classes, reduced %d bytes (avg %.2f bytes/class)%n",
                    optimizedClasses, bytesReduced, (double) bytesReduced / optimizedClasses);
            }
        });
    }
}