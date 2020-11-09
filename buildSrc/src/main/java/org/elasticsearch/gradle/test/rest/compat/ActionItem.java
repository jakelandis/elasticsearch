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

package org.elasticsearch.gradle.test.rest.compat;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.HashSet;
import java.util.Set;

/**
 * A superset of all the possible key/values for all of the actions. Each action (replace, add, remove) is responsible for validation
 * of the supported set of keys and the key/value pairings.
 */
class ActionItem {

    enum Keys {
        VALUE, WITH, LOCATION, OBJECT
    }
    private Set<Keys> nonNull = new HashSet<>(2);
    private JsonNode value;
    private JsonNode with;
    private JsonNode location;
    private JsonNode object;

    public JsonNode getValue() {
        return value;
    }

    public void setValue(JsonNode value) {
        this.value = value;
        nonNull.add(Keys.VALUE);
    }

    public JsonNode getWith() {
        return with;
    }

    public void setWith(JsonNode with) {
        this.with = with;
        nonNull.add(Keys.WITH);
    }


    public JsonNode getLocation() {
        return location;
    }

    public void setLocation(JsonNode location) {
        this.location = location;
        nonNull.add(Keys.LOCATION);
    }

    public JsonNode getObject() {
        return object;
    }

    public void setObject(JsonNode object) {
        this.object = object;
        nonNull.add(Keys.OBJECT);
    }

    public Set<Keys> getNonNull() {
        return nonNull;
    }
}
