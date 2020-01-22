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

    private final String gradleVersionName

    enum UnreleasedVersion {
        current /** N (this version) **/
        , minor /** N-1 **/
        , bugfix /** N-1.x **/
    }

    private UnreleasedVersion unreleasedVersion;
    protected Test runner

    @Input
    Boolean copyRestSpecTests = false


    @Inject
    RestCompatTestTask(BwcVersions bwcVersions, Version version) {
        this.version = version
        this.bwcVersions = bwcVersions
        boolean isSnapshot = bwcVersions.unreleased.contains(version);
        this.gradleVersionName = "v" + version.toString() + "#"
        super.setupRunner()
        if (isSnapshot) {
            for (Version bwcVersion : bwcVersions.unreleased) {
                if (bwcVersion.compareTo(version) == 0) {
                    if (bwcVersions.getCurrentVersion().equals(version)) {
                        this.unreleasedVersion = UnreleasedVersion.current;
                    } else if (bwcVersions.getLatestInMinor().equals(version)) {
                        this.unreleasedVersion = UnreleasedVersion.minor;
                    } else if (bwcVersions.getLatestRevisionInMinor().equals(version)) {
                        this.unreleasedVersion = UnreleasedVersion.bugfix;
                    }
                }
            }
            assert (this.unreleasedVersion != null)
        }
        createCopyRestSpecTask(version.toString(), gradleVersionName, unreleasedVersion)
    }

    public void copyRestSpecTests(boolean copyRestSpecTests) {
        this.copyRestSpecTests = copyRestSpecTests
    }

    /**
     * Creates multiple copyRestSpec tasks. For released artifacts, use a version specific configuration so it can download from a
     * repository. For unreleased artifacts, pull the rest-api-spec from source. This re-uses the bwc:minor and bwc:bugfix branches as
     * exposed from the distribution gradle project.
     */
    private Copy createCopyRestSpecTask(String version, String gradleVersionName, UnreleasedVersion unreleasedVersion) {
        //only create configuration for released artifacts
        if (unreleasedVersion == null) {
            Boilerplate.maybeCreate(project.configurations, "${gradleVersionName}restSpec") {
                project.dependencies.add(
                    "${gradleVersionName}restSpec", "org.elasticsearch:rest-api-spec:${version}"
                )
            }
        }

        return Boilerplate.maybeCreate(project.tasks, "${gradleVersionName}copyRestSpec", Copy) { Copy copy ->
            //need to ensure different versions land in different directories to ensure parallel builds don't stomp on each other
            copy.into(new File(project.sourceSets.test.output.resourcesDir, version))
            if (unreleasedVersion == null) { //released version
                // copy from repository jar
                copy.dependsOn project.configurations."${gradleVersionName}restSpec"
                copy.from({ project.zipTree(project.configurations."${gradleVersionName}restSpec".singleFile) }) {
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
                            if (scopes.TEST != null) {
                                scopes.TEST.plus.add(project.configurations."${gradleVersionName}restSpec")
                            }
                        }
                    }
                }
            } else {
                if (unreleasedVersion.equals(UnreleasedVersion.current)) {
                    copy.from( new File(project.rootDir, "rest-api-spec/src/main/resources")) {
                        includeEmptyDirs = false
                        include 'rest-api-spec/**'
                        //TODO: always pull the tests from source, even for released versions so that we can better support modules and plugins
                        filesMatching('rest-api-spec/test/**') { FileCopyDetails details ->
                            if (copyRestSpecTests == false) {
                                details.exclude()
                            }
                        }
                    }

                } else { //checkout source, then copy
                    copy.dependsOn(":distribution:bwc:${unreleasedVersion}:checkoutBwcBranch")
                    copy.from(new File(project.findProject(":distribution:bwc:${unreleasedVersion}").checkoutDir, "rest-api-spec/src/main/resources")) {
                        includeEmptyDirs = false
                        include 'rest-api-spec/**'
                        //TODO: always pull the tests from source, even for released versions so that we can better support modules and plugins
                        filesMatching('rest-api-spec/test/**') { FileCopyDetails details ->
                            if (copyRestSpecTests == false) {
                                details.exclude()
                            }
                        }
                    }
                }
            }
        }
    }
}
