/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */

package org.elasticsearch.xpack.ilm.action;

import org.elasticsearch.client.node.NodeClient;
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
import org.elasticsearch.xcontent.generated.ilm.PutPolicyModel;
import org.elasticsearch.xcontent.generated.ilm.WarmModel;
import org.elasticsearch.xpack.core.ilm.AllocateAction;
import org.elasticsearch.xpack.core.ilm.DeleteAction;
import org.elasticsearch.xpack.core.ilm.ForceMergeAction;
import org.elasticsearch.xpack.core.ilm.LifecycleAction;
import org.elasticsearch.xpack.core.ilm.LifecyclePolicy;
import org.elasticsearch.xpack.core.ilm.Phase;
import org.elasticsearch.xpack.core.ilm.RolloverAction;
import org.elasticsearch.xpack.core.ilm.ShrinkAction;
import org.elasticsearch.xpack.core.ilm.action.PutLifecycleAction;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class RestPutLifecycleAction extends BaseRestHandler {

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
            PutLifecycleAction.Request putLifecycleRequest = toTransportRequest(lifecycleName, parser);
            putLifecycleRequest.timeout(restRequest.paramAsTime("timeout", putLifecycleRequest.timeout()));
            putLifecycleRequest.masterNodeTimeout(restRequest.paramAsTime("master_timeout", putLifecycleRequest.masterNodeTimeout()));
            return channel -> client.execute(PutLifecycleAction.INSTANCE, putLifecycleRequest, new RestToXContentListener<>(channel));
        }
    }

    private PutLifecycleAction.Request toTransportRequest(String name, XContentParser parser) {
        PutPolicyModel model = PutPolicyModel.PARSER.apply(parser, null);
        Map<String, Phase> phases = new HashMap<>();
        phases.put("hot", getHotPhase(model.policy.phases.hot));
        phases.put("warm", getWarmPhase(model.policy.phases.warm));
        phases.put("cold", getColdPhase(model.policy.phases.cold));
        phases.put("delete", getDeletePhase(model.policy.phases.delete));
        return new PutLifecycleAction.Request(new LifecyclePolicy(name, phases));
    }

    private Phase getHotPhase(HotModel hotModel) {
        Map<String, LifecycleAction> actions = new HashMap<>();
        actions.put("rollover",
            new RolloverAction(ByteSizeValue.parseBytesSizeValue(hotModel.actions.rollover.max_size, "max_size"),
                TimeValue.parseTimeValue(hotModel.actions.rollover.max_age, "max_age"),
                hotModel.actions.rollover.max_docs));
        return new Phase("hot", TimeValue.parseTimeValue(hotModel.min_age, "min_age"), actions);
    }

    private Phase getWarmPhase(WarmModel warmModel) {
        Map<String, LifecycleAction> actions = new HashMap<>();
        actions.put("allocate", getAllocateAction(warmModel.actions.allocate));
        actions.put("forcemerge", new ForceMergeAction(warmModel.actions.forcemerge.max_num_segments.intValue()));
        actions.put("shrink", new ShrinkAction(warmModel.actions.shrink.number_of_shards.intValue()));
        return new Phase("warm", TimeValue.parseTimeValue(warmModel.min_age, "min_age"), actions);
    }

    private Phase getColdPhase(ColdModel coldModel) {
        Map<String, LifecycleAction> actions = new HashMap<>();
        actions.put("allocate", getAllocateAction(coldModel.actions.allocate));
        return new Phase("cold", TimeValue.parseTimeValue(coldModel.min_age, "min_age"), actions);
    }

    private Phase getDeletePhase(DeleteModel deleteModel) {
        return new Phase("delete", TimeValue.parseTimeValue(deleteModel.min_age, "min_age"),
            Collections.singletonMap("delete", new DeleteAction()));
    }


    private AllocateAction getAllocateAction(AllocateModel allocateModel) {
        //TODO: fix this .. need to figure out dynamic key names
        return new AllocateAction(allocateModel.number_of_replicas.intValue(), null, null, null);
    }
}
