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
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Iterator;
import java.util.Map;

public class Insert implements TransformAction {

    private Transform transform;
    private JsonNode toInsert;

    public Insert(String testName, Map<String, JsonNode> raw) {
        raw.forEach((key, v) -> {
                switch (key) {
                    case "find":
                        if (v.asText().trim().startsWith("/")) {
                            transform = new ByLocation("/" + testName + v.asText().trim());
                        } else {
                            transform = new ByMatch();
                        }
                        break;
                    case "insert":
                        toInsert = v;
                        break;
                    default:
                        throw new IllegalArgumentException("Found unexpected key: " + key);
                }
            }
        );
    }

    @Override
    public Transform getTransform() {
        return transform;
    }

    class ByLocation implements Transform.FindByLocation {

        private final JsonPointer location;
        private final String keyName;

        ByLocation(String path) {
            this.location = JsonPointer.compile(path);
            String[] parts = path.split("/");
            keyName = parts[parts.length - 1];
        }

        @Override
        public JsonPointer location() {
            return location;
        }

        @Override
        public ContainerNode<?> transform(ContainerNode<?> parentNode) {
            boolean inserted = false;
            if (parentNode.isObject()) {
                ObjectNode parentObject = (ObjectNode) parentNode;
                // adds or will replace if already exists
                parentObject.set(keyName, toInsert);
                inserted = true;
            } else if (parentNode.isArray()) {
                ArrayNode parentArray = (ArrayNode) parentNode;
                try {
                    //if the trailing part of the location is numeric, we are inserting the node directly into an array
                    int position = Integer.parseInt(keyName);
                    parentArray.insert(position, toInsert);
                    inserted = true;
                } catch (NumberFormatException numberFormatException) {
                    //if the trailing part of the location is not numeric, we are inserting into an object (whose parent is an array)
                    Iterator<JsonNode> it = parentArray.iterator();
                    while (it.hasNext()) {
                        JsonNode node = it.next();
                        if (node.isObject()) {
                            ObjectNode objectNode = (ObjectNode) node;
                            if (objectNode.get(keyName) != null) {
                                objectNode.set(keyName, toInsert);
                                inserted = true;
                            }
                        }
                    }
                }
            }
            if (inserted == false) {
                throw new IllegalArgumentException("Could not find location [" + location + "] to insert [" + toInsert + "]");
            }
            return parentNode;
        }

    }

    class ByMatch implements Transform.FindByNode<JsonNode> {

        @Override
        public ContainerNode<?> transform(ContainerNode<?> parentNode) {
            System.out.println("Inserting by by match!!");
            return null;
        }

        @Override
        public JsonNode nodeToFind() {
            return null;
        }
    }
}
