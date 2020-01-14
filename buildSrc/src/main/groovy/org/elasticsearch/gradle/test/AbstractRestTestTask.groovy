package org.elasticsearch.gradle.test

import org.elasticsearch.gradle.VersionProperties
import org.elasticsearch.gradle.info.BuildParams
import org.elasticsearch.gradle.testclusters.ElasticsearchCluster
import org.elasticsearch.gradle.testclusters.RestTestRunnerTask
import org.elasticsearch.gradle.tool.Boilerplate
import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.file.FileCopyDetails
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.testing.Test
import org.gradle.plugins.ide.idea.IdeaPlugin

abstract class AbstractRestTestTask extends DefaultTask {

    protected Test runner

    protected void setup(){
        setupRunner()
        // copy the rest spec/tests onto the test classpath
        Copy copyRestSpec = createCopyRestSpecTask()
        project.sourceSets.test.output.builtBy(copyRestSpec)
    }

    protected void setupRunner() {
        runner = project.tasks.create("${name}Runner", RestTestRunnerTask.class)
        super.dependsOn(runner)

        project.testClusters {
            "$name" {
                javaHome = BuildParams.runtimeJavaHome
            }
        }
        runner.useCluster project.testClusters."$name"

        runner.include('**/*IT.class')
        runner.systemProperty('tests.rest.load_packaged', 'false')

        if (System.getProperty("tests.rest.cluster") == null) {
            if (System.getProperty("tests.cluster") != null || System.getProperty("tests.clustername") != null) {
                throw new IllegalArgumentException("tests.rest.cluster, tests.cluster, and tests.clustername must all be null or non-null")
            }

            ElasticsearchCluster cluster = project.testClusters."${name}"
            runner.nonInputProperties.systemProperty('tests.rest.cluster', "${-> cluster.allHttpSocketURI.join(",")}")
            runner.nonInputProperties.systemProperty('tests.cluster', "${-> cluster.transportPortURI}")
            runner.nonInputProperties.systemProperty('tests.clustername', "${-> cluster.getName()}")
        } else {
            if (System.getProperty("tests.cluster") == null || System.getProperty("tests.clustername") == null) {
                throw new IllegalArgumentException("tests.rest.cluster, tests.cluster, and tests.clustername must all be null or non-null")
            }
            // an external cluster was specified and all responsibility for cluster configuration is taken by the user
            runner.systemProperty('tests.rest.cluster', System.getProperty("tests.rest.cluster"))
            runner.systemProperty('test.cluster', System.getProperty("tests.cluster"))
            runner.systemProperty('test.clustername', System.getProperty("tests.clustername"))
        }

        // this must run after all projects have been configured, so we know any project
        // references can be accessed as a fully configured
        project.gradle.projectsEvaluated {
            if (enabled == false) {
                runner.enabled = false
                return // no need to add cluster formation tasks if the task won't run!
            }
        }

    }


    @Override
    public Task dependsOn(Object... dependencies) {
        runner.dependsOn(dependencies)
        for (Object dependency : dependencies) {
            if (dependency instanceof Fixture) {
                runner.finalizedBy(((Fixture) dependency).getStopTask())
            }
        }
        return this
    }

    @Override
    public void setDependsOn(Iterable<?> dependencies) {
        runner.setDependsOn(dependencies)
        for (Object dependency : dependencies) {
            if (dependency instanceof Fixture) {
                runner.finalizedBy(((Fixture) dependency).getStopTask())
            }
        }
    }

    public void runner(Closure configure) {
        project.tasks.getByName("${name}Runner").configure(configure)
    }

    private Copy createCopyRestSpecTask() {
        Boilerplate.maybeCreate(project.configurations, 'restSpec') {
            project.dependencies.add(
                'restSpec',
                BuildParams.internal ? project.project(':rest-api-spec') :
                    "org.elasticsearch:rest-api-spec:${VersionProperties.elasticsearch}"
            )
        }

        return Boilerplate.maybeCreate(project.tasks, 'copyRestSpec', Copy) { Copy copy ->
            copy.dependsOn project.configurations.restSpec
            copy.into(project.sourceSets.test.output.resourcesDir)
            copy.from({ project.zipTree(project.configurations.restSpec.singleFile) }) {
                includeEmptyDirs = false
                include 'rest-api-spec/**'
                filesMatching('rest-api-spec/test/**') { FileCopyDetails details ->
                    if (includePackaged == false) {
                        details.exclude()
                    }
                }
            }

            if (project.plugins.hasPlugin(IdeaPlugin)) {
                project.idea {
                    module {
                        if (scopes.TEST != null) {
                            scopes.TEST.plus.add(project.configurations.restSpec)
                        }
                    }
                }
            }
        }
    }
}
