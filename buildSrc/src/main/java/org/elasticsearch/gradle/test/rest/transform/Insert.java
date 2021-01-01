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

package org.elasticsearch.gradle.test.rest.transform;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import java.util.Iterator;
import java.util.Map;

public class Insert extends TransformAction {


    private static JsonNodeFactory jsonNodeFactory = JsonNodeFactory.withExactBigDecimals(false);

    private JsonNode toInsert;

    public Insert(String testName, Map<String, JsonNode> raw) {
        raw.forEach((key, v) -> {
                switch (key) {
                    case "find":
                        if (v.asText().trim().startsWith("/")) {
                            transform = new ByLocation("/" + testName + v.asText().trim());
                        } else {
                            transform = new ByMatch(v);
                        }
                        break;
                    case "insert":
                        toInsert = v;
                        break;
                    default:
                        throw new IllegalArgumentException("Found unexpected key [" + key + "] for test [" + testName + "]");
                }
            }
        );
        if (transform == null) {
            throw new IllegalStateException("Could not find any transformations for test [" + testName + "]");
        }
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
        public void transform(ContainerNode<?> parentNode) {
            boolean inserted = false;
            if (parentNode.isObject()) {
                ObjectNode parentObject = (ObjectNode) parentNode;
                // adds or will replace if already exists
                //TODO: this is wrong .. do like below
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
                            objectNode.set(keyName, toInsert);
                            inserted = true;
                        }
                    }
                }
            }
            if (inserted == false) {
                throw new IllegalArgumentException("Could not find location [" + location + "] to insert [" + toInsert + "]");
            }
        }

    }

    class ByMatch implements FindByMatch {

        private final TextNode keyToFind;
        boolean transformed = false;

        public ByMatch(JsonNode nodeToFind) {

            if (nodeToFind instanceof TextNode == false) {
                throw new IllegalArgumentException("Inserting by find is only supported for object keys ");
            }
            this.keyToFind = (TextNode) nodeToFind;

            //insert by match is only supported by matching simple strings that are the key of an object
        }

        @Override
        public void transform(ContainerNode<?> parentNode) {
            System.out.println("inserting object in parentNode: " + parentNode + " after: " + keyToFind + " with: " + toInsert);
            if (parentNode.isObject()) {
                ObjectNode parentObject = (ObjectNode) parentNode;
                Iterator<Map.Entry<String, JsonNode>> it = parentObject.deepCopy().fields();
                while (it.hasNext()) {
                    Map.Entry<String, JsonNode> entry = it.next();
                    if (nodeToFind().textValue().equals(entry.getKey())) {
                        if (entry.getValue() instanceof ObjectNode) {
                            ObjectNode objectToInsertTo = (ObjectNode) entry.getValue();
                            if (toInsert instanceof ObjectNode) {
                                objectToInsertTo.setAll((ObjectNode) toInsert);
                            } else if (entry.getValue() instanceof ArrayNode) {
                                ArrayNode arrayToInsertTo = (ArrayNode) entry.getValue();
                                arrayToInsertTo.add(toInsert);
                            }
                        } else {
                            throw new IllegalStateException("Can only insert into container node (object or array)");
                        }
                    }
                }
            } else if (parentNode.isArray()) {
                throw new IllegalArgumentException("TODO: support inserting into arrays yo!");
            }
        }

        @Override
        public JsonNode nodeToFind() {
            return keyToFind;
        }
    }
}
