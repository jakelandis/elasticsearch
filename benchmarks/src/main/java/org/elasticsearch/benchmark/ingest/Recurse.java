/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.benchmark.ingest;

import org.elasticsearch.ingest.CompoundProcessor;
import org.elasticsearch.ingest.IngestDocument;
import org.elasticsearch.ingest.Processor;
import org.elasticsearch.ingest.TestTemplateService;
import org.elasticsearch.ingest.ValueSource;
import org.elasticsearch.ingest.common.SetProcessor;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

/**
 * Ingest node processor execution is overly recursive. This test compares the differences for pipelines with
 * multiple processors. Each processor is doing a trivial amount of work. For example, one vs. two should not be 2* slower
 * it should be within the margin of error.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
public class Recurse {


    @State(Scope.Benchmark)
    public static class Processors {

        public CompoundProcessor one;
        public CompoundProcessor two;
        public CompoundProcessor three;
        public CompoundProcessor ten;
        public CompoundProcessor fifty;
        public CompoundProcessor hundred;
        public CompoundProcessor fiveHundred;
        public CompoundProcessor oneThousand;

        @Setup(Level.Trial)
        public void setup() {
            one = getRoot(1);
            two = getRoot(2);
            three = getRoot(3);
            ten = getRoot(10);
            fifty = getRoot(50);
            hundred = getRoot(100);
            fiveHundred = getRoot(500);
            oneThousand = getRoot(1000);
        }

        private CompoundProcessor getRoot(int processorCount) {
            Processor[] processors = new Processor[processorCount+1];
            for (int i = 0; i <= processorCount; i++) {
                processors[i] = createProcessor(i);
            }
            return new CompoundProcessor(processors);
        }

        private SetProcessor createProcessor(int value) {
            return new SetProcessor("mytag", "mydescription", new TestTemplateService.MockTemplateScript.Factory("size"),
                ValueSource.wrap(value, TestTemplateService.instance()),
                null,
                true,
                true
            );
        }
    }

    @State(Scope.Benchmark)
    public static class IngestDocuments {
        public IngestDocument empty;

        @Setup(Level.Trial)
        public void setup() {
            empty = new IngestDocument(new HashMap<>(), new HashMap<>());
        }
    }

    @Benchmark
    public void one(Processors processors, IngestDocuments ingestDocuments){
        processors.one.execute(ingestDocuments.empty, (r, e) -> {});
    }
    @Benchmark
    public void two(Processors processors, IngestDocuments ingestDocuments){
        processors.two.execute(ingestDocuments.empty, (r, e) -> {});
    }
    @Benchmark
    public void three(Processors processors, IngestDocuments ingestDocuments){
        processors.three.execute(ingestDocuments.empty, (r, e) -> {});
    }
    @Benchmark
    public void ten(Processors processors, IngestDocuments ingestDocuments){
        processors.ten.execute(ingestDocuments.empty, (r, e) -> {});
    }
    @Benchmark
    public void fiftyProcessors(Processors processors, IngestDocuments ingestDocuments){
        processors.fifty.execute(ingestDocuments.empty, (r, e) -> {});
    }
    @Benchmark
    public void oneHundred(Processors processors, IngestDocuments ingestDocuments){
         processors.hundred.execute(ingestDocuments.empty, (r, e) -> {});
    }
    @Benchmark
    public void fiveHundred(Processors processors, IngestDocuments ingestDocuments){
        processors.fiveHundred.execute(ingestDocuments.empty, (r, e) -> {});
    }
    @Benchmark
    public void oneThousand(Processors processors, IngestDocuments ingestDocuments){
        processors.oneThousand.execute(ingestDocuments.empty, (r, e) -> {});
    }
}
