package org.elasticsearch.test.rest.yaml;


import com.carrotsearch.randomizedtesting.annotations.Name;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.test.rest.yaml.section.DoSection;
import org.elasticsearch.test.rest.yaml.section.ExecutableSection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.StreamSupport;

public class AbstractRestCompatibilityYamlTestSuite extends ESClientYamlSuiteTestCase {

    private static final String LOCAL_COMPATIBILITY_TESTS_PATH = "/rest-api-spec/test-compatibility";
    private static final Logger logger = LogManager.getLogger(AbstractRestCompatibilityYamlTestSuite.class);

    protected AbstractRestCompatibilityYamlTestSuite(@Name("yaml") ClientYamlTestCandidate testCandidate) {
        super(testCandidate);
    }

    /**
     *<p>
     * Get the REST tests for REST compatibility testing. REST compatibility testing runs a prior version of the REST tests against the
     * current version of Elasticsearch with the compatibility header injected. Tests defined in /rest-api-spec/test-compatibility will
     * also be run, or if has the same file name and test name will override the prior version test.
     *</p>
     * Usage:
     * <pre>
     * {@literal @}ParametersFactory
     *  public static Iterable&lt;Object[]&gt; parameters() throws Exception {
     *      return AbstractRestCompatibilityYamlTestSuite.getTests();
     *  }
     * </pre>
     */
    public static Iterable<Object[]> getTests() throws Exception {
        List<Object[]> finalTestCandidates = new ArrayList<>();
        Iterable<Object[]> bwcCandidates = ESClientYamlSuiteTestCase.createParameters(ExecutableSection.XCONTENT_REGISTRY, getTestsPath());
        Map<ClientYamlTestCandidate, ClientYamlTestCandidate> localCandidates = getLocalCompatibilityTests();

        for (Object[] candidateArray : bwcCandidates) {
            List<ClientYamlTestCandidate> testCandidates = new ArrayList<>(1);
            Arrays.stream(candidateArray).map(o -> (ClientYamlTestCandidate) o).forEach(testCandidate -> {
                if (localCandidates.containsKey(testCandidate)) {
                    logger.info("Overriding test[{}] from version [{}] with local test.",
                        testCandidate.toString(), getCompatSource());
                    testCandidate = localCandidates.remove(testCandidate);
                }
                mutateTestCandidate(testCandidate);
                testCandidates.add(testCandidate);
            });
            finalTestCandidates.add(testCandidates.toArray());
        }
        localCandidates.keySet().forEach(lc -> finalTestCandidates.add(new Object[]{lc}));
        return finalTestCandidates;
    }

    /**
     * Mutates the tests before they are executed.
     * <ul>
     *     <li>
     * Disable checking the warning headers since most of these tests will emit warning headers.  Tests for warning headers should
     * be explicitly added to /rest-api-spec/test-compatibility
     *      </li>
     *      <li>
     * Inject the compatibility header so that compatibility mode will be triggered.
     *      </li>
     * </ul>
     */
    private static void mutateTestCandidate(ClientYamlTestCandidate testCandidate) {
        testCandidate.getTestSection().getExecutableSections().stream().filter(s -> s instanceof DoSection).forEach(ds -> {
            DoSection doSection = (DoSection) ds;
            doSection.checkWarningHeaders(false);
            //TODO: use the real header compatibility header
            doSection.getApiCallSection().addHeaders(Collections.singletonMap("compatible-with", "v7"));
        });
    }

    private static Map<ClientYamlTestCandidate, ClientYamlTestCandidate> getLocalCompatibilityTests() throws Exception {
        Iterable<Object[]> candidates =
            ESClientYamlSuiteTestCase.createParameters(ExecutableSection.XCONTENT_REGISTRY, LOCAL_COMPATIBILITY_TESTS_PATH);
        Map<ClientYamlTestCandidate, ClientYamlTestCandidate> localCompatibilityTests = new HashMap<>();
        StreamSupport.stream(candidates.spliterator(), false)
            .flatMap(Arrays::stream).forEach(o -> localCompatibilityTests.put((ClientYamlTestCandidate) o, (ClientYamlTestCandidate) o));
        return localCompatibilityTests;
    }

    private static String getCompatSource() {
        System.out.println("**************************** compatVersion: " + System.getProperty("compatVersion"));
        return Objects.requireNonNull(System.getProperty("compatVersion"), "Gradle is required to set the compatVersion system property");
    }

    private static String getTestsPath() {
        return "/" + getCompatSource() + ESClientYamlSuiteTestCase.TESTS_PATH;
    }

    @Override
    protected String getSpecPath() {
        return "/" + getCompatSource() + ESClientYamlSuiteTestCase.SPEC_PATH;
    }
}
