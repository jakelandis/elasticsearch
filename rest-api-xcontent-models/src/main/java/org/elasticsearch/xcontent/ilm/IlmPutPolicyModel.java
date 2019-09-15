package org.elasticsearch.xcontent.ilm;

import org.elasticsearch.common.xcontent.GenerateXContentModel;

//TODO: extend toXContent interface and ensure generated XContent implements this
@GenerateXContentModel(model="model/ilm/put_policy.json", packageName="org.elasticsearch.xcontent.ilm", className = "IlmPutPolicyModelImpl")
public interface IlmPutPolicyModel {
}
