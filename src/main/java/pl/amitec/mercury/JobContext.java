package pl.amitec.mercury;

import pl.amitec.mercury.persistence.HashCache;
import pl.amitec.mercury.providers.redbay.RedbayClient;

import java.util.Map;
import java.util.Objects;

public class JobContext {

    private static final String TENANT = "tenant";

    private final RedbayClient redbayClient;

    private final HashCache hashCache;
    private final Map<String, String> config;

    public JobContext(HashCache hashCache, RedbayClient rbc , Map<String, String> config) {
        this.hashCache = hashCache;
        this.redbayClient = rbc;
        this.config = config;
    }

    public HashCache getHashCache() {
        return hashCache;
    }

    public Map<String, String> getConfig() {
        return config;
    }

    public RedbayClient getRedbayClient() {
        return redbayClient;
    }

    public String getTenant() {
        var tenant = config.get(TENANT);
        Objects.requireNonNull(tenant);
        return tenant;
    }

}
