package eu.xenit.actuators.handler;

public class ReadinessConfig {

    public static final String READINESS_MAX_LAG = "READINESS_MAX_LAG";
    public static final String READINESS_TX_LAG_VALIDATION_ENABLED = "READINESS_TX_LAG_VALIDATION_ENABLED";
    public static final String READINESS_CHANGE_SET_LAG_VALIDATION_ENABLED = "READINESS_CHANGE_SET_LAG_VALIDATION_ENABLED";
    public static final String READINESS_REPLICATION_VALIDATION_ENABLED = "READINESS_REPLICATION_VALIDATION_ENABLED";

    public long getMaxLag() {
        return maxLag;
    }

    private final long maxLag;
    private final boolean txValidationEnabled;

    public boolean isTxValidationEnabled() {
        return txValidationEnabled;
    }

    public boolean isChangeSetValidationEnabled() {
        return changeSetValidationEnabled;
    }

    public boolean isReplicationValidationEnabled() {
        return replicationValidationEnabled;
    }

    private final boolean changeSetValidationEnabled;
    private final boolean replicationValidationEnabled;

    public ReadinessConfig() {
        maxLag = getLongConfig(READINESS_MAX_LAG, 1800000L);
        txValidationEnabled = getBooleanConfig(READINESS_TX_LAG_VALIDATION_ENABLED, true);
        changeSetValidationEnabled = getBooleanConfig(READINESS_CHANGE_SET_LAG_VALIDATION_ENABLED, true);
        replicationValidationEnabled = getBooleanConfig(READINESS_REPLICATION_VALIDATION_ENABLED, true);
    }

    private static long getLongConfig(String property, long defaultValue) {
        String envProp = System.getenv().get(property);
        if (envProp == null || envProp.isEmpty()) return defaultValue;
        return Long.parseLong(envProp);
    }

    private static boolean getBooleanConfig(String property, boolean defaultValue) {
        String envProp = System.getenv().get(property);
        if (envProp == null || envProp.isEmpty()) return defaultValue;
        return Boolean.TRUE.equals(Boolean.parseBoolean(envProp));
    }
}