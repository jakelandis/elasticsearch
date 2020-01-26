package org.elasticsearch.test.rest.yaml;

import com.carrotsearch.randomizedtesting.annotations.Name;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.test.rest.yaml.section.DoSection;
import org.elasticsearch.test.rest.yaml.section.ExecutableSection;

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
import java.util.stream.StreamSupport;

public class AbstractRestCompatibilityYamlTestSuite extends ESClientYamlSuiteTestCase {
     // normal tests
// ./gradlew ':modules:ingest-common:integTestRunner'   --tests "org.elasticsearch.ingest.common.IngestCommonClientYamlTestSuiteIT.test"  -Dtests.timestamp=$(date +%S) --info
    //compat tests
// ./gradlew ':modules:ingest-common:integTestRunner'   --tests "org.elasticsearch.ingest.common.IngestCommonRestCompatTestSuiteIT.test"  -Dtests.timestamp=$(date +%S) --info
    private static final Logger logger = LogManager.getLogger(AbstractRestCompatibilityYamlTestSuite.class);

    /**
     * Property that allows to set the root for the compat rest API tests. This value plus SPEC_PATH and TESTS_PATH is the location
     * of the compatibility REST API spec and tests. TESTS_COMPAT_PATH is also considered to allow locally defined tests to specifically
     * test parts of the compatibility. Any tests from TESTS_COMPAT_PATH with the same name will override the test found from
     * REST_TESTS_COMPAT_ROOT + TESTS_PATH.
     */
    public static final String REST_TESTS_COMPAT_ROOT = "tests.rest.compat_root";
    public static final Path TESTS_COMPAT_PATH = Paths.get("rest-api-spec/test-compatibility");

    private static final Path SPEC_SOURCE_PATH = Paths.get("rest-api-spec/src/main/resources");
    private static final Path TESTS_SOURCE_PATH = Paths.get("src/test/resources");

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
        return Paths.get(System.getProperty(REST_TESTS_COMPAT_ROOT)).resolve(SPEC_SOURCE_PATH).resolve(SPEC_PATH);
    }

    private static Path getTestPath()  {
        String resourceDir = null;
        try {
            resourceDir = ESClientYamlSuiteTestCase.class.getResource("/").toURI().toASCIIString();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        //TODO: make this much safer and clean up
        String parts[] = resourceDir.split(System.getProperty("file.separator"));
        int i =0;
        for(; i < parts.length -1 ; i++){
            if("modules".equals(parts[i])){ //TODO: support plugins and the rest-api project
                break;
            }
        }
        Path projectPath = Paths.get(parts[i], parts[i+1]);

        return Paths.get(System.getProperty(REST_TESTS_COMPAT_ROOT)).resolve(projectPath).resolve(TESTS_SOURCE_PATH).resolve(TESTS_PATH);
    }

    private static Path getTestsCompatPath() throws URISyntaxException {
        URL url = ESClientYamlSuiteTestCase.class.getResource("/" + TESTS_COMPAT_PATH.toString());
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
        if(compatTestPath == null){
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
