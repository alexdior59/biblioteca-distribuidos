package edu.uni.common;

public final class AppConfig {
    // Endpoints por defecto
    public static final int GC_REQREP_PORT = 5555;   // PS <-> GC (sync)
    public static final int GC_PUBSUB_PORT = 5560;   // GC -> Actores (pub/sub)
    public static final int GA_REQREP_PORT = 5570;   // Actores <-> GA (sync)

    public static String gcReqRepEndpoint(String host) {
        return "tcp://" + host + ":" + GC_REQREP_PORT;
    }
    public static String gcPubEndpoint(String host) {
        return "tcp://" + host + ":" + GC_PUBSUB_PORT;
    }
    public static String gaEndpoint(String host) {
        return "tcp://" + host + ":" + GA_REQREP_PORT;
    }

    private AppConfig() {}
}
