package com.cloudsimulation.models;

/**
 * Defines complete simulation configuration including infrastructure topology and simulation parameters.
 * Used by YAMLConfigLoader to parse YAML configuration files.
 */
public class ScenarioConfig {
    private String scenarioId;
    private Integer duration;
    private Long seed;
    private Double tickInterval;
    private InfrastructureConfig infrastructureConfig;
    private PolicyConfig policyConfig;
    private WorkloadConfig workloadConfig;

    /**
     * Nested class for infrastructure topology configuration.
     */
    public static class InfrastructureConfig {
        private Integer federationCount;
        private Integer datacentersPerFederation;
        private Integer hostsPerDatacenter;
        private Integer vmsPerHost;

        public InfrastructureConfig() {
        }

        public Integer getFederationCount() {
            return federationCount;
        }

        public void setFederationCount(Integer federationCount) {
            this.federationCount = federationCount;
        }

        public Integer getDatacentersPerFederation() {
            return datacentersPerFederation;
        }

        public void setDatacentersPerFederation(Integer datacentersPerFederation) {
            this.datacentersPerFederation = datacentersPerFederation;
        }

        public Integer getHostsPerDatacenter() {
            return hostsPerDatacenter;
        }

        public void setHostsPerDatacenter(Integer hostsPerDatacenter) {
            this.hostsPerDatacenter = hostsPerDatacenter;
        }

        public Integer getVmsPerHost() {
            return vmsPerHost;
        }

        public void setVmsPerHost(Integer vmsPerHost) {
            this.vmsPerHost = vmsPerHost;
        }
    }

    public ScenarioConfig() {
    }

    public String getScenarioId() {
        return scenarioId;
    }

    public void setScenarioId(String scenarioId) {
        this.scenarioId = scenarioId;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public Long getSeed() {
        return seed;
    }

    public void setSeed(Long seed) {
        this.seed = seed;
    }

    public Double getTickInterval() {
        return tickInterval;
    }

    public void setTickInterval(Double tickInterval) {
        this.tickInterval = tickInterval;
    }

    public InfrastructureConfig getInfrastructureConfig() {
        return infrastructureConfig;
    }

    public void setInfrastructureConfig(InfrastructureConfig infrastructureConfig) {
        this.infrastructureConfig = infrastructureConfig;
    }

    /**
     * Nested class for policy configuration.
     */
    public static class PolicyConfig {
        private String defaultPolicy;
        private Boolean enableMetrics;

        public PolicyConfig() {
        }

        public String getDefaultPolicy() {
            return defaultPolicy;
        }

        public void setDefaultPolicy(String defaultPolicy) {
            this.defaultPolicy = defaultPolicy;
        }

        public Boolean getEnableMetrics() {
            return enableMetrics;
        }

        public void setEnableMetrics(Boolean enableMetrics) {
            this.enableMetrics = enableMetrics;
        }
    }

    public PolicyConfig getPolicyConfig() {
        return policyConfig;
    }

    public void setPolicyConfig(PolicyConfig policyConfig) {
        this.policyConfig = policyConfig;
    }

    public WorkloadConfig getWorkloadConfig() {
        return workloadConfig;
    }

    public void setWorkloadConfig(WorkloadConfig workloadConfig) {
        this.workloadConfig = workloadConfig;
    }
}
