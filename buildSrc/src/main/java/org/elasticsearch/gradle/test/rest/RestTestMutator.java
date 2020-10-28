/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.elasticsearch.gradle.test.rest;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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


public class RestTestMutator {

    public static Map<String, Set<Mutation>> parseMutateInstructions(ObjectMapper mapper, YAMLFactory yaml, File file) throws IOException {
        Map<String, Set<Mutation>> mutations = new HashMap<>(1);
        YAMLParser yamlParser = yaml.createParser(file);
        List<ObjectNode> tests = mapper.readValues(yamlParser, ObjectNode.class).readAll();
        for (ObjectNode test : tests) {
            Iterator<Map.Entry<String, JsonNode>> iterator = test.fields();
            while (iterator.hasNext()) {
                Map.Entry<String, JsonNode> root = iterator.next();
                String testName = root.getKey();
                Iterator<JsonNode> actionsIterator = root.getValue().iterator();
                while (actionsIterator.hasNext()) {
                    Iterator<Map.Entry<String, JsonNode>> actionMapIterator = actionsIterator.next().fields();
                    Map.Entry<String, JsonNode> actionNode;
                    while (actionMapIterator.hasNext()) {
                        actionNode = actionMapIterator.next();
                        Mutation.Action action = Mutation.Action.fromString(actionNode.getKey());
                        Iterator<JsonNode> actionIterator = actionNode.getValue().iterator();
                        while (actionIterator.hasNext()) {
                            Iterator<Map.Entry<String, JsonNode>> actionMap = actionIterator.next().fields();
                            while (actionMap.hasNext()) {
                                Map.Entry<String, JsonNode> mutation = actionMap.next();
                                mutations.computeIfAbsent(testName,
                                    k -> new HashSet<>()).add(new Mutation(action, mutation.getKey(), mutation.getValue()));
                            }
                        }
                    }
                }
            }
        }
        return mutations;
    }

    public static JsonNode mutateTest(Map<String, Set<Mutation>> mutations, ObjectMapper mapper, YAMLFactory yaml, File file) throws IOException {
        YAMLParser yamlParser = yaml.createParser(file);
        List<ObjectNode> tests = mapper.readValues(yamlParser, ObjectNode.class).readAll();
        for (ObjectNode test : tests) {


            Iterator<Map.Entry<String, JsonNode>> iterator = test.fields();
            while (iterator.hasNext()) {
                Map.Entry<String, JsonNode> root = iterator.next();
                String testName = root.getKey();
                System.out.println("************* " + testName + " ******************** ");
                if(mutations.get(testName) != null){
                    mutations.get(testName).forEach( m -> {
                        System.out.println("mutation path = " + m.getJsonPointer());



                        System.out.println("original: " + root.getValue());
                        System.out.println("--->" + root.getValue().requiredAt(m.getJsonPointer()));
                        JsonNode nodeToMutate = root.getValue().requiredAt(m.getJsonPointer());
                        Iterator<JsonNode> it = root.getValue().iterator();






//                        System.out.println(readContext.jsonString());
//                        System.out.println("----> " + readContext.read("$.['Action to list contexts']..['match'][0]"));
                    });
                }

//                Iterator<JsonNode> childIt = root.getValue().iterator();
//                while (childIt.hasNext()) {
//
//                    print(childIt.next());
//                }

            }
        }

//        mutations.forEach((k,v) -> {
//            System.out.println("***************** "+k+"  ************ ");
//            v.forEach( m -> {
//                System.out.println("** " + m);
//            });
//
//        });




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
}
