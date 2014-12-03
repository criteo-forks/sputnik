package pl.touk.sputnik.processor.sonar;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import pl.touk.sputnik.review.ReviewResult;
import pl.touk.sputnik.review.Severity;
import pl.touk.sputnik.review.Violation;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;

/**
 * Parses a json file produced by a run of Sonar.
 */
@Slf4j
@AllArgsConstructor
class SonarResultParser {

    private final File resultFile;

    /**
     * A component in the result file is identified by a key.
     * It may reference another component by its key (called moduleKey).
     */
    @AllArgsConstructor
    private static class Component {
        public String path;
        public String moduleKey;
    }

    /**
     * Parses the file and returns all the issues as a ReviewResult.
     */
    public ReviewResult parseResults() throws IOException {
        try {
            ReviewResult result = new ReviewResult();
            JSONObject report = (JSONObject) new JSONTokener(new FileReader(resultFile)).nextValue();
            JSONArray issues = report.getJSONArray("issues");
            Map<String, Component> components = getComponents(report.getJSONArray("components"));
            for (int i = 0; i < issues.length(); i++) {
                JSONObject issue = (JSONObject) issues.get(i);
                if (!issue.getBoolean("isNew")) {
                    log.debug("Skipping already indexed issue: {}", issue.toString());
                    continue;
                }
                if (!issue.has("line")) {
                    log.debug("Skipping an issue with no line information: {}", issue.toString());
                    continue;
                }
                int line = issue.getInt("line");
                String message = issue.getString("message");
                String file = getIssueFilePath(issue.getString("component"), components);
                result.add(new Violation(file, line, message, getSeverity(issue.getString("severity"))));
            }
            return result;
           }
           catch (Exception e) {
               throw new IOException("Error while anaylzing " + resultFile, e);
        }
    }

    /**
     * Converts a Sonar severity to a Sputnik severity.
     * @param severityName severity to convert.
     */
    static Severity getSeverity(String severityName) {
        switch (severityName) {
        case "BLOCKER":
        case "CRITICAL":
        case "MAJOR":
            return Severity.ERROR;
        case "MINOR":
            return Severity.WARNING;
        case "INFO":
            return Severity.INFO;
        }
        log.warn("Unknown severity: " + severityName);
        return Severity.WARNING;
    }

    /**
     * Extracts all the components from the json data.
     */
    private Map<String, Component> getComponents(JSONArray jsonComponents) throws JSONException {
        Map<String, Component> components = Maps.newHashMap();
        for (int i = 0; i < jsonComponents.length(); i++) {
            JSONObject jsonComponent = jsonComponents.getJSONObject(i);
            if (jsonComponent.has("path")) {
                String moduleKey = jsonComponent.has("moduleKey") ? jsonComponent.getString("moduleKey") : null;
                components.put(jsonComponent.getString("key"),
                               new Component(jsonComponent.getString("path"), moduleKey));
            }
        }
        return components;
    }

    /**
     * Returns the path of the file linked to an issue created by Sonar.
     * The path is relative to the folder where Sonar has been run.
     *
     * @param issueComponent "component" field in an issue.
     * @param components information about all components.
     */
    private String getIssueFilePath(String issueComponent, Map<String, Component> components) {
        Component comp = components.get(issueComponent);
        String file = comp.path;
        if (!Strings.isNullOrEmpty(comp.moduleKey)) {
            Component moduleComp = components.get(comp.moduleKey);
            file = moduleComp.path + '/' + file;
        }
        return file;
    }
}
