/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.elasticsearch.gradle.test.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLParser;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class RestTestMutationParser {

    public static Set<RestTestMutation> parse(ObjectMapper mapper, YAMLFactory yaml, File file) throws IOException {
        Set<RestTestMutation> mutations = new HashSet<>(1);
        YAMLParser yamlParser = yaml.createParser(file);
        List<ObjectNode> tests = mapper.readValues(yamlParser, ObjectNode.class).readAll();
        for (ObjectNode test : tests) {
            Iterator<Map.Entry<String, JsonNode>> iterator = test.fields();
            while (iterator.hasNext()) {
                Map.Entry<String, JsonNode> root = iterator.next();
                String testName = root.getKey();
                Iterator<JsonNode> actionsIterator = root.getValue().iterator();
                while (actionsIterator.hasNext()) {
                    Iterator<Map.Entry<String, JsonNode>> actionMapIterator = actionsIterator.next().fields();
                    Map.Entry<String, JsonNode> actionNode;
                    while (actionMapIterator.hasNext()) {
                        actionNode = actionMapIterator.next();
                        RestTestMutation.Action action = RestTestMutation.Action.fromString(actionNode.getKey());
                        Iterator<JsonNode> replacementIterator = actionNode.getValue().iterator();
                        while (replacementIterator.hasNext()) {
                            Iterator<Map.Entry<String, JsonNode>> replacementMap = replacementIterator.next().fields();
                            while (replacementMap.hasNext()) {
                                Map.Entry<String, JsonNode> mutation = replacementMap.next();
                                mutations.add(new RestTestMutation(
                                    testName, action, RestTestMutation.SectionType.fromString(mutation.getKey()), 0, mutation.getValue()));
                            }
                        }
                    }
                }
            }
        }
        return mutations;
    }
}
