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

package org.elasticsearch.gradle;

import org.elasticsearch.gradle.info.BuildParams;
import org.elasticsearch.gradle.test.RestIntegTestTask;
import org.elasticsearch.gradle.test.rest.YamlRestTestPlugin;
import org.elasticsearch.gradle.testclusters.RestTestRunnerTask;
import org.elasticsearch.gradle.util.GradleUtils;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.plugins.ide.idea.model.IdeaModel;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class RestCompatibilityPlugin implements Plugin<Project> {

    private final String COMPATIBLE_VERSION = "v" + (VersionProperties.getElasticsearchVersion().getMajor() - 1);
    // private final String CURRENT_VERSION = "v" + (VersionProperties.getElasticsearchVersion().getMajor());
    private final String SOURCE_SET_NAME = COMPATIBLE_VERSION + "restCompatibility";
    private final String TEST_SOURCE_SET_NAME = COMPATIBLE_VERSION + "restCompatibilityTest";

    @Override
    public void apply(Project project) {


        // create rest-compatibility source set
        final SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
        SourceSet restCompatSourceSet = sourceSets.create(SOURCE_SET_NAME);

        Configuration restCompatCompileConfig = project.getConfigurations().getByName(restCompatSourceSet.getCompileClasspathConfigurationName());
        Configuration restCompatRuntimeConfig = project.getConfigurations().getByName(restCompatSourceSet.getRuntimeClasspathConfigurationName());
        restCompatSourceSet.setCompileClasspath(restCompatCompileConfig);
        restCompatSourceSet.setRuntimeClasspath(project.getObjects().fileCollection().from(restCompatSourceSet.getOutput(), restCompatRuntimeConfig));

        //compat code depend on main code
        GradleUtils.extendSourceSet(project, SourceSet.MAIN_SOURCE_SET_NAME, SOURCE_SET_NAME);

        //TODO: nuke rest-compatiblity module
        Dependency restCompatDependency = project.getDependencies().project(Map.of("path", ":modules:rest-compatibility")); //TOOD: protect with BuildParams.isInternal
        project.getDependencies().add(restCompatCompileConfig.getName(), restCompatDependency);

        GradleUtils.addTestSourceSet(project, TEST_SOURCE_SET_NAME);
        //tests need access to compat and main code
        GradleUtils.extendSourceSet(project, SOURCE_SET_NAME, TEST_SOURCE_SET_NAME);
        SourceSet restCompatTestSourceSet = sourceSets.getByName(TEST_SOURCE_SET_NAME);
        Configuration restCompatTestCompileConfig = project.getConfigurations().getByName(restCompatTestSourceSet.getCompileClasspathConfigurationName());
        if (BuildParams.isInternal()) {
            Dependency testFrameworkDependency = project.getDependencies().project(Map.of("path", ":test:framework"));
            Dependency restCompatTestDependency = project.getDependencies().project(Map.of("path", ":modules:rest-compatibility", "configuration", "testArtifacts"));
            project.getDependencies().add(restCompatTestCompileConfig.getName(), testFrameworkDependency);
            project.getDependencies().add(restCompatTestCompileConfig.getName(), restCompatDependency);
            project.getDependencies().add(restCompatTestCompileConfig.getName(), restCompatTestDependency);
        } else {

            project.getDependencies().add(restCompatTestCompileConfig.getName(),
                "org.elasticsearch.test:framework:" + VersionProperties.getElasticsearch());
            //TODO: nuke modules:rest-compatibility

        }

        project.getPlugins().withType(YamlRestTestPlugin.class, yamlRestTestPlugin -> {
            String compatYamlTestTaskName = COMPATIBLE_VERSION + YamlRestTestPlugin.SOURCE_SET_NAME;
            Task restCompatibilityTestTask = yamlRestTestPlugin.createTask(compatYamlTestTaskName, project, restCompatTestSourceSet);
            restCompatibilityTestTask.dependsOn(":distribution:bwc:minor:checkoutBwcBranch");

            RestTestRunnerTask runner = (RestTestRunnerTask) project.getTasks().getByName(compatYamlTestTaskName + "Runner");
            runner.systemProperty("tests.rest.tests.path.prefix", "/" + COMPATIBLE_VERSION);



            //TODO: copy to correct location
        });

        GradleUtils.setupIdeForNonTestSourceSet(project, restCompatSourceSet);
        GradleUtils.setupIdeForTestSourceSet(project, restCompatTestSourceSet);
    }
}
