package org.elasticsearch.benchmark.ingest.dissect;

import org.elasticsearch.benchmark.dissect.DissectParser;
import org.elasticsearch.benchmark.grok.Grok;
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

import java.util.Map;
import java.util.concurrent.TimeUnit;


@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 30, timeUnit = TimeUnit.SECONDS)
@Threads(1)
@Fork(2)
public class DissectBenchmark {

    private static final String SYSLOG_LINE = "Mar 16 00:01:25 evita postfix/smtpd[1713]: connect from camomile.cloud9.net[168.100.1.3]";
    private static final String APACHE_LINE = "31.184.238.164 - - [24/Jul/2014:05:35:37 +0530] \"GET /logs/access.log HTTP/1.0\" 200 69849 " +
        "\"http://8rursodiol.enjin.com\" \"Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) " +
        "Chrome/30.0.1599.12785 YaBrowser/13.12.1599.12785 Safari/537.36\" \"www.dlwindianrailways.com\"";

    private static Map<String, String> patterns = Grok.getBuiltinPatterns();

    @State(Scope.Thread)
    public static class DissectSysLogSetup {
        static final DissectParser DISSECTOR = new DissectParser("%{timestamp} %{+timestamp} %{+timestamp} %{logsource} %{program}[%{pid}]: %{message}", " ");
    }
    @State(Scope.Thread)
    public static class DissectApacheLogSetup {
        static final DissectParser DISSECTOR = new DissectParser("%{clientip} %{ident} %{auth} [%{timestamp}] \"%{verb} %{request} HTTP/%{httpversion}\" %{response} %{bytes} \"%{referrer}\" \"%{agent}\" %{->}", " ");
    }

    @State(Scope.Thread)
    public static class GrokSysLogSetup {
        static final Grok GROK = new Grok(patterns, "%{SYSLOGLINE}");
    }
    @State(Scope.Thread)
    public static class GrokApacheLogSetup {
        static final Grok GROK = new Grok(patterns, "%{COMBINEDAPACHELOG}");
    }

    @Benchmark
    public void dissectSysLog(final Blackhole bh) {
        bh.consume(
            DissectSysLogSetup.DISSECTOR.parse(SYSLOG_LINE)
        );
    }

    @Benchmark
    public void dissectApacheLog(final Blackhole bh) {
        bh.consume(
            DissectApacheLogSetup.DISSECTOR.parse(APACHE_LINE)
        );
    }

    @Benchmark
    public void grokSysLog(final Blackhole bh) {
        bh.consume(
            GrokSysLogSetup.GROK.captures(SYSLOG_LINE)
        );
    }
    @Benchmark
    public void grokApacheLog(final Blackhole bh) {
        bh.consume(
            GrokApacheLogSetup.GROK.captures(APACHE_LINE)
        );
    }


    //apache log


//    Grok grok = new Grok(basePatterns, "%{COMBINEDAPACHELOG}");
//assertEquals("31.184.238.164", matches.get("clientip"));
//    assertEquals("-", matches.get("ident"));
//    assertEquals("-", matches.get("auth"));
//    assertEquals("24/Jul/2014:05:35:37 +0530", matches.get("timestamp"));
//    assertEquals("GET", matches.get("verb"));
//    assertEquals("/logs/access.log", matches.get("request"));
//    assertEquals("1.0", matches.get("httpversion"));
//    assertEquals("200", matches.get("response"));
//    assertEquals("69849", matches.get("bytes"));
//    assertEquals("\"http://8rursodiol.enjin.com\"", matches.get("referrer"));
//    assertEquals(null, matches.get("port"));
//    assertEquals("\"Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/30.0.1599.12785 " +
//                     "YaBrowser/13.12.1599.12785 Safari/537.36\"", matches.get("agent"));
}
