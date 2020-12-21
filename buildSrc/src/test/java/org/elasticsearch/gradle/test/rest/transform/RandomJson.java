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
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import java.math.BigInteger;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;

public class RandomJson {
    private final int maxNestedObjects;
    private final int maxNestedArrays;

    private int nestedObjects = 0;
    private int nestedArrays = 0;

    public RandomJson(Random random) {
        this.maxNestedObjects = RandomizedTest.randomIntBetween(1, 10);
        this.maxNestedArrays = RandomizedTest.randomIntBetween(1, 10);

    }

    private static JsonNodeFactory jsonNodeFactory = JsonNodeFactory.withExactBigDecimals(false);

    private enum JsonNodeType {OBJECT, ARRAY, BOOLEAN, FLOAT, INT,  TEXT}

    private EnumSet<JsonNodeType> valueTypes =
        EnumSet.of(JsonNodeType.BOOLEAN, JsonNodeType.FLOAT, JsonNodeType.INT, JsonNodeType.TEXT);

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
        nestedObjects = 0;
        nestedArrays = 0;
        return innerRandomJsonObject(new ObjectNode(jsonNodeFactory));
    }

    public synchronized ArrayNode randomJsonArray() {
        nestedObjects = 0;
        nestedArrays = 0;
        return innerRandomJsonArray(new ArrayNode(jsonNodeFactory));
    }

    /**
     * Top level entries of the array are always objects (key/value)
     */
    public synchronized ArrayNode randomJsonArrayOfObjects() {
        nestedObjects = 0;
        nestedArrays = 0;
        return innerRandomJsonArrayOfObjects(new ArrayNode(jsonNodeFactory));
    }


    public synchronized JsonNode randomJsonValue() {
        return innerRandomJsonValue(RandomizedTest.randomFrom(List.copyOf(valueTypes)));
    }

    public ObjectNode nestedObject(ObjectNode parentObject, int depth, int maxDepth, JsonNode value) {
        String fieldName = RandomizedTest.randomAsciiAlphanumOfLength(5);
        if (depth < maxDepth) {
            ObjectNode objectNode = new ObjectNode(jsonNodeFactory);
            parentObject.set(fieldName, nestedObject(objectNode, ++depth, maxDepth, value));
        } else {
            parentObject.set(fieldName, value);
        }
        return parentObject;
    }

    private ArrayNode innerRandomJsonArrayOfObjects(ArrayNode arrayNode) {
        JsonNodeType jsonNodeType;
        int fieldsInArray = RandomizedTest.randomIntBetween(1, 10);
        for (int i = 0; i < fieldsInArray; i++) {
            ObjectNode objectNode = new ObjectNode(jsonNodeFactory);
            arrayNode.insert(i, innerRandomJsonObject(objectNode));
        }
        return arrayNode;
    }


    private ArrayNode innerRandomJsonArray(ArrayNode arrayNode) {
        JsonNodeType jsonNodeType;
        int fieldsInArray = RandomizedTest.randomIntBetween(1, 10);
        for (int i = 0; i < fieldsInArray; i++) {
            if (++nestedArrays > maxNestedArrays) {
                jsonNodeType =  RandomizedTest.randomFrom(List.copyOf(EnumSet.complementOf(EnumSet.of(JsonNodeType.ARRAY))));
            } else {
                jsonNodeType = RandomizedTest.randomFrom(List.copyOf(EnumSet.allOf(JsonNodeType.class)));
            }
            switch (jsonNodeType) {
                case OBJECT:
                    ObjectNode objectNode = new ObjectNode(jsonNodeFactory);
                    arrayNode.insert(i, innerRandomJsonObject(objectNode));
                    break;
                case ARRAY:
                    ArrayNode innerArrayNode = new ArrayNode(jsonNodeFactory);
                    arrayNode.insert(i, innerRandomJsonArray(innerArrayNode));
                    break;
                default:
                    arrayNode.insert(i, innerRandomJsonValue(jsonNodeType));
                    break;
            }
        }
        return arrayNode;
    }

    private ObjectNode innerRandomJsonObject(ObjectNode parentObject) {
        JsonNodeType jsonNodeType;
        int fieldsInObject = RandomizedTest.scaledRandomIntBetween(1, 10);
        for (int i = 0; i < fieldsInObject; i++) {
            if (++nestedObjects > maxNestedObjects) {
                jsonNodeType =  RandomizedTest.randomFrom(List.copyOf(EnumSet.complementOf(EnumSet.of(JsonNodeType.OBJECT))));
            } else {
                jsonNodeType = RandomizedTest.randomFrom(List.copyOf(EnumSet.allOf(JsonNodeType.class)));
            }

            String fieldName = RandomizedTest.randomAsciiAlphanumOfLength(5);
            switch (jsonNodeType) {
                case OBJECT:
                    ObjectNode objectNode = new ObjectNode(jsonNodeFactory);
                    parentObject.set(fieldName, innerRandomJsonObject(objectNode));
                    break;
                case ARRAY:
                    ArrayNode innerArrayNode = new ArrayNode(jsonNodeFactory);
                    parentObject.set(fieldName, innerRandomJsonArray(innerArrayNode));
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
            case TEXT:
                return TextNode.valueOf(RandomizedTest.randomAsciiAlphanumOfLength(5));
            case BOOLEAN:
                return BooleanNode.valueOf(RandomizedTest.randomBoolean());
            case FLOAT:
                return FloatNode.valueOf(RandomizedTest.randomFloat());
            case INT:
                return BigIntegerNode.valueOf(BigInteger.valueOf(RandomizedTest.randomInt()));

        }
        throw new IllegalArgumentException("jsonNodeType must not be an object or array");
    }


}
