package org.elasticsearch.ingest.common;

import com.carrotsearch.randomizedtesting.annotations.Name;
import com.carrotsearch.randomizedtesting.annotations.ParametersFactory;
import org.elasticsearch.test.rest.yaml.AbstractRestCompatibilityYamlTestSuite;
import org.elasticsearch.test.rest.yaml.ClientYamlTestCandidate;

public class IngestCommonRestCompatTestSuiteIT extends AbstractRestCompatibilityYamlTestSuite {
    public IngestCommonRestCompatTestSuiteIT(@Name("yaml") ClientYamlTestCandidate testCandidate) {
        super(testCandidate);
    }

    @ParametersFactory
    public static Iterable<Object[]> parameters() throws Exception {
        return AbstractRestCompatibilityYamlTestSuite.getTests();
    }
}
