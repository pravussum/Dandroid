package net.mortalsilence.dandroid.comm.discovery;

public enum DiscoveryCache {

    DISCOVERY_CACHE_INSTANCE;
    private String host;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }
}
