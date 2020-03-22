package org.elasticsearch.gradle.test.rest.report;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.reporting.internal.TaskGeneratedSingleFileReport;
import org.gradle.api.tasks.Input;

import javax.inject.Inject;
import java.io.File;

public abstract class RestResourceReport extends TaskGeneratedSingleFileReport {

    boolean subprojects = true;
    @Input
    public boolean isSubprojects() {
        return subprojects;
    }

    public void setSubprojects(boolean subprojects) {
        this.subprojects = subprojects;
    }

    @Inject
    public RestResourceReport(String name, Task task) {
        super(name, task);
    }

    @Input
    public void destination(String file) {
        super.setDestination(new File(file));
    }

    abstract void appendToReport(Project project);
}
