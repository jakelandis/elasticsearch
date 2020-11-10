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

public class MutateTest {
    private static final YAMLFactory yaml = new YAMLFactory();
    private static final ObjectMapper mapper = new ObjectMapper(yaml);

    public static Map<String, TestMutations> readInstructions(File file) throws IOException {
        YAMLParser yamlParser = yaml.createParser(file);
        Map<String, TestMutations> mutations = new HashMap<>();
        MappingIterator<TestMutations> it = mapper.readValues(yamlParser, TestMutations.class);
        while (it.hasNext()) {
            TestMutations testMutations = it.next();
            mutations.put(testMutations.getTestName(), testMutations);
        }
        return mutations;
    }

    public static List<ObjectNode> mutateTest(File file, Map<String, TestMutations> mutations) throws IOException {

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
                TestMutations testMutations = mutations.get(testName);
                if(testMutations == null ){
                    continue;
                }
                System.out.println(currentTest);

                //Get the instructions
                AddAction additions = testMutations.getAddAction();
                RemoveAction remove = testMutations.getRemoveAction();
                ReplaceAction replace = testMutations.getReplaceAction();
                //build a map of find by objects to the instruction
                Map<ObjectNode, Instruction> findByObjects = additions.getAdditions().stream()
                    .filter(a -> a instanceof Find.ByObject).collect(Collectors.toMap(e -> ((ObjectNode) e.find()), e -> e));
                findByObjects.putAll(remove.getRemovals().stream()
                    .filter(a -> a instanceof Find.ByObject).collect(Collectors.toMap(e -> ((ObjectNode) e.find()), e -> e)));
                findByObjects.putAll(replace.getReplacements().stream()
                    .filter(a -> a instanceof Find.ByObject).collect(Collectors.toMap(e -> ((ObjectNode) e.find()), e -> e)));

                process(currentTest, findByObjects);

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

    private static void process(JsonNode currentNode, Map<ObjectNode, Instruction> findByObjects) {
        if (currentNode.isArray()) {
            ArrayNode arrayNode = (ArrayNode) currentNode;
            Iterator<JsonNode> node = arrayNode.elements();

            while (node.hasNext()) {

                process(node.next(), findByObjects);

            }
        }
        else if (currentNode.isObject()) {
            Instruction instruction = findByObjects.get(currentNode);
            System.out.println(currentNode);
            if(instruction != null){
                System.out.println("********************* found it!! [" + instruction + "]");
            }
            currentNode.fields().forEachRemaining(entry -> process(entry.getValue(), findByObjects));
        }
        else {
            System.out.println("value: "  +currentNode);

        }
    }


}
