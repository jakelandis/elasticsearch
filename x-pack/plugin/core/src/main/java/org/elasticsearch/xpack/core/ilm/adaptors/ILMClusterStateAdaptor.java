package org.elasticsearch.xpack.core.ilm.adaptors;


import org.elasticsearch.xcontent.generated.ilm.PolicyModel;
import org.elasticsearch.xcontent.generated.internal.ilm.ClusterStateIlmPolicyModel;
import org.elasticsearch.xpack.core.ilm.IndexLifecycleMetadata;

import java.util.HashMap;
import java.util.Map;

public class ILMClusterStateAdaptor {
    private ILMClusterStateAdaptor() {
    }


    static public ClusterStateIlmPolicyModel toModel(IndexLifecycleMetadata clusterMetaData) {
        Map<String, ClusterStateIlmPolicyModel.ObjectMapItem> policyMap = new HashMap<>();
        clusterMetaData.getPolicyMetadatas().forEach((name, lifecyclePolicyMetadata) -> {
            PolicyModel policyModel = LifecyclePolicyAdaptor.toModel(lifecyclePolicyMetadata.getPolicy());
            policyMap.put(name, new ClusterStateIlmPolicyModel.ObjectMapItem(lifecyclePolicyMetadata.getVersion(),
                lifecyclePolicyMetadata.getModifiedDate(), lifecyclePolicyMetadata.getModifiedDateString(), lifecyclePolicyMetadata.getHeaders(), policyModel));
        });

        return new ClusterStateIlmPolicyModel(clusterMetaData.getOperationMode().toString(), policyMap);
    }

    //TODO: fromModel

}
