/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.gradle.test.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.elasticsearch.gradle.util.GradleUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.file.FileTree;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.SkipWhenEmpty;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.util.PatternFilterable;
import org.gradle.api.tasks.util.PatternSet;
import org.gradle.internal.Factory;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class MutateCompatTestTask extends DefaultTask {

    String sourceSetName;
    private static final String REST_TEST_PREFIX = "rest-api-spec/test";
    private static final String REST_COMPAT_PREFIX = "rest-api-spec/compat";

    private static final YAMLFactory yaml = new YAMLFactory();
    private static final ObjectMapper mapper = new ObjectMapper(yaml);

    private final PatternFilterable testPatternSet;
    private final PatternFilterable compatPatternSet;

    public MutateCompatTestTask() {
        testPatternSet = getPatternSetFactory().create();
        testPatternSet.include(REST_TEST_PREFIX + "/**/*.yml");
        compatPatternSet = getPatternSetFactory().create();
        compatPatternSet.include(REST_COMPAT_PREFIX + "/**/*.yml");
    }

    @Inject
    protected Factory<PatternSet> getPatternSetFactory() {
        throw new UnsupportedOperationException();
    }

    @Input
    public String getSourceSetName() {
        return sourceSetName;
    }

    @Internal
    public File getInputDir(){
        return getSourceSet()
            .orElseThrow(() -> new IllegalArgumentException("could not find source set [" + sourceSetName + "]"))
            .getOutput().getResourcesDir();
    }

    @SkipWhenEmpty
    @InputFiles
    public FileTree getInputFiles() {
        return getProject().files(getInputDir()).getAsFileTree().matching(compatPatternSet);
    }

    @Internal
    public FileTree getTestFiles(){
        return getProject().files(getInputDir()).getAsFileTree().matching(testPatternSet);
    }

    private Optional<SourceSet> getSourceSet() {
        Project project = getProject();
        return project.getConvention().findPlugin(JavaPluginConvention.class) == null
            ? Optional.empty()
            : Optional.ofNullable(GradleUtils.getJavaSourceSets(project).findByName(getSourceSetName()));
    }

    @TaskAction
    void mutate() throws IOException {
        for (File file : getInputFiles()) {
            Map<String, Set<Mutation>> mutations = RestTestMutator.parseMutateInstructions(mapper, yaml, file);
            if(mutations.isEmpty() == false){
                Map<String, File> testFileNameToTestFile = getTestFiles().getFiles().stream().collect(Collectors.toMap(File::getName, f -> f));
                List<ObjectNode> mutatedTests = RestTestMutator.mutateTest(mutations, mapper, yaml, testFileNameToTestFile.get(file.getName()));

                for(ObjectNode t : mutatedTests){
                    mapper.writeValue(System.out, t);
                }
            }
        }
    }
}
