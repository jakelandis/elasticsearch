/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.ingest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.script.MockScriptEngine;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptModule;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.test.ESTestCase;
import org.hamcrest.CoreMatchers;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;

public class ConditionalProcessorTests extends ESTestCase {

    public void testChecksCondition() throws Exception {
        String conditionalField = "field1";
        String scriptName = "conditionalScript";
        String trueValue = "truthy";
        ScriptService scriptService = new ScriptService(Settings.builder().build(),
            Collections.singletonMap(
                Script.DEFAULT_SCRIPT_LANG,
                new MockScriptEngine(
                    Script.DEFAULT_SCRIPT_LANG,
                    Collections.singletonMap(
                        scriptName, ctx -> trueValue.equals(ctx.get(conditionalField))
                    )
                )
            ),
            new HashMap<>(ScriptModule.CORE_CONTEXTS)
        );
        Map<String, Object> document = new HashMap<>();
        ConditionalProcessor processor = new ConditionalProcessor(
            randomAlphaOfLength(10),
            new Script(
                ScriptType.INLINE, Script.DEFAULT_SCRIPT_LANG,
                scriptName, Collections.emptyMap()), scriptService,
            new Processor() {
                @Override
                public IngestDocument execute(final IngestDocument ingestDocument) throws Exception {
                    Thread.sleep(2); //to ensure timer is incremented
                    if(ingestDocument.hasField("error")){
                        throw new RuntimeException("error");
                    }
                    ingestDocument.setFieldValue("foo", "bar");
                    return ingestDocument;
                }

                @Override
                public String getType() {
                    return null;
                }

                @Override
                public String getTag() {
                    return null;
                }
            });

        //false, never call processor never increments metrics
        String falseValue = "falsy";
        IngestDocument ingestDocument = RandomDocumentPicks.randomIngestDocument(random(), document);
        ingestDocument.setFieldValue(conditionalField, falseValue);
        processor.execute(ingestDocument);
        assertThat(ingestDocument.getSourceAndMetadata().get(conditionalField), is(falseValue));
        assertThat(ingestDocument.getSourceAndMetadata(), not(hasKey("foo")));
        assertStats(processor, 0, 0, 0);

        ingestDocument = RandomDocumentPicks.randomIngestDocument(random(), document);
        ingestDocument.setFieldValue(conditionalField, falseValue);
        ingestDocument.setFieldValue("error", true);
        processor.execute(ingestDocument);
        assertStats(processor, 0, 0, 0);

        //true, always call processor and increments metrics
        ingestDocument = RandomDocumentPicks.randomIngestDocument(random(), document);
        ingestDocument.setFieldValue(conditionalField, trueValue);
        processor.execute(ingestDocument);
        assertThat(ingestDocument.getSourceAndMetadata().get(conditionalField), is(trueValue));
        assertThat(ingestDocument.getSourceAndMetadata().get("foo"), is("bar"));
        assertStats(processor, 1, 0, 2);

        ingestDocument = RandomDocumentPicks.randomIngestDocument(random(), document);
        ingestDocument.setFieldValue(conditionalField, trueValue);
        ingestDocument.setFieldValue("error", true);
        IngestDocument finalIngestDocument = ingestDocument;
        expectThrows(RuntimeException.class, () -> processor.execute(finalIngestDocument));
        assertStats(processor, 2, 1, 4);
    }

    @SuppressWarnings("unchecked")
    public void testActsOnImmutableData() throws Exception {
        assertMutatingCtxThrows(ctx -> ctx.remove("foo"));
        assertMutatingCtxThrows(ctx -> ctx.put("foo", "bar"));
        assertMutatingCtxThrows(ctx -> ((List<Object>)ctx.get("listField")).add("bar"));
        assertMutatingCtxThrows(ctx -> ((List<Object>)ctx.get("listField")).remove("bar"));
    }

    private static void assertMutatingCtxThrows(Consumer<Map<String, Object>> mutation) throws Exception {
        String scriptName = "conditionalScript";
        CompletableFuture<Exception> expectedException = new CompletableFuture<>();
        ScriptService scriptService = new ScriptService(Settings.builder().build(),
            Collections.singletonMap(
                Script.DEFAULT_SCRIPT_LANG,
                new MockScriptEngine(
                    Script.DEFAULT_SCRIPT_LANG,
                    Collections.singletonMap(
                        scriptName, ctx -> {
                            try {
                                mutation.accept(ctx);
                            } catch (Exception e) {
                                expectedException.complete(e);
                            }
                            return false;
                        }
                    )
                )
            ),
            new HashMap<>(ScriptModule.CORE_CONTEXTS)
        );
        Map<String, Object> document = new HashMap<>();
        ConditionalProcessor processor = new ConditionalProcessor(
            randomAlphaOfLength(10),
            new Script(
                ScriptType.INLINE, Script.DEFAULT_SCRIPT_LANG,
                scriptName, Collections.emptyMap()), scriptService, null
        );
        IngestDocument ingestDocument = RandomDocumentPicks.randomIngestDocument(random(), document);
        ingestDocument.setFieldValue("listField", new ArrayList<>());
        processor.execute(ingestDocument);
        Exception e = expectedException.get();
        assertThat(e, instanceOf(UnsupportedOperationException.class));
        assertEquals("Mutating ingest documents in conditionals is not supported", e.getMessage());
        assertStats(processor, 0, 0, 0);
    }

    private static void assertStats(ConditionalProcessor conditionalProcessor, long count, long failed, long time) {
        IngestStats.Stats stats = conditionalProcessor.getMetric().createStats();
        assertThat(stats.getIngestCount(), equalTo(count));
        assertThat(stats.getIngestCurrent(), equalTo(0L));
        assertThat(stats.getIngestFailedCount(), equalTo(failed));
        assertThat(stats.getIngestTimeInMillis(), greaterThanOrEqualTo(time));
    }
}
