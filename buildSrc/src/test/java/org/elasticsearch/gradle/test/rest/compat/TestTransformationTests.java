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

import org.elasticsearch.gradle.test.GradleUnitTestCase;

import java.io.File;
import java.util.Map;

public class TestTransformationTests extends GradleUnitTestCase {

    public void testSingleTest() throws Exception {

        File singleTest = new File(getClass().getResource("/10_single.yml").toURI());
        TransformTest.readTransformations(singleTest);


    }

    public void testMultipleTests() throws Exception {

        File instructions = new File(getClass().getResource("/20_multiple.yml").toURI());

        File originalTest = new File(getClass().getResource("/71_context_api.yml").toURI());
        Map<String, TestTransformation> mutations = TransformTest.readTransformations(instructions);
        mutations.forEach((k,v) -> System.out.println("** Found mutations for test: " + k + "\n" + v) );
        TransformTest.transformTest(originalTest, mutations );



    }


}
