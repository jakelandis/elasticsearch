/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.security.action.stats;

import org.elasticsearch.action.FailedNodeException;
import org.elasticsearch.action.support.nodes.BaseNodesResponse;
import org.elasticsearch.cluster.ClusterName;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.xcontent.ToXContentObject;
import org.elasticsearch.xcontent.XContentBuilder;

import java.io.IOException;
import java.util.List;

public class SecurityStatsResponse extends BaseNodesResponse<SecurityStatsNodeResponse> implements Writeable, ToXContentObject {


    protected SecurityStatsResponse(StreamInput in) throws IOException {
        super(in);
    }

    public SecurityStatsResponse(ClusterName clusterName, List < SecurityStatsNodeResponse > nodes, List< FailedNodeException > failures) {
        super(clusterName, nodes, failures);
    }

    @Override
    protected List<SecurityStatsNodeResponse> readNodesFrom(StreamInput in) throws IOException {
        return null;
    }

    @Override
    protected void writeNodesTo(StreamOutput out, List<SecurityStatsNodeResponse> nodes) throws IOException {

    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        return null;
    }
}
