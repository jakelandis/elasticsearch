package org.elasticsearch.rest.compat;

import com.carrotsearch.randomizedtesting.annotations.Name;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.io.PathUtils;
import org.elasticsearch.test.rest.yaml.ClientYamlTestCandidate;
import org.elasticsearch.test.rest.yaml.ESClientYamlSuiteTestCase;
import org.elasticsearch.test.rest.yaml.section.DoSection;
import org.elasticsearch.test.rest.yaml.section.ExecutableSection;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.StreamSupport;

public class AbstractRestCompatYamlTestSuite extends ESClientYamlSuiteTestCase {

    private static final Logger logger = LogManager.getLogger(AbstractRestCompatYamlTestSuite.class);

    private static final String TESTS_PATH_OVERRIDE = "/rest-api-spec/override";

    protected AbstractRestCompatYamlTestSuite(@Name("yaml") ClientYamlTestCandidate testCandidate) {
        super(testCandidate);
    }


    //gradle will handle putting the correct version of the specs in the correct directory.
    // for modules and plugins there will be one set per modules or plugin, hence the multiple testsClassPaths
    // once we have the multiple tests class paths
    // this handles ensuring that tests are properly overriden
    public static Iterable<Object[]> getTests() throws Exception {


        Set<String> testsClassPaths = new HashSet<>();
        Path absoluteTestsPath = PathUtils.get(ESClientYamlSuiteTestCase.class.getResource(ESClientYamlSuiteTestCase.TESTS_PATH).toURI());
        if (Files.isDirectory(absoluteTestsPath)) {
            Files.walk(absoluteTestsPath).forEach(file -> {
                if (file.toString().endsWith(".yml")) {
                    String testClassPath = Path.of(ESClientYamlSuiteTestCase.TESTS_PATH).resolve(absoluteTestsPath.relativize(file.getParent().getParent())).toString().replace("\\", "/");
                    testsClassPaths.add(testClassPath);
                }
            });
        }
        Map<ClientYamlTestCandidate, ClientYamlTestCandidate> testOverrides = getTestsOverrides();
        List<Object[]> tests = new ArrayList<>(100);

        for (String testsClassPath : testsClassPaths) {

            Iterable<Object[]> candidates = ESClientYamlSuiteTestCase.createParameters(ExecutableSection.XCONTENT_REGISTRY, testsClassPath);
            StreamSupport.stream(candidates.spliterator(), false)
                .flatMap(Arrays::stream).map(o -> (ClientYamlTestCandidate) o)
                .forEach(testCandidate -> {
                    List<ClientYamlTestCandidate> testCandidates = new ArrayList<>(100);
                    if (testOverrides.containsKey(testCandidate)) {
                        testCandidate = testOverrides.remove(testCandidate);
                    }
                    testCandidates.add(testCandidate);
                    //disable checking for warning headers, we know that many of the tests will have deprecation and compatibility warnings.
                    //the deprecation and compatibility warnings should be explicitly tested via the REST tests from this version.
                    testCandidate.getTestSection().getExecutableSections().stream().filter(s -> s instanceof DoSection).forEach(ds -> {
                        DoSection doSection = (DoSection) ds;
                        doSection.checkWarningHeaders(false);
                        //TODO: use real header
                        // add the compatibility header
                        doSection.getApiCallSection().addHeaders(Collections.singletonMap("compatible-with", "v7"));
                    });

                    if (testCandidates.isEmpty() == false) {
                        tests.add(testCandidates.toArray());
                    }

                });
        }
        if(testOverrides.isEmpty() == false){
            fail("You have lingering test overrides");
            testOverrides.forEach((k,v) -> System.out.println(k));
        }
        return tests;
    }

    private static Map<ClientYamlTestCandidate, ClientYamlTestCandidate> getTestsOverrides() throws Exception {
        Iterable<Object[]> candidates = ESClientYamlSuiteTestCase.createParameters(ExecutableSection.XCONTENT_REGISTRY, TESTS_PATH_OVERRIDE);
        Map<ClientYamlTestCandidate, ClientYamlTestCandidate> testOverrides = new HashMap<>(100);
        StreamSupport.stream(candidates.spliterator(), false)
            .flatMap(Arrays::stream).forEach(o -> testOverrides.put((ClientYamlTestCandidate) o, (ClientYamlTestCandidate) o));
        return testOverrides;
    }
}
