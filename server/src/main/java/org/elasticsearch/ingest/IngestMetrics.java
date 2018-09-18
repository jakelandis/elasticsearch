package org.elasticsearch.ingest;

import org.elasticsearch.common.metrics.CounterMetric;
import org.elasticsearch.common.metrics.MeanMetric;

/**
 * Metrics common to ingest node pipeline and processors. Call {@link #createStats()} to convert metrics to stats useful for API's.
 */
class IngestMetrics {

    private final MeanMetric ingestMetric = new MeanMetric();
    private final CounterMetric ingestCurrent = new CounterMetric();
    private final CounterMetric ingestFailed = new CounterMetric();

    void preIngest() {
        ingestCurrent.inc();
    }

    void postIngest(long ingestTimeInMillis) {
        ingestCurrent.dec();
        ingestMetric.inc(ingestTimeInMillis);
    }

    void ingestFailed() {
        ingestFailed.inc();
    }

    IngestStats.Stats createStats() {
        return new IngestStats.Stats(ingestMetric.count(), ingestMetric.sum(), ingestCurrent.count(), ingestFailed.count());
    }
}
