package org.elasticsearch.test.rest.yaml;

import com.carrotsearch.randomizedtesting.annotations.Name;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.test.rest.yaml.section.DoSection;
import org.elasticsearch.test.rest.yaml.section.ExecutableSection;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.StreamSupport;

public class AbstractRestCompatibilityYamlTestSuite extends ESClientYamlSuiteTestCase {
    // normal tests
// ./gradlew ':modules:ingest-common:integTestRunner'   --tests "org.elasticsearch.ingest.common.IngestCommonClientYamlTestSuiteIT.test"  -Dtests.timestamp=$(date +%S) --info
    //compat tests
// ./gradlew ':modules:ingest-common:integTestRunner'   --tests "org.elasticsearch.ingest.common.IngestCommonRestCompatTestSuiteIT.test"  -Dtests.timestamp=$(date +%S) --info
    private static final Logger logger = LogManager.getLogger(AbstractRestCompatibilityYamlTestSuite.class);

    public static final String REST_SPEC_COMPAT_ROOT = "tests.rest.spec_root_compat";

    //TODO: fix this comment
    /**
     * Property that allows to set the root for the compat rest API tests. This value plus SPEC_PATH and TESTS_PATH is the location
     * of the compatibility REST API spec and tests. TESTS_COMPAT_PATH is also considered to allow locally defined tests to specifically
     * test parts of the compatibility. Any tests from TESTS_COMPAT_PATH with the same name will override the test found from
     * REST_TESTS_COMPAT_ROOT + TESTS_PATH.
     */
    public static final String REST_TESTS_COMPAT_ROOT = "tests.rest.test_root_compat";
    private static final Path TESTS_SOURCE_PATH = Paths.get("src/test/resources");
    public static final String TESTS_COMPAT_CLASS_PATH = "/rest-api-spec/test-compatibility";

    protected AbstractRestCompatibilityYamlTestSuite(@Name("yaml") ClientYamlTestCandidate testCandidate) {
        super(testCandidate);
    }

    public static Iterable<Object[]> getTests() throws Exception {
        List<Object[]> finalTestCandidates = new ArrayList<>();
        Iterable<Object[]> bwcCandidates = ESClientYamlSuiteTestCase.createParameters(ExecutableSection.XCONTENT_REGISTRY, getTestPath());
        Map<ClientYamlTestCandidate, ClientYamlTestCandidate> localCandidates = getLocalCompatibilityTests();

        for (Object[] candidateArray : bwcCandidates) {
            List<ClientYamlTestCandidate> testCandidates = new ArrayList<>(1);
            Arrays.stream(candidateArray).map(o -> (ClientYamlTestCandidate) o).forEach(testCandidate -> {
                if (localCandidates.containsKey(testCandidate)) {
                    logger.info("Overriding test[{}] from  [{}] with local test.",
                        testCandidate.toString(), getTestPath());
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

    @Override
    protected Path getSpecPath() {
        Path compatSpec = Paths.get(System.getProperty(REST_SPEC_COMPAT_ROOT)).resolve(SPEC_PATH);
        logger.info("Reading REST compatible spec from [{}]", compatSpec);
        return compatSpec;
    }

    private static Path getTestPath() {
        String resourceDir = null;
        Path projectPath = null;
        try {
            //use this project's resourceDir to figure out which module or plugin we are running.
            resourceDir = ESClientYamlSuiteTestCase.class.getResource("/").toURI().toASCIIString();
            String parts[] = resourceDir.split(System.getProperty("file.separator"));
            int i = 0;
            for (; i < parts.length - 1; i++) {
                if ("modules".equals(parts[i]) || "plugins".equals(parts[i])) { //TODO: rest-api project
                    break;
                }
            }
            //for example: modules/ingest-common
            projectPath = Objects.requireNonNull(Paths.get(parts[i], parts[i + 1]),
                "Could not find the project path for [" + resourceDir + "]");
        } catch (Exception e) {
            //shouldn't happen, but this code is kinda fragile since it depends on specific directory structure convention.
            throw new IllegalStateException("Could not determine the path for the compatible REST tests from ["
                + resourceDir + "]. This is likely an bug.", e);
        }
        // for example:
        // /Users/me/elasticsearch/distribution/bwc/minor/build/bwc/checkout-7.x/modules/ingest-common/src/test/resources/rest-api-spec/test
        Path compatTestsRoot = Paths.get(System.getProperty(REST_TESTS_COMPAT_ROOT)).resolve(projectPath);
        Path compatTests = compatTestsRoot.resolve(TESTS_SOURCE_PATH).resolve(TESTS_PATH);
        if(new File(compatTestsRoot.toUri()).exists()){
            logger.info("Reading REST compatible tests from [{}]", compatTests);
            return compatTests;
        }else{
            //TODO: make this execute a no-op test to allow this to pass ... this is for new modules that don't have
            logger.info("Can not run REST compatible tests for [{}] since we could not find that module or plugin at [{}]",
                projectPath, compatTests);
            throw new IllegalStateException("TODO: change this to a no-op test so it does not fail !");
        }
    }

    private static Path getTestsCompatPath() throws URISyntaxException {
        URL url = ESClientYamlSuiteTestCase.class.getResource(TESTS_COMPAT_CLASS_PATH);
        return url == null ? null : Paths.get(url.toURI());
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
        Path compatTestPath = getTestsCompatPath();
        if (compatTestPath == null) {
            return Collections.emptyMap();
        }
        Iterable<Object[]> candidates =
            ESClientYamlSuiteTestCase.createParameters(ExecutableSection.XCONTENT_REGISTRY, getTestsCompatPath());
        Map<ClientYamlTestCandidate, ClientYamlTestCandidate> localCompatibilityTests = new HashMap<>();
        StreamSupport.stream(candidates.spliterator(), false)
            .flatMap(Arrays::stream).forEach(o -> localCompatibilityTests.put((ClientYamlTestCandidate) o, (ClientYamlTestCandidate) o));
        return localCompatibilityTests;
    }
}
