/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */

package org.elasticsearch.xpack.ilm.action;

import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.BytesRestResponse;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestResponse;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.rest.action.RestBuilderListener;
import org.elasticsearch.rest.action.RestResponseListener;
import org.elasticsearch.rest.action.RestToXContentListener;
import org.elasticsearch.xcontent.generated.ilm.AllocateModel;
import org.elasticsearch.xcontent.generated.ilm.ColdModel;
import org.elasticsearch.xcontent.generated.ilm.DeleteModel;
import org.elasticsearch.xcontent.generated.ilm.GetPolicyModel;
import org.elasticsearch.xcontent.generated.ilm.HotModel;
import org.elasticsearch.xcontent.generated.ilm.InnerDeleteModel;
import org.elasticsearch.xcontent.generated.ilm.PhasesModel;
import org.elasticsearch.xcontent.generated.ilm.PolicyModel;
import org.elasticsearch.xcontent.generated.ilm.WarmModel;
import org.elasticsearch.xpack.core.ilm.AllocateAction;
import org.elasticsearch.xpack.core.ilm.ForceMergeAction;
import org.elasticsearch.xpack.core.ilm.LifecycleAction;
import org.elasticsearch.xpack.core.ilm.Phase;
import org.elasticsearch.xpack.core.ilm.RolloverAction;
import org.elasticsearch.xpack.core.ilm.SetPriorityAction;
import org.elasticsearch.xpack.core.ilm.ShrinkAction;
import org.elasticsearch.xpack.core.ilm.action.GetLifecycleAction;

import java.io.IOException;
import java.security.Policy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RestGetLifecycleAction extends BaseRestHandler {

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
        String[] lifecycleNames = Strings.splitStringByCommaToArray(restRequest.param("name"));
        GetLifecycleAction.Request getLifecycleRequest = new GetLifecycleAction.Request(lifecycleNames);
        getLifecycleRequest.timeout(restRequest.paramAsTime("timeout", getLifecycleRequest.timeout()));
        getLifecycleRequest.masterNodeTimeout(restRequest.paramAsTime("master_timeout", getLifecycleRequest.masterNodeTimeout()));
        // return channel -> client.execute(GetLifecycleAction.INSTANCE, getLifecycleRequest, new RestToXContentListener<>(channel));
        return channel -> client.execute(GetLifecycleAction.INSTANCE, getLifecycleRequest, new RestBuilderListener<>(channel) {

            @Override
            public RestResponse buildResponse(GetLifecycleAction.Response response, XContentBuilder builder) throws Exception {
                return toRestResponse(restRequest, response, builder);
            }
        });
    }

    RestResponse toRestResponse(RestRequest restRequest, GetLifecycleAction.Response response, XContentBuilder builder) throws IOException {
        Map<String, PolicyModel> rootMap = new HashMap<>();
        List<GetLifecycleAction.LifecyclePolicyResponseItem> policies = response.getPolicies();
        for (GetLifecycleAction.LifecyclePolicyResponseItem policy : policies) {
            rootMap.put(policy.getLifecyclePolicy().getName(), new PolicyModel(getPhasesModel(policy)));
        }
        new GetPolicyModel(rootMap).toXContent(builder, restRequest);
        return new BytesRestResponse(RestStatus.OK, builder);
    }


    private PhasesModel getPhasesModel(GetLifecycleAction.LifecyclePolicyResponseItem policy) {
        HotModel hotModel = null;
        WarmModel warmModel = null;
        ColdModel coldModel = null;
        DeleteModel deleteModel = null;

        for (Phase phase : policy.getLifecyclePolicy().getPhases().values()) {
            switch (phase.getName()) {
                case "hot":
                    hotModel = getHotModel(phase);
                    break;
                case "warm":
                    warmModel = getWamModel(phase);
                    break;
                case "cold":
                    coldModel = getColdModel(phase);
                    break;
                case "delete":
                    deleteModel = getDeleteModel(phase);
                    break;
                default:
                    break;
            }
        }

        return new PhasesModel(hotModel, warmModel, coldModel, deleteModel);
    }

    private HotModel getHotModel(Phase phase) {
        RolloverAction rollover = (RolloverAction) phase.getActions().get("rollover");
        SetPriorityAction setPriority = (SetPriorityAction) phase.getActions().get("set_priority");
        HotModel.Actions.Rollover rolloverModel = null;
        HotModel.Actions.SetPriority setPriorityModel = null;

        if (rollover != null) {
            new HotModel.Actions.Rollover(rollover.getMaxAge().getStringRep(), rollover.getMaxSize().getStringRep(), rollover.getMaxDocs());
        }
        if(setPriority != null){
            setPriorityModel = new HotModel.Actions.SetPriority(setPriority.getRecoveryPriority().longValue());
        }
        return new HotModel(phase.getMinimumAge().getStringRep(), new HotModel.Actions(rolloverModel, setPriorityModel));
    }

    private WarmModel getWamModel(Phase phase) {
        ForceMergeAction forceMerge = (ForceMergeAction) phase.getActions().get("forcemerge");
        ShrinkAction shrink = (ShrinkAction) phase.getActions().get("shrink");

        WarmModel.Actions.Forcemerge forceMergeModel = null;
        WarmModel.Actions.Shrink shrinkModel = null;

        if (forceMerge != null) {
            forceMergeModel = new WarmModel.Actions.Forcemerge((long) forceMerge.getMaxNumSegments());
        }
        if (shrink != null) {
            shrinkModel = new WarmModel.Actions.Shrink((long) shrink.getNumberOfShards());
        }
        return new WarmModel(phase.getMinimumAge().getStringRep(), new WarmModel.Actions(forceMergeModel, shrinkModel, getAllocateModel(phase)));
    }

    private ColdModel getColdModel(Phase phase){
        return new ColdModel(phase.getMinimumAge().getStringRep(), new ColdModel.Actions(getAllocateModel(phase)));
    }

    private DeleteModel getDeleteModel(Phase phase){
        return new DeleteModel(phase.getMinimumAge().getStringRep(), new DeleteModel.Actions(new InnerDeleteModel()));
    }

    @Nullable
    private AllocateModel getAllocateModel(Phase phase) {
        AllocateAction allocate = (AllocateAction) phase.getActions().get("allocate");
        AllocateModel allocateModel = null;
        if (allocate != null) {
            allocateModel = new AllocateModel(allocate.getNumberOfReplicas() == null ? null : allocate.getNumberOfReplicas().longValue(), allocate.getRequire(), allocate.getInclude(), allocate.getExclude());
        }
        return allocateModel;
    }
}
