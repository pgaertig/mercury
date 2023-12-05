package pl.amitec.mercury;

import pl.amitec.mercury.persistence.Cache;
import pl.amitec.mercury.integrators.polsoft.SyncStats;
import pl.amitec.mercury.clients.bitbee.BitbeeClient;

import java.util.Map;
import java.util.Objects;

public record JobContext(
        Cache hashCache,
        BitbeeClient bitbeeClient,
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
