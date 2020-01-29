package org.elasticsearch.test.rest.yaml;

import com.carrotsearch.randomizedtesting.annotations.Name;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.test.rest.yaml.section.DoSection;
import org.elasticsearch.test.rest.yaml.section.ExecutableSection;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class AbstractRestCompatYamlTestSuite extends ESClientYamlSuiteTestCase {

    private static final Logger staticLogger = LogManager.getLogger(AbstractRestCompatYamlTestSuite.class);

    public static final String REST_SPEC_COMPAT_ROOT = "tests.rest.spec_root_compat";

    public static final String REST_TESTS_COMPAT_ROOT = "tests.rest.test_root_compat";

    public static final String TESTS_COMPAT_CLASS_PATH = "/rest-api-spec/test-compatibility";

    protected AbstractRestCompatYamlTestSuite(@Name("yaml") ClientYamlTestCandidate testCandidate) {
        super(testCandidate);
    }

    public static Iterable<Object[]> createParameters(Map<String, String> filterValues) throws Exception {
        List<Object[]> finalTestCandidates = new ArrayList<>();
        Iterable<Object[]> bwcCandidates = ESClientYamlSuiteTestCase.createParameters(ExecutableSection.XCONTENT_REGISTRY, getTestPath(), filterValues);
        Map<ClientYamlTestCandidate, ClientYamlTestCandidate> localCandidates = getLocalCompatibilityTests();

        for (Object[] candidateArray : bwcCandidates) {
            List<ClientYamlTestCandidate> testCandidates = new ArrayList<>(1);
            Arrays.stream(candidateArray).map(o -> (ClientYamlTestCandidate) o).forEach(testCandidate -> {
                if (localCandidates.containsKey(testCandidate)) {
                    staticLogger.info("Overriding test[{}] from  [{}] with local test.",
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

    public static Iterable<Object[]> createParameters() throws Exception {
        return createParameters(Collections.emptyMap());
    }

    @Override
    protected Path getSpecPath() {
        Path compatSpec = Paths.get(System.getProperty(REST_SPEC_COMPAT_ROOT)).resolve(SPEC_PATH);
        staticLogger.info("Reading REST compatible spec from [{}]", compatSpec);
        return compatSpec;
    }

    private static Path getTestPath() {
        String resourceDir = null;
        Path relativeRoot = null;
        Path compatTests = null;
        try {
            //use this project's resourceDir to figure out which module or plugin we are running.
            resourceDir = ESClientYamlSuiteTestCase.class.getResource("/").toURI().toASCIIString();
            String parts[] = resourceDir.split(System.getProperty("file.separator"));
            int i = 0;

            ///Users/jakelandis/workspace/8x/elasticsearch-alt1/plugins/discovery-ec2/qa/amazon-ec2/build-idea/resources

            for (; i < parts.length - 3; i++) { //intentionally length - 3
                if ("modules".equals(parts[i]) || "plugins".equals(parts[i])) {
                    if ("qa".equals(parts[i + 2])) {
                        relativeRoot = Paths.get(parts[i], parts[i + 1], parts[i + 2], parts[i + 3]);
                    } else {
                        relativeRoot = Paths.get(parts[i], parts[i + 1]);
                    }
                    break;
                }
                if ("x-pack".equals(parts[i]) && "plugin".equals(parts[i + 1])) {
                    relativeRoot = Paths.get(parts[i], parts[i + 1], parts[i + 2]);
                    break;
                }
                if ("qa".equals(parts[i]) && "rest-compatibility".equals(parts[i + 1])) {
                    relativeRoot = Paths.get("rest-api-spec/src/main/resources");
                    break;
                }
            }
            Objects.requireNonNull(relativeRoot, "Could not find the relative root path from [" + resourceDir + "]");
            Path searchPath = Paths.get(System.getProperty(REST_TESTS_COMPAT_ROOT)).resolve(relativeRoot);
            Set<Path> paths;
            if (searchPath.endsWith("qa") || searchPath.endsWith("rest-compatibility")) {
                paths = Files.walk(searchPath).filter(p -> p.endsWith(TESTS_PATH)).collect(Collectors.toSet());
            } else { //filter out any path with "qa" so parent's of nested "qa" projects can find the correct set of tests
                paths = Files.walk(searchPath).filter(p -> p.toString().contains("qa") == false)
                    .filter(p -> p.endsWith(TESTS_PATH)).collect(Collectors.toSet());
            }
            if (paths.size() > 1) {
                throw new IllegalStateException("Found multiple path candidates for the compat tests path. " +
                    "Only one level nesting for a project called qa that is under the main plugin or module project is supported. " +
                    "For example plugin/foo/qa/fooTest and plugin/foo/qa/fooTest2 are supported. " +
                    "However, plugin/foo/notqa/fooTest is not supported ");
            }
            compatTests = paths.iterator().next();
            if (new File(compatTests.toUri()).exists()) {
                staticLogger.info("Reading REST compatible tests from [{}]", compatTests);
                return compatTests;
            } else {
                //TODO: make this execute a no-op test to allow this to pass ... this is for new modules that don't have a bwc equivelant
                staticLogger.info("Can not run REST compatible tests for [{}] since we could not find that module or plugin at [{}]",
                    relativeRoot, compatTests);
                throw new IllegalStateException("TODO: change this to a no-op test so it does not fail !");
            }
        } catch (Exception e) {
            //shouldn't happen, but this code is kinda fragile since it depends on specific directory structure convention.
            throw new IllegalStateException("Could not determine the path for the compatible REST tests from ["
                + resourceDir + "]. This is likely an bug.", e);
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
            ESClientYamlSuiteTestCase.createParameters(ExecutableSection.XCONTENT_REGISTRY, getTestsCompatPath(), Collections.emptyMap());
        Map<ClientYamlTestCandidate, ClientYamlTestCandidate> localCompatibilityTests = new HashMap<>();
        StreamSupport.stream(candidates.spliterator(), false)
            .flatMap(Arrays::stream).forEach(o -> localCompatibilityTests.put((ClientYamlTestCandidate) o, (ClientYamlTestCandidate) o));
        return localCompatibilityTests;
    }
}
