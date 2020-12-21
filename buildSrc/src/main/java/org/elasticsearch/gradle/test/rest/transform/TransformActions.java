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

package org.elasticsearch.gradle.test.rest.transform;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Target for data binding.. using data binding since stream parsing directly does not handle multiple YAML files.
 */
public class TransformActions {
    private String testName;

    private enum Action {REPLACE, INSERT, REMOVE}


    List<TransformAction> transforms = new ArrayList<>();

    @JsonAnySetter
    public void testName(String testName, List<Map<String, JsonNode>> transforms) {
        this.testName = testName;
        for (Map<String, JsonNode> transform : transforms) {
            boolean hasFindKey = false;
            Action action = null;
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
                    this.transforms.add(new Replace(testName, transform));
                    break;
                case INSERT:
                    this.transforms.add(new Insert(testName, transform));
                    break;
                case REMOVE:
                    this.transforms.add(new Remove(testName, transform));
                    break;
                default:
                    throw new IllegalArgumentException("Test [" + testName + "] does not define a valid action. Valid actions are [" + Arrays.toString(Action.values()) + "]");

            }
        }
    }

    public String getTestName() {
        return testName;
    }

    public List<TransformAction> getTransforms() {
        return transforms;
    }
}



