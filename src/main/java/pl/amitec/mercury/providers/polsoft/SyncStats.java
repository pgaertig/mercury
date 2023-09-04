package pl.amitec.mercury.providers.polsoft;

import java.util.concurrent.atomic.AtomicInteger;

public record SyncStats(
        AtomicInteger entries,
        AtomicInteger succeed,
        AtomicInteger cached,
        AtomicInteger failed) {
    public SyncStats() {
        this(
                new AtomicInteger(0),
                new AtomicInteger(0),
                new AtomicInteger(0),
                new AtomicInteger(0));
    }

    public void incEntries() {
        entries.incrementAndGet();
    }

    public void incSucceed() {
        succeed.incrementAndGet();
    }
    public void incCached() {
        cached.incrementAndGet();
    }

    public void incFailed() {
        failed.incrementAndGet();
    }
}
