package eu.jameshamilton.optimizer;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

public class OptimizationStats {
    private static final Logger logger = LogManager.getLogger(OptimizationStats.class);

    // Pass-specific optimization metrics
    private final Map<Integer, Map<String, LongAdder>> applicationCountByPass =
            new ConcurrentHashMap<>();
    private final Map<Integer, Map<String, LongAdder>> passSuccesses = new ConcurrentHashMap<>();

    // File-level metrics
    private final Map<Integer, LongAdder> filesProcessedByPass = new ConcurrentHashMap<>();
    private final Map<Integer, LongAdder> parseSuccessesByPass = new ConcurrentHashMap<>();
    private final Map<String, ConcurrentHashMap<String, LongAdder>> fileErrorsByType =
            new ConcurrentHashMap<>();

    // Class optimization metrics
    private final Map<Integer, LongAdder> optimizedClassCount = new ConcurrentHashMap<>();
    private final Map<Integer, LongAdder> totalBytesReduced = new ConcurrentHashMap<>();

    // Thread-local storage for tentative successes
    private final ThreadLocal<Map<String, Integer>> tentativeSuccesses =
            ThreadLocal.withInitial(HashMap::new);

    private int currentPass = 1;

    public void setPass(int pass) {
        this.currentPass = pass;
    }

    public void printPassSummary() {
        long files = filesProcessedByPass.getOrDefault(currentPass, new LongAdder()).sum();
        long ok = parseSuccessesByPass.getOrDefault(currentPass, new LongAdder()).sum();
        long err = Math.max(0, files - ok);
        long classes = optimizedClassCount.getOrDefault(currentPass, new LongAdder()).sum();
        long bytes = totalBytesReduced.getOrDefault(currentPass, new LongAdder()).sum();
        double avg = classes > 0 ? (double) bytes / classes : 0.0;

        logger.info(
                "Pass {}: classes={} ok={} err={} classesOptimized={} bytesReduced={} avgBytesReduced={}",
                currentPass,
                files,
                ok,
                err,
                classes,
                bytes,
                avg);
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

    public void recordError(String fileName) {
        recordError(fileName, null);
    }

    public void recordError(String fileName, @Nullable Exception e) {
        String errorType = e == null ? "Unknown" : e.getClass().getSimpleName();
        fileErrorsByType
                .computeIfAbsent(fileName, _ -> new ConcurrentHashMap<>())
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
        successes.forEach(
                (name, count) -> {
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
        logger.info("Summary");
        applicationCountByPass.forEach(
                (pass, attemptMap) -> {
                    // Build a table as a single string for readability in a simple console layout
                    StringBuilder sb = new StringBuilder();

                    // Compute column widths
                    int nameColWidth =
                            Math.max(
                                    "Optimization".length(),
                                    attemptMap.keySet().stream()
                                            .mapToInt(name -> Math.min(30, name.length()))
                                            .max()
                                            .orElse(0));
                    int countColWidth = Math.max("Removed".length(), 10);

                    String title = String.format("Pass %d Optimizations", pass);
                    String header =
                            String.format(
                                    "%-" + nameColWidth + "s | %" + countColWidth + "s",
                                    "Optimization",
                                    "Removed");
                    String sep = "-".repeat(Math.max(title.length(), header.length()));

                    sb.append(title).append('\n');
                    sb.append(sep).append('\n');
                    sb.append(header).append('\n');
                    sb.append("-".repeat(nameColWidth))
                            .append("-+-")
                            .append("-".repeat(countColWidth))
                            .append('\n');

                    attemptMap.forEach(
                            (name, attempts) -> {
                                long removedInstructions =
                                        passSuccesses
                                                .getOrDefault(pass, Map.of())
                                                .getOrDefault(
                                                        name,
                                                        new java.util.concurrent.atomic.LongAdder())
                                                .sum();

                                String trimmed = name.length() > 30 ? name.substring(0, 30) : name;
                                sb.append(
                                                String.format(
                                                        "%-"
                                                                + nameColWidth
                                                                + "s | %"
                                                                + countColWidth
                                                                + "d",
                                                        trimmed,
                                                        removedInstructions))
                                        .append('\n');
                            });

                    long bytesReduced =
                            totalBytesReduced
                                    .getOrDefault(pass, new java.util.concurrent.atomic.LongAdder())
                                    .sum();
                    long optimizedClasses =
                            optimizedClassCount
                                    .getOrDefault(pass, new java.util.concurrent.atomic.LongAdder())
                                    .sum();
                    if (optimizedClasses > 0) {
                        sb.append(sep).append('\n');
                        sb.append(
                                        String.format(
                                                "Optimized %d classes, reduced %d bytes (avg %.2f bytes/class)",
                                                optimizedClasses,
                                                bytesReduced,
                                                (double) bytesReduced / optimizedClasses))
                                .append('\n');
                    }

                    // Log as a single INFO entry so it appears as a nice block in the console
                    logger.info("\n{}", sb.toString());
                });
    }
}
