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

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import org.elasticsearch.gradle.test.rest.compat.ActionItem.Keys;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import static org.elasticsearch.gradle.test.rest.compat.ActionItem.Keys.LOCATION;
import static org.elasticsearch.gradle.test.rest.compat.ActionItem.Keys.OBJECT;

public class AddAction {

    List<Add<?>> additions = new ArrayList<>();


    public AddAction(List<ActionItem> actionItems) {
        for (ActionItem actionItem : actionItems) {
            EnumSet<Keys> actions = EnumSet.copyOf(actionItem.getNonNull());
            if (actions.stream().noneMatch(action -> action.equals(LOCATION))) {
                throw new IllegalStateException("'add' requires 'location' defined");
            }
            if (actions.stream().noneMatch(action -> action.equals(OBJECT))) {
                throw new IllegalStateException("'add' requires 'object' defined");
            }
            EnumSet<Keys> invalidActions = EnumSet.complementOf(EnumSet.of(LOCATION, OBJECT));
            Set<Keys> invalid = actions.stream().filter(invalidActions::contains).collect(Collectors.toSet());
            if (invalid.isEmpty() == false) {
                throw new IllegalStateException("found invalid key(s) in 'add' definition [" +
                    invalid.stream().map(a -> a.name().toLowerCase(Locale.ROOT)).collect(Collectors.joining(",")) + "]");
            }
            additions.add(new LocationAdd(JsonPointer.compile(actionItem.getLocation().asText()), actionItem.getObject()));
        }
    }

    @Override
    public String toString() {
        return "AddAction{" +
            "additions=" + additions +
            '}';
    }

    static class LocationAdd implements Add<JsonPointer> {
        private final JsonPointer location;
        private final JsonNode node;

        LocationAdd(JsonPointer location, JsonNode node) {
            this.location = location;
            this.node = node;
        }

        public JsonPointer find() {
            return location;
        }

        public JsonNode node() {
            return node;
        }

        @Override
        public String toString() {
            return "LocationAdd{" +
                "location=" + location +
                ", node=" + node +
                '}';
        }
    }

    interface Add<F> {
        F find();

        JsonNode node();
    }
}
