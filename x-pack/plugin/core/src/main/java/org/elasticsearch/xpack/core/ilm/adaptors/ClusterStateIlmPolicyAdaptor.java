package org.elasticsearch.xpack.core.ilm.adaptors;


import org.elasticsearch.xcontent.generated.ilm.PolicyModel;
import org.elasticsearch.xcontent.generated.internal.ilm.ClusterStateIlmPolicyModel;
import org.elasticsearch.xpack.core.ilm.IndexLifecycleMetadata;
import org.elasticsearch.xpack.core.ilm.LifecyclePolicy;
import org.elasticsearch.xpack.core.ilm.LifecyclePolicyMetadata;
import org.elasticsearch.xpack.core.ilm.OperationMode;

import java.util.HashMap;
import java.util.Map;

public class ClusterStateIlmPolicyAdaptor {
    private ClusterStateIlmPolicyAdaptor() {
    }


    static public ClusterStateIlmPolicyModel toModel(IndexLifecycleMetadata clusterMetaData) {
        Map<String, ClusterStateIlmPolicyModel.Policies.ObjectMapItem> policyMap = new HashMap<>();
        clusterMetaData.getPolicyMetadatas().forEach((name, lifecyclePolicyMetadata) -> {
            PolicyModel policyModel = LifecyclePolicyAdaptor.toModel(lifecyclePolicyMetadata.getPolicy());
            policyMap.put(name, new ClusterStateIlmPolicyModel.Policies.ObjectMapItem(lifecyclePolicyMetadata.getVersion(),
                lifecyclePolicyMetadata.getModifiedDate(), lifecyclePolicyMetadata.getModifiedDateString(), lifecyclePolicyMetadata.getHeaders(), policyModel));
        });

        return new ClusterStateIlmPolicyModel(clusterMetaData.getOperationMode().toString(), new ClusterStateIlmPolicyModel.Policies(policyMap));
    }

    static public IndexLifecycleMetadata fromModel(ClusterStateIlmPolicyModel model) {
        Map<String, LifecyclePolicyMetadata> policies = new HashMap<>();
        for (Map.Entry<String, ClusterStateIlmPolicyModel.Policies.ObjectMapItem> entry : model.policies.objectMap.entrySet()) {
            String policyName = entry.getKey();
            ClusterStateIlmPolicyModel.Policies.ObjectMapItem item = entry.getValue();
            LifecyclePolicy policy = LifecyclePolicyAdaptor.fromModel(item.policy, policyName);
            policies.put(policyName, new LifecyclePolicyMetadata(policy, item.headers, item.version, item.modified_date));
        }

        return new IndexLifecycleMetadata(policies, OperationMode.fromString(model.operation_mode));
    }

}
