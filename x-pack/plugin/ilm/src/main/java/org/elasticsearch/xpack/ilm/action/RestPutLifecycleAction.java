/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */

package org.elasticsearch.xpack.ilm.action;

import org.apache.logging.log4j.LogManager;
import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.logging.DeprecationLogger;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.action.RestToXContentListener;
import org.elasticsearch.xcontent.generated.ilm.LifecyclePolicyModel;
import org.elasticsearch.xcontent.generated.v7.ilm.LifecyclePolicyV7Model;
import org.elasticsearch.xcontent.generated.v7.ilm.WarmV7Model;
import org.elasticsearch.xpack.core.ilm.AllocateAction;
import org.elasticsearch.xpack.core.ilm.ForceMergeAction;
import org.elasticsearch.xpack.core.ilm.LifecycleAction;
import org.elasticsearch.xpack.core.ilm.LifecyclePolicy;
import org.elasticsearch.xpack.core.ilm.Phase;
import org.elasticsearch.xpack.core.ilm.ReadOnlyAction;
import org.elasticsearch.xpack.core.ilm.ShrinkAction;
import org.elasticsearch.xpack.core.ilm.UnfollowAction;
import org.elasticsearch.xpack.core.ilm.action.PutLifecycleAction;
import org.elasticsearch.xpack.core.ilm.adaptors.LifecyclePolicyAdaptor;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class RestPutLifecycleAction extends BaseRestHandler {

    private static final DeprecationLogger deprecationLogger = new DeprecationLogger(LogManager.getLogger(RestGetLifecycleAction.class));
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
            return fromModelV7(LifecyclePolicyV7Model.PARSER.apply(parser, null), name);
        } else {
            return fromModel(LifecyclePolicyModel.PARSER.apply(parser, null), name);
        }
    }

    private static PutLifecycleAction.Request fromModel(LifecyclePolicyModel model, String name) {
        return new PutLifecycleAction.Request(LifecyclePolicyAdaptor.fromModel(model, name));
    }


    /**
     * Convert version 7 REST request to a Transport request
     */

    public static PutLifecycleAction.Request fromModelV7(LifecyclePolicyV7Model model, String name) {
        Map<String, Phase> phases = new HashMap<>();
        if (model.policy.phases.hot != null) {
            phases.put("hot", LifecyclePolicyAdaptor.getHotPhase(model.policy.phases.hot));
        }
        if (model.policy.phases.warm != null) {
            phases.put("warm", getWarmPhase(model.policy.phases.warm));
        }
        if (model.policy.phases.cold != null) {
            phases.put("cold", LifecyclePolicyAdaptor.getColdPhase(model.policy.phases.cold));
        }
        if (model.policy.phases.delete != null) {
            phases.put("delete", LifecyclePolicyAdaptor.getDeletePhase(model.policy.phases.delete));
        }
        return new PutLifecycleAction.Request(new LifecyclePolicy(name, phases));
    }


    @SuppressWarnings("Duplicates")
    private static Phase getWarmPhase(WarmV7Model warmModel) {
        Map<String, LifecycleAction> actions = new HashMap<>();
        if (warmModel.actions.allocate != null) {
            actions.put(AllocateAction.NAME, LifecyclePolicyAdaptor.getAllocateAction(warmModel.actions.allocate));
        }
        if (warmModel.actions.forcemerge != null && warmModel.actions.forcemerge.max_num_segments != null) {
            actions.put(ForceMergeAction.NAME, new ForceMergeAction(warmModel.actions.forcemerge.max_num_segments.intValue()));
            deprecationLogger.deprecatedAndMaybeLog("ilm_max_num_segments", "[max_num_segments] has been deprecated please use [max_number_segments]");
        } else if (warmModel.actions.forcemerge != null && warmModel.actions.forcemerge.max_number_segments != null) {
            actions.put(ForceMergeAction.NAME, new ForceMergeAction(warmModel.actions.forcemerge.max_number_segments.intValue()));
        }
        if (warmModel.actions.shrink != null) {
            actions.put(ShrinkAction.NAME, new ShrinkAction(warmModel.actions.shrink.number_of_shards.intValue()));
        }
        if (warmModel.actions.readonly != null) {
            actions.put(ReadOnlyAction.NAME, new ReadOnlyAction());
        }
        if (warmModel.actions.unfollow != null) {
            actions.put(UnfollowAction.NAME, new UnfollowAction());
        }
        return new Phase("warm", LifecyclePolicyAdaptor.getTimeValue(warmModel.min_age, "min_age"), actions);
    }
}
