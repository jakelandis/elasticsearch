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

import org.elasticsearch.client.TimedRequest;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.xcontent.ToXContentObject;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.xcontent.generated.ilm.PolicyModel;

import java.io.IOException;
import java.util.Objects;

public class PutLifecyclePolicyRequest extends TimedRequest {

    private final PolicyModel policy;
    private final String name;

    public PutLifecyclePolicyRequest(PolicyModel policy, String name) {
        if (policy == null) {
            throw new IllegalArgumentException("policy definition cannot be null");
        }
        if (Strings.isNullOrEmpty(name)) {
            throw new IllegalArgumentException("policy name must be present");
        }
        this.policy = policy;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public PolicyModel getLifecyclePolicy() {
        return policy;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PutLifecyclePolicyRequest that = (PutLifecyclePolicyRequest) o;
        return Objects.equals(getLifecyclePolicy(), that.getLifecyclePolicy());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getLifecyclePolicy());
    }
}
