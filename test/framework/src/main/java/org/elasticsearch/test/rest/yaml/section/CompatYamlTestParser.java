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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.collect.Tuple;
import org.elasticsearch.common.xcontent.XContentParser;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.junit.Assert.assertThat;


public class CompatYamlTestParser {

    public static void parse(XContentParser parser) throws IOException {
        Map<String, Mutation> matchMutations = new HashMap<>();
        Map<String, Object> yaml = parser.map();
        yaml.forEach((testName, topLevelContent) -> {
            if (topLevelContent instanceof List) {
                List<Map<String, List<Map<String, ?>>>> topLevelList = (List<Map<String, List<Map<String, ?>>>>) topLevelContent;
                topLevelList.stream().flatMap(c -> c.entrySet().stream()).forEach((e) -> {
                    String executionSection = e.getKey();
                    Mutation mutation = null;
                    for (Map<String, ?> section : e.getValue()) {
                        Object mutant = null;
                        int index = 0;
                        Mutation.Action action = null;
                        for (Map.Entry<String, ?> actionOrIndex : section.entrySet()) {
                            if ("index".equals(actionOrIndex.getKey())) {
                                index = (int) actionOrIndex.getValue();
                            } else {
                                action = Mutation.Action.fromString(actionOrIndex.getKey());
                                mutant = actionOrIndex.getValue();
                            }
                        }
                        mutation = new Mutation(action, index, mutant);
                    }
                    if ("match".equals(executionSection)) {
                        assert mutation != null;
                        matchMutations.put(testName, mutation);
                    } else {
                        throw new RuntimeException("only match mutations are supported");
                    }
                });
            } else {
                throw new RuntimeException("hey this should be a list !!");
            }
        });
        matchMutations.forEach((k, v) -> System.out.println("**************** " + k + " ::" + v));
    }

    static class Mutation {
        enum Action {
            REPLACE,
            REMOVE,
            ADD;
            static Action fromString(String actionString) {
                return EnumSet.allOf(Action.class).stream().filter(a -> a.name().equalsIgnoreCase(actionString))
                    .findFirst().orElseThrow(() -> new IllegalArgumentException("Invalid action."));
            }
        }

        private final Mutation.Action action;
        private final int index;
        private final Object mutant;

        public Mutation(Mutation.Action action, int index, Object mutant) {
            this.action = Objects.requireNonNull(action);
            this.index = index;
            this.mutant = mutant;
        }

        @Override
        public String toString() {
            return "Mutation{" +
                "action=" + action +
                ", index=" + index +
                ", mutant=" + mutant +
                '}';
        }
    }
}
