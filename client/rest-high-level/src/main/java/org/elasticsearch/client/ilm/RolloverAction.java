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

import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.unit.TimeValue;

import java.util.Objects;


public class RolloverAction implements LifecycleAction {
    public static final String NAME = "rollover";

    private final ByteSizeValue maxSize;
    private final Long maxDocs;
    private final TimeValue maxAge;

    public RolloverAction(ByteSizeValue maxSize, TimeValue maxAge, Long maxDocs) {
        if (maxSize == null && maxAge == null && maxDocs == null) {
            throw new IllegalArgumentException("At least one rollover condition must be set.");
        }
        this.maxSize = maxSize;
        this.maxAge = maxAge;
        this.maxDocs = maxDocs;
    }
    public ByteSizeValue getMaxSize() {
        return maxSize;
    }

    public TimeValue getMaxAge() {
        return maxAge;
    }

    public Long getMaxDocs() {
        return maxDocs;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public int hashCode() {
        return Objects.hash(maxSize, maxAge, maxDocs);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        RolloverAction other = (RolloverAction) obj;
        return Objects.equals(maxSize, other.maxSize) &&
            Objects.equals(maxAge, other.maxAge) &&
            Objects.equals(maxDocs, other.maxDocs);
    }

    @Override
    public String toString() {
        return "RolloverAction{" +
            "maxSize=" + maxSize +
            ", maxDocs=" + maxDocs +
            ", maxAge=" + maxAge +
            '}';
    }
}
