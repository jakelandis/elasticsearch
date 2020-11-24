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
import org.elasticsearch.gradle.test.rest.compat.TransformKeyValue.Key;

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

public class RemoveTransformation implements Transformation {

    private static JsonNodeFactory jsonNodeFactory = JsonNodeFactory.withExactBigDecimals(false);
    private final List<Transform> removals = new ArrayList<>();
    private final String testName;


    public RemoveTransformation(String testName, List<TransformKeyValue> rawTransforms) {
        this.testName = testName;
        for (TransformKeyValue rawTransform : rawTransforms) {

            EnumSet<Key> actions = EnumSet.copyOf(rawTransform.getAllKeys());
            EnumSet<Key> validActions = EnumSet.of(LOCATION, OBJECT);

            Set<Key> valid = actions.stream().filter(validActions::contains).collect(Collectors.toSet());
            if (valid.isEmpty()) {
                throw new IllegalStateException("'remove' requires one of the following defined 'location' or 'object'");
            } else if (valid.size() != 1) {
                throw new IllegalStateException("'valid' requires only one of the following defined 'location', 'object' [" +
                    valid.stream().map(a -> a.name().toLowerCase(Locale.ROOT)).collect(Collectors.joining(",")) + "]");
            }

            EnumSet<Key> invalidActions = EnumSet.complementOf(validActions);
            Set<Key> invalid = actions.stream().filter(invalidActions::contains).collect(Collectors.toSet());
            if (invalid.isEmpty() == false) {
                throw new IllegalStateException("found invalid key(s) in 'add' definition [" +
                    invalid.stream().map(a -> a.name().toLowerCase(Locale.ROOT)).collect(Collectors.joining(",")) + "]");
            }
            switch (valid.iterator().next()) {
                case LOCATION:
                    removals.add(new RemoveAtLocation("/" + testName + rawTransform.getLocation().asText()));
                    break;
                case OBJECT:
                    removals.add(new RemoveObject(rawTransform.getObject()));
                    break;
                default:
                    assert false : "unexpected remove value, this is a bug with validation";
            }
        }

    }


    @Override
    public List<Transform> getTransforms() {
        return Collections.unmodifiableList(removals);
    }

    static class RemoveAtLocation implements Transform.FindByLocation {
        private final JsonPointer location;
        private final String keyName;

        RemoveAtLocation(String path) {
            this.location = JsonPointer.compile(path);
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
                        // do nothing ... removes it
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

    static class RemoveObject implements Transform.FindByNode<ObjectNode> {
        private final ObjectNode objectNode;

        RemoveObject(ObjectNode objectNode) {
            this.objectNode = objectNode;
        }

        @Override
        public ObjectNode nodeToFind() {
            return objectNode;
        }

        @Override
        public ContainerNode<?> transform(ContainerNode<?> input) {
            if (input.isObject()) {
                ObjectNode copy = new ObjectNode(jsonNodeFactory);
                Iterator<Map.Entry<String, JsonNode>> it = input.fields();
                while (it.hasNext()) {
                    Map.Entry<String, JsonNode> currentKeyValue = it.next();
                    if (currentKeyValue.getValue().equals(objectNode)) {
                        // do nothing ... removes it
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
}
