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

import java.util.Map;

public class Insert implements Transform {

    Transform transform;
    JsonNode toInsert;

    public Insert(String testName, Map<String, JsonNode> raw) {
        raw.forEach((key, v) -> {
                switch (key) {
                    case "find":
                        if (v.asText().startsWith("/")) {
                            transform = new ByLocation("/" + testName + v.asText());
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
    public ContainerNode<?> transform(ContainerNode<?> parentNode) {
        return transform.transform(parentNode);
    }

    class ByLocation implements Transform.FindByLocation {

        private final JsonPointer location;
        private int depth;
        private final String keyName;
        ByLocation(String path){
            this.location = JsonPointer.compile(path);
            String[] parts = path.split("/");
            depth = parts.length - 1;
            keyName = parts[depth];
        }

        @Override
        public JsonPointer location() {
            return location;
        }

        @Override
        public int compareTo(FindByLocation o) {
            return 0;
        }

        @Override
        public ContainerNode<?> transform(ContainerNode<?> parentNode) {
            return null;
        }
    }

    class ByMatch implements Transform.FindByNode<JsonNode> {

        @Override
        public ContainerNode<?> transform(ContainerNode<?> parentNode) {
            return null;
        }

        @Override
        public JsonNode nodeToFind() {
            return null;
        }
    }
}
