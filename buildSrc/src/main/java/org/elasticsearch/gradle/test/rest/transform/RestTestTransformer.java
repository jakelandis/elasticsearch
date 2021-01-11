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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Perform the transformations against the set of RestTests from a given file.
 */
public class RestTestTransformer {
    /**
     * Transforms a REST test based on the requested {@link RestTestTransform}'s
     *
     * @param tests           The REST tests from the same file. Uses linked list so we can easily add to the beginning of the list.
     * @param transformations The set of transformations to perform against the test
     * @return the transformed tests
     */
    public List<ObjectNode> transformRestTests(LinkedList<ObjectNode> tests, List<RestTestTransform<?>> transformations) {
        ObjectNode setupSection = null;
        ObjectNode teardownSection = null;

        // Collect any global setup transformations
        List<RestTestTransformGlobalSetup> setupTransforms = transformations.stream()
            .filter(transform -> transform instanceof RestTestTransformGlobalSetup)
            .map(transform -> (RestTestTransformGlobalSetup) transform)
            .collect(Collectors.toList());

        // Collect any global setup transformations
        List<RestTestTransformGlobalTeardown> teardownTransforms = transformations.stream()
            .filter(transform -> transform instanceof RestTestTransformGlobalTeardown)
            .map(transform -> (RestTestTransformGlobalTeardown) transform)
            .collect(Collectors.toList());

        // transform the tests and include the global setup and teardown as part of the transform
        for (ObjectNode test : tests) {
            Iterator<Map.Entry<String, JsonNode>> testsIterator = test.fields();
            while (testsIterator.hasNext()) {
                Map.Entry<String, JsonNode> testObject = testsIterator.next();
                String testName = testObject.getKey();
                if ("setup".equals(testName)) {
                    setupSection = test;
                }
                if ("teardown".equals(testName)) {
                    teardownSection = test;
                }
                Map<String, RestTestTransformByObjectKey> objectKeyFinders = transformations.stream()
                    .filter(transform -> transform instanceof RestTestTransformByObjectKey)
                    .map(transform -> (RestTestTransformByObjectKey) transform)
                    .collect(Collectors.toMap(RestTestTransformByObjectKey::getKeyToFind, transform -> transform));

                transformByObjectKeyName(test, objectKeyFinders);
            }
        }

        // transform the global setup
        if (setupTransforms.isEmpty() == false) {
            int i = 0;
            for (RestTestTransformGlobalSetup setupTransform : setupTransforms) {
                ObjectNode result = setupTransform.transformSetup(setupSection);
                if (result != null && setupSection == null && i++ == 0) {
                    tests.addFirst(result);
                }
            }
        }

        // transform the global teardown
        if (setupTransforms.isEmpty() == false) {
            int i = 0;
            for (RestTestTransformGlobalTeardown teardownTransform : teardownTransforms) {
                ObjectNode result = teardownTransform.transformTeardown(teardownSection);
                if (result != null && setupSection == null && i++ == 0) {
                    tests.addLast(result);
                }
            }
        }

        boolean debug = true;
        if(debug == true) {
            YAMLFactory yaml = new YAMLFactory();
            ObjectMapper mapper = new ObjectMapper(yaml);
            try (SequenceWriter sequenceWriter = mapper.writer().writeValues(System.out)) {
                for (ObjectNode transformedTest : tests) {
                    sequenceWriter.write(transformedTest);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return tests;
    }

    /**
     * Recursive method to find any matching object key names.
     *
     * @param currentNode      The current node that is being evaluated.
     * @param objectKeyFinders A Map of the key to find to the
     */
    private void transformByObjectKeyName(JsonNode currentNode, Map<String, RestTestTransformByObjectKey> objectKeyFinders) {
        if (currentNode.isArray()) {
            currentNode.elements().forEachRemaining(node -> {
                transformByObjectKeyName(node, objectKeyFinders);
            });
        } else if (currentNode.isObject()) {
            currentNode.fields().forEachRemaining(entry -> {
                RestTestTransformByObjectKey transform = objectKeyFinders.get(entry.getKey());
                if (transform == null) {
                    transformByObjectKeyName(entry.getValue(), objectKeyFinders);
                } else {
                    transform.transformTest((ObjectNode) currentNode);
                }
            });
        }
    }
}
