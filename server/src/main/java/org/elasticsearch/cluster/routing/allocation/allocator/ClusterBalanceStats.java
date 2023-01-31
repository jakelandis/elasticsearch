/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.cluster.routing.allocation.allocator;

import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.metadata.Metadata;
import org.elasticsearch.cluster.node.DiscoveryNodeRole;
import org.elasticsearch.cluster.routing.RoutingNode;
import org.elasticsearch.cluster.routing.ShardRouting;
import org.elasticsearch.cluster.routing.allocation.WriteLoadForecaster;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.common.util.Maps;
import org.elasticsearch.xcontent.ToXContentObject;
import org.elasticsearch.xcontent.XContentBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToDoubleFunction;

public record ClusterBalanceStats(Map<String, TierBalanceStats> tiers) implements Writeable, ToXContentObject {

    public static ClusterBalanceStats EMPTY = new ClusterBalanceStats(Map.of());

    public static ClusterBalanceStats createFrom(ClusterState clusterState, WriteLoadForecaster writeLoadForecaster) {
        var tierToNodeStats = new HashMap<String, List<NodeStats>>();
        for (RoutingNode routingNode : clusterState.getRoutingNodes()) {
            var dataRoles = routingNode.node().getRoles().stream().filter(DiscoveryNodeRole::canContainData).toList();
            if (dataRoles.isEmpty()) {
                continue;
            }
            var nodeStats = NodeStats.createFrom(routingNode, clusterState.metadata(), writeLoadForecaster);
            for (DiscoveryNodeRole role : dataRoles) {
                tierToNodeStats.computeIfAbsent(role.roleName(), ignored -> new ArrayList<>()).add(nodeStats);
            }
        }
        return new ClusterBalanceStats(Maps.transformValues(tierToNodeStats, TierBalanceStats::createFrom));
    }

    public static ClusterBalanceStats readFrom(StreamInput in) throws IOException {
        return new ClusterBalanceStats(in.readImmutableMap(StreamInput::readString, TierBalanceStats::readFrom));
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeMap(tiers, StreamOutput::writeString, StreamOutput::writeWriteable);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        return builder.map(tiers);
    }

    public record TierBalanceStats(MetricStats shardCount, MetricStats totalWriteLoad, MetricStats totalShardSize)
        implements
            Writeable,
            ToXContentObject {

        private static TierBalanceStats createFrom(List<NodeStats> nodes) {
            return new TierBalanceStats(
                MetricStats.createFrom(nodes, it -> it.shards),
                MetricStats.createFrom(nodes, it -> it.totalWriteLoad),
                MetricStats.createFrom(nodes, it -> it.totalShardSize)
            );
        }

        public static TierBalanceStats readFrom(StreamInput in) throws IOException {
            return new TierBalanceStats(MetricStats.readFrom(in), MetricStats.readFrom(in), MetricStats.readFrom(in));
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            shardCount.writeTo(out);
            totalWriteLoad.writeTo(out);
            totalShardSize.writeTo(out);
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            return builder.startObject()
                .field("shard_count", shardCount)
                .field("total_write_load", totalWriteLoad)
                .field("total_shard_size", totalShardSize)
                .endObject();
        }
    }

    public record MetricStats(double total, double min, double max, double average, double stdDev) implements Writeable, ToXContentObject {

        private static MetricStats createFrom(List<NodeStats> nodes, ToDoubleFunction<NodeStats> metricExtractor) {
            assert nodes.isEmpty() == false : "Stats must be created from non empty nodes";
            double total = 0.0;
            double total2 = 0.0;
            double min = Double.POSITIVE_INFINITY;
            double max = Double.NEGATIVE_INFINITY;
            int count = 0;
            for (NodeStats node : nodes) {
                var metric = metricExtractor.applyAsDouble(node);
                if (Double.isNaN(metric)) {
                    continue;
                }
                total += metric;
                total2 += Math.pow(metric, 2);
                min = Math.min(min, metric);
                max = Math.max(max, metric);
                count++;
            }
            double average = count == 0 ? Double.NaN : total / count;
            double stdDev = count == 0 ? Double.NaN : Math.sqrt(total2 / count - Math.pow(average, 2));
            return new MetricStats(total, min, max, average, stdDev);
        }

        public static MetricStats readFrom(StreamInput in) throws IOException {
            return new MetricStats(in.readDouble(), in.readDouble(), in.readDouble(), in.readDouble(), in.readDouble());
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            out.writeDouble(total);
            out.writeDouble(min);
            out.writeDouble(max);
            out.writeDouble(average);
            out.writeDouble(stdDev);
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            return builder.startObject()
                .field("total", total)
                .field("min", min)
                .field("max", max)
                .field("average", average)
                .field("std_dev", stdDev)
                .endObject();
        }
    }

    private record NodeStats(int shards, double totalWriteLoad, long totalShardSize) {

        private static NodeStats createFrom(RoutingNode routingNode, Metadata metadata, WriteLoadForecaster writeLoadForecaster) {
            double totalWriteLoad = 0.0;
            long totalShardSize = 0L;

            for (ShardRouting shardRouting : routingNode) {
                var indexMetadata = metadata.index(shardRouting.index());
                assert indexMetadata != null;
                totalWriteLoad += writeLoadForecaster.getForecastedWriteLoad(indexMetadata).orElse(0.0);
                totalShardSize += indexMetadata.getForecastedShardSizeInBytes().orElse(0);
            }

            return new NodeStats(routingNode.size(), totalWriteLoad, totalShardSize);
        }
    }
}
