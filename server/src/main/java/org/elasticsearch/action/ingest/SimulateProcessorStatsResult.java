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
package org.elasticsearch.action.ingest;

import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.collect.Tuple;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.common.xcontent.ConstructingObjectParser;
import org.elasticsearch.common.xcontent.ToXContentObject;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.ingest.IngestStats;

import java.io.IOException;

import static org.elasticsearch.common.xcontent.ConstructingObjectParser.constructorArg;

public class SimulateProcessorStatsResult implements Writeable, ToXContentObject {

    public static final String PROCESSOR_STATS_FIELD = "processor_stats";
    private final Tuple<String, IngestStats.Stats> processorStat;

    public static final ConstructingObjectParser<SimulateProcessorStatsResult, Void> PARSER =
        new ConstructingObjectParser<>("simulate_processor_stat_result",
            a -> new SimulateProcessorStatsResult((Tuple<String, IngestStats.Stats>) a[0]));

    static {
        PARSER.declareObjectArray(constructorArg(), SimulateProcessorStatsResult.PARSER, new ParseField(PROCESSOR_STATS_FIELD));
    }

    public SimulateProcessorStatsResult(Tuple<String, IngestStats.Stats> processorStat) {
        this.processorStat = processorStat;
    }

    /**
     * Read from a stream.
     */
    SimulateProcessorStatsResult(StreamInput in) throws IOException {
        this(new Tuple<>(in.readString(), new IngestStats.Stats(in)));
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeString(processorStat.v1());
        processorStat.v2().writeTo(out);
    }


    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.startObject(processorStat.v1());
        processorStat.v2().toXContent(builder, params);
        builder.endObject();
        builder.endObject();
        return builder;
    }

    public static SimulateProcessorStatsResult fromXContent(XContentParser parser) {
        return PARSER.apply(parser, null);
    }
}
