/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */

package org.elasticsearch.xpack.ilm.action;

import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.action.RestToXContentListener;
import org.elasticsearch.xcontent.generated.ilm.LifecyclePolicyModel;
import org.elasticsearch.xcontent.generated.v7.ilm.LifecyclePolicyV7Model;
import org.elasticsearch.xpack.core.ilm.action.PutLifecycleAction;
import org.elasticsearch.xpack.ilm.adaptors.request.PutLifecycleAdaptor;
import org.elasticsearch.xpack.ilm.adaptors.request.PutLifecycleAdaptorV7;

import java.io.IOException;

public class RestPutLifecycleAction extends BaseRestHandler {

    public RestPutLifecycleAction(RestController controller) {
        controller.registerHandler(RestRequest.Method.PUT, "/_ilm/policy/{name}", this);
    }

    @Override
    public String getName() {
        return "ilm_put_action";
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest restRequest, NodeClient client) throws IOException {
        String lifecycleName = restRequest.param("name");
        try (XContentParser parser = restRequest.contentParser()) {
            PutLifecycleAction.Request putLifecycleRequest = toTransportRequest(restRequest, lifecycleName, parser);
            putLifecycleRequest.timeout(restRequest.paramAsTime("timeout", putLifecycleRequest.timeout()));
            putLifecycleRequest.masterNodeTimeout(restRequest.paramAsTime("master_timeout", putLifecycleRequest.masterNodeTimeout()));
            return channel -> client.execute(PutLifecycleAction.INSTANCE, putLifecycleRequest, new RestToXContentListener<>(channel));
        }
    }

    private PutLifecycleAction.Request toTransportRequest(RestRequest request, String name, XContentParser parser) {
        if (request.isVersion7()) {
            return PutLifecycleAdaptorV7.fromModel(LifecyclePolicyV7Model.PARSER.apply(parser, null), name);
        } else {
            return PutLifecycleAdaptor.fromModel(LifecyclePolicyModel.PARSER.apply(parser, null), name);
        }
    }
}
