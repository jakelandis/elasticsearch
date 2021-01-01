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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BigIntegerNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.FloatNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import java.math.BigInteger;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static com.fasterxml.jackson.databind.node.JsonNodeType.ARRAY;
import static com.fasterxml.jackson.databind.node.JsonNodeType.BOOLEAN;
import static com.fasterxml.jackson.databind.node.JsonNodeType.NUMBER;
import static com.fasterxml.jackson.databind.node.JsonNodeType.OBJECT;
import static com.fasterxml.jackson.databind.node.JsonNodeType.STRING;

public class RandomRestTest {


    private int maxNesting;
    private int nestingCount;

    private enum ExecutableSection {DO, SET, TRANSFORM_AND_SET, MATCH, IS_TRUE, IS_FALSE, GT, GTE, LTE, CONTAINS, LENGTH}


    public RandomRestTest() {
        //helps to prevent stack overflow by too much nesting/recursion
        this.maxNesting = 20;
    }

    private static JsonNodeFactory jsonNodeFactory = JsonNodeFactory.withExactBigDecimals(false);

    private EnumSet<JsonNodeType> nodeTypes = EnumSet.of(ARRAY, BOOLEAN, NUMBER, OBJECT, STRING);

    private EnumSet<JsonNodeType> valueTypes =
        EnumSet.of(BOOLEAN, NUMBER, STRING);

    private EnumSet<JsonNodeType> containerTypes =
        EnumSet.of(OBJECT, ARRAY);

    public synchronized ObjectNode getRandomRestTest(String testName) {
        ObjectNode testNode = new ObjectNode(jsonNodeFactory);
        testNode.set(testName, randomJsonArrayOfObjects());

        return testNode;
    }

    public synchronized JsonNode getRandomJsonNode() {
        int num = RandomizedTest.randomIntBetween(1, 3);
        switch (num) {
            case 1:
                return randomJsonObject();
            case 2:
                return randomJsonArray();
            case 3:
                return randomJsonValue();
        }
        throw new IllegalStateException("impossible");
    }

    public synchronized ObjectNode randomJsonObject() {
        nestingCount = 0;
        return innerRandomJsonObject(new ObjectNode(jsonNodeFactory), 0);
    }

    public synchronized ArrayNode randomJsonArray() {
        nestingCount = 0;
        return innerRandomJsonArray(new ArrayNode(jsonNodeFactory), 0);
    }

    /**
     * Top level entries of the array are always objects (key/value)
     */
    public synchronized ArrayNode randomJsonArrayOfObjects() {
        nestingCount = 0;
        return innerRandomJsonArrayOfObjects(new ArrayNode(jsonNodeFactory), 0);
    }


    public synchronized JsonNode randomJsonValue() {
        return innerRandomJsonValue(RandomizedTest.randomFrom(List.copyOf(valueTypes)));
    }

    public synchronized ObjectNode randomNestedObject(ObjectNode parentObject, int depth, int maxDepth) {
        String fieldName = RandomizedTest.randomAsciiAlphanumOfLength(5);
        if (depth < maxDepth) {
            ObjectNode objectNode = new ObjectNode(jsonNodeFactory);
            parentObject.set(fieldName, randomNestedObject(objectNode, ++depth, maxDepth));
        } else {
            parentObject.set(fieldName, randomJsonValue());
        }
        return parentObject;
    }

    private ArrayNode innerRandomJsonArrayOfObjects(ArrayNode arrayNode, int objectDepth) {
        int fieldsInArray = RandomizedTest.randomIntBetween(1, 10);
        for (int i = 0; i < fieldsInArray; i++) {
            ObjectNode objectNode = new ObjectNode(jsonNodeFactory);
            arrayNode.insert(i, innerRandomJsonObject(objectNode, objectDepth));
        }
        return arrayNode;
    }

    private ArrayNode innerRandomJsonArray(ArrayNode arrayNode, int objectDepth) {
        JsonNodeType jsonNodeType;
        int fieldsInArray = RandomizedTest.randomIntBetween(1, 10);
        System.out.println("nesting count: " + nestingCount);
        for (int i = 0; i < fieldsInArray; i++) {
            if (nestingCount > maxNesting) {
                  jsonNodeType = RandomizedTest.randomFrom(List.copyOf(EnumSet.copyOf(nodeTypes)
                    .stream()
                      .filter(n -> n.equals(ARRAY) == false)
                      .filter(n -> n.equals(OBJECT) == false)
                      .collect(Collectors.toList())));
            } else {
                jsonNodeType =  RandomizedTest.randomFrom(List.copyOf(nodeTypes));
            }
            switch (jsonNodeType) {
                case OBJECT:
                    nestingCount++;
                    ObjectNode objectNode = new ObjectNode(jsonNodeFactory);
                    arrayNode.insert(i, innerRandomJsonObject(objectNode, ++objectDepth));
                    break;
                case ARRAY:
                    nestingCount++;
                    ArrayNode innerArrayNode = new ArrayNode(jsonNodeFactory);
                    arrayNode.insert(i, innerRandomJsonArray(innerArrayNode, objectDepth));
                    break;
                default:
                    arrayNode.insert(i, innerRandomJsonValue(jsonNodeType));
                    break;
            }
        }
        return arrayNode;
    }

    private ObjectNode innerRandomJsonObject(ObjectNode parentObject, int objectDepth) {
        JsonNodeType jsonNodeType;
        int fieldsInObject;
        String fieldName;
        if (objectDepth == 0) {
            //make the top level random Json/yaml look like a test
            fieldName = RandomizedTest.randomFrom(EnumSet.allOf(ExecutableSection.class)
                .stream().map(e -> e.name().toLowerCase(Locale.ROOT)).collect(Collectors.toList()));
            fieldsInObject = 1;
        } else {
            fieldName = RandomizedTest.randomAsciiAlphanumOfLength(5);
            fieldsInObject = RandomizedTest.scaledRandomIntBetween(1, 10);
        }
        RandomizedTest.scaledRandomIntBetween(1, 10);
        for (int i = 0; i < fieldsInObject; i++) {
            System.out.println("nesting count: " + nestingCount);
            if (nestingCount > maxNesting) {
                jsonNodeType = RandomizedTest.randomFrom(List.copyOf(EnumSet.copyOf(nodeTypes)
                    .stream()
                    .filter(n -> n.equals(OBJECT) == false)
                    .filter(n -> n.equals(ARRAY) == false)
                    .collect(Collectors.toList())));
            } else {
                jsonNodeType = RandomizedTest.randomFrom(List.copyOf(nodeTypes));
            }
            switch (jsonNodeType) {
                case OBJECT:
                    nestingCount++;
                    ObjectNode objectNode = new ObjectNode(jsonNodeFactory);
                    parentObject.set(fieldName, innerRandomJsonObject(objectNode, ++objectDepth));
                    break;
                case ARRAY:
                    nestingCount++;
                    ArrayNode innerArrayNode = new ArrayNode(jsonNodeFactory);
                    parentObject.set(fieldName, innerRandomJsonArray(innerArrayNode, objectDepth));
                    break;
                default:
                    parentObject.set(fieldName, innerRandomJsonValue(jsonNodeType));
                    break;
            }
        }
        return parentObject;
    }

    private JsonNode innerRandomJsonValue(JsonNodeType jsonNodeType) {
        switch (jsonNodeType) {
            case STRING:
                return TextNode.valueOf(RandomizedTest.randomAsciiAlphanumOfLength(5));
            case BOOLEAN:
                return BooleanNode.valueOf(RandomizedTest.randomBoolean());
            case NUMBER:
                if (RandomizedTest.randomBoolean()) {
                    return FloatNode.valueOf(RandomizedTest.randomFloat());
                } else {
                    return BigIntegerNode.valueOf(BigInteger.valueOf(RandomizedTest.randomInt()));
                }
        }
        throw new IllegalArgumentException("jsonNodeType must not be an object or array. found [" + jsonNodeType + "]");
    }
}
