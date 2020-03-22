package org.elasticsearch.gradle.test.rest.report;

import org.gradle.api.Task;
import org.gradle.api.internal.CollectionCallbackActionDecorator;
import org.gradle.api.reporting.internal.TaskReportContainer;
import org.gradle.api.tasks.Internal;

import javax.inject.Inject;
import java.util.Map;
import java.util.Set;

/**
 * Reports container for Rest Resources.
 */
public class RestResourcesReportsContainer extends TaskReportContainer<RestResourceReport> {

    private final Map<String, Class<? extends RestResourceReport>> reports =
        Map.of("tests", RestTestLocationReport.class);

    @Inject
    public RestResourcesReportsContainer(Task task, CollectionCallbackActionDecorator callbackActionDecorator) {
        super(RestResourceReport.class, task, callbackActionDecorator);
        reports.forEach((k, v) -> {
            add(v, k, task);
        });
    }

    RestResourceReport getReport(String reportName) {
        return getByName(reportName);
    }

    @Internal
    Set<String> getReportNames() {
        return reports.keySet();
    }
}
