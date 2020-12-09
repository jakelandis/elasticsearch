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
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLParser;


import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class TransformTest {
    private static final YAMLFactory yaml = new YAMLFactory();
    private static final ObjectMapper mapper = new ObjectMapper(yaml);
    private static JsonNodeFactory jsonNodeFactory = JsonNodeFactory.withExactBigDecimals(false);

    public static Map<String, TestTransformation> readTransformations(File file) throws IOException {
        YAMLParser yamlParser = yaml.createParser(file);


        Map<String, TestTransformation> mutations = new HashMap<>();
        //Using data binding over stream parsing since:
        // stream parsing does not understand "---" separators and data binding can neatly organize these by test

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

                JsonNode currentTest = testObject.getValue();
                TestTransformation testTransformation = mutations.get(testName);
                if (testTransformation == null) {
                    continue;
                }
                System.out.println("*************** " + currentTest + " *******************");

                //Always transform by location before find by match since add/remove of JsonNodes the nodes pointed at by the JsonPointer
                List<TransformAction> testTransformationActions = testTransformation.getTestTransformations(testName);
                if (testTransformationActions != null) {
                    Consumer<Transform.FindByLocation> findByLocationTransform = transform -> {
                        JsonNode parentNode = test.at(transform.location().head());
                        if (parentNode != null && parentNode.isContainerNode()) {
                            transform.transform((ContainerNode<?>) parentNode);
                        } else {
                            throw new IllegalArgumentException("Could not find location [" + transform.location() + "]");
                        }
                    };

                    //Transform FindByLocation/Replace first since insert/remove can change the node where the JsonPointer points.
                    testTransformationActions.stream()
                        .filter(a -> a.getTransform() instanceof Transform.FindByLocation)
                        .filter(b -> b instanceof Replace == true)
                        .map(c -> (Transform.FindByLocation) c.getTransform()).forEachOrdered(findByLocationTransform);

                    //Transform FindByLocation/Insert and FindByLocation/Remove
                    testTransformationActions.stream()
                        .filter(a -> a.getTransform() instanceof Transform.FindByLocation)
                        .map(b -> (Transform.FindByLocation) b.getTransform()).forEachOrdered(findByLocationTransform);

                    //Map all the FindByMatch Actions by the JsonNode they need to match
                    Map<JsonNode, TransformAction> findByMatchTransforms = testTransformationActions.stream()
                        .filter(a -> a.getTransform() instanceof Transform.FindByMatch)
                        .collect(Collectors.toMap(b -> ((Transform.FindByMatch) b.getTransform()).nodeToFind(), c -> c));

                    //Transform all FindByMatch's
                    transformByMatch(findByMatchTransforms, test, currentTest);
                }
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
            System.out.println("Current Object: "  +currentNode + " type: " + currentNode.getNodeType());
            TransformAction transformAction = findByMatchTransforms.remove(objectNode);
            if (transformAction != null) {
                transformAction.transform(parentNode);
            }
            currentNode.fields().forEachRemaining(entry -> {
                ObjectNode objectItem = new ObjectNode(jsonNodeFactory);
                objectItem.set(entry.getKey(), entry.getValue());
                TransformAction action = findByMatchTransforms.remove(objectItem);
                if (action != null) {
                    action.transform(objectNode);
                }
                transformByMatch(findByMatchTransforms, objectNode, entry.getValue());
            });
        }


        else {
            //value node
          //  System.out.println("Value Node: " + currentNode);
            TransformAction transformAction = findByMatchTransforms.remove(currentNode);
            if (transformAction != null) {
                transformAction.transform(parentNode);
            }
        }
    }
}
