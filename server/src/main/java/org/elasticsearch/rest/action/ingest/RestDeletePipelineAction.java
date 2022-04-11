/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.rest.action.ingest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.ingest.DeletePipelineRequest;
import org.elasticsearch.client.internal.node.NodeClient;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.action.RestToXContentListener;

import java.io.IOException;
import java.util.List;

import static org.elasticsearch.rest.RestRequest.Method.DELETE;

public class RestDeletePipelineAction extends BaseRestHandler {

    private static final Logger logger = LogManager.getLogger("custom_timer");

    @Override
    public List<Route> routes() {
        return List.of(new Route(DELETE, "/_ingest/pipeline/{id}"));
    }

    @Override
    public String getName() {
        return "ingest_delete_pipeline_action";
    }

    @Override
    public RestChannelConsumer prepareRequest(RestRequest restRequest, NodeClient client) throws IOException {
        long start = System.nanoTime();
        DeletePipelineRequest request = new DeletePipelineRequest(restRequest.param("id"));
        request.masterNodeTimeout(restRequest.paramAsTime("master_timeout", request.masterNodeTimeout()));
        request.timeout(restRequest.paramAsTime("timeout", request.timeout()));
        RestChannelConsumer returnValue = channel -> client.admin()
            .cluster()
            .deletePipeline(request, new RestToXContentListener<>(channel));
        logger.info("DELETE pipeline: " + (System.nanoTime() - start));
        return returnValue;
    }
}
