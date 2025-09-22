package pl.amitec.mercury;

import lombok.Builder;
import pl.amitec.mercury.clients.bitbee.BitbeeClient;
import pl.amitec.mercury.persistence.Cache;

import java.util.Map;
import java.util.Objects;

@Builder
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

    public String getSource() {
        var source = config.get("bitbee.source");
        if(source == null) {
            source = config.get("name");
        }
        return source;
    }

}
