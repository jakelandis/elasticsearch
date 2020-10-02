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
package org.elasticsearch.test.rest.yaml.section;

import org.elasticsearch.common.xcontent.XContentParser;

import java.io.IOException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;


public class CompatYamlTestParser {

    public static Map<MutationSection, Set<Mutation>> parse(XContentParser parser) throws IOException {
        Map<MutationSection, Set<Mutation>> allMutations = new HashMap<>();
        Set<Mutation> matchMutations = new HashSet<>();
        allMutations.put(MutationSection.MATCH, matchMutations);
        Map<String, Object> yaml = parser.map();
        yaml.forEach((testName, topLevelContent) -> {
            if (topLevelContent instanceof List) {
                List<Map<String, List<Map<String, ?>>>> topLevelList = (List<Map<String, List<Map<String, ?>>>>) topLevelContent;
                topLevelList.stream().flatMap(c -> c.entrySet().stream()).forEach((e) -> {
                    Mutation.Action action = Mutation.Action.fromString(e.getKey());
                    for (Map<String, ?> section : e.getValue()) {
                        Object mutant = null;
                        int index = 0;
                        String executionSection = null;
                        for (Map.Entry<String, ?> indexOrExecutionSection : section.entrySet()) {
                            if ("index".equals(indexOrExecutionSection.getKey())) {
                                index = (int) indexOrExecutionSection.getValue();
                            } else {
                                executionSection = indexOrExecutionSection.getKey();
                                mutant = indexOrExecutionSection.getValue();
                            }
                        }
                        if ("match".equals(executionSection)) {
                            matchMutations.add(new Mutation(testName, action, index, mutant));
                        } else {
                            throw new RuntimeException("only match mutations are supported");
                        }
                    }
                });
            } else {
                throw new RuntimeException("hey this should be a list !!");
            }
        });
        allMutations.forEach(((mutationSection, m) -> {
            m.forEach((mutation) -> System.out.println("************** --> " + mutationSection.name() + "::" + mutation));
        }));
        return allMutations;
    }

    public static class Mutation {
        enum Action {
            REPLACE,
            REMOVE,
            ADD;

            static Action fromString(String actionString) {
                return EnumSet.allOf(Action.class).stream().filter(a -> a.name().equalsIgnoreCase(actionString))
                    .findFirst().orElseThrow(() -> new IllegalArgumentException("Invalid action."));
            }
        }

        private final String testName;
        private final Mutation.Action action;
        private final int index;
        private final Object mutant;

        public Mutation(String testName, Mutation.Action action, int index, Object mutant) {
            this.testName = testName;
            this.action = Objects.requireNonNull(action);
            this.index = index;
            this.mutant = mutant;
        }

        @Override
        public String toString() {
            return "Mutation{" +
                "testName='" + testName + '\'' +
                ", action=" + action +
                ", index=" + index +
                ", mutant=" + mutant +
                '}';
        }
    }

    public enum MutationSection {
        MATCH
    }


}
