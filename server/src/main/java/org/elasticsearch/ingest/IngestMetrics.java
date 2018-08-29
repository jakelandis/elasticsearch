package org.elasticsearch.ingest;

import org.elasticsearch.common.metrics.CounterMetric;
import org.elasticsearch.common.metrics.MeanMetric;

/**
 * //TODO: something about not need to care if using abstractprocessor
 */
public class IngestMetrics {

    private final MeanMetric ingestMetric = new MeanMetric();
    private final CounterMetric ingestCurrent = new CounterMetric();
    private final CounterMetric ingestFailed = new CounterMetric();

    public void preIngest() {
        ingestCurrent.inc();
    }

    public void postIngest(long ingestTimeInMillis) {
        ingestCurrent.dec();
        ingestMetric.inc(ingestTimeInMillis);
    }

    public void ingestFailed() {
        ingestFailed.inc();
    }

    public IngestStats.Stats createStats() {
        return new IngestStats.Stats(ingestMetric.count(), ingestMetric.sum(), ingestCurrent.count(), ingestFailed.count());
    }
}
