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
import com.fasterxml.jackson.databind.node.ArrayNode;
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
import static org.elasticsearch.gradle.test.rest.compat.TransformKeyValue.Key.VALUE;

/**
 * Adds items to a {@link JsonNode}. Currently supports adding a {@link ObjectNode} at provided {@link JsonPointer}
 */
public class AddTransformation implements Transformation {

    private static JsonNodeFactory jsonNodeFactory = JsonNodeFactory.withExactBigDecimals(false);
    private final List<Transform> additions = new ArrayList<>();

    public AddTransformation(String testName, List<TransformKeyValue> rawTransforms) {
        for (TransformKeyValue rawTransform : rawTransforms) {
            EnumSet<Key> actions = EnumSet.copyOf(rawTransform.getAllKeys());

            if (actions.stream().noneMatch(action -> action.equals(LOCATION))) {
                throw new IllegalStateException("'add' requires 'location' defined");
            }
            if (actions.stream().noneMatch(action -> action.equals(VALUE))) {
                throw new IllegalStateException("'add' requires 'value' defined");
            }
            EnumSet<TransformKeyValue.Key> invalidActions = EnumSet.complementOf(EnumSet.of(LOCATION, VALUE));
            Set<Key> invalid = actions.stream().filter(invalidActions::contains).collect(Collectors.toSet());
            if (invalid.isEmpty() == false) {
                throw new IllegalStateException("found invalid key(s) in 'add' definition [" +
                    invalid.stream().map(a -> a.name().toLowerCase(Locale.ROOT)).collect(Collectors.joining(",")) + "]");
            }
            additions.add(new AddNodeToLocation("/" + testName + rawTransform.getLocation().asText(), rawTransform.getValue()));
        }
    }

    @Override
    public List<Transform> getTransforms() {
        return Collections.unmodifiableList(additions);
    }

    static class AddNodeToLocation implements Transform.FindByLocation {
        private final JsonPointer location;
        private final JsonNode node;
        private final int partCount;
        private final String keyName;

        AddNodeToLocation(String path, JsonNode node) {
            this.location = JsonPointer.compile(path);
            this.node = node;
            String[] parts = path.split("/");
            partCount = parts.length - 1;
            keyName = parts[partCount];
        }

        @Override
        public JsonPointer location() {
            return location;
        }

        @Override
        public ContainerNode<?> transform(ContainerNode<?> input) {
            if (input.isObject()) {
                System.out.println("*********  is object !!");
                ObjectNode copy = new ObjectNode(jsonNodeFactory);

                Iterator<Map.Entry<String, JsonNode>> it = input.fields();
                while (it.hasNext()) {
                    Map.Entry<String, JsonNode> currentKeyValue = it.next();
                    copy.set(currentKeyValue.getKey(), currentKeyValue.getValue());
                }
                copy.set(keyName, node);
                return copy;
            } else if (input.isArray()) {
                System.out.println("*********  is array !!");
                ArrayNode copy = new ArrayNode(jsonNodeFactory);
                int arrayLocation = 0;
                try {
                    arrayLocation = Integer.parseInt(keyName);
                } catch (NumberFormatException e) {
                    //do nothing
                }
                int i = 0;
                boolean added = false;
                Iterator<JsonNode> it = input.elements();
                while (it.hasNext()) {
                    if (arrayLocation == i++) {
                        copy.add(node);
                        added = true;
                    }
                    copy.add(it.next());
                }
                if(added == false){
                    throw new IllegalStateException("Could not add ["+ node + "] at [" + this.location + "]");
                }
                return copy;
            }
            //impossible since object/arrays are the only types of container nodes
            throw new IllegalStateException("Only Object/Array container nodes are supported");
        }

        @Override
        public int compareTo(FindByLocation o) {
            //We want the Add's to always go last (hence 100), but also the deeper paths to go first with in the Add's (hence partCount)
            return 100 - partCount;
        }
    }
}
