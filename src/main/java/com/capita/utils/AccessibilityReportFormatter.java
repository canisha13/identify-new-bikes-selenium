package com.capita.utils;

import com.deque.html.axecore.results.CheckedNode;
import com.deque.html.axecore.results.Rule;

import java.util.List;

public final class AccessibilityReportFormatter {

    private AccessibilityReportFormatter() {}

    public static String formatViolations(List<Rule> violations) {
        if (violations == null || violations.isEmpty()) {
            return "No accessibility violations were found.";
        }

        StringBuilder report = new StringBuilder();
        report.append(System.lineSeparator())
                .append("Accessibility violations found: ")
                .append(violations.size())
                .append(System.lineSeparator());

        int violationNumber = 1;

        for (Rule violation : violations) {
            report.append(System.lineSeparator())
                    .append("==================================================")
                    .append(System.lineSeparator())
                    .append("Violation ").append(violationNumber++)
                    .append(System.lineSeparator())
                    .append("Rule ID: ").append(violation.getId())
                    .append(System.lineSeparator())
                    .append("Impact: ").append(violation.getImpact())
                    .append(System.lineSeparator())
                    .append("Description: ").append(violation.getDescription())
                    .append(System.lineSeparator())
                    .append("Help: ").append(violation.getHelp())
                    .append(System.lineSeparator())
                    .append("Help URL: ").append(violation.getHelpUrl())
                    .append(System.lineSeparator());

            List<CheckedNode> nodes = violation.getNodes();
            if (nodes != null) {
                report.append("Affected elements: ").append(nodes.size())
                        .append(System.lineSeparator());

                int nodeNumber = 1;
                for (CheckedNode node : nodes) {
                    report.append(System.lineSeparator())
                            .append("Element ").append(nodeNumber++)
                            .append(System.lineSeparator())
                            .append("Target: ").append(node.getTarget())
                            .append(System.lineSeparator())
                            .append("HTML: ").append(node.getHtml())
                            .append(System.lineSeparator())
                            .append("Failure summary: ").append(node.getFailureSummary())
                            .append(System.lineSeparator());
                }
            }
        }

        return report.toString();
    }
}