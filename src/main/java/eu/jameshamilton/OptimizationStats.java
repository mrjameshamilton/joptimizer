package eu.jameshamilton;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

public class OptimizationStats {
    private final Map<String, LongAdder> applicationCount = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> successCount = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> instructionsRemoved = new ConcurrentHashMap<>();
    
    public void recordAttempt(String optimizationName) {
        applicationCount.computeIfAbsent(optimizationName, _ -> new LongAdder()).increment();
    }
    
    public void recordSuccess(String optimizationName, int instructionsRemovedCount) {
        successCount.computeIfAbsent(optimizationName, _ -> new LongAdder()).increment();
        instructionsRemoved.computeIfAbsent(optimizationName, _ -> new LongAdder()).add(instructionsRemovedCount);
    }
    
    public Map<String, OptimizationMetrics> getMetrics() {
        Map<String, OptimizationMetrics> metrics = new ConcurrentHashMap<>();
        
        applicationCount.forEach((name, attempts) -> {
            long successfulAttempts = successCount.getOrDefault(name, new LongAdder()).sum();
            long removedInstructions = instructionsRemoved.getOrDefault(name, new LongAdder()).sum();
            
            metrics.put(name, new OptimizationMetrics(
                attempts.sum(),
                successfulAttempts,
                removedInstructions
            ));
        });
        
        return metrics;
    }
    
    public void printSummary() {
        System.out.println("\nOptimization Statistics Summary:");
        System.out.println("================================");
        
        getMetrics().forEach((name, metrics) -> {
            double successRate = metrics.attempts() > 0 
                ? (metrics.successes() * 100.0) / metrics.attempts()
                : 0.0;
                
            System.out.printf("%-30s:\n", name);
            System.out.printf("  Attempts: %-10d\n", metrics.attempts());
            System.out.printf("  Successes: %-10d (%.2f%%)\n", metrics.successes(), successRate);
            System.out.printf("  Instructions Removed: %-10d\n", metrics.instructionsRemoved());
            System.out.println();
        });
    }
    
    public record OptimizationMetrics(
        long attempts,
        long successes,
        long instructionsRemoved
    ) {}
}