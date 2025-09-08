package pl.amitec.mercury.integrators.polsoft;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.amitec.mercury.JobContext;
import pl.amitec.mercury.PlanExecution;
import pl.amitec.mercury.SyncStats;
import pl.amitec.mercury.clients.bitbee.BitbeeClient;
import pl.amitec.mercury.formats.Charsets;
import pl.amitec.mercury.persistence.Cache;
import pl.amitec.mercury.persistence.HashCache;
import pl.amitec.mercury.transport.FilesystemTransport;

import java.time.Duration;
import java.util.Map;

public class PsFlow {

    private static final Logger LOG = LoggerFactory.getLogger(PsFlow.class);

    private Map<String, String> config;
    private final BitbeeClient bitbeeClient;
    private final Cache cache;

    public static PsFlow configure(Map<String, String> config) {
        BitbeeClient bitbeeClient = new BitbeeClient(config);
        Cache cache = new HashCache("data/mercury-hash-cache.db");
        return new PsFlow(config, bitbeeClient, cache);
    }

    public PsFlow(Map<String, String> config, BitbeeClient bitbeeClient, Cache cache) {
        this.config = config;
        this.bitbeeClient = bitbeeClient;
        this.cache = cache;
    }

    public void watch(PlanExecution planExecution) {
        Config staticConfig = planExecution.loadConfig(Config.class);

        var source = PolsoftFtp.configure(config);

        if(Boolean.parseBoolean(config.getOrDefault("polsoft.orders.enabled", "true"))) {
            planExecution.getTaskExecutor().execute("orders", () -> {
                while(true) {
                    processOrders();
                    try {
                        source.syncDirToRemote("data/IMPORT_ODDZ_1", "data/IMPORT_ODDZ_1-sent", "/IMPORT_ODDZ_1");
                    } catch (Exception e) {
                        LOG.error(
                                String.format("Failed to sync dir to remote for %s", source), e);
                    }
                    try {
                        Thread.sleep(Duration.ofMinutes(1));
                    } catch (InterruptedException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            });
        } else {
                LOG.info("Skipping orders processing (polsoft.orders.enabled=false)");
        }

        planExecution.getTaskExecutor().execute("pull", () -> {
            while(true) {
                try {
                    source.waitForContentPull("/EKSPORT_ODDZ_1").ifPresent((state) -> {
                        sync(state);
                        source.failureCleanup(state);
                    });
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
        });
    }

    private void processOrders() {
        JobContext ctx = new JobContext(cache,
                bitbeeClient, config, new SyncStats());
        var writeTransport = FilesystemTransport.configure(config, false, Charsets.ISO_8859_2);
        try {
            bitbeeClient.session(() -> {
                new OrderSync().sync(ctx, writeTransport, config.get("polsoft.department"));
            });
        } catch (Exception e) {
            LOG.error("Failed to sync orders", e);
        }
    }

    private void sync(ContentState state) {
        try {
            bitbeeClient.session(() -> {
                JobContext ctx = new JobContext(cache, bitbeeClient, config, new SyncStats());
                new VariantSync().sync(ctx, state.getTransport(), "1",null);
                new ClientSync().sync(ctx, state.getTransport(), "1", null);
                if(Boolean.parseBoolean(config.getOrDefault("polsoft.invoices.enabled", "true"))) {
                    new InvoiceSync().sync(ctx, state.getTransport(), "1", null); //TODO dept
                } else {
                    LOG.info("Skipping invoices processing (polsoft.invoices.enabled=false)");
                }
            });
        } catch (Exception e) {
            LOG.error(
                    String.format("Failed to sync variants/clients, state: %s", state), e);
        }
    }

    /**
     * Checks FTP connection and credentials, throws exception if not working
     * Check Bitbee connection and credentials, throws exception if not working
     */
    public void test() {
        var sourceFtp = PolsoftFtp.configure(config);
        boolean pass = true;
        try {
            sourceFtp.withConnected((ftp) -> {
                LOG.info("Test - FTP connection OK");
            });
        } catch (Exception e) {
            LOG.error("Test - FTP connection failed", e);
            pass = false;
        }
        try {
            LOG.info("Test - Bitbee connection OK, shop info: {}", bitbeeClient.getShopInfo());
        } catch (Exception e) {
            LOG.error("Test - Bitbee connection failed", e);
            pass = false;
        }
    }
}
