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
import org.elasticsearch.ingest.common.UppercaseProcessor;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Ingest node processor execution is overly recursive. This test compares the differences for pipelines with
 * multiple processors. Each processor is doing a trivial amount of work. For example, one vs. two should not be 2* slower
 * it should be within the margin of error.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
//@Fork(jvmArgsAppend = "-Xmx2g")
public class Recurse {

    enum Type {SET, UPPER_WITH_THROW, UPPER}

    static Type type = Type.SET; // change this to change the test

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

            one = getRoot(1, type);
            two = getRoot(2, type);
            three = getRoot(3, type);
            ten = getRoot(10, type);
            fifty = getRoot(50, type);
            hundred = getRoot(100, type);
            fiveHundred = getRoot(500, type);
            oneThousand = getRoot(1000, type);
        }

        private CompoundProcessor getRoot(int processorCount, Type type) {
            Processor[] processors = new Processor[processorCount + 1];
            for (int i = 0; i <= processorCount; i++) {
                switch (type) {
                    case SET -> processors[i] = createSetProcessor(i);
                    case UPPER ->  processors[i] = createUpperCaseProcessor();
                    case UPPER_WITH_THROW ->  processors[i] = createUpperCaseProcessor();
                }

            }
            return new CompoundProcessor(processors);
        }

        private SetProcessor createSetProcessor(int value) {
            return new SetProcessor("mytag", "mydescription", new TestTemplateService.MockTemplateScript.Factory("size"),
                ValueSource.wrap(value, TestTemplateService.instance()),
                null,
                true,
                true
            );
        }

        private UppercaseProcessor createUpperCaseProcessor() {
            return new UppercaseProcessor("mytag", "mydescription", "myfield", true, "myfield");
        }

    }

    @State(Scope.Benchmark)
    public static class IngestDocuments {
        // public IngestDocument doc;

        public List<IngestDocument> aWholeBunch; //use unique documents as defined by object reference to avoid any possible object referenced JVM optimizations


        @Setup(Level.Trial)
        public void setup() {

            aWholeBunch = getDocs(1000, type); //should be equal to or above the max number of processors in the tests
        }

        public List<IngestDocument> getDocs(int docCount, Type type) {
            List<IngestDocument> docs = new ArrayList<>(docCount);
            for (int i = 0; i <= docCount; i++) {
                if (Type.UPPER.equals(type)) {
                    HashMap<String, Object> mymap = new HashMap<>();
                    mymap.put("myfield", "foo"); // this will prevent a thrown exception since this matches the processor
                    docs.add(new IngestDocument(mymap, new HashMap<>()));
                } else {
                    docs.add(new IngestDocument(new HashMap<>(), new HashMap<>())); //new instance to ensure no object equality for the test
                }

            }
            return docs;
        }
    }


    @Benchmark
    public void one(Processors processors, IngestDocuments ingestDocuments) {
        for (IngestDocument doc : ingestDocuments.aWholeBunch) {
            processors.one.execute(doc, (r, e) -> {
            });
        }
    }

    @Benchmark
    public void two(Processors processors, IngestDocuments ingestDocuments) {
        for (IngestDocument doc : ingestDocuments.aWholeBunch) {
            processors.two.execute(doc, (r, e) -> {
            });
        }
    }

    @Benchmark
    public void three(Processors processors, IngestDocuments ingestDocuments) {
        for (IngestDocument doc : ingestDocuments.aWholeBunch) {
            processors.three.execute(doc, (r, e) -> {
            });
        }
    }

    @Benchmark
    public void ten(Processors processors, IngestDocuments ingestDocuments) {
        for (IngestDocument doc : ingestDocuments.aWholeBunch) {
            processors.ten.execute(doc, (r, e) -> {
            });
        }
    }

    @Benchmark
    public void fiftyProcessors(Processors processors, IngestDocuments ingestDocuments) {
        for (IngestDocument doc : ingestDocuments.aWholeBunch) {
            processors.fifty.execute(doc, (r, e) -> {
            });
        }
    }

    @Benchmark
    public void oneHundred(Processors processors, IngestDocuments ingestDocuments) {
        for (IngestDocument doc : ingestDocuments.aWholeBunch) {
            processors.hundred.execute(doc, (r, e) -> {
            });
        }
    }

    @Benchmark
    public void fiveHundred(Processors processors, IngestDocuments ingestDocuments) {
        for (IngestDocument doc : ingestDocuments.aWholeBunch) {
            processors.fiveHundred.execute(doc, (r, e) -> {
            });
        }
    }

    @Benchmark
    public void oneThousand(Processors processors, IngestDocuments ingestDocuments) {
        for (IngestDocument doc : ingestDocuments.aWholeBunch) {
            processors.oneThousand.execute(doc, (r, e) -> {
            });
        }
    }
}
