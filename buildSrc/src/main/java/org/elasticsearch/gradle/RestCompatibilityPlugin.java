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
import org.elasticsearch.gradle.test.rest.CopyRestApiTask;
import org.elasticsearch.gradle.test.rest.CopyRestTestsTask;
import org.elasticsearch.gradle.test.rest.RestResourcesExtension;
import org.elasticsearch.gradle.test.rest.RestResourcesPlugin;
import org.elasticsearch.gradle.test.rest.YamlRestTestPlugin;
import org.elasticsearch.gradle.testclusters.RestTestRunnerTask;
import org.elasticsearch.gradle.util.GradleUtils;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.bundling.Zip;

import java.io.File;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collector;
import java.util.stream.Collectors;

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



        GradleUtils.addTestSourceSet(project, TEST_SOURCE_SET_NAME);
        //tests need access to compat and main code
        GradleUtils.extendSourceSet(project, SOURCE_SET_NAME, TEST_SOURCE_SET_NAME);
        SourceSet restCompatTestSourceSet = sourceSets.getByName(TEST_SOURCE_SET_NAME);
        Configuration restCompatTestCompileConfig = project.getConfigurations().getByName(restCompatTestSourceSet.getCompileClasspathConfigurationName());
        if (BuildParams.isInternal()) {
            Dependency testFrameworkDependency = project.getDependencies().project(Map.of("path", ":test:framework"));
            project.getDependencies().add(restCompatTestCompileConfig.getName(), testFrameworkDependency);
        } else {

            project.getDependencies().add(restCompatTestCompileConfig.getName(),
                "org.elasticsearch.test:framework:" + VersionProperties.getElasticsearch());

        }

        //required so that the checkoutDir extension is populated
        project.evaluationDependsOn(":distribution:bwc");

        project.getPlugins().withType(YamlRestTestPlugin.class, yamlRestTestPlugin -> {
            String compatYamlTestTaskName = COMPATIBLE_VERSION + YamlRestTestPlugin.SOURCE_SET_NAME;
            Task restCompatibilityTestTask = yamlRestTestPlugin.createTask(compatYamlTestTaskName, project, restCompatTestSourceSet);
            restCompatibilityTestTask.dependsOn(":distribution:bwc:minor:checkoutBwcBranch");
            RestTestRunnerTask runner = (RestTestRunnerTask) project.getTasks().getByName(compatYamlTestTaskName + "Runner");
            //TODO: support a pre-fix with the copy
            runner.systemProperty("tests.rest.tests.path.prefix", "/" + COMPATIBLE_VERSION);

            //add dependency on server-rest-compatibility
            restCompatibilityTestTask.dependsOn(":modules:server-rest-compatibility:bundlePlugin");
            Zip bundle = (Zip) project.findProject(":modules:server-rest-compatibility").getTasks().getByName("bundlePlugin");
            runner.getClusters().forEach(c -> c.module(bundle.getArchiveFile()));


            File checkoutDir = (File) project.findProject(":distribution:bwc:minor").getExtensions().getExtraProperties().get("checkoutDir");
            String rootDir = project.getRootDir().getAbsolutePath();
            String projectDir = project.getProjectDir().getAbsolutePath();
            File compatProjectDir = new File(checkoutDir, projectDir.replace(rootDir, ""));
            //rest-api-spec has the api and tests under then main folder
            AtomicReference<String> restResourceParent = new AtomicReference<>(YamlRestTestPlugin.SOURCE_SET_NAME);
            if (":rest-api-spec".equals(project.getPath())) {
                restResourceParent.set("main");
            }

            Configuration compatApiConfig = project.getConfigurations().create("compatApiConfig");
            project.getDependencies().add(compatApiConfig.getName(),
                project.files(new File(compatProjectDir, "/src/" + restResourceParent.get() + "/resources/" + CopyRestApiTask.REST_API_PREFIX)));
            //TODO: ensure the core is always copied by adding core to the dependency here

            Configuration compatTestConfig = project.getConfigurations().create("compatTestConfig");
            project.getDependencies().add(compatTestConfig.getName(),
                project.files(new File(compatProjectDir, "/src/" + restResourceParent.get() + "/resources/" + CopyRestTestsTask.REST_TEST_PREFIX)));


            RestResourcesExtension extension = project.getExtensions().getByType(RestResourcesExtension.class);
            Provider<CopyRestTestsTask> copyRestYamlTestTask = project.getTasks()
                .register(COMPATIBLE_VERSION + "copyYamlTestsTask", CopyRestTestsTask.class, task -> {
                    task.includeCore.set(extension.restTests.getIncludeCore());
                    task.includeXpack.set(extension.restTests.getIncludeXpack());
                    task.copyToPrefix = COMPATIBLE_VERSION;
                    if (project.getPath().contains("x-pack")) {
                        task.xpackConfig = compatTestConfig;
                    } else {
                        task.coreConfig = compatTestConfig;
                    }
                    task.sourceSetName = restCompatTestSourceSet.getName();
                    task.dependsOn(compatTestConfig);
                });


            Provider<CopyRestApiTask> copyRestYamlApiTask = project.getTasks()
                .register(COMPATIBLE_VERSION + "copyYamlApiTask", CopyRestApiTask.class, task -> {
                    task.includeCore.set(extension.restTests.getIncludeCore());
                    task.includeXpack.set(extension.restTests.getIncludeXpack());
                    task.copyToPrefix = COMPATIBLE_VERSION;
                    if (project.getPath().contains("x-pack")) {
                        task.xpackConfig = compatApiConfig;
                    } else {
                        task.coreConfig = compatApiConfig;
                    }
                    task.sourceSetName = restCompatTestSourceSet.getName();
                    task.dependsOn(compatTestConfig);
                });

            runner.dependsOn(copyRestYamlApiTask);
            runner.dependsOn(copyRestYamlTestTask);
        });

        GradleUtils.setupIdeForNonTestSourceSet(project, restCompatSourceSet);
        GradleUtils.setupIdeForTestSourceSet(project, restCompatTestSourceSet);
    }
}
