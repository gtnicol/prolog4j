/* GNU Prolog for Java
 * Copyright (C) 2025  Daniel Thomas
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA  02111-1307, USA. The text of license can be also found
 * at http://www.gnu.org/copyleft/lgpl.html
 */
package gnu.prolog.test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Validates test output from Prolog test suites (Vanilla and Inria).
 * Detects failures based on:
 * - Java exceptions/stack traces
 * - Inria suite error counts
 * - ClassNotFoundException errors
 */
public class TestResultValidator {

    private static final Pattern INRIA_ERROR_PATTERN =
        Pattern.compile("\\*\\s*(\\d+)\\s*\\*\\s*BIPs gave a total of \\*\\s*(\\d+)\\s*\\* unexpected answers");

    private static final Pattern EXCEPTION_PATTERN =
        Pattern.compile("^\\s*at\\s+[\\w.$]+\\.[\\w]+\\(", Pattern.MULTILINE);

    private static final Pattern CLASS_NOT_FOUND_PATTERN =
        Pattern.compile("java\\.lang\\.ClassNotFoundException:");

    public static void main(final String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: TestResultValidator <test-suite-name> <output-file> [baseline-file]");
            System.exit(1);
        }

        final String suiteName = args[0];
        final Path outputPath = Paths.get(args[1]);
        final Path baselinePath = args.length > 2 ? Paths.get(args[2]) : null;

        if (!Files.exists(outputPath)) {
            System.err.println("ERROR: Output file not found: " + outputPath);
            System.exit(1);
        }

        try {
            final List<String> lines = Files.readAllLines(outputPath);
            final String content = String.join("\n", lines);

            // Load baseline if provided
            final Set<String> baselineFailures = loadBaseline(baselinePath);

            final TestResult result = validateTestOutput(suiteName, content, baselineFailures);

            System.out.println("=== " + suiteName + " Test Results ===");
            System.out.println("Status: " + (result.passed ? "PASSED" : "FAILED"));

            if (result.errorCount > 0) {
                System.out.println("Errors: " + result.errorCount);
            }
            if (result.bipCount > 0) {
                System.out.println("BIPs with errors: " + result.bipCount);
            }
            if (result.hasExceptions) {
                System.out.println("Java exceptions detected: YES");
            }
            if (result.hasClassNotFound) {
                System.out.println("ClassNotFoundException detected: YES");
            }

            if (!result.passed) {
                System.err.println("\n" + suiteName + " tests FAILED!");
                if (result.failureReason != null) {
                    System.err.println("Reason: " + result.failureReason);
                }
                System.exit(1);
            } else {
                System.out.println("\n" + suiteName + " tests PASSED!");
            }

        } catch (final IOException e) {
            System.err.println("ERROR: Failed to read output file: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static Set<String> loadBaseline(final Path baselinePath) throws IOException {
        final Set<String> baseline = new HashSet<>();
        if (baselinePath != null && Files.exists(baselinePath)) {
            final List<String> lines = Files.readAllLines(baselinePath);
            for (final String line : lines) {
                final String trimmed = line.trim();
                // Skip empty lines and comments
                if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                    baseline.add(trimmed);
                }
            }
        }
        return baseline;
    }

    private static List<String> extractFailingBips(final String content) {
        final List<String> failing = new ArrayList<>();
        final String[] lines = content.split("\n");
        boolean inFailureSection = false;
        boolean passedHeader = false;

        for (final String line : lines) {
            if (line.contains("BIPs gave") && line.contains("unexpected answers")) {
                inFailureSection = true;
                continue;
            }
            if (inFailureSection) {
                if (line.contains("results should be examined")) {
                    passedHeader = true;
                    continue;
                }
                if (!passedHeader) {
                    continue;  // Skip lines before "results should be examined"
                }
                final String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;  // Skip empty lines but keep reading
                }
                if (line.contains("time =") || line.contains("SUCCESS")) {
                    break;  // End of failure list
                }
                failing.add(trimmed);
            }
        }
        return failing;
    }

    private static TestResult validateTestOutput(final String suiteName, final String content,
                                                   final Set<String> baselineFailures) {
        final TestResult result = new TestResult();
        result.passed = true;

        // Check for Java exceptions (stack traces)
        final Matcher exceptionMatcher = EXCEPTION_PATTERN.matcher(content);
        if (exceptionMatcher.find()) {
            result.hasExceptions = true;
            result.passed = false;
            result.failureReason = "Java exceptions/stack traces found in output";
        }

        // Check for ClassNotFoundException
        final Matcher classNotFoundMatcher = CLASS_NOT_FOUND_PATTERN.matcher(content);
        if (classNotFoundMatcher.find()) {
            result.hasClassNotFound = true;
            result.passed = false;
            result.failureReason = "ClassNotFoundException found - missing builtin implementations";
        }

        // For Inria suite, check error count and compare against baseline
        if ("inria".equalsIgnoreCase(suiteName)) {
            final Matcher inriaMatcher = INRIA_ERROR_PATTERN.matcher(content);
            if (inriaMatcher.find()) {
                result.bipCount = Integer.parseInt(inriaMatcher.group(1));
                result.errorCount = Integer.parseInt(inriaMatcher.group(2));

                if (result.errorCount > 0 || result.bipCount > 0) {
                    // Extract actual failing BIPs
                    final List<String> actualFailures = extractFailingBips(content);
                    final Set<String> actualSet = new HashSet<>(actualFailures);

                    // Check for regressions: new failures or more failures
                    final Set<String> newFailures = new HashSet<>(actualSet);
                    newFailures.removeAll(baselineFailures);

                    final Set<String> fixedFailures = new HashSet<>(baselineFailures);
                    fixedFailures.removeAll(actualSet);

                    if (!newFailures.isEmpty()) {
                        // New failures detected - this is a regression
                        result.passed = false;
                        result.failureReason = "REGRESSION: New failing BIPs detected: " +
                            String.join(", ", newFailures) + " (total: " + result.bipCount +
                            " BIPs, " + result.errorCount + " errors)";
                    } else if (actualSet.size() > baselineFailures.size()) {
                        // More failures than baseline (even if all are in baseline)
                        result.passed = false;
                        result.failureReason = "REGRESSION: More failing BIPs than baseline (" +
                            actualSet.size() + " vs " + baselineFailures.size() + ")";
                    } else {
                        // Within acceptable baseline
                        result.passed = result.passed && !result.hasExceptions && !result.hasClassNotFound;
                        if (!fixedFailures.isEmpty()) {
                            result.failureReason = "IMPROVEMENT: Fixed BIPs: " +
                                String.join(", ", fixedFailures) + " (remaining: " +
                                actualSet.size() + " BIPs, " + result.errorCount + " errors)";
                        } else {
                            result.failureReason = "Known failures within baseline: " + result.bipCount +
                                " BIPs gave " + result.errorCount + " unexpected answers";
                        }
                    }
                }
            } else if (content.contains("All bips passed")) {
                // Explicitly passed
                result.passed = result.passed && !result.hasExceptions && !result.hasClassNotFound;
            } else {
                // Inria suite should have one of these patterns
                result.passed = false;
                result.failureReason = "Could not find test completion pattern in Inria output";
            }
        }

        // For Vanilla suite, we mainly check for exceptions
        // The vanilla suite has known acceptable warnings like "predicate is not discontiguous"

        return result;
    }

    private static class TestResult {
        boolean passed;
        String failureReason;
        int errorCount;
        int bipCount;
        boolean hasExceptions;
        boolean hasClassNotFound;
    }
}
