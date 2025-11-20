package com.cloudsimulation.metrics;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Aggregates per-cycle algorithm metrics for entire simulation.
 * Supports multi-algorithm comparison by tracking metrics per algorithm name.
 * Thread-safe for concurrent algorithm evaluation.
 */
public class MetricsAggregator {
    private final Map<String, List<AlgorithmMetrics>> metricsPerAlgorithm;

    /**
     * Creates a new MetricsAggregator instance.
     */
    public MetricsAggregator() {
        this.metricsPerAlgorithm = new ConcurrentHashMap<>();
    }

    /**
     * Adds per-cycle metrics to aggregation.
     *
     * @param metrics AlgorithmMetrics from single evaluation cycle
     */
    public void addMetrics(AlgorithmMetrics metrics) {
        metricsPerAlgorithm
            .computeIfAbsent(metrics.getAlgorithmName(), k -> new ArrayList<>())
            .add(metrics);
    }

    /**
     * Gets aggregated metrics for specified algorithm.
     *
     * @param algorithmName Algorithm identifier
     * @return Aggregated AlgorithmMetrics for entire simulation, or null if algorithm not found
     */
    public AlgorithmMetrics getAggregated(String algorithmName) {
        List<AlgorithmMetrics> metricsList = metricsPerAlgorithm.get(algorithmName);
        if (metricsList == null || metricsList.isEmpty()) {
            return null;
        }

        // Aggregate metrics
        int totalMigrations = metricsList.stream()
            .mapToInt(AlgorithmMetrics::getTotalMigrations)
            .sum();

        int totalOverloads = metricsList.stream()
            .mapToInt(AlgorithmMetrics::getOverloadEvents)
            .sum();

        int totalSlaViolations = metricsList.stream()
            .mapToInt(AlgorithmMetrics::getSlaViolations)
            .sum();

        double avgUtilization = metricsList.stream()
            .mapToDouble(AlgorithmMetrics::getAverageUtilization)
            .average()
            .orElse(0.0);

        long avgDecisionTime = (long) metricsList.stream()
            .mapToLong(AlgorithmMetrics::getDecisionTimeMs)
            .average()
            .orElse(0.0);

        // Use last timestamp as simulation end time
        double lastTimestamp = metricsList.get(metricsList.size() - 1).getTimestamp();

        return new AlgorithmMetrics.Builder()
            .algorithmName(algorithmName)
            .timestamp(lastTimestamp)
            .totalMigrations(totalMigrations)
            .overloadEvents(totalOverloads)
            .averageUtilization(avgUtilization)
            .slaViolations(totalSlaViolations)
            .decisionTimeMs(avgDecisionTime)
            .build();
    }

    /**
     * Gets all algorithm names tracked.
     *
     * @return List of algorithm identifiers
     */
    public List<String> getTrackedAlgorithms() {
        return new ArrayList<>(metricsPerAlgorithm.keySet());
    }
}
