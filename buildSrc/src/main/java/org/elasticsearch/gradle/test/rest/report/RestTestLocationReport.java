package org.elasticsearch.gradle.test.rest.report;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.reporting.internal.TaskGeneratedSingleFileReport;
import org.gradle.api.tasks.Input;

import javax.inject.Inject;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public abstract class RestTestLocationReport extends RestResourceReport {
    @Inject
    public RestTestLocationReport(String name, Task task) {
        super(name, task);
    }

    @Override
    void appendToReport(Project project) {
        try (FileWriter fileWriter = new FileWriter(this.getOutputLocation().getAsFile().get(), true)) {
            if (isSubprojects()) {
                for (Project p : project.getSubprojects()) {
                    fileWriter.append(p.getPath() + "," + p.getBuildDir() + System.lineSeparator());
                }
            } else {
                fileWriter.append(project.getPath() + "," + project.getBuildDir() + System.lineSeparator());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(project.getPath());
        System.out.println("Generating rest test location report to " + getOutputLocation().getAsFile().get().getAbsolutePath());
    }
}
