package pl.amitec.mercury;

import pl.amitec.mercury.persistence.Cache;
import pl.amitec.mercury.persistence.HashCache;
import pl.amitec.mercury.providers.polsoft.SyncStats;
import pl.amitec.mercury.providers.redbay.RedbayClient;

import java.util.Map;
import java.util.Objects;

public record JobContext(
        Cache hashCache,
        RedbayClient redbayClient,
        Map<String, String> config,
        SyncStats syncStats
) {

    private static final String TENANT = "tenant";

    public String getTenant() {
        var tenant = config.get(TENANT);
        Objects.requireNonNull(tenant);
        return tenant;
    }

}
