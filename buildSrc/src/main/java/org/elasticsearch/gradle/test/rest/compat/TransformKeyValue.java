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
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.HashSet;
import java.util.Set;

/**
 * The set of key/value pairings that describes a single {@link Transform}.
 */
class TransformKeyValue {

    /**
     * An enumeration to record which keys are set for any given {@link Transform}. This can be used to for quick validation of acceptable
     * keys defined with the scope of a given {@link Transformation}
     */
    enum Key {
        VALUE, WITH, LOCATION, OBJECT
    }

    private Set<Key> all = new HashSet<>(2);
    private JsonNode value;
    private JsonNode with;
    private JsonNode location;
    private ObjectNode object;

    public JsonNode getValue() {
        return value;
    }

    public void setValue(JsonNode value) {
        this.value = value;
        all.add(Key.VALUE);
    }

    public JsonNode getWith() {
        return with;
    }

    public void setWith(JsonNode with) {
        this.with = with;
        all.add(Key.WITH);
    }

    public JsonNode getLocation() {
        return location;
    }

    public void setLocation(JsonNode location) {
        this.location = location;
        all.add(Key.LOCATION);
    }

    public ObjectNode getObject() {
        return object;
    }

    public void setObject(ObjectNode object) {
        this.object = object;
        all.add(Key.OBJECT);    }

    public Set<Key> getAllKeys() {
        return all;
    }
}
