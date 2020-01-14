package org.elasticsearch.rest.compat;

import com.carrotsearch.randomizedtesting.annotations.Name;
import com.carrotsearch.randomizedtesting.annotations.ParametersFactory;
import org.elasticsearch.test.rest.yaml.ClientYamlTestCandidate;

/**
 * Runs the prior version's elasticsearch/plugin REST tests against a cluster of the current (this) version.
 */
public class RestCompatPluginYamlTestSuiteIT extends AbstractRestCompatYamlTestSuite {

    //TODO: ensure plugins are installed.

    public RestCompatPluginYamlTestSuiteIT(@Name("yaml") ClientYamlTestCandidate testCandidate) {
        super(testCandidate);
    }

    @ParametersFactory
    public static Iterable<Object[]> parameters() throws Exception {
        return AbstractRestCompatYamlTestSuite.getPriorVersionTests("plugins");
    }
}
