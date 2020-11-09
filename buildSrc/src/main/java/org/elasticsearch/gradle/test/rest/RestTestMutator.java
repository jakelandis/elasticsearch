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


import org.apache.log4j.Logger;


public class RestTestMutator {

    private static final Logger logger = Logger.getLogger(RestTestMutator.class);

//    public static Map<String, Set<MutationOld>> parseMutateInstructions(ObjectMapper mapper, YAMLFactory yaml, File file) throws IOException {
//        Map<String, Set<MutationOld>> mutations = new HashMap<>(1);
//        YAMLParser yamlParser = yaml.createParser(file);
//        List<ObjectNode> tests = mapper.readValues(yamlParser, ObjectNode.class).readAll();
//        for (ObjectNode test : tests) {
//            Iterator<Map.Entry<String, JsonNode>> iterator = test.fields();
//            while (iterator.hasNext()) {
//                Map.Entry<String, JsonNode> root = iterator.next();
//                String testName = root.getKey();
//                Iterator<JsonNode> actionsIterator = root.getValue().iterator();
//                while (actionsIterator.hasNext()) {
//                    Iterator<Map.Entry<String, JsonNode>> actionMapIterator = actionsIterator.next().fields();
//                    Map.Entry<String, JsonNode> actionNode;
//                    while (actionMapIterator.hasNext()) {
//                        actionNode = actionMapIterator.next();
//                        MutationType action = MutationType.fromString(actionNode.getKey());
//                        Iterator<JsonNode> actionIterator = actionNode.getValue().iterator();
//                        while (actionIterator.hasNext()) {
//                            Iterator<Map.Entry<String, JsonNode>> actionMap = actionIterator.next().fields();
//                            while (actionMap.hasNext()) {
//                                Map.Entry<String, JsonNode> mutation = actionMap.next();
//                                mutations.computeIfAbsent(testName,
//                                    k -> new HashSet<>()).add(new MutationOld(action, mutation.getKey(), mutation.getValue()));
//                            }
//                        }
//                    }
//                }
//            }
//        }
//        return mutations;
//    }
//
//
//    public static List<ObjectNode> mutateTest(Map<String, Set<MutationOld>> mutations, ObjectMapper mapper, YAMLFactory yaml, File file) throws IOException {
//        JsonNodeFactory jsonNodeFactory = JsonNodeFactory.withExactBigDecimals(false);
//        YAMLParser yamlParser = yaml.createParser(file);
//        List<ObjectNode> tests = mapper.readValues(yamlParser, ObjectNode.class).readAll();
//        List<Pair<String, ObjectNode>> mutatedTests = new ArrayList<>(tests.size());
//        for (ObjectNode test : tests) {
//            Iterator<Map.Entry<String, JsonNode>> iterator = test.fields();
//            while (iterator.hasNext()) {
//                Map.Entry<String, JsonNode> testObject = iterator.next();
//                String testName = testObject.getKey();
//
//                System.out.println("********** Original [" + testName + "] ************* ");
//                System.out.println(testObject.getValue().toPrettyString());
//
//                if(logger.isTraceEnabled()) {
//                    logger.trace("********** Original [" + testName + "] ************* ");
//                    logger.trace(testObject.getValue().toPrettyString());
//                }
//                Set<MutationOld> testMutationOlds = mutations.get(testName);
//                if (testMutationOlds != null && testMutationOlds.isEmpty() == false) {
//                    Map<String, AtomicInteger> keyCounts = new HashMap<>();
//                    //we know that the tests all have an array of do,match,gte, etc. objects directly under the testObject test name
//                    ArrayNode executableArrayNode = (ArrayNode) testObject.getValue();
//                    List<Pair<MutationOld.Location, JsonNode>> executables = new ArrayList<>();
//                    Iterator<JsonNode> arrayValues = executableArrayNode.iterator();
//                    //add the custom location to all of the top level array values
//                    while (arrayValues.hasNext()) {
//                        JsonNode arrayValue = arrayValues.next();
//                        if (JsonNodeType.OBJECT.equals(arrayValue.getNodeType())) {
//                            ObjectNode valueAsObject = (ObjectNode) arrayValue;
//                            Iterator<Map.Entry<String, JsonNode>> objectIterator = valueAsObject.fields();
//                            String key = objectIterator.next().getKey();
//                            assert objectIterator.hasNext() == false : "found unexpected structure for YAML test [" + testName + "]";
//                            int keyCount = keyCounts.computeIfAbsent(key, k -> new AtomicInteger(-1)).incrementAndGet();
//                            MutationOld.Location location = new MutationOld.Location(ExecutableSection.fromString(key), keyCount);
//                            executables.add(Pair.of(location, arrayValue));
//                        }
//                    }
//
//                    List<JsonNode> mutatedInstructions = new ArrayList<>();
//                    if (executables.isEmpty() == false) {
//                        for (Pair<MutationOld.Location, JsonNode> executable : executables) {
//                            Set<MutationOld> foundMutationOlds = testMutationOlds.stream().filter(m -> m.getLocation().equals(executable.getLeft())).collect(Collectors.toSet());
//                            if (foundMutationOlds.isEmpty() == false) {
//                                for (MutationOld foundMutationOld : foundMutationOlds) {
//                                    if (foundMutationOld.getLocation().getDoSectionSub() != null) {
//                                        // we don't want to replace the whole do section, only the specific sub  e.g. do.0.catch
//                                        JsonNode doSectionParent = executable.getRight();
//                                        ObjectNode doSection = (ObjectNode) doSectionParent.get(ExecutableSection.DO.name().toLowerCase(Locale.ROOT));
//                                        Map<String, JsonNode> mutatedDoSectionSubs = new HashMap<>();
//                                        Iterator<Map.Entry<String, JsonNode>> doIterator = doSection.fields();
//                                        while (doIterator.hasNext()) {
//                                            Map.Entry<String, JsonNode> doSectionSubObject = doIterator.next();
//                                            if (DoSectionChildren.fromString(doSectionSubObject.getKey()).isPresent()) {
//                                                String doSectionSubKey = foundMutationOld.getLocation().getDoSectionSub().name().toLowerCase(Locale.ROOT);
//                                                switch (foundMutationOld.getAction()) {
//                                                    case REPLACE:
//                                                    case ADD:
//                                                        //since this is an object, add and replace result in the same output
//                                                        mutatedDoSectionSubs.put(doSectionSubKey, foundMutationOld.getJsonNode());
//                                                        break;
//                                                    case REMOVE:
//                                                        //do nothing
//                                                        break;
//                                                    default:
//                                                        assert false : "replace, remove, add are the only valid actions";
//                                                }
//                                                testMutationOlds.remove(foundMutationOld);
//                                            } else {
//                                                mutatedDoSectionSubs.put(doSectionSubObject.getKey(), doSectionSubObject.getValue());
//                                            }
//                                        }
//                                        doSection.removeAll();
//                                        doSection.setAll(mutatedDoSectionSubs);
//                                        mutatedInstructions.add(doSectionParent);
//                                        testMutationOlds.remove(foundMutationOld);
//                                    } else {
//                                        String sectionName = foundMutationOld.getLocation().getSection().name().toLowerCase(Locale.ROOT);
//                                        switch (foundMutationOld.getAction()) {
//                                            case REPLACE:
//                                                mutatedInstructions.add(new ObjectNode(jsonNodeFactory, Map.of(sectionName, foundMutationOld.getJsonNode())));
//                                                break;
//                                            case REMOVE:
//                                                //do nothing
//                                                break;
//                                            case ADD:
//                                                mutatedInstructions.add(executable.getRight());
//                                                mutatedInstructions.add(new ObjectNode(jsonNodeFactory, Map.of(sectionName, foundMutationOld.getJsonNode())));
//                                                break;
//                                            default:
//                                                assert false : "replace, remove, add are the only valid actions";
//                                        }
//                                        testMutationOlds.remove(foundMutationOld);
//                                    }
//                                }
//                            } else { //preserve original
//                                mutatedInstructions.add(executable.getRight());
//                            }
//                        }
//                    }
//                    //in case we are just adding
//                    //TODO: ensure we get the ordering right.
//                    for (MutationOld mutationOld : testMutationOlds) {
//                        switch (mutationOld.getAction()) {
//                            case ADD:
//                                mutatedInstructions.add(mutationOld.getJsonNode());
//                                testMutationOlds.remove(mutationOld);
//                                break;
//                        }
//                    }
//                    if (testMutationOlds.isEmpty() == false) {
//                        throw new IllegalStateException("mutations were requested, but no matches were found [" + testMutationOlds.stream().map(MutationOld::toString).collect(Collectors.joining(",")) + "]");
//                    }
//                    executableArrayNode.removeAll();
//                    executableArrayNode.addAll(mutatedInstructions);
//                }
//                System.out.println("********** Mutated [" + testName + "] ************* ");
//                System.out.println(testObject.getValue().toPrettyString());
//                if(logger.isTraceEnabled()) {
//                    logger.trace("********** Mutated [" + testName + "] ************* ");
//                    logger.trace(testObject.getValue().toPrettyString());
//                }
//
//                mutatedTests.add(Pair.of(testName, test));
//            }
//        }
//        return tests;
//    }


}
