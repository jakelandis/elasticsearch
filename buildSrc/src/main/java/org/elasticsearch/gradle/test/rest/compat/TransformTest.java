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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLParser;

import java.io.File;
import java.io.IOException;
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


                //collect all of the FindByNodes
                Set<Transform.FindByNode<? extends JsonNode>> findByNodeSet = testTransformation.getAllTransforms()
                    .stream().filter(a -> a instanceof Transform.FindByNode)
                    .map(e -> (Transform.FindByNode<? extends JsonNode>) e).collect(Collectors.toSet());
                // map them by the node to find for quick lookups
                Map<JsonNode, Set<Transform.FindByNode<? extends JsonNode>>> findByNodeMap = new HashMap<>(findByNodeSet.size());
                findByNodeSet.forEach(t -> {
                    findByNodeMap.computeIfAbsent(t.nodeToFind(), k -> new HashSet<>()).add(t);
                });

               transformByEquality(test, currentTest, findByNodeMap);

               System.out.println(test.toPrettyString());

            }
        }
        return null;
    }

    private static void transformByEquality(ContainerNode<?> parentNode, JsonNode currentNode, Map<JsonNode, Set<Transform.FindByNode<? extends JsonNode>>> findByNodeMap) {
        if (currentNode.isArray()) {
            ArrayNode arrayNode = (ArrayNode) currentNode;
            Iterator<JsonNode> node = arrayNode.elements();

            while (node.hasNext()) {

                transformByEquality(arrayNode, node.next(), findByNodeMap);

            }
        } else if (currentNode.isObject()) {
            ObjectNode objectNode = (ObjectNode) currentNode;

            Set<Transform.FindByNode<? extends JsonNode>> findByNodes = findByNodeMap.get(currentNode);

            if (findByNodes != null) {
                findByNodes.forEach(findByNode -> {
                    System.out.println("********************* found it!! [" + findByNode + "]");
                    JsonNode result = findByNode.transform(parentNode);
                    if(result != null) {
                        System.out.println("*** result: " + result);
                        if (parentNode.isObject()) {
                            ObjectNode parentObject = (ObjectNode) parentNode;
                            parentObject.removeAll();
                            parentObject.setAll((ObjectNode) result);
                        }
                    }
                });
            }
            currentNode.fields().forEachRemaining(entry -> transformByEquality(objectNode, entry.getValue(), findByNodeMap));
        } else {
            System.out.println("value: " + currentNode);

        }
    }


}
