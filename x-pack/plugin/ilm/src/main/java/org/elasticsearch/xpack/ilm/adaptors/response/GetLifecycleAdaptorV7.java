package org.elasticsearch.xpack.ilm.adaptors.response;

import org.apache.logging.log4j.LogManager;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.logging.DeprecationLogger;
import org.elasticsearch.xcontent.generated.ilm.AllocateModel;
import org.elasticsearch.xcontent.generated.ilm.ColdModel;
import org.elasticsearch.xcontent.generated.ilm.DeleteModel;
import org.elasticsearch.xcontent.generated.ilm.HotModel;
import org.elasticsearch.xcontent.generated.ilm.InnerDeleteModel;
import org.elasticsearch.xcontent.generated.ilm.LifecyclePolicyWithMetadataModel;
import org.elasticsearch.xcontent.generated.ilm.PhasesModel;
import org.elasticsearch.xcontent.generated.ilm.PolicyModel;
import org.elasticsearch.xcontent.generated.ilm.UnfollowModel;
import org.elasticsearch.xcontent.generated.ilm.WarmModel;
import org.elasticsearch.xcontent.generated.v7.ilm.LifecyclePolicyWithMetadataV7Model;
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
import org.elasticsearch.xpack.ilm.action.RestGetLifecycleAction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GetLifecycleAdaptorV7 {

    private static final DeprecationLogger deprecationLogger = new DeprecationLogger(LogManager.getLogger(RestGetLifecycleAction.class));

    private GetLifecycleAdaptorV7() {
    }

    public static LifecyclePolicyWithMetadataV7Model toModel(GetLifecycleAction.Response response) {
        List<GetLifecycleAction.LifecyclePolicyResponseItem> policies = response.getPolicies();
        Map<String, LifecyclePolicyWithMetadataV7Model.ObjectMapItem> rootMap = new HashMap<>();
        for (GetLifecycleAction.LifecyclePolicyResponseItem policy : policies) {
            rootMap.put(policy.getLifecyclePolicy().getName(), new LifecyclePolicyWithMetadataV7Model.ObjectMapItem(policy.getVersion(), policy.getModifiedDate(), new PolicyV7Model(getPhasesModel(policy))));
        }
        return new LifecyclePolicyWithMetadataV7Model(rootMap);
    }

    private static PhasesV7Model getPhasesModel(GetLifecycleAction.LifecyclePolicyResponseItem policy) {
        HotModel hotModel = null;
        WarmV7Model warmModel = null;
        ColdModel coldModel = null;
        DeleteModel deleteModel = null;

        for (Phase phase : policy.getLifecyclePolicy().getPhases().values()) {
            switch (phase.getName()) {
                case "hot":
                    hotModel = GetLifecycleAdaptor.getHotModel(phase);
                    break;
                case "warm":
                    warmModel = getWamModel(phase);
                    break;
                case "cold":
                    coldModel = GetLifecycleAdaptor.getColdModel(phase);
                    break;
                case "delete":
                    deleteModel = GetLifecycleAdaptor.getDeleteModel(phase);
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
        return new WarmV7Model(phase.getMinimumAge().getStringRep(), new WarmV7Model.Actions(forceMergeModel, shrinkModel, GetLifecycleAdaptor.getAllocateModel(phase), GetLifecycleAdaptor.getUnfollowModel(phase), readOnlyModel));
    }


}
