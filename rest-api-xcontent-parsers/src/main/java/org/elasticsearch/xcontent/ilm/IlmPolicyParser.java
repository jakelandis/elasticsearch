package org.elasticsearch.xcontent.ilm;

import org.elasticsearch.common.xcontent.GenerateXContentParser;

//TODO: extend toXContent interface and ensure generated XContent implements this
@GenerateXContentParser(file="ilm.put_lifecycle.json", jPath="ilm.put_lifecycle/body", packageName="org.elasticsearch.xcontent.v8.ilm", className = "IlmPolicyParser")
public interface IlmPolicyParser {
}
