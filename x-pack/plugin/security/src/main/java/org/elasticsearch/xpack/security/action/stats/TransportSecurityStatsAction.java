/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.security.action.stats;

import org.elasticsearch.action.FailedNodeException;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.nodes.TransportNodesAction;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.tasks.Task;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;

import java.io.IOException;
import java.util.List;

public class TransportSecurityStatsAction extends TransportNodesAction<
    SecurityStatsRequest,
    SecurityStatsResponse,
    SecurityStatsNodeRequest,
    SecurityStatsNodeResponse> {

    @Inject
    public TransportSecurityStatsAction(
        TransportService transportService,
        ClusterService clusterService,
        ThreadPool threadPool,
        ActionFilters actionFilters
    ) {
        super(
            SecurityStatsAction.NAME,
            threadPool,
            clusterService,
            transportService,
            actionFilters,
            SecurityStatsRequest::new,
            SecurityStatsNodeRequest::new,
            ThreadPool.Names.MANAGEMENT
        );
    }

    @Override
    protected SecurityStatsResponse newResponse(
        SecurityStatsRequest request,
        List<SecurityStatsNodeResponse> securityStatsNodeResponses,
        List<FailedNodeException> failures
    ) {
        return null;
    }

    @Override
    protected SecurityStatsNodeRequest newNodeRequest(SecurityStatsRequest request) {
        return null;
    }

    @Override
    protected SecurityStatsNodeResponse newNodeResponse(StreamInput in, DiscoveryNode node) throws IOException {
        return null;
    }

    @Override
    protected SecurityStatsNodeResponse nodeOperation(SecurityStatsNodeRequest request, Task task) {
        return null;
    }
}
