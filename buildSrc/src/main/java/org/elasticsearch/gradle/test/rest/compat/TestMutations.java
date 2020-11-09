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

import java.util.List;
import java.util.Map;

public class TestMutations {

    private String testName;
    private ReplaceAction replaceAction;
    private AddAction addAction;
    private RemoveAction removeAction;

    @JsonAnySetter
    public void testName(String testName,  Map<String, List<ActionItem>> actions) {
        this.testName = testName;
        this.replaceAction = new ReplaceAction(actions.get("replace"));
        this.addAction = new AddAction(actions.get("add"));
        this.removeAction = new RemoveAction(actions.get("remove"));

    }
    public String getTestName() {
        return testName;
    }

    @Override
    public String toString() {
        return "TestMutations{" +
            "testName='" + testName + '\'' +
            ", replaceAction=" + replaceAction +
            ", addAction=" + addAction +
            ", removeAction=" + removeAction +
            '}';
    }


}





