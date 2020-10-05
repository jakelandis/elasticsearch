/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.gradle.test.rest;


import com.fasterxml.jackson.databind.JsonNode;

import java.util.EnumSet;
import java.util.Objects;

public class RestTestMutation {
    private final String testName;
    private final Action action;
    private final int index;
    private final JsonNode mutation;
    private final SectionType sectionType;

    enum Action {
        REPLACE,
        REMOVE,
        ADD;

        static Action fromString(String actionString) {
            return EnumSet.allOf(Action.class).stream().filter(a -> a.name().equalsIgnoreCase(actionString))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Invalid action."));
        }
    }

    enum SectionType {
        MATCH;

        static SectionType fromString(String executionSection) {
            return EnumSet.allOf(SectionType.class).stream().filter(a -> a.name().equalsIgnoreCase(executionSection))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Unsupported execution section [" + executionSection + "]"));
        }
    }

    public RestTestMutation(String testName, Action action, SectionType sectionType, int index, JsonNode mutation) {
        this.testName = testName;
        this.action = Objects.requireNonNull(action);
        this.index = index;
        this.mutation = mutation;
        this.sectionType = sectionType;
    }

    @Override
    public String toString() {
        return "TestMutation{" +
            "testName='" + testName + '\'' +
            ", action=" + action +
            ", index=" + index +
            ", mutation=" + mutation +
            ", sectionType=" + sectionType +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RestTestMutation mutation = (RestTestMutation) o;
        return index == mutation.index &&
            Objects.equals(testName, mutation.testName) &&
            sectionType == mutation.sectionType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(testName, index, sectionType);
    }
}

