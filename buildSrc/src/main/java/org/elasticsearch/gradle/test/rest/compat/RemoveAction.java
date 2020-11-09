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

public class RemoveAction {

    List<Remove<?>> removals = new ArrayList<>();

    @Override
    public String toString() {
        return "RemoveAction{" +
            "removals=" + removals +
            '}';
    }

    public RemoveAction(List<ActionItem> actionItems) {
        for (ActionItem actionItem : actionItems) {

            EnumSet<ActionItem.Keys> actions = EnumSet.copyOf(actionItem.getNonNull());
            EnumSet<Keys> validActions = EnumSet.of(LOCATION, OBJECT);

            Set<Keys> valid = actions.stream().filter(validActions::contains).collect(Collectors.toSet());
            if (valid.isEmpty()) {
                throw new IllegalStateException("'remove' requires one of the following defined 'location' or 'object'");
            } else if (valid.size() != 1) {
                throw new IllegalStateException("'valid' requires only one of the following defined 'location', 'object' [" +
                    valid.stream().map(a -> a.name().toLowerCase(Locale.ROOT)).collect(Collectors.joining(",")) + "]");
            }

            EnumSet<Keys> invalidActions = EnumSet.complementOf(validActions);
            Set<Keys> invalid = actions.stream().filter(invalidActions::contains).collect(Collectors.toSet());
            if (invalid.isEmpty() == false) {
                throw new IllegalStateException("found invalid key(s) in 'add' definition [" +
                    invalid.stream().map(a -> a.name().toLowerCase(Locale.ROOT)).collect(Collectors.joining(",")) + "]");
            }
            switch (valid.iterator().next()) {
                case LOCATION:
                    removals.add(new LocationRemove(JsonPointer.compile(actionItem.getLocation().asText())));
                    break;
                case OBJECT:
                    removals.add(new ObjectRemove(actionItem.getObject()));
                    break;
                default:
                    assert false : "unexpected remove value, this is a bug";
            }
        }

    }

    static class LocationRemove implements Remove<JsonPointer> {
        private final JsonPointer location;

        LocationRemove(JsonPointer location) {
            this.location = location;
        }

        @Override
        public JsonPointer find() {
            return location;
        }

        @Override
        public String toString() {
            return "LocationRemove{" +
                "location=" + location +
                '}';
        }
    }

    static class ObjectRemove implements Remove<JsonNode> {
        private final JsonNode object;

        ObjectRemove(JsonNode object) {
            this.object = object;
        }

        @Override
        public JsonNode find() {
            return object;
        }

        @Override
        public String toString() {
            return "ObjectRemove{" +
                "object=" + object +
                '}';
        }
    }

    interface Remove<F> {
        F find();
    }
}
