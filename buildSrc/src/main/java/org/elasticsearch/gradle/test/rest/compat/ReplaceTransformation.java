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
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import static org.elasticsearch.gradle.test.rest.compat.TransformKeyValue.Key.LOCATION;
import static org.elasticsearch.gradle.test.rest.compat.TransformKeyValue.Key.OBJECT;
import static org.elasticsearch.gradle.test.rest.compat.TransformKeyValue.Key.VALUE;
import static org.elasticsearch.gradle.test.rest.compat.TransformKeyValue.Key.WITH;


class ReplaceTransformation implements Transformation {
    private final List<Transform> replacements = new ArrayList<>();

    public ReplaceTransformation(List<TransformKeyValue> rawTransforms) {
        for (TransformKeyValue rawTransform : rawTransforms) {
            EnumSet<TransformKeyValue.Key> actions = EnumSet.copyOf(rawTransform.getAllKeys());
            if (actions.stream().noneMatch(action -> action.equals(WITH))) {
                throw new IllegalStateException("'replace' requires 'with' defined");
            }
            EnumSet<TransformKeyValue.Key> invalidActions = EnumSet.complementOf(EnumSet.of(VALUE, OBJECT, LOCATION, WITH));
            Set<TransformKeyValue.Key> invalid = actions.stream().filter(invalidActions::contains).collect(Collectors.toSet());
            if (invalid.isEmpty() == false) {
                throw new IllegalStateException("found invalid key(s) in 'replace' definition [" +
                    invalid.stream().map(a -> a.name().toLowerCase(Locale.ROOT)).collect(Collectors.joining(",")) + "]");
            }
            EnumSet<TransformKeyValue.Key> findActions = EnumSet.of(VALUE, OBJECT, LOCATION);
            Set<TransformKeyValue.Key> find = actions.stream().filter(findActions::contains).collect(Collectors.toSet());
            if (find.isEmpty()) {
                throw new IllegalStateException("'replace' requires one of the following defined 'value', 'object', or 'location'");
            } else if (find.size() != 1) {
                throw new IllegalStateException("'replace' requires only one of the following defined 'value', 'object', or 'location' [" +
                    find.stream().map(a -> a.name().toLowerCase(Locale.ROOT)).collect(Collectors.joining(",")) + "]");
            }

            switch (find.iterator().next()) {
                case VALUE:
                    replacements.add(new ReplaceValue(rawTransform.getValue().asText(), rawTransform.getWith().asText()));
                    break;
                case OBJECT:
                    replacements.add(new ReplaceObject(rawTransform.getObject(), rawTransform.getWith()));
                    break;
                case LOCATION:
                    replacements.add(new ReplaceAtLocation(JsonPointer.compile(rawTransform.getLocation().asText()), rawTransform.getWith()));
                    break;
                default:
                    assert false : "unexpected replace value, this is a bug";
            }
        }
    }

    @Override

    public List<Transform> getTransforms() {
        return Collections.unmodifiableList(replacements);
    }

    @Override
    public String toString() {
        return "ReplaceAction{" +
            "replacements=" + replacements +
            '}';
    }

    static class ReplaceValue implements Transform.FindByValue<String> {
        private final String toReplace;
        private final String replacement;

        ReplaceValue(String toReplace, String replacement) {
            this.toReplace = toReplace;
            this.replacement = replacement;
        }

        @Override
        public String valueToFind() {
            return toReplace;
        }

        @Override
        public JsonNode transform(JsonNode input) {
            return null;
        }
    }

    static class ReplaceObject implements Transform.FindByNode<ObjectNode> {
        private final ObjectNode toReplace;
        private final JsonNode replacement;

        ReplaceObject(ObjectNode object, JsonNode replacement) {
            this.toReplace = object;
            this.replacement = replacement;
        }

        @Override
        public JsonNode transform(JsonNode input) {
            return null;
        }

        @Override
        public ObjectNode nodeToFind() {
            return toReplace;
        }

        @Override
        public String toString() {
            return "ReplaceObject{" +
                "toReplace=" + toReplace +
                ", replacement=" + replacement +
                '}';
        }
    }

    static class ReplaceAtLocation implements Transform.FindByLocation {
        private final JsonPointer location;
        private final JsonNode replacement;

        ReplaceAtLocation(JsonPointer location, JsonNode replacement) {
            this.location = location;
            this.replacement = replacement;
        }

        @Override
        public JsonPointer location() {
            return location;
        }

        @Override
        public JsonNode transform(JsonNode input) {
            return null;
        }
    }
}
