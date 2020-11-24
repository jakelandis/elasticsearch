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
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.elasticsearch.gradle.test.rest.compat.TransformKeyValue.Key.LOCATION;
import static org.elasticsearch.gradle.test.rest.compat.TransformKeyValue.Key.OBJECT;
import static org.elasticsearch.gradle.test.rest.compat.TransformKeyValue.Key.VALUE;
import static org.elasticsearch.gradle.test.rest.compat.TransformKeyValue.Key.WITH;


class ReplaceTransformation implements Transformation {
    private static JsonNodeFactory jsonNodeFactory = JsonNodeFactory.withExactBigDecimals(false);
    private final List<Transform> replacements = new ArrayList<>();
    private final String testName;

    public ReplaceTransformation(String testName, List<TransformKeyValue> rawTransforms) {
        this.testName = testName;
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
                    replacements.add(new ReplaceStringValue(rawTransform.getValue().asText(), rawTransform.getWith().asText()));
                    break;
                case OBJECT:
                    replacements.add(new ReplaceObject(rawTransform.getObject(), rawTransform.getWith()));
                    break;
                case LOCATION:
                    replacements.add(new ReplaceAtLocation("/" + testName + rawTransform.getLocation().asText(), rawTransform.getWith()));
                    break;
                default:
                    assert false : "unexpected replace value, this is a bug with validation";
            }
        }
    }

    @Override

    public List<Transform> getTransforms() {
        return Collections.unmodifiableList(replacements);
    }

    //TODO: support other value types ? (not here..but like in a ReplaceBooleanValue)
    static class ReplaceStringValue implements Transform.FindByValue<String> {
        private final String toReplace;
        private final String replacement;

        ReplaceStringValue(String toReplace, String replacement) {
            this.toReplace = toReplace;
            this.replacement = replacement;
        }

        @Override
        public String valueToFind() {
            return toReplace;
        }

        @Override
        public ContainerNode<?> transform(ContainerNode<?> input) {
            if (input.isObject()) {
                ObjectNode copy = new ObjectNode(jsonNodeFactory);
                Iterator<Map.Entry<String, JsonNode>> it = input.fields();
                while (it.hasNext()) {
                    Map.Entry<String, JsonNode> currentKeyValue = it.next();
                    if (currentKeyValue.getValue().asText().equals(toReplace)) {
                        copy.set(currentKeyValue.getKey(), new TextNode(replacement));
                    } else {
                        copy.set(currentKeyValue.getKey(), currentKeyValue.getValue());
                    }
                }
                return copy;
            } else if (input.isArray()) {
                throw new UnsupportedOperationException("TODO: support transforming arrays");
            }
            //impossible since object/arrays are the only types of container nodes
            throw new IllegalStateException("Only Object/Array container nodes are supported");
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
        public ContainerNode<?> transform(ContainerNode<?> input) {
            if (input.isObject()) {
                ObjectNode copy = new ObjectNode(jsonNodeFactory);
                Iterator<Map.Entry<String, JsonNode>> it = input.fields();
                while (it.hasNext()) {
                    Map.Entry<String, JsonNode> currentKeyValue = it.next();
                    if (currentKeyValue.getValue().equals(toReplace)) {
                        copy.set(currentKeyValue.getKey(), replacement);
                    } else {
                        copy.set(currentKeyValue.getKey(), currentKeyValue.getValue());
                    }
                }
                return copy;
            } else if (input.isArray()) {
                throw new UnsupportedOperationException("TODO: support transforming arrays");
            }
            //impossible since object/arrays are the only types of container nodes
            throw new IllegalStateException("Only Object/Array container nodes are supported");
        }

        @Override
        public ObjectNode nodeToFind() {
            return toReplace;
        }
    }

    static class ReplaceAtLocation implements Transform.FindByLocation {
        private final JsonPointer location;
        private final JsonNode replacement;
        private final String keyName;

        ReplaceAtLocation(String path, JsonNode replacement) {
            this.location = JsonPointer.compile(path);
            this.replacement = replacement;
            String[] parts = path.split("/");
            this.keyName = parts[parts.length - 1];
        }

        @Override
        public JsonPointer location() {
            return location;
        }


        @Override
        public ContainerNode<?> transform(ContainerNode<?> input) {
            if (input.isObject()) {
                ObjectNode copy = new ObjectNode(jsonNodeFactory);
                Iterator<Map.Entry<String, JsonNode>> it = input.fields();
                while (it.hasNext()) {
                    Map.Entry<String, JsonNode> currentKeyValue = it.next();
                    if (currentKeyValue.getKey().equals(keyName)) {
                        copy.set(currentKeyValue.getKey(), replacement);
                    } else {
                        copy.set(currentKeyValue.getKey(), currentKeyValue.getValue());
                    }
                }
                return copy;
            } else if (input.isArray()) {
                throw new UnsupportedOperationException("TODO: support transforming arrays");
            }
            //impossible since object/arrays are the only types of container nodes
            throw new IllegalStateException("Only Object/Array container nodes are supported");

        }

        @Override
        public int compareTo(FindByLocation o) {
            return 0;
        }
    }


}
