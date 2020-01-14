package org.elasticsearch.rest.compat;

import com.carrotsearch.randomizedtesting.annotations.Name;
import com.carrotsearch.randomizedtesting.annotations.ParametersFactory;
import org.elasticsearch.test.rest.yaml.ClientYamlTestCandidate;

/**
 * Runs the prior version's elasticsearch/rest-api-spec REST tests against a cluster of the current (this) version.
 */
public class RestCompatSpecYamlTestSuiteIT extends AbstractRestCompatYamlTestSuite {

    public RestCompatSpecYamlTestSuiteIT(@Name("yaml") ClientYamlTestCandidate testCandidate) {
        super(testCandidate);
    }

    @ParametersFactory
    public static Iterable<Object[]> parameters() throws Exception {
        return AbstractRestCompatYamlTestSuite.getTests();
    }
}
