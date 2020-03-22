package org.elasticsearch.gradle.test.rest.report;

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.reporting.Reporting;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.TaskAction;
import org.gradle.util.ClosureBackedAction;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;

public class RestResourcesReportsTask extends DefaultTask implements Reporting<RestResourcesReportsContainer> {

    private final RestResourcesReportsContainer reports;

    public RestResourcesReportsTask() {
        this.reports = getObjectFactory().newInstance(RestResourcesReportsContainer.class, this);
    }

    @Inject
    protected ObjectFactory getObjectFactory() {
        throw new UnsupportedOperationException();
    }

    @TaskAction
    public void run() {
        reports.getReportNames().forEach(name -> {
            RestResourceReport report = reports.getReport(name);
            if (report.isEnabled()) {
                try {
                    Files.deleteIfExists(  report.getOutputLocation().get().getAsFile().toPath());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                report.appendToReport(getProject());
            }
        });
    }

    @Override
    @Nested
    public final RestResourcesReportsContainer getReports() {
        return reports;
    }

    @Override
    public RestResourcesReportsContainer reports(@DelegatesTo(value = RestResourcesReportsContainer.class,
        strategy = Closure.DELEGATE_FIRST) Closure closure) {
        return reports(new ClosureBackedAction<>(closure));
    }

    @Override
    public RestResourcesReportsContainer reports(Action<? super RestResourcesReportsContainer> configureAction) {
        configureAction.execute(reports);
        return reports;
    }
}
