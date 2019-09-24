/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */

package org.elasticsearch.xpack.ilm.action;

import org.apache.logging.log4j.LogManager;
import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.Nullable;
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
import org.elasticsearch.xcontent.generated.ilm.AllocateModel;
import org.elasticsearch.xcontent.generated.ilm.ColdModel;
import org.elasticsearch.xcontent.generated.ilm.DeleteModel;
import org.elasticsearch.xcontent.generated.ilm.GetPolicyModel;
import org.elasticsearch.xcontent.generated.ilm.HotModel;
import org.elasticsearch.xcontent.generated.ilm.InnerDeleteModel;
import org.elasticsearch.xcontent.generated.ilm.PhasesModel;
import org.elasticsearch.xcontent.generated.ilm.PolicyModel;
import org.elasticsearch.xcontent.generated.ilm.UnfollowModel;
import org.elasticsearch.xcontent.generated.ilm.WarmModel;
import org.elasticsearch.xcontent.generated.v7.ilm.GetPolicyV7Model;
import org.elasticsearch.xcontent.generated.v7.ilm.PhasesV7Model;
import org.elasticsearch.xcontent.generated.v7.ilm.PolicyV7Model;
import org.elasticsearch.xcontent.generated.v7.ilm.WarmV7Model;
import org.elasticsearch.xpack.core.ilm.AllocateAction;
import org.elasticsearch.xpack.core.ilm.ForceMergeAction;
import org.elasticsearch.xpack.core.ilm.FreezeAction;
import org.elasticsearch.xpack.core.ilm.Phase;
import org.elasticsearch.xpack.core.ilm.ReadOnlyAction;
import org.elasticsearch.xpack.core.ilm.RolloverAction;
import org.elasticsearch.xpack.core.ilm.SetPriorityAction;
import org.elasticsearch.xpack.core.ilm.ShrinkAction;
import org.elasticsearch.xpack.core.ilm.UnfollowAction;
import org.elasticsearch.xpack.core.ilm.action.GetLifecycleAction;

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
        boolean isVersion7 = restRequest.isVersion7();
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
        List<GetLifecycleAction.LifecyclePolicyResponseItem> policies = response.getPolicies();
        if (isVersion7) {
            Map<String, GetPolicyV7Model.ObjectMapItem> rootMap = new HashMap<>();
            for (GetLifecycleAction.LifecyclePolicyResponseItem policy : policies) {
                rootMap.put(policy.getLifecyclePolicy().getName(), new GetPolicyV7Model.ObjectMapItem(policy.getVersion(), policy.getModifiedDate(), new PolicyV7Model(getPhasesModelV7(policy))));
            }
            new GetPolicyV7Model(rootMap).toXContent(builder, restRequest);
        } else {
            Map<String, GetPolicyModel.ObjectMapItem> rootMap = new HashMap<>();
            for (GetLifecycleAction.LifecyclePolicyResponseItem policy : policies) {
                rootMap.put(policy.getLifecyclePolicy().getName(), new GetPolicyModel.ObjectMapItem(policy.getVersion(), policy.getModifiedDate(), new PolicyModel(getPhasesModel(policy))));
            }
            new GetPolicyModel(rootMap).toXContent(builder, restRequest);
        }
        return new BytesRestResponse(RestStatus.OK, builder);
    }

    private PhasesV7Model getPhasesModelV7(GetLifecycleAction.LifecyclePolicyResponseItem policy) {
        HotModel hotModel = null;
        WarmV7Model warmModel = null;
        ColdModel coldModel = null;
        DeleteModel deleteModel = null;

        for (Phase phase : policy.getLifecyclePolicy().getPhases().values()) {
            switch (phase.getName()) {
                case "hot":
                    hotModel = getHotModel(phase);
                    break;
                case "warm":
                    warmModel = getWamModelV7(phase);
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
        return new PhasesV7Model(hotModel, warmModel, coldModel, deleteModel);
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
        RolloverAction rollover = (RolloverAction) phase.getActions().get(RolloverAction.NAME);
        SetPriorityAction setPriority = (SetPriorityAction) phase.getActions().get(SetPriorityAction.NAME);

        HotModel.Actions.Rollover rolloverModel = null;
        HotModel.Actions.SetPriority setPriorityModel = null;


        if (rollover != null) {
            rolloverModel = new HotModel.Actions.Rollover(rollover.getMaxAge().getStringRep(), rollover.getMaxSize().getStringRep(), rollover.getMaxDocs());
        }
        if (setPriority != null) {
            setPriorityModel = new HotModel.Actions.SetPriority(setPriority.getRecoveryPriority().longValue());
        }
        return new HotModel(phase.getMinimumAge().getStringRep(), new HotModel.Actions(rolloverModel, setPriorityModel, getUnfollowModel(phase)));
    }

    private WarmV7Model getWamModelV7(Phase phase) {
        ForceMergeAction forceMerge = (ForceMergeAction) phase.getActions().get(ForceMergeAction.NAME);
        ShrinkAction shrink = (ShrinkAction) phase.getActions().get(ShrinkAction.NAME);
        ReadOnlyAction readOnly = (ReadOnlyAction) phase.getActions().get(ReadOnlyAction.NAME);
        WarmV7Model.Actions.Forcemerge forceMergeModel = null;
        WarmV7Model.Actions.Shrink shrinkModel = null;
        WarmV7Model.Actions.Readonly readOnlyModel = null;

        if (forceMerge != null) {
            //TODO: should this return both the deprecated and new ? ... or just the deprecated ?
            forceMergeModel = new WarmV7Model.Actions.Forcemerge((long) forceMerge.getMaxNumSegments(), (long) forceMerge.getMaxNumSegments());
            deprecationLogger.deprecatedAndMaybeLog("ilm_max_num_segments", "[max_num_segments] has been deprecated please use [max_number_segments]");
        }

        if (shrink != null) {
            shrinkModel = new WarmV7Model.Actions.Shrink((long) shrink.getNumberOfShards());
        }
        if (readOnly != null) {
            readOnlyModel = new WarmV7Model.Actions.Readonly();
        }
        return new WarmV7Model(phase.getMinimumAge().getStringRep(), new WarmV7Model.Actions(forceMergeModel, shrinkModel, getAllocateModel(phase), getUnfollowModel(phase), readOnlyModel));
    }

    private WarmModel getWamModel(Phase phase) {
        ForceMergeAction forceMerge = (ForceMergeAction) phase.getActions().get(ForceMergeAction.NAME);
        ShrinkAction shrink = (ShrinkAction) phase.getActions().get(ShrinkAction.NAME);
        ReadOnlyAction readOnly = (ReadOnlyAction) phase.getActions().get(ReadOnlyAction.NAME);
        WarmModel.Actions.Forcemerge forceMergeModel = null;
        WarmModel.Actions.Shrink shrinkModel = null;
        WarmModel.Actions.Readonly readOnlyModel = null;

        if (forceMerge != null) {
            forceMergeModel = new WarmModel.Actions.Forcemerge((long) forceMerge.getMaxNumSegments());
        }
        if (shrink != null) {
            shrinkModel = new WarmModel.Actions.Shrink((long) shrink.getNumberOfShards());
        }
        if (readOnly != null) {
            readOnlyModel = new WarmModel.Actions.Readonly();
        }
        return new WarmModel(phase.getMinimumAge().getStringRep(), new WarmModel.Actions(forceMergeModel, shrinkModel, getAllocateModel(phase), getUnfollowModel(phase), readOnlyModel));
    }

    private ColdModel getColdModel(Phase phase) {
        FreezeAction freeze = (FreezeAction) phase.getActions().get(FreezeAction.NAME);
        ColdModel.Actions.Freeze freezeModel = null;
        if (freeze != null) {
            freezeModel = new ColdModel.Actions.Freeze();
        }
        return new ColdModel(phase.getMinimumAge().getStringRep(), new ColdModel.Actions(getAllocateModel(phase), getUnfollowModel(phase), freezeModel));
    }

    private DeleteModel getDeleteModel(Phase phase) {
        return new DeleteModel(phase.getMinimumAge().getStringRep(), new DeleteModel.Actions(new InnerDeleteModel()));
    }

    @Nullable
    private AllocateModel getAllocateModel(Phase phase) {
        AllocateAction allocate = (AllocateAction) phase.getActions().get(AllocateAction.NAME);
        AllocateModel allocateModel = null;
        if (allocate != null) {
            allocateModel = new AllocateModel(allocate.getNumberOfReplicas() == null ? null : allocate.getNumberOfReplicas().longValue(), allocate.getRequire(), allocate.getInclude(), allocate.getExclude());
        }
        return allocateModel;
    }

    @Nullable
    private UnfollowModel getUnfollowModel(Phase phase) {
        return phase.getActions().get(UnfollowAction.NAME) != null ? new UnfollowModel() : null;
    }
}
