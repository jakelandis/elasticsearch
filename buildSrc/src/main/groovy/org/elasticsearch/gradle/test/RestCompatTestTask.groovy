package org.elasticsearch.gradle.test

import org.elasticsearch.gradle.BwcVersions
import org.elasticsearch.gradle.Version
import org.elasticsearch.gradle.tool.Boilerplate
import org.gradle.api.file.FileCopyDetails
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.testing.Test
import org.gradle.plugins.ide.idea.IdeaPlugin

import javax.inject.Inject

class RestCompatTestTask extends AbstractRestTestTask {

    private final Version version
    private final BwcVersions bwcVersions
    private final boolean isSnapshot;
    private final String versionPrefix;
    private final String versionPostfix;

    protected Test runner

    @Input
    Boolean copyRestSpecTests = false


    @Inject
    RestCompatTestTask(BwcVersions bwcVersions, Version version) {
        this.version = version
        this.bwcVersions = bwcVersions
        this.isSnapshot = bwcVersions.unreleased.contains(version);
        this.versionPrefix = "v" + version.toString() + "#"
        this.versionPostfix = isSnapshot ? "-SNAPSHOT" : ""
        super.setupRunner()
        createCopyRestSpecTask(version.toString(), versionPrefix, versionPostfix, isSnapshot)
    }

    public void copyRestSpecTests(boolean copyRestSpecTests) {
        this.copyRestSpecTests = copyRestSpecTests
    }

    /**
     * Creates multiple copyRestSpec tasks. For released artifacts, use a version specific configuration so it can download from a
     * repository. For unreleased artifacts, pull the rest-api-spec from source. This re-uses the bwc:minor and bwc:bugfix branches as
     * exposed from the distribution gradle project.
     */
    private Copy createCopyRestSpecTask(String version, String versionPrefix, String versionPostfix, boolean isSnapshot) {
        //only create configuration for released artifacts
        if (isSnapshot == false) {
            Boilerplate.maybeCreate(project.configurations, "${versionPrefix}restSpec") {
                project.dependencies.add(
                    "${versionPrefix}restSpec", "org.elasticsearch:rest-api-spec:${version}${versionPostfix}"
                )
            }
        }

        return Boilerplate.maybeCreate(project.tasks, "${versionPrefix}copyRestSpec", Copy) { Copy copy ->
            //need to ensure different versions land in different directories to ensure parallel builds don't stomp on each other
            copy.into(new File(project.sourceSets.test.output.resourcesDir, version))
            if (isSnapshot) {
                //TODO: figure out which source to pull from
                //copy files from source
                copy.into(project.sourceSets.test.output.resourcesDir)
                copy.dependsOn(":distribution:bwc:minor:checkoutBwcBranch")
                copy.from(new File(project.findProject(":distribution:bwc:minor").checkoutDir, "rest-api-spec/src/main/resources")) {
                    includeEmptyDirs = false
                    include 'rest-api-spec/**'
                    //TODO: always pull the tests from source, even for released versions so that we can better support modules and plugins
                    filesMatching('rest-api-spec/test/**') { FileCopyDetails details ->
                        if (copyRestSpecTests == false) {
                            details.exclude()
                        }
                    }
                }
            } else {
                // copy from repository jar
                copy.dependsOn project.configurations."${versionPrefix}restSpec"
                copy.from({ project.zipTree(project.configurations."${versionPrefix}restSpec".singleFile) }) {
                    includeEmptyDirs = false
                    include 'rest-api-spec/**'
                    filesMatching('rest-api-spec/test/**') { FileCopyDetails details ->
                        if (copyRestSpecTests == false) {
                            details.exclude()
                        }
                    }
                }
                if (project.plugins.hasPlugin(IdeaPlugin)) {
                    project.idea {
                        module {
                            if (scopes.TEST != null && isSnapshot == false) {
                                scopes.TEST.plus.add(project.configurations."${versionPrefix}restSpec")
                            }
                        }
                    }
                }
            }
        }
    }
}
