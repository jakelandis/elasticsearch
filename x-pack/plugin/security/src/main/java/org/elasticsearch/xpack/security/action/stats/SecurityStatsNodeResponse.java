/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.security.action.stats;

import org.elasticsearch.action.support.nodes.BaseNodeResponse;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.common.io.stream.StreamInput;

import java.io.IOException;

public class SecurityStatsNodeResponse extends BaseNodeResponse {

    protected SecurityStatsNodeResponse(StreamInput in) throws IOException {
        super(in);
    }

    protected SecurityStatsNodeResponse(DiscoveryNode node) {
        super(node);
    }
}
