/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.ml.rest.inference;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.client.internal.node.NodeClient;
import org.elasticsearch.core.RestApiVersion;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestResponse;
import org.elasticsearch.rest.action.RestToXContentListener;
import org.elasticsearch.xcontent.XContentBuilder;
import org.elasticsearch.xcontent.XContentParser;
import org.elasticsearch.xpack.core.ml.action.PutTrainedModelAction;
import org.elasticsearch.xpack.core.ml.inference.TrainedModelConfig;

import java.io.IOException;
import java.util.List;

import static org.elasticsearch.rest.RestRequest.Method.PUT;
import static org.elasticsearch.xpack.ml.MachineLearning.BASE_PATH;

public class RestPutTrainedModelAction extends BaseRestHandler {

    private static final Logger logger = LogManager.getLogger("custom_timer");

    @Override
    public List<Route> routes() {
        return List.of(
            Route.builder(PUT, BASE_PATH + "trained_models/{" + TrainedModelConfig.MODEL_ID + "}")
                .replaces(PUT, BASE_PATH + "inference/{" + TrainedModelConfig.MODEL_ID + "}", RestApiVersion.V_8)
                .build()
        );
    }

    @Override
    public String getName() {
        return "xpack_ml_put_trained_model_action";
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest restRequest, NodeClient client) throws IOException {
        long start = System.nanoTime();
        String id = restRequest.param(TrainedModelConfig.MODEL_ID.getPreferredName());
        XContentParser parser = restRequest.contentParser();
        boolean deferDefinitionDecompression = restRequest.paramAsBoolean(PutTrainedModelAction.DEFER_DEFINITION_DECOMPRESSION, false);
        PutTrainedModelAction.Request putRequest = PutTrainedModelAction.Request.parseRequest(id, deferDefinitionDecompression, parser);
        putRequest.timeout(restRequest.paramAsTime("timeout", putRequest.timeout()));
        return channel -> client.execute(PutTrainedModelAction.INSTANCE, putRequest, new RestToXContentListener<>(channel) {

            @Override
            public RestResponse buildResponse(PutTrainedModelAction.Response response, XContentBuilder builder) throws Exception {
                RestResponse returnValue = super.buildResponse(response, builder);
                logger.info("PUT model: " + (System.nanoTime() - start));
                return returnValue;
            }
        });

    }
}
