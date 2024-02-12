/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.search.aggregations.metrics;

import org.elasticsearch.TransportVersion;
import org.elasticsearch.TransportVersions;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregatorFactories;
import org.elasticsearch.search.aggregations.AggregatorFactory;
import org.elasticsearch.search.aggregations.support.AggregationContext;
import org.elasticsearch.search.aggregations.support.CoreValuesSourceType;
import org.elasticsearch.search.aggregations.support.ValuesSourceAggregationBuilder;
import org.elasticsearch.search.aggregations.support.ValuesSourceConfig;
import org.elasticsearch.search.aggregations.support.ValuesSourceRegistry;
import org.elasticsearch.search.aggregations.support.ValuesSourceType;
import org.elasticsearch.xcontent.ObjectParser;
import org.elasticsearch.xcontent.ParseField;
import org.elasticsearch.xcontent.XContentBuilder;

import java.io.IOException;
import java.util.Map;

public class ValueCountAggregationBuilder extends ValuesSourceAggregationBuilder.SingleMetricAggregationBuilder<
    ValueCountAggregationBuilder> {
    public static final String NAME = "value_count";
    private boolean excludeDeletedDocuments;
    public static final ParseField EXCLUDE_DELETED_DOCS = new ParseField("exclude_deleted_docs");


    public static final ValuesSourceRegistry.RegistryKey<MetricAggregatorSupplier> REGISTRY_KEY = new ValuesSourceRegistry.RegistryKey<>(
        NAME,
        MetricAggregatorSupplier.class
    );

    public static final ObjectParser<ValueCountAggregationBuilder, String> PARSER = ObjectParser.fromBuilder(
        NAME,
        ValueCountAggregationBuilder::new
    );
    static {
        ValuesSourceAggregationBuilder.declareFields(PARSER, true, true, false);
        PARSER.declareBoolean(ValueCountAggregationBuilder::excludeDeletedDocs, EXCLUDE_DELETED_DOCS);
    }


    public static void registerAggregators(ValuesSourceRegistry.Builder builder) {
        ValueCountAggregatorFactory.registerAggregators(builder);
    }

    public ValueCountAggregationBuilder(String name) {
        super(name);
    }

    protected ValueCountAggregationBuilder(
        ValueCountAggregationBuilder clone,
        AggregatorFactories.Builder factoriesBuilder,
        Map<String, Object> metadata
    ) {
        super(clone, factoriesBuilder, metadata);
    }

    @Override
    protected ValuesSourceType defaultValueSourceType() {
        return CoreValuesSourceType.KEYWORD;
    }

    @Override
    protected AggregationBuilder shallowCopy(AggregatorFactories.Builder factoriesBuilder, Map<String, Object> metadata) {
        return new ValueCountAggregationBuilder(this, factoriesBuilder, metadata);
    }

    @Override
    public boolean supportsSampling() {
        return true;
    }

    private void excludeDeletedDocs(Boolean excludeDeletedDocuments) {
        this.excludeDeletedDocuments = excludeDeletedDocuments;
    }

    private Boolean excludeDeletedDocs() {
        return excludeDeletedDocuments;
    }

    /**
     * Read from a stream.
     */
    public ValueCountAggregationBuilder(StreamInput in) throws IOException {
     // read and ignore the flag that indicates if the field is a script or not
        super(in);
        //TODO: protect with transport version
        in.readBoolean();
    }

    @Override
    protected void innerWriteTo(StreamOutput out) throws IOException {
        //TODO: protect with transport version
        out.writeBoolean(excludeDeletedDocuments);
    }

    @Override
    protected boolean serializeTargetValueType(TransportVersion version) {
        return true;
    }

    @Override
    protected ValueCountAggregatorFactory innerBuild(
        AggregationContext context,
        ValuesSourceConfig config,
        AggregatorFactory parent,
        AggregatorFactories.Builder subFactoriesBuilder
    ) throws IOException {
        MetricAggregatorSupplier aggregatorSupplier = context.getValuesSourceRegistry().getAggregator(REGISTRY_KEY, config);
        return new ValueCountAggregatorFactory(name, config, context, parent, subFactoriesBuilder, metadata, aggregatorSupplier);
    }

    @Override
    public XContentBuilder doXContentBody(XContentBuilder builder, Params params) throws IOException {
        //TODO: double check this is right
        builder.field(EXCLUDE_DELETED_DOCS.getPreferredName(), excludeDeletedDocuments);
        return builder;
    }

    @Override
    public String getType() {
        return NAME;
    }

    @Override
    public TransportVersion getMinimalSupportedVersion() {
        return TransportVersions.ZERO;
    }
}
