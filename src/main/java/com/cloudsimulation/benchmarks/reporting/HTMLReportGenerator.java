package com.cloudsimulation.benchmarks.reporting;

import com.cloudsimulation.benchmarks.core.BenchmarkResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Generates HTML performance reports with interactive Chart.js visualizations.
 * Creates standalone HTML files with embedded data and charts.
 */
public class HTMLReportGenerator {
    private static final Logger logger = LoggerFactory.getLogger(HTMLReportGenerator.class);

    private static final String DEFAULT_OUTPUT_DIR = "target/benchmarks/reports";
    private static final String REPORT_FILENAME_PATTERN = "benchmark-report-%s.html";

    private final String outputDirectory;

    public HTMLReportGenerator() {
        this(DEFAULT_OUTPUT_DIR);
    }

    public HTMLReportGenerator(String outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    /**
     * Generate HTML report from benchmark results
     */
    public Path generateReport(List<BenchmarkResult> results) throws IOException {
        return generateReport(results, null);
    }

    /**
     * Generate HTML report with baseline comparison
     */
    public Path generateReport(List<BenchmarkResult> results, List<String> regressions) throws IOException {
        if (results == null || results.isEmpty()) {
            throw new IllegalArgumentException("No results to generate report from");
        }

        // Create output directory
        Path outputDir = Paths.get(outputDirectory);
        Files.createDirectories(outputDir);

        // Generate filename with timestamp
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String filename = String.format(REPORT_FILENAME_PATTERN, timestamp);
        Path reportPath = outputDir.resolve(filename);

        logger.info("Generating HTML report: {}", reportPath);

        // Generate HTML content
        StringBuilder html = new StringBuilder();
        generateHTMLHeader(html);
        generateExecutiveSummary(html, results, regressions);
        generateChartsSection(html, results);
        generateDetailedResults(html, results);
        generateEnvironmentInfo(html);
        generateHTMLFooter(html);

        // Write to file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(reportPath.toFile()))) {
            writer.write(html.toString());
        }

        logger.info("HTML report generated successfully");
        return reportPath;
    }

    private void generateHTMLHeader(StringBuilder html) {
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"en\">\n");
        html.append("<head>\n");
        html.append("    <meta charset=\"UTF-8\">\n");
        html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("    <title>CloudSim Performance Benchmark Report</title>\n");
        html.append("    <script src=\"https://cdn.jsdelivr.net/npm/chart.js@4.4.0/dist/chart.umd.min.js\"></script>\n");
        html.append("    <style>\n");
        html.append("        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 20px; background: #f5f5f5; }\n");
        html.append("        .container { max-width: 1400px; margin: 0 auto; background: white; padding: 30px; box-shadow: 0 0 10px rgba(0,0,0,0.1); }\n");
        html.append("        h1 { color: #2c3e50; border-bottom: 3px solid #3498db; padding-bottom: 10px; }\n");
        html.append("        h2 { color: #34495e; margin-top: 30px; border-bottom: 1px solid #ecf0f1; padding-bottom: 8px; }\n");
        html.append("        .summary { background: #ecf0f1; padding: 20px; border-radius: 5px; margin: 20px 0; }\n");
        html.append("        .summary-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 15px; }\n");
        html.append("        .summary-item { background: white; padding: 15px; border-radius: 5px; border-left: 4px solid #3498db; }\n");
        html.append("        .summary-item.passed { border-left-color: #27ae60; }\n");
        html.append("        .summary-item.failed { border-left-color: #e74c3c; }\n");
        html.append("        .summary-label { font-size: 0.85em; color: #7f8c8d; text-transform: uppercase; }\n");
        html.append("        .summary-value { font-size: 1.5em; font-weight: bold; color: #2c3e50; margin-top: 5px; }\n");
        html.append("        .charts-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(500px, 1fr)); gap: 20px; margin: 20px 0; }\n");
        html.append("        .chart-container { background: white; padding: 20px; border-radius: 5px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }\n");
        html.append("        table { width: 100%; border-collapse: collapse; margin: 20px 0; }\n");
        html.append("        th, td { padding: 12px; text-align: left; border-bottom: 1px solid #ecf0f1; }\n");
        html.append("        th { background: #34495e; color: white; font-weight: 600; }\n");
        html.append("        tr:hover { background: #f8f9fa; }\n");
        html.append("        .pass { color: #27ae60; font-weight: bold; }\n");
        html.append("        .fail { color: #e74c3c; font-weight: bold; }\n");
        html.append("        .regression { background: #ffe6e6; }\n");
        html.append("        .regression-list { background: #fff3cd; border-left: 4px solid #ffc107; padding: 15px; margin: 15px 0; }\n");
        html.append("        .env-info { background: #e8f4f8; padding: 15px; border-radius: 5px; margin: 20px 0; font-family: monospace; font-size: 0.9em; }\n");
        html.append("    </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append("<div class=\"container\">\n");
        html.append("    <h1>CloudSim Performance Benchmark Report</h1>\n");
        html.append("    <p>Generated: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("</p>\n");
    }

    private void generateExecutiveSummary(StringBuilder html, List<BenchmarkResult> results, List<String> regressions) {
        long passedCount = results.stream().filter(BenchmarkResult::isPassed).count();
        long failedCount = results.size() - passedCount;

        html.append("    <h2>Executive Summary</h2>\n");
        html.append("    <div class=\"summary\">\n");
        html.append("        <div class=\"summary-grid\">\n");

        html.append("            <div class=\"summary-item\">\n");
        html.append("                <div class=\"summary-label\">Total Benchmarks</div>\n");
        html.append("                <div class=\"summary-value\">").append(results.size()).append("</div>\n");
        html.append("            </div>\n");

        html.append("            <div class=\"summary-item passed\">\n");
        html.append("                <div class=\"summary-label\">Passed</div>\n");
        html.append("                <div class=\"summary-value\">").append(passedCount).append("</div>\n");
        html.append("            </div>\n");

        html.append("            <div class=\"summary-item failed\">\n");
        html.append("                <div class=\"summary-label\">Failed</div>\n");
        html.append("                <div class=\"summary-value\">").append(failedCount).append("</div>\n");
        html.append("            </div>\n");

        if (regressions != null) {
            html.append("            <div class=\"summary-item ").append(regressions.isEmpty() ? "passed" : "failed").append("\">\n");
            html.append("                <div class=\"summary-label\">Regressions</div>\n");
            html.append("                <div class=\"summary-value\">").append(regressions.size()).append("</div>\n");
            html.append("            </div>\n");
        }

        html.append("        </div>\n");
        html.append("    </div>\n");

        // Show regressions if any
        if (regressions != null && !regressions.isEmpty()) {
            html.append("    <div class=\"regression-list\">\n");
            html.append("        <h3>Performance Regressions Detected</h3>\n");
            html.append("        <ul>\n");
            for (String regression : regressions) {
                html.append("            <li>").append(escapeHtml(regression)).append("</li>\n");
            }
            html.append("        </ul>\n");
            html.append("    </div>\n");
        }
    }

    private void generateChartsSection(StringBuilder html, List<BenchmarkResult> results) {
        html.append("    <h2>Performance Visualizations</h2>\n");
        html.append("    <div class=\"charts-grid\">\n");

        // Chart 1: Benchmark Execution Times
        generateExecutionTimeChart(html, results);

        // Chart 2: Memory Usage
        generateMemoryChart(html, results);

        // Chart 3: Throughput Metrics
        generateThroughputChart(html, results);

        // Chart 4: Pass/Fail Status
        generateStatusChart(html, results);

        html.append("    </div>\n");
    }

    private void generateExecutionTimeChart(StringBuilder html, List<BenchmarkResult> results) {
        html.append("        <div class=\"chart-container\">\n");
        html.append("            <h3>Execution Times</h3>\n");
        html.append("            <canvas id=\"executionTimeChart\"></canvas>\n");
        html.append("            <script>\n");
        html.append("                new Chart(document.getElementById('executionTimeChart'), {\n");
        html.append("                    type: 'bar',\n");
        html.append("                    data: {\n");
        html.append("                        labels: [");

        // Labels
        html.append(results.stream()
                .map(r -> "'" + r.getBenchmarkName() + "'")
                .collect(Collectors.joining(", ")));
        html.append("],\n");

        html.append("                        datasets: [{\n");
        html.append("                            label: 'Execution Time (ms)',\n");
        html.append("                            data: [");

        // Data - look for time metrics
        html.append(results.stream()
                .map(r -> String.valueOf(findTimeMetric(r)))
                .collect(Collectors.joining(", ")));
        html.append("],\n");

        html.append("                            backgroundColor: 'rgba(52, 152, 219, 0.6)',\n");
        html.append("                            borderColor: 'rgba(52, 152, 219, 1)',\n");
        html.append("                            borderWidth: 1\n");
        html.append("                        }]\n");
        html.append("                    },\n");
        html.append("                    options: { responsive: true, scales: { y: { beginAtZero: true } } }\n");
        html.append("                });\n");
        html.append("            </script>\n");
        html.append("        </div>\n");
    }

    private void generateMemoryChart(StringBuilder html, List<BenchmarkResult> results) {
        BenchmarkResult memoryBenchmark = results.stream()
                .filter(r -> r.getBenchmarkName().equals("memory"))
                .findFirst()
                .orElse(null);

        if (memoryBenchmark == null) {
            return;
        }

        html.append("        <div class=\"chart-container\">\n");
        html.append("            <h3>Memory Usage Breakdown</h3>\n");
        html.append("            <canvas id=\"memoryChart\"></canvas>\n");
        html.append("            <script>\n");
        html.append("                new Chart(document.getElementById('memoryChart'), {\n");
        html.append("                    type: 'pie',\n");
        html.append("                    data: {\n");
        html.append("                        labels: ['Infrastructure', 'VMs', 'Cloudlets'],\n");
        html.append("                        datasets: [{\n");
        html.append("                            data: [");
        html.append(String.format("%.2f, %.2f, %.2f",
                memoryBenchmark.getMetric("infrastructure_memory_mb"),
                memoryBenchmark.getMetric("vm_memory_mb"),
                memoryBenchmark.getMetric("cloudlet_memory_mb")));
        html.append("],\n");
        html.append("                            backgroundColor: ['rgba(231, 76, 60, 0.6)', 'rgba(46, 204, 113, 0.6)', 'rgba(52, 152, 219, 0.6)']\n");
        html.append("                        }]\n");
        html.append("                    },\n");
        html.append("                    options: { responsive: true }\n");
        html.append("                });\n");
        html.append("            </script>\n");
        html.append("        </div>\n");
    }

    private void generateThroughputChart(StringBuilder html, List<BenchmarkResult> results) {
        BenchmarkResult throughputBenchmark = results.stream()
                .filter(r -> r.getBenchmarkName().equals("throughput"))
                .findFirst()
                .orElse(null);

        if (throughputBenchmark == null) {
            return;
        }

        html.append("        <div class=\"chart-container\">\n");
        html.append("            <h3>Throughput Metrics</h3>\n");
        html.append("            <canvas id=\"throughputChart\"></canvas>\n");
        html.append("            <script>\n");
        html.append("                new Chart(document.getElementById('throughputChart'), {\n");
        html.append("                    type: 'bar',\n");
        html.append("                    data: {\n");
        html.append("                        labels: ['VMs/sec', 'Cloudlets/sec'],\n");
        html.append("                        datasets: [{\n");
        html.append("                            label: 'Operations per Second',\n");
        html.append("                            data: [");
        html.append(String.format("%.2f, %.2f",
                throughputBenchmark.getMetric("vms_per_second"),
                throughputBenchmark.getMetric("cloudlets_per_second")));
        html.append("],\n");
        html.append("                            backgroundColor: 'rgba(46, 204, 113, 0.6)',\n");
        html.append("                            borderColor: 'rgba(46, 204, 113, 1)',\n");
        html.append("                            borderWidth: 1\n");
        html.append("                        }]\n");
        html.append("                    },\n");
        html.append("                    options: { responsive: true, scales: { y: { beginAtZero: true } } }\n");
        html.append("                });\n");
        html.append("            </script>\n");
        html.append("        </div>\n");
    }

    private void generateStatusChart(StringBuilder html, List<BenchmarkResult> results) {
        long passed = results.stream().filter(BenchmarkResult::isPassed).count();
        long failed = results.size() - passed;

        html.append("        <div class=\"chart-container\">\n");
        html.append("            <h3>Benchmark Status</h3>\n");
        html.append("            <canvas id=\"statusChart\"></canvas>\n");
        html.append("            <script>\n");
        html.append("                new Chart(document.getElementById('statusChart'), {\n");
        html.append("                    type: 'doughnut',\n");
        html.append("                    data: {\n");
        html.append("                        labels: ['Passed', 'Failed'],\n");
        html.append("                        datasets: [{\n");
        html.append("                            data: [").append(passed).append(", ").append(failed).append("],\n");
        html.append("                            backgroundColor: ['rgba(46, 204, 113, 0.6)', 'rgba(231, 76, 60, 0.6)']\n");
        html.append("                        }]\n");
        html.append("                    },\n");
        html.append("                    options: { responsive: true }\n");
        html.append("                });\n");
        html.append("            </script>\n");
        html.append("        </div>\n");
    }

    private void generateDetailedResults(StringBuilder html, List<BenchmarkResult> results) {
        html.append("    <h2>Detailed Results</h2>\n");

        for (BenchmarkResult result : results) {
            html.append("    <h3>").append(escapeHtml(result.getBenchmarkName())).append("</h3>\n");
            html.append("    <table>\n");
            html.append("        <thead>\n");
            html.append("            <tr><th>Metric</th><th>Value</th></tr>\n");
            html.append("        </thead>\n");
            html.append("        <tbody>\n");

            html.append("            <tr><td><strong>Status</strong></td><td class=\"")
                    .append(result.isPassed() ? "pass" : "fail")
                    .append("\">")
                    .append(result.isPassed() ? "PASSED" : "FAILED")
                    .append("</td></tr>\n");

            if (!result.isPassed()) {
                html.append("            <tr class=\"regression\"><td><strong>Failure Reason</strong></td><td>")
                        .append(escapeHtml(result.getFailureReason()))
                        .append("</td></tr>\n");
            }

            for (Map.Entry<String, Double> metric : result.getMetrics().entrySet()) {
                html.append("            <tr><td>").append(escapeHtml(metric.getKey())).append("</td><td>");
                html.append(String.format("%.4f", metric.getValue()));
                html.append("</td></tr>\n");
            }

            html.append("        </tbody>\n");
            html.append("    </table>\n");
        }
    }

    private void generateEnvironmentInfo(StringBuilder html) {
        html.append("    <h2>Environment Information</h2>\n");
        html.append("    <div class=\"env-info\">\n");
        html.append("        Java Version: ").append(System.getProperty("java.version")).append("<br>\n");
        html.append("        Java Vendor: ").append(System.getProperty("java.vendor")).append("<br>\n");
        html.append("        OS: ").append(System.getProperty("os.name")).append(" ").append(System.getProperty("os.version")).append("<br>\n");
        html.append("        Architecture: ").append(System.getProperty("os.arch")).append("<br>\n");
        html.append("        Processors: ").append(Runtime.getRuntime().availableProcessors()).append("<br>\n");
        html.append("        Max Memory: ").append(Runtime.getRuntime().maxMemory() / (1024 * 1024)).append(" MB<br>\n");
        html.append("    </div>\n");
    }

    private void generateHTMLFooter(StringBuilder html) {
        html.append("</div>\n");
        html.append("</body>\n");
        html.append("</html>\n");
    }

    /**
     * Find a representative time metric from benchmark result
     */
    private double findTimeMetric(BenchmarkResult result) {
        // Priority order of time metrics to display
        String[] timeMetrics = {
                "total_time_ms", "initialization_time_ms", "execution_time_ms",
                "simulation_execution_ms", "avg_pause_ms"
        };

        for (String metric : timeMetrics) {
            if (result.getMetrics().containsKey(metric)) {
                return result.getMetric(metric);
            }
        }

        // Default to first metric if no time metric found
        return result.getMetrics().isEmpty() ? 0.0 :
                result.getMetrics().values().iterator().next();
    }

    /**
     * Escape HTML special characters
     */
    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    public Path getOutputPath() {
        return Paths.get(outputDirectory);
    }
}
