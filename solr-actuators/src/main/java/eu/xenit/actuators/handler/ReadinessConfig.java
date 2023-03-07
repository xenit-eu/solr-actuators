package eu.xenit.actuators.handler;

public class ReadinessConfig {

    public static final String READINESS_MAX_LAG = "READINESS_MAX_LAG";

    public long getMaxLag() {
        return maxLag;
    }

    private final long maxLag;

    public ReadinessConfig() {
        Long tmpMaxLag = getLongConfig(READINESS_MAX_LAG);
        if (tmpMaxLag == null) {
            this.maxLag = 1800000L;
        } else {
            this.maxLag = tmpMaxLag;
        }
    }
    private static Long getLongConfig(String property) {
        String envProp = System.getenv().get(property);
        if (envProp == null || envProp.isEmpty()) return null;
        return Long.parseLong(envProp);
    }
}