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
package org.elasticsearch.test.rest.yaml;

import com.carrotsearch.randomizedtesting.annotations.Name;
import com.carrotsearch.randomizedtesting.annotations.ParametersFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.Version;
import org.elasticsearch.rest.CompatibleConstants;
import org.elasticsearch.test.rest.yaml.section.DoSection;
import org.elasticsearch.test.rest.yaml.section.ExecutableSection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.StreamSupport;

/**
 * Injects the compatible header and handles the test overrides
 */
public class ESClientCompatibleYamlSuiteTestCase extends ESClientYamlSuiteTestCase {
    protected ESClientCompatibleYamlSuiteTestCase(@Name("yaml") ClientYamlTestCandidate testCandidate) {
        super(testCandidate);
    }

    private static final Logger staticLogger = LogManager.getLogger(ESClientCompatibleYamlSuiteTestCase.class);

    public static final String OVERRIDE_TESTS_PATH = "/rest-api-spec/override/test";

    @ParametersFactory
    public static Iterable<Object[]> createParameters() throws Exception {
        List<Object[]> finalTestCandidates = new ArrayList<>();
        Iterable<Object[]> bwcCandidates = ESClientYamlSuiteTestCase.createParameters();
        Map<ClientYamlTestCandidate, ClientYamlTestCandidate> testOverrides = getTestOverrides();

        for (Object[] candidateArray : bwcCandidates) {
            List<ClientYamlTestCandidate> testCandidates = new ArrayList<>(1);
            Arrays.stream(candidateArray).map(o -> (ClientYamlTestCandidate) o).forEach(testCandidate -> {
                if (testOverrides.containsKey(testCandidate)) {
                    staticLogger.info("Overriding test [{}] with local test.", testCandidate.toString());
                    //todo: merge such that only override parts
                    testCandidate = testOverrides.remove(testCandidate);
                }
                mutateTestCandidate(testCandidate);
                testCandidates.add(testCandidate);
            });
            finalTestCandidates.add(testCandidates.toArray());
        }
        testOverrides.keySet().forEach(lc -> finalTestCandidates.add(new Object[] { lc }));
        return finalTestCandidates;
    }

    private static void mutateTestCandidate(ClientYamlTestCandidate testCandidate) {
        testCandidate.getSetupSection().getExecutableSections().stream().filter(s -> s instanceof DoSection).forEach(updateDoSection());
        testCandidate.getTestSection().getExecutableSections().stream().filter(s -> s instanceof DoSection).forEach(updateDoSection());
    }

    private static Consumer<? super ExecutableSection> updateDoSection() {
        return ds -> {
            DoSection doSection = (DoSection) ds;
            // TODO: be more selective here
            doSection.setIgnoreWarnings(true);

            String compatibleHeader = createCompatibleHeader();
            // TODO for cat apis accept headers would break tests which expect txt response
            if (doSection.getApiCallSection().getApi().startsWith("cat") == false) {
                doSection.getApiCallSection()
                    .addHeaders(
                        Map.of(
                            CompatibleConstants.COMPATIBLE_ACCEPT_HEADER,
                            compatibleHeader,
                            CompatibleConstants.COMPATIBLE_CONTENT_TYPE_HEADER,
                            compatibleHeader
                        )
                    );
            }

        };
    }

    private static String createCompatibleHeader() {
        return "application/vnd.elasticsearch+json;compatible-with=" + Version.minimumRestCompatibilityVersion().major;
    }

    private static Map<ClientYamlTestCandidate, ClientYamlTestCandidate> getTestOverrides() throws Exception {
        //TODO: create new format for overrides that makes it easy to identify what to override
      //  Iterable<Object[]> candidates = ESClientYamlSuiteTestCase.createParameters(ExecutableSection.XCONTENT_REGISTRY, OVERRIDE_TESTS_PATH);
        Map<ClientYamlTestCandidate, ClientYamlTestCandidate> localCompatibilityTests = new HashMap<>();
//        StreamSupport.stream(candidates.spliterator(), false)
//            .flatMap(Arrays::stream)
//            .forEach(o -> localCompatibilityTests.put((ClientYamlTestCandidate) o, (ClientYamlTestCandidate) o));
        return localCompatibilityTests;
    }
}
