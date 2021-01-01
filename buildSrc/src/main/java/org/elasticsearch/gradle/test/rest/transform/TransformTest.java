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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLParser;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Utility class to read the transformations from disk and then perform the in-memory transformation for the REST tests.
 * This class does not serialize the transformed tests to back to disk.
 */
public class TransformTest {
    //Utility class
    private TransformTest() {
    }

    private static final YAMLFactory yaml = new YAMLFactory();
    private static final ObjectMapper mapper = new ObjectMapper(yaml);
    private static JsonNodeFactory jsonNodeFactory = JsonNodeFactory.withExactBigDecimals(false);

    /**
     * Get the list of {@link TransformAction} grouped by test name.
     *
     * @param file The YAML file that contains the definitions of each find/action
     * @return the Map of test names to list of {@link TransformAction}
     * @throws IOException if Jackson throws em
     */
    public static Map<String, List<TransformAction>> getTransformationsByTestName(File file) throws IOException {
        YAMLParser yamlParser = yaml.createParser(file);
        Map<String, List<TransformAction>> transformsByTestName = new HashMap<>();
        MappingIterator<TransformActions> it = mapper.readValues(yamlParser, TransformActions.class);
        while (it.hasNext()) {
            TransformActions transforms = it.next();
            transformsByTestName.putIfAbsent(transforms.getTestName(), transforms.getTransforms());
        }
        return transformsByTestName;
    }

    /**
     * In memory transformation of REST tests per given file.
     *
     * @param file       The YAML file that contains the REST tests to transform.
     * @param transforms the Map of test names to list of {@link TransformAction}
     * @return the list of root nodes to serialize
     * @throws IOException if Jackson throws em
     */
    public static List<ObjectNode> transformRestTests(File file, Map<String, List<TransformAction>> transforms) throws IOException {
        YAMLParser yamlParser = yaml.createParser(file);
        List<ObjectNode> tests = mapper.readValues(yamlParser, ObjectNode.class).readAll();
        return innerTransformRestTests(tests, transforms);
    }

    static List<ObjectNode> innerTransformRestTests(List<ObjectNode> tests, Map<String, List<TransformAction>> transforms){
        //A YAML file can have multiple tests
        for (ObjectNode test : tests) {
            Iterator<Map.Entry<String, JsonNode>> testsIterator = test.fields();
            //each file can have multiple tests
            while (testsIterator.hasNext()) {
                Map.Entry<String, JsonNode> testObject = testsIterator.next();
                String testName = testObject.getKey();

                JsonNode currentTest = testObject.getValue();
                List<TransformAction> testTransformation = transforms.get(testName);
                if (testTransformation == null || testTransformation.isEmpty()) {
                    continue;
                }
                System.out.println("*************** " + currentTest + " *******************");

                //Always transform by location before find by match since add/remove of JsonNodes the nodes pointed at by the JsonPointer


                Consumer<Transform.FindByLocation> findByLocationTransform = transform -> {
                    JsonNode parentNode = test.at(transform.location().head());
                    if (parentNode != null && parentNode.isContainerNode()) {
                        transform.transform((ContainerNode<?>) parentNode);
                    } else {
                        throw new IllegalArgumentException("Could not find location [" + transform.location() + "]");
                    }
                };

                //Transform FindByLocation/Replace first since insert/remove can change the node where the JsonPointer points.
                testTransformation.stream()
                    .filter(a -> a.getTransform() instanceof Transform.FindByLocation)
                    .filter(b -> b instanceof Replace == true)
                    .map(c -> (Transform.FindByLocation) c.getTransform()).forEachOrdered(findByLocationTransform);

                //Transform FindByLocation/Insert and FindByLocation/Remove
                testTransformation.stream()
                    .filter(a -> a.getTransform() instanceof Transform.FindByLocation)
                    .map(b -> (Transform.FindByLocation) b.getTransform()).forEachOrdered(findByLocationTransform);

                //Map all the FindByMatch Actions by the JsonNode they need to match
                Map<JsonNode, TransformAction> findByMatchTransforms = testTransformation.stream()
                    .filter(a -> a.getTransform() instanceof Transform.FindByMatch)
                    .collect(Collectors.toMap(b -> ((Transform.FindByMatch) b.getTransform()).nodeToFind(), c -> c));

                //Transform all FindByMatch's
                transformByMatch(findByMatchTransforms, test, currentTest);

                //error if left over.
                //if insert... can only find by object keys to insert.

            }
            System.out.println(test.toPrettyString());
        }
        return tests;

    }

    private static void transformByMatch(Map<JsonNode, TransformAction> findByMatchTransforms, ContainerNode<?> parentNode, JsonNode currentNode) {

        if (currentNode.isArray()) {
            ArrayNode arrayNode = (ArrayNode) currentNode;
            Iterator<JsonNode> node = arrayNode.elements();
            while (node.hasNext()) {
//                JsonNode arrayItem = node.next();
//                TransformAction transformAction = findByMatchTransforms.remove(arrayItem);
//                if (transformAction != null) {
//                    transformAction.transform(parentNode);
//                }
                transformByMatch(findByMatchTransforms, arrayNode, node.next());
            }
        } else if (currentNode.isObject()) {
            ObjectNode objectNode = (ObjectNode) currentNode;
            System.out.println("Current Object: " + currentNode + " type: " + currentNode.getNodeType());
            TransformAction transformAction = findByMatchTransforms.remove(objectNode);
            if (transformAction != null) {
                transformAction.transform(parentNode);
            }
            currentNode.fields().forEachRemaining(entry -> {
                //check if we are finding an object key
                TransformAction action = findByMatchTransforms.get(TextNode.valueOf(entry.getKey()));
                if (action != null) {
                    action.transform(objectNode);
                }
                transformByMatch(findByMatchTransforms, objectNode, entry.getValue());
            });
        } else {
            //value node

            TransformAction transformAction = findByMatchTransforms.remove(currentNode);
            if (transformAction != null) {
                transformAction.transform(parentNode);
            }
        }
    }
}
