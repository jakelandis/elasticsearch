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
import static org.elasticsearch.gradle.test.rest.compat.ActionItem.Keys.VALUE;
import static org.elasticsearch.gradle.test.rest.compat.ActionItem.Keys.WITH;


class ReplaceAction {
    List<Replace<?, ?>> replacements = new ArrayList<>();

    public ReplaceAction(List<ActionItem> actionItems) {
        for (ActionItem actionItem : actionItems) {
            EnumSet<Keys> actions = EnumSet.copyOf(actionItem.getNonNull());
            if (actions.stream().noneMatch(action -> action.equals(WITH))) {
                throw new IllegalStateException("'replace' requires 'with' defined");
            }
            EnumSet<Keys> invalidActions = EnumSet.complementOf(EnumSet.of(VALUE, OBJECT, LOCATION, WITH));
            Set<Keys> invalid = actions.stream().filter(invalidActions::contains).collect(Collectors.toSet());
            if (invalid.isEmpty() == false) {
                throw new IllegalStateException("found invalid key(s) in 'replace' definition [" +
                    invalid.stream().map(a -> a.name().toLowerCase(Locale.ROOT)).collect(Collectors.joining(",")) + "]");
            }
            EnumSet<Keys> findActions = EnumSet.of(VALUE, OBJECT, LOCATION);
            Set<Keys> find = actions.stream().filter(findActions::contains).collect(Collectors.toSet());
            if (find.isEmpty()) {
                throw new IllegalStateException("'replace' requires one of the following defined 'value', 'object', or 'location'");
            } else if (find.size() != 1) {
                throw new IllegalStateException("'replace' requires only one of the following defined 'value', 'object', or 'location' [" +
                    find.stream().map(a -> a.name().toLowerCase(Locale.ROOT)).collect(Collectors.joining(",")) + "]");
            }

            switch (find.iterator().next()) {
                case VALUE:
                    replacements.add(new ValueReplace(actionItem.getValue().asText(), actionItem.getWith().asText()));
                    break;
                case OBJECT:
                    replacements.add(new ObjectReplace(actionItem.getObject(), actionItem.getWith()));
                    break;
                case LOCATION:
                    replacements.add(new LocationReplace(JsonPointer.compile(actionItem.getLocation().asText()), actionItem.getWith()));
                    break;
                default:
                    assert false : "unexpected replace value, this is a bug";

            }
        }
    }

    @Override
    public String toString() {
        return "ReplaceAction{" +
            "replacements=" + replacements +
            '}';
    }

    static class ValueReplace implements Replace<String, String> {
        private final String replace;
        private final String with;

        ValueReplace(String replace, String with) {
            this.replace = replace;
            this.with = with;
        }

        public String replace() {
            return replace;
        }

        public String with() {
            return with;
        }

        @Override
        public String toString() {
            return "ValueReplace{" +
                "replace='" + replace + '\'' +
                ", with='" + with + '\'' +
                '}';
        }
    }

    static class ObjectReplace implements Replace<JsonNode, JsonNode> {
        private final JsonNode replace;
        private final JsonNode with;

        ObjectReplace(JsonNode replace, JsonNode with) {
            this.replace = replace;
            this.with = with;
        }

        public JsonNode replace() {
            return replace;
        }

        public JsonNode with() {
            return with;
        }

        @Override
        public String toString() {
            return "ObjectReplace{" +
                "replace=" + replace +
                ", with=" + with +
                '}';
        }

    }

    static class LocationReplace implements Replace<JsonPointer, JsonNode> {
        private final JsonPointer replace;
        private final JsonNode with;

        LocationReplace(JsonPointer replace, JsonNode with) {
            this.replace = replace;
            this.with = with;
        }

        public JsonPointer replace() {
            return replace;
        }

        public JsonNode with() {
            return with;
        }

        @Override
        public String toString() {
            return "LocationReplace{" +
                "replace=" + replace +
                ", with=" + with +
                '}';
        }
    }

    interface Replace<R, W> {
        R replace();

        W with();
    }
}
