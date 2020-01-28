package org.elasticsearch.rest;

import com.carrotsearch.randomizedtesting.annotations.Name;
import com.carrotsearch.randomizedtesting.annotations.ParametersFactory;
import org.elasticsearch.test.rest.yaml.AbstractRestCompatYamlTestSuite;
import org.elasticsearch.test.rest.yaml.ClientYamlTestCandidate;

public class CoreRestCompatTestSuiteIT extends AbstractRestCompatYamlTestSuite {
    public CoreRestCompatTestSuiteIT(@Name("yaml") ClientYamlTestCandidate testCandidate) {
        super(testCandidate);
    }

    @ParametersFactory
    public static Iterable<Object[]> parameters() throws Exception {
        return AbstractRestCompatYamlTestSuite.createParameters();
    }
}
