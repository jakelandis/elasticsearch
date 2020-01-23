package org.elasticsearch.gradle.test

import org.elasticsearch.gradle.BwcVersions

import javax.inject.Inject

class RestCompatTestTask extends AbstractRestTestTask {

    @Inject
    RestCompatTestTask(BwcVersions.VersionInfo versionInfo) {
        super.setupRunner()
        //used by the tests so they can run directly from the checked out source
        runner.nonInputProperties.systemProperty("compatVersion", project.findProject(versionInfo.gradleProjectPath).ext["checkoutDir${versionInfo.version}"])
        this.dependsOn "${versionInfo.gradleProjectPath}:v${versionInfo.version}#checkoutBwcBranch"
    }
}


