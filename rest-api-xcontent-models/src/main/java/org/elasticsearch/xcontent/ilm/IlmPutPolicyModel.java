package org.elasticsearch.xcontent.ilm;

import org.elasticsearch.common.xcontent.GenerateXContentModel;
import org.elasticsearch.common.xcontent.ToXContent;

@GenerateXContentModel(model="model/ilm/put_policy.json", packageName="org.elasticsearch.xcontent.generated.model.ilm", className = "IlmPutPolicyModelImpl")
public interface IlmPutPolicyModel extends ToXContent {
}
