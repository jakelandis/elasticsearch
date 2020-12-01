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

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

/**
 * The complete set of {@link Transformation}'s and {@link Transform} per test.
 */
public class TestTransformation {
    private String testName;

    private enum Action {REPLACE, INSERT, REMOVE}

    Map<String, Set<Transform>> testTransformations = new HashMap<>();

    @JsonAnySetter
    public void testName(String testName, List<Map<String, JsonNode>> transforms) {
        this.testName = testName;
        System.out.println("************** " + testName + " *********************");

        for (Map<String, JsonNode> transform : transforms) {
            boolean hasFindKey = false;
            Action action = null;
            System.out.println("-----------------");

            for (Map.Entry<String, JsonNode> entry : transform.entrySet()) {
                final String actionString = entry.getKey();
                switch (actionString) {
                    case "find":
                        hasFindKey = true;
                        break;
                    case "replace":
                        action = Action.REPLACE;
                        break;
                    case "insert":
                        action = Action.INSERT;
                        break;
                    case "remove":
                        action = Action.REMOVE;
                        break;
                    default:
                        throw new IllegalArgumentException("Found invalid action [" + action + "] for test [" + testName + "]");
                }
            }
            if (hasFindKey == false) {
                throw new IllegalArgumentException("Test [" + testName + "] does not define a 'find' entry");
            }
            switch (action) {
                case REPLACE:
                    break;
                case INSERT:
                    testTransformations.computeIfAbsent(testName, k -> new HashSet<>()).add(new Insert(transform));
                    break;
                case REMOVE:
                    break;
                default:
                    throw new IllegalArgumentException("Test [" + testName + "] does not define a valid action. Valid actions are [" + Arrays.toString(Action.values()) + "]");

            }
        }
    }

    public String getTestName() {
        return testName;
    }

    //TODO: DELETE ME !!
    public List<Transform> getAllTransforms() {
        return null;
    }


}



