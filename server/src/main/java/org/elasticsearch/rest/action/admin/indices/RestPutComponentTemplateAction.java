/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.rest.action.admin.indices;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.admin.indices.template.put.PutComponentTemplateAction;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.internal.node.NodeClient;
import org.elasticsearch.cluster.metadata.ComponentTemplate;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestResponse;
import org.elasticsearch.rest.action.RestToXContentListener;
import org.elasticsearch.xcontent.XContentBuilder;

import java.io.IOException;
import java.util.List;

import static org.elasticsearch.rest.RestRequest.Method.POST;
import static org.elasticsearch.rest.RestRequest.Method.PUT;

public class RestPutComponentTemplateAction extends BaseRestHandler {

    private static final Logger logger = LogManager.getLogger("custom_timer");

    @Override
    public List<Route> routes() {
        return List.of(new Route(POST, "/_component_template/{name}"), new Route(PUT, "/_component_template/{name}"));
    }

    @Override
    public String getName() {
        return "put_component_template_action";
    }

    @Override
    public RestChannelConsumer prepareRequest(final RestRequest request, final NodeClient client) throws IOException {
        long start = System.nanoTime();
        PutComponentTemplateAction.Request putRequest = new PutComponentTemplateAction.Request(request.param("name"));
        putRequest.masterNodeTimeout(request.paramAsTime("master_timeout", putRequest.masterNodeTimeout()));
        putRequest.create(request.paramAsBoolean("create", false));
        putRequest.cause(request.param("cause", "api"));
        putRequest.componentTemplate(ComponentTemplate.parse(request.contentParser()));
        return channel -> client.execute(PutComponentTemplateAction.INSTANCE, putRequest, new RestToXContentListener<>(channel) {
            @Override
            public RestResponse buildResponse(AcknowledgedResponse acknowledgedResponse, XContentBuilder builder) throws Exception {
                RestResponse returnValue = super.buildResponse(acknowledgedResponse, builder);
                logger.info("PUT component template: " + (System.nanoTime() - start));
                return returnValue;
            }
        });
    }
}
