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


import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLParser;
import net.sf.saxon.expr.Component;
import org.apache.commons.lang3.tuple.Pair;
import org.gradle.internal.impldep.org.apache.ivy.osgi.p2.P2Artifact;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;


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

                Set<Mutation> testMutations = mutations.get(testName);
                if (testMutations != null && testMutations.isEmpty() == false) {
                    Map<String, AtomicInteger> keyCounts = new HashMap<>();


                    //we know that the tests all have an array of do,match,gte, etc. objects directly under the root test name
                    ArrayNode testRoot = (ArrayNode) root.getValue();

                    List<Pair<Mutation.Location, JsonNode>> instructions = new ArrayList<>();

                    Iterator<JsonNode> arrayValues = testRoot.iterator();
                    while (arrayValues.hasNext()) {
                        JsonNode arrayValue = arrayValues.next();


                        if (JsonNodeType.OBJECT.equals(arrayValue.getNodeType())) {
                            ObjectNode valueAsObject = (ObjectNode) arrayValue;

                            Iterator<Map.Entry<String, JsonNode>> objectIterator = valueAsObject.fields();

                            String key = objectIterator.next().getKey();

                            int keyCount = keyCounts.computeIfAbsent(key, k -> new AtomicInteger(-1)).incrementAndGet();
                            Mutation.Location location = new Mutation.Location(key, keyCount);

                            instructions.add(Pair.of(location, arrayValue));

                            if (objectIterator.hasNext()) {
                                throw new IllegalStateException("Expected only 1 key per array under test [" + testName + "]" +
                                    " but found multiple keys [" + key + ", " + objectIterator.next().getKey() + " ]. This is likely a bug.");
                            }
                        }


                        //
//                        instructions.add();
                    }


                    System.out.println("********************");
                    instructions.forEach(p -> System.out.println("location: " + p.getLeft() + " node: " + p.getRight()));
                    System.out.println("********************");

                    List<JsonNode> mutatedInstructions = new ArrayList<>();
                    for(Pair<Mutation.Location, JsonNode> instruction : instructions){
                        Optional<Mutation> foundMutation = testMutations.stream().filter(m -> m.getLocation().equals(instruction.getLeft())).findFirst();
                        if(foundMutation.isPresent()) {
                            switch(foundMutation.get().getAction()){
                                case REPLACE:
                                    mutatedInstructions.add(foundMutation.get().getJsonNode());
                                    break;
                                case REMOVE:
                                    //do nothing
                                    break;
                                case ADD_BEFORE:
                                    mutatedInstructions.add(foundMutation.get().getJsonNode());
                                    mutatedInstructions.add(instruction.getRight());
                                case ADD_AFTER:
                                    mutatedInstructions.add(instruction.getRight());
                                    mutatedInstructions.add(foundMutation.get().getJsonNode());
                                    break;
                            }
                        }else{ //preserve original
                            mutatedInstructions.add(instruction.getRight());
                        }

                    }

                    //  keyCounts.forEach((k, v) -> System.out.println("key: " + k + " count: " + v));
                    testRoot.removeAll();
                     testRoot.addAll(mutatedInstructions);


                }
//                    //we know that the tests all have an array of do,match,gte, etc. objects directly under the root test name
//                    List<JsonNode> instructions = new LinkedList<>();
//                    Iterator<JsonNode> it = root.getValue().iterator();
//                    while (it.hasNext()) {
//                        instructions.add(it.next());
//                    }
//                    Mutation mutation = testMutations.iterator().next();
//
//                    String parts[] = mutation.getJsonPointer().split("\\.");
//                    String instruction = parts[0];
//                    int pos = parts.length == 2 ? Integer.parseInt(parts[1]) : -1;
//                    //replace
//                    instructions.set(pos, mutation.getJsonNode());
//                }


                Iterator<JsonNode> childIt = root.getValue().iterator();

                while (childIt.hasNext()) {

                    print(childIt.next());
                }
            }


        }


        return null;
    }


    private static void print(final JsonNode node) throws IOException {
        Iterator<Map.Entry<String, JsonNode>> fieldsIterator = node.fields();
        JsonNode parent = node;

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
