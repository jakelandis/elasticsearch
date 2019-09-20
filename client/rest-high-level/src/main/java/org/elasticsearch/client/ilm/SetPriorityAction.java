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

import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.xcontent.ConstructingObjectParser;
import org.elasticsearch.common.xcontent.ObjectParser;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.ToXContentObject;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;

import java.io.IOException;

/**
 * A {@link LifecycleAction} which sets the index's priority. The higher the priority, the faster the recovery.
 */

public class SetPriorityAction implements LifecycleAction{
    public static final String NAME = "set_priority";


    //package private for testing
    final Integer recoveryPriority;



    public SetPriorityAction(@Nullable Integer recoveryPriority) {
        if (recoveryPriority != null && recoveryPriority < 0) {
            throw new IllegalArgumentException("[priority] must be 0 or greater");
        }
        this.recoveryPriority = recoveryPriority;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SetPriorityAction that = (SetPriorityAction) o;

        return recoveryPriority != null ? recoveryPriority.equals(that.recoveryPriority) : that.recoveryPriority == null;
    }

    @Override
    public int hashCode() {
        return recoveryPriority != null ? recoveryPriority.hashCode() : 0;
    }


    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String toString() {
        return "SetPriorityAction{" +
            "recoveryPriority=" + recoveryPriority +
            '}';
    }
}
