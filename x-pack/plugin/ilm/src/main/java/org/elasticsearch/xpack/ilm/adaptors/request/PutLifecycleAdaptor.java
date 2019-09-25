package org.elasticsearch.xpack.ilm.adaptors.request;

import org.elasticsearch.common.Strings;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.xcontent.generated.ilm.AllocateModel;
import org.elasticsearch.xcontent.generated.ilm.ColdModel;
import org.elasticsearch.xcontent.generated.ilm.DeleteModel;
import org.elasticsearch.xcontent.generated.ilm.HotModel;
import org.elasticsearch.xcontent.generated.ilm.LifecyclePolicyModel;
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
import org.elasticsearch.xpack.core.ilm.action.PutLifecycleAction;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class PutLifecycleAdaptor {

    private PutLifecycleAdaptor(){}

    public static PutLifecycleAction.Request fromModel(LifecyclePolicyModel model, String name) {
        Map<String, Phase> phases = new HashMap<>();
        if (model.policy.phases.hot != null) {
            phases.put("hot", getHotPhase(model.policy.phases.hot));
        }
        if (model.policy.phases.warm != null) {
            phases.put("warm", getWarmPhase(model.policy.phases.warm));
        }
        if (model.policy.phases.cold != null) {
            phases.put("cold", getColdPhase(model.policy.phases.cold));
        }
        if (model.policy.phases.delete != null) {
            phases.put("delete", getDeletePhase(model.policy.phases.delete));
        }
        return new PutLifecycleAction.Request(new LifecyclePolicy(name, phases));
    }

    static Phase getHotPhase(HotModel hotModel) {
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

    static Phase getWarmPhase(WarmModel warmModel) {
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

    static Phase getColdPhase(ColdModel coldModel) {
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

    static Phase getDeletePhase(DeleteModel deleteModel) {
        return new Phase("delete", getTimeValue(deleteModel.min_age, "min_age"),
            Collections.singletonMap("delete", new DeleteAction()));
    }


    static AllocateAction getAllocateAction(AllocateModel allocateModel) {
        return new AllocateAction(
            allocateModel.number_of_replicas == null ? null : allocateModel.number_of_replicas.intValue(),
            allocateModel.include, allocateModel.exclude, allocateModel.require);
    }

    static TimeValue getTimeValue(String value, String key) {
        if (Strings.isNullOrEmpty(value)) {
            return TimeValue.ZERO;
        } else {
            return TimeValue.parseTimeValue(value, key);
        }
    }
}
