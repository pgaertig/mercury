package pl.amitec.mercury.providers.polsoft;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.amitec.mercury.JobContext;
import pl.amitec.mercury.formats.Charsets;
import pl.amitec.mercury.persistence.Cache;
import pl.amitec.mercury.persistence.HashCache;
import pl.amitec.mercury.providers.redbay.RedbayClient;
import pl.amitec.mercury.transport.FilesystemTransport;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

public class PsFlow {

    private static final Logger LOG = LoggerFactory.getLogger(PsFlow.class);

    private Map<String, String> config;
    private final RedbayClient redbayClient;
    private final Cache cache;

    public static PsFlow configure(Map<String, String> config) {
        RedbayClient redbayClient = new RedbayClient(config);
        Cache cache = new HashCache("data/mercury-hash-cache.db");
        return new PsFlow(config, redbayClient, cache);
    }

    public PsFlow(Map<String, String> config, RedbayClient redbayClient, Cache cache) {
        this.config = config;
        this.redbayClient = redbayClient;
        this.cache = cache;
    }

    public void watch() {
        var source = PolsoftFtp.configure(config);
        while(true) {
            processOrders();

            try {
                source.syncDirToRemote("data/IMPORT_ODDZ_1", "data/IMPORT_ODDZ_1-sent", "/IMPORT_ODDZ_1");
            } catch (Exception e) {
                LOG.error(
                        String.format("Failed to sync dir to remote for %s", source), e);
            }

            try {
                PolsoftFtp.State state = source.waitForContentPull("/EKSPORT_ODDZ_1");
                if(state != null) {
                    sync(state);
                    source.failureCleanup(state);
                }
                try {
                    Thread.sleep(Duration.ofSeconds(60));
                } catch (InterruptedException e) {
                    LOG.info("Thread interrupted");
                    return;
                }
            } catch (Exception e) {
                LOG.error(
                        String.format("Failed to pull complete content from source %s. Waiting 5 minutes", source), e);
                try {
                    Thread.sleep(Duration.ofMinutes(5));
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
            }

        }
    }

    private void processOrders() {
        JobContext ctx = new JobContext(cache,
                redbayClient, config, new SyncStats());
        var writeTransport = FilesystemTransport.configure(config, false, Charsets.ISO_8859_2);
        try {
            redbayClient.session(() -> {
                new OrderSync().sync(ctx, writeTransport, config.get("polsoft.department"));
            });
        } catch (Exception e) {
            LOG.error("Failed to sync orders", e);
        }
    }

    private void sync(PolsoftFtp.State state) {
        try {
            redbayClient.session(() -> {
                JobContext ctx = new JobContext(cache, redbayClient, config, new SyncStats());
                new VariantSync().sync(ctx, state.transport, "1",null);
                new ClientSync().sync(ctx, state.transport, "1", null);
            });
        } catch (Exception e) {
            LOG.error(
                    String.format("Failed to sync variants/clients, state: %s", state), e);
        }
    }
}
