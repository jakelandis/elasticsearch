package org.elasticsearch.rest.compatibility;

import com.carrotsearch.randomizedtesting.annotations.Name;
import com.carrotsearch.randomizedtesting.annotations.ParametersFactory;
import org.elasticsearch.test.rest.yaml.AbstractRestCompatibilityYamlTestSuite;
import org.elasticsearch.test.rest.yaml.ClientYamlTestCandidate;

//TODO: delete
//./gradlew  :qa:rest-compatibility:clean :qa:rest-compatibility:v7.5.0#restCompatTestRunner --tests "org.elasticsearch.rest.compatibility.RestCompatibilitySpecYamlTestSuiteIT.test {yaml=info/*}" --info

/**
 * Runs the prior version's elasticsearch/rest-api-spec REST tests against a cluster of the current (this) version.
 */
public class RestCompatibilitySpecYamlTestSuiteIT extends AbstractRestCompatibilityYamlTestSuite {

    public RestCompatibilitySpecYamlTestSuiteIT(@Name("yaml") ClientYamlTestCandidate testCandidate) {
        super(testCandidate);
    }

    @ParametersFactory
    public static Iterable<Object[]> parameters() throws Exception {
        return AbstractRestCompatibilityYamlTestSuite.getTests();
    }
}
