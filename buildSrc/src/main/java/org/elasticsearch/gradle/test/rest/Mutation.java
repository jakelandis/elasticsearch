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

public class Mutation {

    enum Action {
        REPLACE,
        REMOVE,
        ADD;

        static Action fromString(String actionString) {
            return EnumSet.allOf(Action.class).stream().filter(a -> a.name().equalsIgnoreCase(actionString))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Invalid action."));
        }
    }


    private final Action action;
    private final String jsonPointer;
    private final JsonNode jsonNode;

    public Mutation(Action action, String jsonPointer, JsonNode jsonNode) {

        this.action = Objects.requireNonNull(action);
        this.jsonNode = jsonNode;
        this.jsonPointer = jsonPointer;
    }

    public Action getAction() {
        return action;
    }

    public String getJsonPointer() {
        return jsonPointer;
    }

    public JsonNode getJsonNode() {
        return jsonNode;
    }

    @Override
    public String toString() {
        return "Mutation{" +
            "action=" + action +
            ", jsonPointer='" + jsonPointer + '\'' +
            ", jsonNode=" + jsonNode +
            '}';
    }
}

