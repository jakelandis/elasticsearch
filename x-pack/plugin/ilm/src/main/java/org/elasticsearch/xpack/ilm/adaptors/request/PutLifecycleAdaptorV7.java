package org.elasticsearch.xpack.ilm.adaptors.request;

import org.apache.logging.log4j.LogManager;
import org.elasticsearch.common.logging.DeprecationLogger;
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
import org.elasticsearch.xpack.ilm.action.RestPutLifecycleAction;

import java.util.HashMap;
import java.util.Map;

public class PutLifecycleAdaptorV7 {

    private static final DeprecationLogger deprecationLogger = new DeprecationLogger(LogManager.getLogger(RestPutLifecycleAction.class));

    private PutLifecycleAdaptorV7(){}

    public static PutLifecycleAction.Request fromModel(LifecyclePolicyV7Model model, String name) {
        Map<String, Phase> phases = new HashMap<>();
        if (model.policy.phases.hot != null) {
            phases.put("hot", PutLifecycleAdaptor.getHotPhase(model.policy.phases.hot));
        }
        if (model.policy.phases.warm != null) {
            phases.put("warm", getWarmPhase(model.policy.phases.warm));
        }
        if (model.policy.phases.cold != null) {
            phases.put("cold", PutLifecycleAdaptor.getColdPhase(model.policy.phases.cold));
        }
        if (model.policy.phases.delete != null) {
            phases.put("delete", PutLifecycleAdaptor.getDeletePhase(model.policy.phases.delete));
        }
        return new PutLifecycleAction.Request(new LifecyclePolicy(name, phases));
    }


    @SuppressWarnings("Duplicates")
    private static  Phase getWarmPhase(WarmV7Model warmModel) {
        Map<String, LifecycleAction> actions = new HashMap<>();
        if (warmModel.actions.allocate != null) {
            actions.put(AllocateAction.NAME, PutLifecycleAdaptor.getAllocateAction(warmModel.actions.allocate));
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
        return new Phase("warm", PutLifecycleAdaptor.getTimeValue(warmModel.min_age, "min_age"), actions);
    }

}
