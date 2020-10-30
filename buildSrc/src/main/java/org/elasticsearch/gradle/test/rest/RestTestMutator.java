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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLParser;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
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
                System.out.println("********** Original ["+testName+"] ************* ");
                System.out.println(root.getValue().toPrettyString());
                Set<Mutation> testMutations = mutations.get(testName);
                if (testMutations != null && testMutations.isEmpty() == false) {
                    Map<String, AtomicInteger> keyCounts = new HashMap<>();
                    //we know that the tests all have an array of do,match,gte, etc. objects directly under the root test name
                    ArrayNode testRoot = (ArrayNode) root.getValue();
                    List<Pair<Mutation.Location, JsonNode>> executables = new ArrayList<>();
                    Iterator<JsonNode> arrayValues = testRoot.iterator();
                    //add the custom location to all of the top level array values
                    while (arrayValues.hasNext()) {
                        JsonNode arrayValue = arrayValues.next();
                        if (JsonNodeType.OBJECT.equals(arrayValue.getNodeType())) {
                            ObjectNode valueAsObject = (ObjectNode) arrayValue;
                            Iterator<Map.Entry<String, JsonNode>> objectIterator = valueAsObject.fields();
                            String key = objectIterator.next().getKey();
                            assert objectIterator.hasNext() == false : "found unexpected structure for YAML test [" + testName + "]";
                            int keyCount = keyCounts.computeIfAbsent(key, k -> new AtomicInteger(-1)).incrementAndGet();
                            Mutation.Location location = new Mutation.Location(Mutation.ExecutableSection.fromString(key), keyCount);
                            executables.add(Pair.of(location, arrayValue));
                        }
                    }

                    List<JsonNode> mutatedInstructions = new ArrayList<>();
                    if (executables.isEmpty() == false) {
                        for (Pair<Mutation.Location, JsonNode> executable : executables) {
                            Set<Mutation> foundMutations = testMutations.stream().filter(m -> m.getLocation().equals(executable.getLeft())).collect(Collectors.toSet());
                            if (foundMutations.isEmpty() == false) {
                                for (Mutation foundMutation : foundMutations) {
                                    foundMutation.getJsonNode();
                                    if(foundMutation.getLocation().getDoSectionSub() != null){

                                        // we don't want to replace the whole do section, only the specific sub  e.g. do.0.catch
                                        JsonNode doSectionParent = executable.getRight();
                                        ObjectNode doSection = (ObjectNode) doSectionParent.get(Mutation.ExecutableSection.DO.name().toLowerCase(Locale.ROOT));
                                        Map<String, JsonNode> mutatedDoSectionSubs = new HashMap<>();
                                        Iterator<Map.Entry<String, JsonNode>> doIterator = doSection.fields();
                                        while(doIterator.hasNext()){
                                            Map.Entry<String, JsonNode> doSectionSubObject = doIterator.next();
                                            if(Mutation.DoSectionSub.fromString(doSectionSubObject.getKey()).isPresent()){
                                                String doSectionSubKey = foundMutation.getLocation().getDoSectionSub().name().toLowerCase(Locale.ROOT);
                                                switch (foundMutation.getAction()) {
                                                    case REPLACE:
                                                        mutatedDoSectionSubs.put(doSectionSubKey, foundMutation.getJsonNode());
                                                        testMutations.remove(foundMutation);
                                                        break;
                                                    case REMOVE:
                                                        //do not add to list
                                                        testMutations.remove(foundMutation);
                                                        break;
                                                    case ADD_BEFORE:
                                                        mutatedDoSectionSubs.put(doSectionSubObject.getKey(), doSectionSubObject.getValue());
                                                        mutatedDoSectionSubs.put(doSectionSubKey, foundMutation.getJsonNode());
                                                        testMutations.remove(foundMutation);
                                                        break;
                                                    case ADD_AFTER:
                                                        mutatedDoSectionSubs.put(doSectionSubKey, foundMutation.getJsonNode());
                                                        mutatedDoSectionSubs.put(doSectionSubObject.getKey(), doSectionSubObject.getValue());
                                                        testMutations.remove(foundMutation);
                                                        break;
                                                }
                                            }else{
                                                mutatedDoSectionSubs.put(doSectionSubObject.getKey(), doSectionSubObject.getValue());
                                            }
                                        }

                                        doSection.removeAll();
                                        doSection.setAll(mutatedDoSectionSubs);
                                        mutatedInstructions.add(doSectionParent);
                                        testMutations.remove(foundMutation);


                                    } else {
                                        switch (foundMutation.getAction()) {
                                            case REPLACE:
                                                mutatedInstructions.add(foundMutation.getJsonNode());
                                                testMutations.remove(foundMutation);
                                                break;
                                            case REMOVE:
                                                //do not add to list
                                                testMutations.remove(foundMutation);
                                                break;
                                            case ADD_BEFORE:
                                                mutatedInstructions.add(foundMutation.getJsonNode());
                                                testMutations.remove(foundMutation);
                                                mutatedInstructions.add(executable.getRight());
                                                break;
                                            case ADD_AFTER:
                                                mutatedInstructions.add(executable.getRight());
                                                mutatedInstructions.add(foundMutation.getJsonNode());
                                                testMutations.remove(foundMutation);
                                                break;
                                        }
                                    }
                                }
                            } else { //preserve original
                                mutatedInstructions.add(executable.getRight());
                            }
                        }
                    }
                    //in case we are just adding
                    //TODO: ensure we get the ordering right.
                    for (Mutation mutation : testMutations) {
                        switch (mutation.getAction()) {
                            case ADD_BEFORE:
                                mutatedInstructions.add(mutation.getJsonNode());
                                testMutations.remove(mutation);
                                break;

                            case ADD_AFTER:
                                mutatedInstructions.add(mutation.getJsonNode());
                                testMutations.remove(mutation);
                                break;
                        }
                    }


                    if (testMutations.isEmpty() == false) {
                        throw new IllegalStateException("mutations were requested, but no matches were found [" + testMutations.stream().map(Mutation::toString).collect(Collectors.joining(",")) + "]");
                    }

                    testRoot.removeAll();
                    testRoot.addAll(mutatedInstructions);
                }



                System.out.println("********** FINAL ["+testName+"] ************* ");
                System.out.println(root.getValue().toPrettyString());
            }

        }

        return null;
    }



}
