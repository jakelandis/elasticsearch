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

import com.carrotsearch.randomizedtesting.RandomizedTest;
import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsEqual.equalTo;

public class InsertObjectTests extends RandomizedTest {


    private static JsonNodeFactory jsonNodeFactory = JsonNodeFactory.withExactBigDecimals(false);
    private static final String FIND = "find";
    private static final String INSERT = "insert";

    private ObjectNode restTestObjectNode;
    private RandomRestTest randomRestTest;
    private String existingKeyName;
    private String nonExistingKeyName;
    private JsonPointer existingLocation;
    private JsonPointer nonExistingLocation;
    private JsonNode valueToInsert;
    private static String testName;

    @Before
    public void setup() {
        System.out.println("Seed: " + getContext().getRunnerSeedAsString());
        randomRestTest = new RandomRestTest();
        testName = RandomizedTest.randomAsciiAlphanumOfLengthBetween(2, 8) + " " + RandomizedTest.randomAsciiAlphanumOfLengthBetween(2, 8);
        restTestObjectNode = randomRestTest.getRandomRestTest(testName);

        // safe casting due to how this is constructed. top level root object->array of single top level root objects
        ArrayNode arrayNode = (ArrayNode) restTestObjectNode.get(testName);
        int arrayPosition = RandomizedTest.randomIntBetween(1, arrayNode.size()) - 1;
        System.out.println("Array position: " + arrayPosition);
        ObjectNode objectNode = (ObjectNode) arrayNode.get(arrayPosition);
        Iterator<Map.Entry<String, JsonNode>> arrayFields = objectNode.fields();

        //find an existing key name and non-existing key name
        List<String> keyNames = new ArrayList<>();
        while (arrayFields.hasNext()) {
            Map.Entry<String, JsonNode> objectInArray = arrayFields.next();
            keyNames.add(objectInArray.getKey());
        }
        existingKeyName = RandomizedTest.randomFrom(keyNames);
        System.out.println("keyName: " + existingKeyName);
        nonExistingKeyName = RandomizedTest.randomAsciiAlphanumOfLength(5);
        assert keyNames.contains(existingKeyName) == true;
        assert keyNames.contains(nonExistingKeyName) == false;

        existingLocation = JsonPointer.compile("/" + testName + "/" + arrayPosition + "/" + existingKeyName);
        nonExistingLocation = JsonPointer.compile("/" + testName + "/" + arrayPosition + "/" + nonExistingLocation);



        valueToInsert = randomRestTest.getRandomJsonNode();
    }

    //Insert into existing should over write the existing value
    @Test
    public void testInsertIntoExistingObjectByTextMatch() {

        Map<String, JsonNode> findByTextType = new HashMap<>();
        findByTextType.put(FIND, TextNode.valueOf(existingKeyName));
        findByTextType.put(INSERT, valueToInsert);

        Insert insert = new Insert(testName, findByTextType);
        assertThat(insert.getTransform(), instanceOf(Insert.ByMatch.class));

        JsonNode existingValue = restTestObjectNode.at(existingLocation);
        System.out.println(existingValue);


        //Consider just hard coding this one against a known place since we can really only support a subset for insertion

        // also consider changing the verbage to be find_by_key , find_by_value, find_by_location... and insert_into_object / insert_into_array

        System.out.println("****************");
        System.out.println(randomRestTest);
        System.out.println("****************");


        transformTest(insert);

        JsonNode newValue = restTestObjectNode.at(existingLocation);
        assertThat(newValue, equalTo(valueToInsert));
        JsonNodeType a = existingValue.getNodeType();
        System.out.println("------------");
        System.out.println(restTestObjectNode.at(existingLocation));
        System.out.println("------------");

    }


    private List<ObjectNode> transformTest(TransformAction transform) {
        return transformTest(Collections.singletonList(transform));
    }

    private List<ObjectNode> transformTest(List<TransformAction> transforms) {
        return TransformTest.innerTransformRestTests(Collections.singletonList(restTestObjectNode),
            Collections.singletonMap(testName, transforms));
    }





}
