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
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLParser;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TransformTest {
    private static final YAMLFactory yaml = new YAMLFactory();
    private static final ObjectMapper mapper = new ObjectMapper(yaml);

    public static Map<String, TestTransformation> readInstructions(File file) throws IOException {
        YAMLParser yamlParser = yaml.createParser(file);
        Map<String, TestTransformation> mutations = new HashMap<>();
        MappingIterator<TestTransformation> it = mapper.readValues(yamlParser, TestTransformation.class);
        while (it.hasNext()) {
            TestTransformation testTransformation = it.next();
            mutations.put(testTransformation.getTestName(), testTransformation);
        }
        return mutations;
    }

    public static List<ObjectNode> mutateTest(File file, Map<String, TestTransformation> mutations) throws IOException {

        YAMLParser yamlParser = yaml.createParser(file);
        List<ObjectNode> tests = mapper.readValues(yamlParser, ObjectNode.class).readAll();

        for (ObjectNode test : tests) {
            Iterator<Map.Entry<String, JsonNode>> testsIterator = test.fields();
            //each file can have multiple tests
            while (testsIterator.hasNext()) {
                Map.Entry<String, JsonNode> testObject = testsIterator.next();
                String testName = testObject.getKey();
                System.out.println("@@ test:" + testName );
                JsonNode currentTest = testObject.getValue();
                TestTransformation testTransformation = mutations.get(testName);
                if(testTransformation == null ){
                    continue;
                }
                System.out.println(currentTest);

                //Get all of the FindByNodes into a keyed map for quick lookup
                Map<JsonNode, Transform.FindByNode<? extends JsonNode>> findByNodeMap = testTransformation.getAllTransforms().stream().filter(a -> a instanceof Transform.FindByNode)
                    .map(e -> (Transform.FindByNode<? extends JsonNode>) e).collect(Collectors.toMap(Transform.FindByNode::nodeToFind, f -> f));

                process(currentTest, findByNodeMap);

            }
        }
        return null;
    }

    private static void print(final JsonNode node) throws IOException {
        Iterator<Map.Entry<String, JsonNode>> fieldsIterator = node.fields();

        while (fieldsIterator.hasNext()) {
            Map.Entry<String, JsonNode> field = fieldsIterator.next();
            final String key = field.getKey();
            System.out.println("Key: " + key);
            final JsonNode value = field.getValue();
            if (value.isContainerNode()) {
                print(value); // RECURSIVE CALL
            } else {
                System.out.println("Value: " + value);
            }
        }
    }

    private static void process(JsonNode currentNode, Map<JsonNode, Transform.FindByNode<? extends JsonNode>> findByNodeMap) {
        if (currentNode.isArray()) {
            ArrayNode arrayNode = (ArrayNode) currentNode;
            Iterator<JsonNode> node = arrayNode.elements();

            while (node.hasNext()) {

                process(node.next(), findByNodeMap);

            }
        }
        else if (currentNode.isObject()) {
            //find by node
            Transform.FindByNode<? extends JsonNode> findByNode =  findByNodeMap.get(currentNode);

            if(findByNode != null){
                System.out.println("********************* found it!! [" + findByNode + "]");
               // findByNodeMap.transform(currentNode);

            }
            currentNode.fields().forEachRemaining(entry -> process(entry.getValue(), findByNodeMap));
        }
        else {
            System.out.println("value: "  +currentNode);

        }
    }


}
