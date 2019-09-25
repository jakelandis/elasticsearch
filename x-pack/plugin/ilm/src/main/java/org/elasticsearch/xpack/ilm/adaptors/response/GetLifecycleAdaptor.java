package org.elasticsearch.xpack.ilm.adaptors.response;

import org.elasticsearch.common.Nullable;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GetLifecycleAdaptor {
    private GetLifecycleAdaptor() {
    }

    public static LifecyclePolicyWithMetadataModel toModel(GetLifecycleAction.Response response) {
        List<GetLifecycleAction.LifecyclePolicyResponseItem> policies = response.getPolicies();
        Map<String, LifecyclePolicyWithMetadataModel.ObjectMapItem> rootMap = new HashMap<>();
        for (GetLifecycleAction.LifecyclePolicyResponseItem policy : policies) {
            rootMap.put(policy.getLifecyclePolicy().getName(), new LifecyclePolicyWithMetadataModel.ObjectMapItem(policy.getVersion(), policy.getModifiedDate(), new PolicyModel(getPhasesModel(policy))));
        }
        return new LifecyclePolicyWithMetadataModel(rootMap);
    }

    static PhasesModel getPhasesModel(GetLifecycleAction.LifecyclePolicyResponseItem policy) {
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

    static HotModel getHotModel(Phase phase) {
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

    static WarmModel getWamModel(Phase phase) {
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

    static ColdModel getColdModel(Phase phase) {
        FreezeAction freeze = (FreezeAction) phase.getActions().get(FreezeAction.NAME);
        ColdModel.Actions.Freeze freezeModel = null;
        if (freeze != null) {
            freezeModel = new ColdModel.Actions.Freeze();
        }
        return new ColdModel(phase.getMinimumAge().getStringRep(), new ColdModel.Actions(getAllocateModel(phase), getUnfollowModel(phase), freezeModel));
    }

    static DeleteModel getDeleteModel(Phase phase) {
        return new DeleteModel(phase.getMinimumAge().getStringRep(), new DeleteModel.Actions(new InnerDeleteModel()));
    }

    @Nullable
    static AllocateModel getAllocateModel(Phase phase) {
        AllocateAction allocate = (AllocateAction) phase.getActions().get(AllocateAction.NAME);
        AllocateModel allocateModel = null;
        if (allocate != null) {
            allocateModel = new AllocateModel(allocate.getNumberOfReplicas() == null ? null : allocate.getNumberOfReplicas().longValue(), allocate.getRequire(), allocate.getInclude(), allocate.getExclude());
        }
        return allocateModel;
    }

    @Nullable
    static UnfollowModel getUnfollowModel(Phase phase) {
        return phase.getActions().get(UnfollowAction.NAME) != null ? new UnfollowModel() : null;
    }
}
