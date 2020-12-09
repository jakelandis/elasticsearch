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

import java.util.Iterator;
import java.util.Map;

public class Replace extends TransformAction {

    private static JsonNodeFactory jsonNodeFactory = JsonNodeFactory.withExactBigDecimals(false);
    private JsonNode toReplace;

    public Replace(String testName, Map<String, JsonNode> raw) {
        raw.forEach((key, v) -> {
                switch (key) {
                    case "find":
                        if (v.asText().trim().startsWith("/")) {
                            transform = new ByLocation("/" + testName + v.asText().trim());
                        } else {
                            transform = new ByMatch(v);
                        }
                        break;
                    case "replace":
                        toReplace = v;
                        break;
                    default:
                        throw new IllegalArgumentException("Found unexpected key: " + key);
                }
            }
        );
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
            boolean replaced = false;
            if (parentNode.isObject()) {
                ObjectNode parentObject = (ObjectNode) parentNode;
                // replace if found
                if (parentObject.get(keyName) != null) {
                    parentObject.set(keyName, toReplace);
                    replaced = true;
                }
            } else if (parentNode.isArray()) {
                ArrayNode parentArray = (ArrayNode) parentNode;
                try {
                    //if the trailing part of the location is numeric, we are inserting the node directly into an array
                    int position = Integer.parseInt(keyName);
                    //replace only if there is something at that position
                    if (position <= parentArray.size()) {
                        parentArray.insert(position, toReplace);
                        replaced = true;
                    }
                } catch (NumberFormatException numberFormatException) {
                    //if the trailing part of the location is not numeric, we are inserting into an object (whose parent is an array)
                    Iterator<JsonNode> it = parentArray.iterator();
                    while (it.hasNext()) {
                        JsonNode node = it.next();
                        if (node.isObject()) {
                            ObjectNode objectNode = (ObjectNode) node;
                            //replace only if there is data at that key
                            if (objectNode.get(keyName) != null) {
                                objectNode.set(keyName, toReplace);
                                replaced = true;
                            }
                        }
                    }
                }
            }
            if (replaced == false) {
                throw new IllegalArgumentException("Could not find anything at location [" + location + "] to replace");
            }
            return parentNode;
        }

    }

    class ByMatch implements FindByMatch {

        private final JsonNode nodeToFind;

        public ByMatch(JsonNode nodeToFind) {
            this.nodeToFind = nodeToFind;
        }

        @Override
        public ContainerNode<?> transform(ContainerNode<?> parentNode) {
            System.out.println("Replacing object in parentNode: " + parentNode + " for: " + nodeToFind + " with: " + toReplace);
            if (parentNode.isObject()) {
                ObjectNode parentObject = (ObjectNode) parentNode;
                Iterator<Map.Entry<String, JsonNode>> it = parentObject.deepCopy().fields();
                while (it.hasNext()) {
                    Map.Entry<String, JsonNode> entry = it.next();
                    if (toReplace instanceof ObjectNode) {
                        ObjectNode objectItem = new ObjectNode(jsonNodeFactory);
                        objectItem.set(entry.getKey(), entry.getValue());
                        if (objectItem.equals(nodeToFind)) {
                            parentObject.remove(entry.getKey());
                            parentObject.setAll((ObjectNode) toReplace);
                        }
                    }else{
                        parentObject.set(entry.getKey(), toReplace);
                    }
                }
            } else if (parentNode.isArray()) {
                throw new IllegalArgumentException("TODO: support arrays yo!");
            }

            return parentNode;
        }

        @Override
        public JsonNode nodeToFind() {
            return nodeToFind;
        }
    }
}


