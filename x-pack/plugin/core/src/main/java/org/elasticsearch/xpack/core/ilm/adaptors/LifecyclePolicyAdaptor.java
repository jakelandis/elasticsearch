package org.elasticsearch.xpack.core.ilm.adaptors;

import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.xcontent.generated.ilm.AllocateModel;
import org.elasticsearch.xcontent.generated.ilm.ColdModel;
import org.elasticsearch.xcontent.generated.ilm.DeleteModel;
import org.elasticsearch.xcontent.generated.ilm.HotModel;
import org.elasticsearch.xcontent.generated.ilm.InnerDeleteModel;
import org.elasticsearch.xcontent.generated.ilm.LifecyclePolicyModel;
import org.elasticsearch.xcontent.generated.ilm.PhasesModel;
import org.elasticsearch.xcontent.generated.ilm.PolicyModel;
import org.elasticsearch.xcontent.generated.ilm.UnfollowModel;
import org.elasticsearch.xcontent.generated.ilm.WarmModel;
import org.elasticsearch.xpack.core.ilm.AllocateAction;
import org.elasticsearch.xpack.core.ilm.DeleteAction;
import org.elasticsearch.xpack.core.ilm.ForceMergeAction;
import org.elasticsearch.xpack.core.ilm.FreezeAction;
import org.elasticsearch.xpack.core.ilm.LifecycleAction;
import org.elasticsearch.xpack.core.ilm.LifecyclePolicy;
import org.elasticsearch.xpack.core.ilm.Phase;
import org.elasticsearch.xpack.core.ilm.ReadOnlyAction;
import org.elasticsearch.xpack.core.ilm.RolloverAction;
import org.elasticsearch.xpack.core.ilm.SetPriorityAction;
import org.elasticsearch.xpack.core.ilm.ShrinkAction;
import org.elasticsearch.xpack.core.ilm.UnfollowAction;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class LifecyclePolicyAdaptor {

    private LifecyclePolicyAdaptor() {
    }

    /**
     * FROM MODEL
     */
    public static LifecyclePolicy fromModel(LifecyclePolicyModel model, String name) {
        return fromModel(model.policy, name);
    }

    public static LifecyclePolicy fromModel(PolicyModel model, String name) {
        Map<String, Phase> phases = new HashMap<>();
        if (model.phases.hot != null) {
            phases.put("hot", getHotPhase(model.phases.hot));
        }
        if (model.phases.warm != null) {
            phases.put("warm", getWarmPhase(model.phases.warm));
        }
        if (model.phases.cold != null) {
            phases.put("cold", getColdPhase(model.phases.cold));
        }
        if (model.phases.delete != null) {
            phases.put("delete", getDeletePhase(model.phases.delete));
        }
        return new LifecyclePolicy(name, phases);
    }

    public static Phase getHotPhase(HotModel hotModel) {
        Map<String, LifecycleAction> actions = new HashMap<>();
        if (hotModel.actions.rollover != null) {
            actions.put(RolloverAction.NAME, new RolloverAction(ByteSizeValue.parseBytesSizeValue(hotModel.actions.rollover.max_size, "max_size"),
                getTimeValue(hotModel.actions.rollover.max_age, "max_age"),
                hotModel.actions.rollover.max_docs));
        }
        if (hotModel.actions.set_priority != null) {
            actions.put(SetPriorityAction.NAME, new SetPriorityAction(hotModel.actions.set_priority.priority.intValue()));
        }
        if (hotModel.actions.unfollow != null) {
            actions.put(UnfollowAction.NAME, new UnfollowAction());
        }
        return new Phase("hot", getTimeValue(hotModel.min_age, "min_age"), actions);
    }

    public static Phase getWarmPhase(WarmModel warmModel) {
        Map<String, LifecycleAction> actions = new HashMap<>();
        if (warmModel.actions.allocate != null) {
            actions.put(AllocateAction.NAME, getAllocateAction(warmModel.actions.allocate));
        }
        if (warmModel.actions.forcemerge != null) {
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
        return new Phase("warm", getTimeValue(warmModel.min_age, "min_age"), actions);
    }

    public static Phase getColdPhase(ColdModel coldModel) {
        Map<String, LifecycleAction> actions = new HashMap<>();
        if (coldModel.actions.allocate != null) {
            actions.put(AllocateAction.NAME, getAllocateAction(coldModel.actions.allocate));
        }
        if (coldModel.actions.freeze != null) {
            actions.put(FreezeAction.NAME, new FreezeAction());
        }
        if (coldModel.actions.unfollow != null) {
            actions.put(UnfollowAction.NAME, new UnfollowAction());
        }
        return new Phase("cold", getTimeValue(coldModel.min_age, "min_age"), actions);
    }

    public static Phase getDeletePhase(DeleteModel deleteModel) {
        return new Phase("delete", getTimeValue(deleteModel.min_age, "min_age"),
            Collections.singletonMap("delete", new DeleteAction()));
    }


    public static AllocateAction getAllocateAction(AllocateModel allocateModel) {
        return new AllocateAction(
            allocateModel.number_of_replicas == null ? null : allocateModel.number_of_replicas.intValue(),
            allocateModel.include, allocateModel.exclude, allocateModel.require);
    }

    public static TimeValue getTimeValue(String value, String key) {
        if (Strings.isNullOrEmpty(value)) {
            return TimeValue.ZERO;
        } else {
            return TimeValue.parseTimeValue(value, key);
        }
    }

    /**
     * TO MODEL
     */

    public static PolicyModel toModel(LifecyclePolicy policy) {
        return new PolicyModel(getPhasesModel(policy));
    }

    static PhasesModel getPhasesModel(LifecyclePolicy policy) {
        HotModel hotModel = null;
        WarmModel warmModel = null;
        ColdModel coldModel = null;
        DeleteModel deleteModel = null;

        for (Phase phase : policy.getPhases().values()) {
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

    public static HotModel getHotModel(Phase phase) {
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

    public static WarmModel getWamModel(Phase phase) {
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

    public static ColdModel getColdModel(Phase phase) {
        FreezeAction freeze = (FreezeAction) phase.getActions().get(FreezeAction.NAME);
        ColdModel.Actions.Freeze freezeModel = null;
        if (freeze != null) {
            freezeModel = new ColdModel.Actions.Freeze();
        }
        return new ColdModel(phase.getMinimumAge().getStringRep(), new ColdModel.Actions(getAllocateModel(phase), getUnfollowModel(phase), freezeModel));
    }

    public static DeleteModel getDeleteModel(Phase phase) {
        return new DeleteModel(phase.getMinimumAge().getStringRep(), new DeleteModel.Actions(new InnerDeleteModel()));
    }

    @Nullable
    public static AllocateModel getAllocateModel(Phase phase) {
        AllocateAction allocate = (AllocateAction) phase.getActions().get(AllocateAction.NAME);
        AllocateModel allocateModel = null;
        if (allocate != null) {
            allocateModel = new AllocateModel(allocate.getNumberOfReplicas() == null ? null : allocate.getNumberOfReplicas().longValue(), allocate.getRequire(), allocate.getInclude(), allocate.getExclude());
        }
        return allocateModel;
    }

    @Nullable
    public static UnfollowModel getUnfollowModel(Phase phase) {
        return phase.getActions().get(UnfollowAction.NAME) != null ? new UnfollowModel() : null;
    }
}
