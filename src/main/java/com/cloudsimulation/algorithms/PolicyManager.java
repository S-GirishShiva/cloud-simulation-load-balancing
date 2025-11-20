package com.cloudsimulation.algorithms;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages registration and switching of load balancing policies.
 *
 * <p>Thread-safe manager that maintains a registry of available policies
 * and tracks which policy is currently active. Supports dynamic policy
 * switching during simulation runtime.</p>
 *
 * <p><b>Thread Safety:</b></p>
 * <ul>
 *   <li>Uses ConcurrentHashMap for thread-safe policy storage</li>
 *   <li>Uses volatile for active policy reference</li>
 *   <li>All public methods are thread-safe</li>
 * </ul>
 *
 * @see LoadBalancingPolicy
 */
public class PolicyManager {
    private final ConcurrentHashMap<String, LoadBalancingPolicy> policies;
    private volatile String activePolicyName;

    /**
     * Constructs a new PolicyManager with no registered policies.
     */
    public PolicyManager() {
        this.policies = new ConcurrentHashMap<>();
        this.activePolicyName = null;
    }

    /**
     * Registers a new policy in the manager.
     *
     * <p>If a policy with the same name already exists, it will be replaced.</p>
     *
     * @param name Policy identifier (must match policy.getName())
     * @param policy LoadBalancingPolicy implementation
     * @throws IllegalArgumentException if name is null/empty or policy is null
     */
    public void addPolicy(String name, LoadBalancingPolicy policy) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Policy name cannot be null or empty");
        }
        if (policy == null) {
            throw new IllegalArgumentException("Policy cannot be null");
        }
        policies.put(name, policy);
    }

    /**
     * Retrieves a policy by name.
     *
     * @param name Policy identifier
     * @return LoadBalancingPolicy instance, or null if not found
     * @throws IllegalArgumentException if name is null or empty
     */
    public LoadBalancingPolicy getPolicy(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Policy name cannot be null or empty");
        }
        return policies.get(name);
    }

    /**
     * Sets the active policy by name.
     *
     * <p>The policy must already be registered via addPolicy().
     * This operation is atomic and thread-safe.</p>
     *
     * @param name Policy identifier to activate
     * @throws IllegalArgumentException if name is null/empty or policy not found
     */
    public void setActivePolicy(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Policy name cannot be null or empty");
        }
        if (!policies.containsKey(name)) {
            throw new IllegalArgumentException("Policy '" + name + "' not found. Register it first using addPolicy()");
        }
        this.activePolicyName = name;
    }

    /**
     * Gets the currently active policy.
     *
     * @return Active LoadBalancingPolicy instance
     * @throws IllegalStateException if no active policy has been set
     */
    public LoadBalancingPolicy getActivePolicy() {
        if (activePolicyName == null) {
            throw new IllegalStateException("No active policy set. Use setActivePolicy() first");
        }
        LoadBalancingPolicy policy = policies.get(activePolicyName);
        if (policy == null) {
            throw new IllegalStateException("Active policy '" + activePolicyName + "' no longer exists");
        }
        return policy;
    }

    /**
     * Gets the name of the currently active policy.
     *
     * @return Active policy name, or null if no policy is active
     */
    public String getActivePolicyName() {
        return activePolicyName;
    }

    /**
     * Removes a policy from the registry.
     *
     * <p>Cannot remove the currently active policy - must switch to
     * a different policy first.</p>
     *
     * @param name Policy identifier to remove
     * @throws IllegalArgumentException if name is null/empty
     * @throws IllegalStateException if trying to remove the active policy
     */
    public void removePolicy(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Policy name cannot be null or empty");
        }
        if (name.equals(activePolicyName)) {
            throw new IllegalStateException(
                "Cannot remove active policy '" + name + "'. Switch to a different policy first"
            );
        }
        policies.remove(name);
    }

    /**
     * Gets the names of all registered policies.
     *
     * @return Set of policy names (empty set if no policies registered)
     */
    public Set<String> getAllPolicyNames() {
        return policies.keySet();
    }

    /**
     * Gets the number of registered policies.
     *
     * @return Count of policies in registry
     */
    public int getPolicyCount() {
        return policies.size();
    }

    /**
     * Checks if a policy with the given name is registered.
     *
     * @param name Policy identifier
     * @return true if policy exists, false otherwise
     */
    public boolean hasPolicy(String name) {
        return name != null && policies.containsKey(name);
    }

    /**
     * Clears all policies from the registry.
     * Resets the active policy to null.
     */
    public void clear() {
        policies.clear();
        activePolicyName = null;
    }
}
