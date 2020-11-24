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
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLParser;
import org.apache.commons.lang3.tuple.Pair;


import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class TransformTest {
    private static final YAMLFactory yaml = new YAMLFactory();
    private static final ObjectMapper mapper = new ObjectMapper(yaml);

    public static Map<String, TestTransformation> readTransformations(File file) throws IOException {
        YAMLParser yamlParser = yaml.createParser(file);
        Map<String, TestTransformation> mutations = new HashMap<>();
        MappingIterator<TestTransformation> it = mapper.readValues(yamlParser, TestTransformation.class);
        while (it.hasNext()) {
            TestTransformation testTransformation = it.next();
            mutations.put(testTransformation.getTestName(), testTransformation);
        }
        return mutations;
    }

    public static List<ObjectNode> transformTest(File file, Map<String, TestTransformation> mutations) throws IOException {
        YAMLParser yamlParser = yaml.createParser(file);
        List<ObjectNode> tests = mapper.readValues(yamlParser, ObjectNode.class).readAll();
        //A YAML file can have multiple tests
        for (ObjectNode test : tests) {
            Iterator<Map.Entry<String, JsonNode>> testsIterator = test.fields();
            //each file can have multiple tests
            while (testsIterator.hasNext()) {
                Map.Entry<String, JsonNode> testObject = testsIterator.next();
                String testName = testObject.getKey();
                System.out.println("@@ test:" + testName);
                JsonNode currentTest = testObject.getValue();
                TestTransformation testTransformation = mutations.get(testName);
                if (testTransformation == null) {
                    continue;
                }
                System.out.println(currentTest);

                //transform by location first, push results to a copy so for multiple location transformations the JSON Pointer is always in
                //reference to the original un-transformed document.
                //transformByLocation(test);

                //collect all of the FindByValues
                Set<Transform.FindByValue<?>> findByValueSet = testTransformation.getAllTransforms()
                    .stream().filter(a -> a instanceof Transform.FindByValue)
                    .map(e -> (Transform.FindByValue<?>) e).collect(Collectors.toSet());
                // map them by the value to find for quick lookups
                Map<Object, Set<Transform.FindByValue<?>>> findByValueMap = new HashMap<>(findByValueSet.size());
                findByValueSet.forEach(t -> {
                    findByValueMap.computeIfAbsent(t.valueToFind(), k -> new HashSet<>()).add(t);
                });

                //collect all of the FindByNodes
                Set<Transform.FindByNode<? extends JsonNode>> findByNodeSet = testTransformation.getAllTransforms()
                    .stream().filter(a -> a instanceof Transform.FindByNode)
                    .map(e -> (Transform.FindByNode<? extends JsonNode>) e).collect(Collectors.toSet());
                // map them by the node to find for quick lookups
                Map<JsonNode, Set<Transform.FindByNode<? extends JsonNode>>> findByNodeMap = new HashMap<>(findByNodeSet.size());
                findByNodeSet.forEach(t -> {
                    findByNodeMap.computeIfAbsent(t.nodeToFind(), k -> new HashSet<>()).add(t);
                });

                //collect all of the FindByLocation
                List<Transform.FindByLocation> findByLocationSet = testTransformation.getAllTransforms()
                    .stream().filter(a -> a instanceof Transform.FindByLocation)
                    .map(e -> (Transform.FindByLocation) e).sorted().collect(Collectors.toList());


                //TODO: ensure that ADD is always last since ADD will change the location pointers..remove is safe because it leave behind
                // an empty shell
                for (Transform.FindByLocation findByLocation : findByLocationSet) {
                    JsonNode parentNode = test.at(findByLocation.location().head());
                    if (parentNode != null && parentNode.isContainerNode()) {

                        ContainerNode<?> result = findByLocation.transform((ContainerNode<?>) parentNode);
                        assert result != null;
                        if (parentNode.isObject()) {
                            ObjectNode parentObject = (ObjectNode) parentNode;
                            parentObject.removeAll();
                            parentObject.setAll((ObjectNode) result);
                        } else if (parentNode.isArray()) {
                            ArrayNode parentObject = (ArrayNode) parentNode;
                            parentObject.removeAll();
                            parentObject.addAll((ArrayNode) result);
                        }

                    } else {
                        // throw new IllegalArgumentException("TOOD: bad json pointer");
                    }
                }

                transformByEquality(test, currentTest, findByNodeMap, findByValueMap);

                System.out.println(test.toPrettyString());
            }
        }
        return null;
    }

    private static ObjectNode transformByLocation(ObjectNode root) {
        return root.deepCopy();
    }

    private static void transformByEquality(ContainerNode<?> parentNode, JsonNode currentNode, Map<JsonNode,
        Set<Transform.FindByNode<? extends JsonNode>>> findByNodeMap, Map<Object, Set<Transform.FindByValue<?>>> findByValueMap) {
        if (currentNode.isArray()) {
            ArrayNode arrayNode = (ArrayNode) currentNode;
            Iterator<JsonNode> node = arrayNode.elements();
            //TODO: support transform by parent array
            while (node.hasNext()) {
                transformByEquality(arrayNode, node.next(), findByNodeMap, findByValueMap);
            }
        } else if (currentNode.isObject()) {
            ObjectNode objectNode = (ObjectNode) currentNode;
            Set<Transform.FindByNode<? extends JsonNode>> findByNodes = findByNodeMap.get(currentNode);
            if (findByNodes != null) {
                findByNodes.forEach(findByNode -> {
                    ContainerNode<?> result = findByNode.transform(parentNode);
                    assert result != null;
                    if (parentNode.isObject()) {
                        ObjectNode parentObject = (ObjectNode) parentNode;
                        parentObject.removeAll();
                        parentObject.setAll((ObjectNode) result);
                    }
                });
            }
            currentNode.fields().forEachRemaining(entry -> transformByEquality(objectNode, entry.getValue(), findByNodeMap, findByValueMap));
        } else {
            Set<Transform.FindByValue<?>> findByValues;
            if (currentNode.isTextual()) {
                findByValues = findByValueMap.get(currentNode.asText());
            } else {
                //TODO: support other value types ?
                throw new UnsupportedOperationException("TODO: support all of the things !!");
            }

            if (findByValues != null) {
                findByValues.forEach(findByValue -> {
                    ContainerNode<?> result = findByValue.transform(parentNode);
                    assert result != null;
                    if (parentNode.isObject()) {
                        ObjectNode parentObject = (ObjectNode) parentNode;
                        parentObject.removeAll();
                        parentObject.setAll((ObjectNode) result);
                    }
                });
            }
            System.out.println("value: " + currentNode);
        }
    }
}
