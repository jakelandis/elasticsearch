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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * The complete set of {@link Transformation}'s and {@link Transform} per test.
 */
public class TestTransformation {
    private String testName;
    private ReplaceTransformation replaceTransformation;
    private AddTransformation addTransformation;
    private RemoveTransformation removeTransformation;
    private List<Transform> allTransforms;

    @JsonAnySetter
    public void testName(String testName, Map<String, List<TransformKeyValue>> actions) {
        this.testName = testName;
        this.replaceTransformation = new ReplaceTransformation(testName, actions.get("replace") == null ? Collections.emptyList() : actions.get("replace") );
        this.addTransformation = new AddTransformation(testName, actions.get("add") == null ? Collections.emptyList() : actions.get("add") );
        this.removeTransformation = new RemoveTransformation(testName, actions.get("remove") == null ? Collections.emptyList() : actions.get("remove") );
        allTransforms = new ArrayList<>(
            replaceTransformation.getTransforms().size()
                + addTransformation.getTransforms().size()
                + removeTransformation.getTransforms().size()
        );
        allTransforms.addAll(replaceTransformation.getTransforms());
        allTransforms.addAll(addTransformation.getTransforms());
        allTransforms.addAll(removeTransformation.getTransforms());
    }

    public String getTestName() {
        return testName;
    }

    public List<Transform> getAllTransforms() {
        return allTransforms;
    }
}



