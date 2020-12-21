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
import com.carrotsearch.randomizedtesting.generators.RandomNumbers;
import com.carrotsearch.randomizedtesting.generators.RandomPicks;
import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;

public class InsertObjectTests extends RandomizedTest {


    private static JsonNodeFactory jsonNodeFactory = JsonNodeFactory.withExactBigDecimals(false);
    private ObjectNode testNode;

    private RandomJson randomJson;
    private String existingKeyName;
    private String nonExistingKeyName;
    private JsonPointer existingLocation;
    private JsonPointer nonExistingLocation;
    private JsonNode valueToInsert;
    private static final String FIND = "find";
    private static final String INSERT = "insert";
    private static final String TEST_NAME = "test_name";

    @Before
    public void setup() {
        randomJson = new RandomJson(getRandom());
        // a rest test is always a single test name object with an inner array or objects
        testNode = new ObjectNode(jsonNodeFactory);
        testNode.set(TEST_NAME, randomJson.randomJsonArrayOfObjects());
        System.out.println("****************");
        System.out.println(testNode);
        System.out.println("****************");
        // safe casting due to how this is constructed in setup
        ArrayNode arrayNode = (ArrayNode) testNode.get(TEST_NAME);
        int arrayPosition = RandomNumbers.randomIntBetween(getRandom(), 1, arrayNode.size()) - 1;
        System.out.println("Array position: " +  arrayPosition);
        ObjectNode objectNode = (ObjectNode) arrayNode.get(arrayPosition);
        Iterator<Map.Entry<String, JsonNode>> arrayFields = objectNode.fields();

        //find an existing key name and non-existing key name
        List<String> keyNames = new ArrayList<>();
        while(arrayFields.hasNext()){
            Map.Entry<String, JsonNode> objectInArray = arrayFields.next();
            keyNames.add(objectInArray.getKey());
        }
        existingKeyName = RandomPicks.randomFrom(getRandom(), keyNames);
        System.out.println("keyName: " + existingKeyName);
        nonExistingKeyName = RandomizedTest.randomAsciiAlphanumOfLength(5);
        assert keyNames.contains(existingKeyName) == true;
        assert keyNames.contains(nonExistingKeyName) == false;

        existingLocation = JsonPointer.compile("/" + TEST_NAME + "/" + arrayPosition + "/" + existingKeyName);
        nonExistingLocation = JsonPointer.compile("/" + TEST_NAME + "/" + arrayPosition + "/" + nonExistingLocation);

        valueToInsert = randomJson.getRandomJsonNode();
    }

    //Insert into existing should over write the existing value
    @Test
    public void testInsertIntoExistingObjectByTextMatch() {

        Map<String, JsonNode> findByTextType = new HashMap<>();
        findByTextType.put(FIND, TextNode.valueOf(existingKeyName));
        findByTextType.put(INSERT, valueToInsert);

        Insert insert = new Insert(TEST_NAME, findByTextType);
        assertThat(insert.getTransform(), instanceOf(Insert.ByMatch.class));

        System.out.println("------------");
        System.out.println(testNode.at(existingLocation));
        System.out.println("------------");



    }

}
