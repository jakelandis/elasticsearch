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

package org.elasticsearch.discovery.ec2;

import com.carrotsearch.randomizedtesting.annotations.Name;
import com.carrotsearch.randomizedtesting.annotations.ParametersFactory;
import org.elasticsearch.test.rest.yaml.AbstractRestCompatYamlTestSuite;
import org.elasticsearch.test.rest.yaml.ClientYamlTestCandidate;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class AmazonEC2DiscoveryRestCompatYamlTestSuiteIT extends AbstractRestCompatYamlTestSuite {

    public AmazonEC2DiscoveryRestCompatYamlTestSuiteIT(@Name("yaml") ClientYamlTestCandidate testCandidate) {
        super(testCandidate);
    }

    @ParametersFactory
    public static Iterable<Object[]> parameters() throws Exception {
        Map<String, String> filterValues = new HashMap<>();
        filterValues.put("expected_nodes", Objects.requireNonNull(System.getProperty("expected_nodes"), "expected_nodes can not be null"));
        return AbstractRestCompatYamlTestSuite.createParameters(filterValues);
    }
}
