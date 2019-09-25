/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */

package org.elasticsearch.xpack.ilm.action;

import org.apache.logging.log4j.LogManager;
import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.logging.DeprecationLogger;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.BytesRestResponse;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestResponse;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.rest.action.RestBuilderListener;
import org.elasticsearch.xcontent.generated.ilm.ColdModel;
import org.elasticsearch.xcontent.generated.ilm.DeleteModel;
import org.elasticsearch.xcontent.generated.ilm.HotModel;
import org.elasticsearch.xcontent.generated.ilm.LifecyclePolicyWithMetadataModel;
import org.elasticsearch.xcontent.generated.ilm.PolicyModel;
import org.elasticsearch.xcontent.generated.v7.ilm.LifecyclePolicyWithMetadataV7Model;
import org.elasticsearch.xcontent.generated.v7.ilm.PhasesV7Model;
import org.elasticsearch.xcontent.generated.v7.ilm.PolicyV7Model;
import org.elasticsearch.xcontent.generated.v7.ilm.WarmV7Model;
import org.elasticsearch.xpack.core.ilm.ForceMergeAction;
import org.elasticsearch.xpack.core.ilm.Phase;
import org.elasticsearch.xpack.core.ilm.ReadOnlyAction;
import org.elasticsearch.xpack.core.ilm.ShrinkAction;
import org.elasticsearch.xpack.core.ilm.action.GetLifecycleAction;
import org.elasticsearch.xpack.core.ilm.adaptors.LifecyclePolicyAdaptor;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RestGetLifecycleAction extends BaseRestHandler {

    private static final DeprecationLogger deprecationLogger = new DeprecationLogger(LogManager.getLogger(RestGetLifecycleAction.class));

    public RestGetLifecycleAction(RestController controller) {
        controller.registerHandler(RestRequest.Method.GET, "/_ilm/policy", this);
        controller.registerHandler(RestRequest.Method.GET, "/_ilm/policy/{name}", this);
    }

    @Override
    public String getName() {
        return "ilm_get_action";
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest restRequest, NodeClient client) {
        boolean isVersion7 = restRequest.isVersion7(); //TODO: bake this into the BaseRestHandler
        String[] lifecycleNames = Strings.splitStringByCommaToArray(restRequest.param("name"));
        GetLifecycleAction.Request getLifecycleRequest = new GetLifecycleAction.Request(lifecycleNames);
        getLifecycleRequest.timeout(restRequest.paramAsTime("timeout", getLifecycleRequest.timeout()));
        getLifecycleRequest.masterNodeTimeout(restRequest.paramAsTime("master_timeout", getLifecycleRequest.masterNodeTimeout()));
        // return channel -> client.execute(GetLifecycleAction.INSTANCE, getLifecycleRequest, new RestToXContentListener<>(channel));
        return channel -> client.execute(GetLifecycleAction.INSTANCE, getLifecycleRequest, new RestBuilderListener<>(channel) {

            @Override
            public RestResponse buildResponse(GetLifecycleAction.Response response, XContentBuilder builder) throws Exception {
                return toRestResponse(restRequest, response, builder, isVersion7);
            }
        });
    }

    RestResponse toRestResponse(RestRequest restRequest, GetLifecycleAction.Response response, XContentBuilder builder, boolean isVersion7) throws IOException {
        if (isVersion7) {
            toModelV7(response).toXContent(builder, restRequest);
        } else {
            toModel(response).toXContent(builder, restRequest);
        }
        return new BytesRestResponse(RestStatus.OK, builder);
    }

    private static LifecyclePolicyWithMetadataModel toModel(GetLifecycleAction.Response response) {
        List<GetLifecycleAction.LifecyclePolicyResponseItem> policies = response.getPolicies();
        Map<String, LifecyclePolicyWithMetadataModel.ObjectMapItem> rootMap = new HashMap<>();
        for (GetLifecycleAction.LifecyclePolicyResponseItem policy : policies) {
            PolicyModel policyModel = LifecyclePolicyAdaptor.toModel(policy.getLifecyclePolicy());
            rootMap.put(policy.getLifecyclePolicy().getName(), new LifecyclePolicyWithMetadataModel.ObjectMapItem(policy.getVersion(), policy.getModifiedDate(), policyModel));
        }
        return new LifecyclePolicyWithMetadataModel(rootMap);
    }

    /**
     * Convert the Transport response to a version 7 REST response
     */
    @SuppressWarnings("Duplicates")
    private static LifecyclePolicyWithMetadataV7Model toModelV7(GetLifecycleAction.Response response) {
        List<GetLifecycleAction.LifecyclePolicyResponseItem> policies = response.getPolicies();
        Map<String, LifecyclePolicyWithMetadataV7Model.ObjectMapItem> rootMap = new HashMap<>();
        for (GetLifecycleAction.LifecyclePolicyResponseItem policy : policies) {
            rootMap.put(policy.getLifecyclePolicy().getName(), new LifecyclePolicyWithMetadataV7Model.ObjectMapItem(policy.getVersion(), policy.getModifiedDate(), new PolicyV7Model(getPhasesModel(policy))));
        }
        return new LifecyclePolicyWithMetadataV7Model(rootMap);
    }

    @SuppressWarnings("Duplicates")
    private static PhasesV7Model getPhasesModel(GetLifecycleAction.LifecyclePolicyResponseItem policy) {
        HotModel hotModel = null;
        WarmV7Model warmModel = null;
        ColdModel coldModel = null;
        DeleteModel deleteModel = null;

        for (Phase phase : policy.getLifecyclePolicy().getPhases().values()) {
            switch (phase.getName()) {
                case "hot":
                    hotModel = LifecyclePolicyAdaptor.getHotModel(phase);
                    break;
                case "warm":
                    warmModel = getWamModel(phase);
                    break;
                case "cold":
                    coldModel = LifecyclePolicyAdaptor.getColdModel(phase);
                    break;
                case "delete":
                    deleteModel = LifecyclePolicyAdaptor.getDeleteModel(phase);
                    break;
                default:
                    break;
            }
        }
        return new PhasesV7Model(hotModel, warmModel, coldModel, deleteModel);
    }

    @SuppressWarnings("Duplicates")
    private static WarmV7Model getWamModel(Phase phase) {
        ForceMergeAction forceMerge = (ForceMergeAction) phase.getActions().get(ForceMergeAction.NAME);
        ShrinkAction shrink = (ShrinkAction) phase.getActions().get(ShrinkAction.NAME);
        ReadOnlyAction readOnly = (ReadOnlyAction) phase.getActions().get(ReadOnlyAction.NAME);
        WarmV7Model.Actions.Forcemerge forceMergeModel = null;
        WarmV7Model.Actions.Shrink shrinkModel = null;
        WarmV7Model.Actions.Readonly readOnlyModel = null;

        if (forceMerge != null) {
            forceMergeModel = new WarmV7Model.Actions.Forcemerge((long) forceMerge.getMaxNumSegments(), (long) forceMerge.getMaxNumSegments());
            deprecationLogger.deprecatedAndMaybeLog("ilm_max_num_segments", "[max_num_segments] has been deprecated please use [max_number_segments]");
        }

        if (shrink != null) {
            shrinkModel = new WarmV7Model.Actions.Shrink((long) shrink.getNumberOfShards());
        }
        if (readOnly != null) {
            readOnlyModel = new WarmV7Model.Actions.Readonly();
        }
        return new WarmV7Model(phase.getMinimumAge().getStringRep(), new WarmV7Model.Actions(forceMergeModel, shrinkModel, LifecyclePolicyAdaptor.getAllocateModel(phase), LifecyclePolicyAdaptor.getUnfollowModel(phase), readOnlyModel));
    }

}
