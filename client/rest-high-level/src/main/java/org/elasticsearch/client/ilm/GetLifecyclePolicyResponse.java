/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


package org.elasticsearch.client.ilm;

import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xcontent.generated.ilm.GetPolicyModel;
import org.elasticsearch.xcontent.generated.ilm.PolicyModel;

import java.io.IOException;
import java.util.Objects;

import static org.elasticsearch.common.xcontent.XContentParserUtils.ensureExpectedToken;

public class GetLifecyclePolicyResponse {

    private final ImmutableOpenMap<String, PolicyModel> policies;


    public GetLifecyclePolicyResponse(ImmutableOpenMap<String, PolicyModel> policies) {
        this.policies = policies;
    }

    public ImmutableOpenMap<String, PolicyModel> getPolicies() {
        return policies;
    }


    public static GetLifecyclePolicyResponse fromXContent(XContentParser parser) throws IOException {
        ImmutableOpenMap.Builder<String, PolicyModel> policies = ImmutableOpenMap.builder();


        GetPolicyModel model = GetPolicyModel.PARSER.parse(parser, null);
        model.objectMap.forEach((k,v) -> {
            policies.put(k,v.policy);
            });

        return new GetLifecyclePolicyResponse(policies.build());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GetLifecyclePolicyResponse that = (GetLifecyclePolicyResponse) o;
        return Objects.equals(getPolicies(), that.getPolicies());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getPolicies());
    }
}
