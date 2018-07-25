package org.elasticsearch.benchmark.ingest.dissect;

import org.elasticsearch.benchmark.dissect.DissectParser;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 1, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 5, timeUnit = TimeUnit.SECONDS)
@Threads(1)
@Fork(1)
public class DissectBenchmark {


    @Benchmark
    public void aWarmupVLong(final Blackhole bh) {
        bh.consume(
            DissectBenchmark.DissectVLongDelims.DISSECTOR.parse(DissectBenchmark.DissectVLongDelims.SRC)
        );
    }

    @Benchmark
    public void dOneDelim(final Blackhole bh) {
        bh.consume(
            DissectOneDelim.DISSECTOR.parse(DissectOneDelim.SRC)
        );
    }

    @Benchmark
    public void eTwoDelim(final Blackhole bh) {
        bh.consume(
            DissectTwoDelims.DISSECTOR.parse(DissectTwoDelims.SRC)
        );
    }

    @Benchmark
    public void cLongDelims(final Blackhole bh) {
        bh.consume(
            DissectLongDelims.DISSECTOR.parse(DissectLongDelims.SRC)
        );
    }

    @Benchmark
    public void bVLongDelims(final Blackhole bh) {
        bh.consume(
            DissectBenchmark.DissectVLongDelims.DISSECTOR.parse(DissectBenchmark.DissectVLongDelims.SRC)
        );
    }


    @State(Scope.Thread)
    public static class DissectOneDelim {
        public static final String SRC = DissectBenchmark.Source.buildSrc(DissectBenchmark.Source.delims1, 10);
        public static final DissectParser DISSECTOR = new DissectParser(DissectBenchmark.Source.buildMpp(DissectBenchmark.Source.delims1, 10),"");
    }

    @State(Scope.Thread)
    public static class DissectTwoDelims {
        public static final String SRC = DissectBenchmark.Source.buildSrc(DissectBenchmark.Source.delims2, 10);
        public static final DissectParser DISSECTOR = new DissectParser(DissectBenchmark.Source.buildMpp(DissectBenchmark.Source.delims2, 10),"");
    }

    @State(Scope.Thread)
    public static class DissectLongDelims {
        public static final String SRC = DissectBenchmark.Source.buildSrc(DissectBenchmark.Source.delims3, 10);
        public static final DissectParser DISSECTOR = new DissectParser(DissectBenchmark.Source.buildMpp(DissectBenchmark.Source.delims3, 10),"");
    }

    @State(Scope.Thread)
    public static class DissectVLongDelims {
        public static final String SRC = DissectBenchmark.Source.buildSrc(DissectBenchmark.Source.delimsX, 10);
        public static final DissectParser DISSECTOR = new DissectParser(DissectBenchmark.Source.buildMpp(DissectBenchmark.Source.delimsX, 10),"");
    }


    private static class Source {
        private static final String src = "QQQQQwwwww‰‰‰‰‰rrrrrtttttYYYYYuuuuuIIIIIøøøøøpppppåååååsssssdddddfffffgggggHHHHHjjjjjkkkkkLLLLLzzzzz";
        private static final String mpp = "%{a}%{b}%{c}%{d}%{e}%{f}%{g}%{h}%{i}%{j}";

        public static final String[] delims1 = {" ", ".", ",", "/", "?", "|", "!", "$"};
        public static final String[] delims2 = {" *", ". ", ",.", "/,", "?/", "|?", "!|", "$!"};
        public static final String[] delims3 = {" *^", ". *", ",. ", "/,.", "?/,", "|?/", "!|?", "$!|"};
        public static final String[] delimsX = {" *..........^", ". ..........*", ",........... ", "/,...........", "?/..........,", "|?........../", "!|..........?", "$!..........|"};

        public static String buildSrc(final String[] delims, final int count) {
            final int d = delims.length;
            final StringBuilder sb = new StringBuilder();
            int k = 0;
            for(int i = 0; i < count; i++) {
                k = (i * 5) % src.length();
                sb.append(src.substring(k, k + 5));
                sb.append(delims[i % d]);
            }
            sb.append("MMMMM");
            return sb.toString();
        }

        public static String buildMpp(final String[] delims, final int count) {
            final int d = delims.length;
            final StringBuilder sb = new StringBuilder();
            int k = 0;
            for(int i = 0; i < count; i++) {
                k = (i * 4) % mpp.length();
                sb.append(mpp.substring(k, k + 4));
                sb.append(delims[i % d]);
            }
            sb.append("%{k}");
            return sb.toString();
        }
    }



    /*
     * It is better to run the benchmark from command-line instead of IDE.
     *
     * To run, in command-line: $ ./gradlew clean jmh
     */

    public static void main(final String[] args) throws RunnerException {
        final Options options = new OptionsBuilder()
            .include(DissectBenchmark.class.getSimpleName())
            .build();

        new Runner(options).run();
    }
}
