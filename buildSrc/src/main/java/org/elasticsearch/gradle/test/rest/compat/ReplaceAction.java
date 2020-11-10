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
import java.util.Collections;
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
    private final List<Item<?, ?>> replacements = new ArrayList<>();

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
                    replacements.add(new ValueItem(actionItem.getValue().asText(), actionItem.getWith().asText()));
                    break;
                case OBJECT:
                    replacements.add(new ByObjectItem(actionItem.getObject(), actionItem.getWith()));
                    break;
                case LOCATION:
                    replacements.add(new ByLocationItem(JsonPointer.compile(actionItem.getLocation().asText()), actionItem.getWith()));
                    break;
                default:
                    assert false : "unexpected replace value, this is a bug";

            }
        }
    }

    public List<Item<?, ?>> getReplacements() {
        return Collections.unmodifiableList(replacements);
    }

    @Override
    public String toString() {
        return "ReplaceAction{" +
            "replacements=" + replacements +
            '}';
    }

    static class ValueItem implements Item<String, String>, Find.ByValue {
        private final String find;
        private final String replacement;

        ValueItem(String find, String replacement) {
            this.find = find;
            this.replacement = replacement;
        }

        public String find() {
            return find;
        }

        public String replacement() {
            return replacement;
        }

        @Override
        public String toString() {
            return "ValueReplace{" +
                "find='" + find + '\'' +
                ", replacement='" + replacement + '\'' +
                '}';
        }
    }

    static class ByObjectItem implements Item<JsonNode, JsonNode>, Find.ByObject {
        private final JsonNode find;
        private final JsonNode replacement;

        ByObjectItem(JsonNode find, JsonNode replacement) {
            this.find = find;
            this.replacement = replacement;
        }

        public JsonNode find() {
            return find;
        }

        public JsonNode replacement() {
            return replacement;
        }

        @Override
        public String toString() {
            return "ObjectReplace{" +
                "find=" + find +
                ", replacement=" + replacement +
                '}';
        }
    }

    static class ByLocationItem implements Item<JsonPointer, JsonNode>, Find.ByLocation {
        private final JsonPointer find;
        private final JsonNode replacement;

        ByLocationItem(JsonPointer find, JsonNode replacement) {
            this.find = find;
            this.replacement = replacement;
        }

        public JsonPointer find() {
            return find;
        }

        public JsonNode replacement() {
            return replacement;
        }

        @Override
        public String toString() {
            return "LocationReplace{" +
                "find=" + find +
                ", replacement=" + replacement +
                '}';
        }
    }

    interface Item<F, R> extends Instruction{
        F find();

        R replacement();
    }
}
