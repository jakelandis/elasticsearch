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
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.action.RestToXContentListener;
import org.elasticsearch.xcontent.generated.ilm.AllocateModel;
import org.elasticsearch.xcontent.generated.ilm.ColdModel;
import org.elasticsearch.xcontent.generated.ilm.DeleteModel;
import org.elasticsearch.xcontent.generated.ilm.HotModel;
import org.elasticsearch.xcontent.generated.ilm.LifecyclePolicyModel;
import org.elasticsearch.xcontent.generated.ilm.WarmModel;
import org.elasticsearch.xcontent.generated.v7.ilm.LifecyclePolicyV7Model;
import org.elasticsearch.xcontent.generated.v7.ilm.WarmV7Model;
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

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class RestPutLifecycleAction extends BaseRestHandler {

    private static final DeprecationLogger deprecationLogger = new DeprecationLogger(LogManager.getLogger(RestPutLifecycleAction.class));

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
            return toTransportRequestV7(LifecyclePolicyV7Model.PARSER.apply(parser, null), name);
        } else {
            return toTransportRequest(LifecyclePolicyModel.PARSER.apply(parser, null), name);
        }
    }

    private PutLifecycleAction.Request toTransportRequest(LifecyclePolicyModel model, String name) {
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

    private PutLifecycleAction.Request toTransportRequestV7(LifecyclePolicyV7Model v7Model, String name) {
        Map<String, Phase> phases = new HashMap<>();
        if (v7Model.policy.phases.hot != null) {
            phases.put("hot", getHotPhase(v7Model.policy.phases.hot));
        }
        if (v7Model.policy.phases.warm != null) {
            phases.put("warm", getWarmV7Phase(v7Model.policy.phases.warm));
        }
        if (v7Model.policy.phases.cold != null) {
            phases.put("cold", getColdPhase(v7Model.policy.phases.cold));
        }
        if (v7Model.policy.phases.delete != null) {
            phases.put("delete", getDeletePhase(v7Model.policy.phases.delete));
        }
        return new PutLifecycleAction.Request(new LifecyclePolicy(name, phases));
    }

    private Phase getHotPhase(HotModel hotModel) {
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

    private Phase getWarmV7Phase(WarmV7Model warmV7Model) {
        Map<String, LifecycleAction> actions = new HashMap<>();
        if (warmV7Model.actions.allocate != null) {
            actions.put(AllocateAction.NAME, getAllocateAction(warmV7Model.actions.allocate));
        }
        if (warmV7Model.actions.forcemerge != null && warmV7Model.actions.forcemerge.max_num_segments != null) {
            actions.put(ForceMergeAction.NAME, new ForceMergeAction(warmV7Model.actions.forcemerge.max_num_segments.intValue()));
            deprecationLogger.deprecatedAndMaybeLog("ilm_max_num_segments", "[max_num_segments] has been deprecated please use [max_number_segments]");
        } else if (warmV7Model.actions.forcemerge != null && warmV7Model.actions.forcemerge.max_number_segments != null) {
            actions.put(ForceMergeAction.NAME, new ForceMergeAction(warmV7Model.actions.forcemerge.max_number_segments.intValue()));
        }
        if (warmV7Model.actions.shrink != null) {
            actions.put(ShrinkAction.NAME, new ShrinkAction(warmV7Model.actions.shrink.number_of_shards.intValue()));
        }
        if (warmV7Model.actions.readonly != null) {
            actions.put(ReadOnlyAction.NAME, new ReadOnlyAction());
        }
        if (warmV7Model.actions.unfollow != null) {
            actions.put(UnfollowAction.NAME, new UnfollowAction());
        }
        return new Phase("warm", getTimeValue(warmV7Model.min_age, "min_age"), actions);
    }

    private Phase getWarmPhase(WarmModel warmModel) {
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

    private Phase getColdPhase(ColdModel coldModel) {
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

    private Phase getDeletePhase(DeleteModel deleteModel) {
        return new Phase("delete", getTimeValue(deleteModel.min_age, "min_age"),
            Collections.singletonMap("delete", new DeleteAction()));
    }


    private AllocateAction getAllocateAction(AllocateModel allocateModel) {
        return new AllocateAction(
            allocateModel.number_of_replicas == null ? null : allocateModel.number_of_replicas.intValue(),
            allocateModel.include, allocateModel.exclude, allocateModel.require);
    }

    private TimeValue getTimeValue(String value, String key) {
        if (Strings.isNullOrEmpty(value)) {
            return TimeValue.ZERO;
        } else {
            return TimeValue.parseTimeValue(value, key);
        }
    }


}
