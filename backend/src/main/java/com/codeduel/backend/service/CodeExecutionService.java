package com.codeduel.backend.service;

import com.codeduel.backend.config.CodeExecutionProperties;
import com.codeduel.backend.dto.TestResultResponse;
import com.codeduel.backend.model.TestCase;
import com.codeduel.backend.model.enums.DifficultyLevel;
import com.codeduel.backend.model.enums.SubmissionStatus;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CodeExecutionService {

    private final CodeExecutionProperties config;
    private final ObjectMapper objectMapper;

    /**
     * Executes user code against all test cases using Python directly on the host.
     *
     * @param code       The user's Python source code
     * @param testCases  The test cases to run against
     * @param difficulty The challenge difficulty (determines timeout)
     * @return List of results per test case
     */
    public List<TestResultResponse> execute(String code, List<TestCase> testCases, DifficultyLevel difficulty) {
        Path tempDir = null;
        try {
            tempDir = prepareTempDirectory(code, testCases);
            int perTestTimeout = config.getTimeoutForDifficulty(difficulty);

            String runnerOutput = runPythonDirectly(tempDir, perTestTimeout, testCases.size());

            return parseAndEvaluateResults(runnerOutput, testCases);
        } catch (Exception e) {
            log.error("Code execution failed: {}", e.getMessage(), e);
            return buildErrorResults(testCases, e.getMessage());
        } finally {
            cleanupTempDir(tempDir);
        }
    }

    /**
     * Creates a temp directory with: solution.py, test_cases.json, runner.py
     */
    private Path prepareTempDirectory(String code, List<TestCase> testCases) throws IOException {
        Path tempDir = Files.createTempDirectory("codeduel-");

        // Write user's code
        Files.writeString(tempDir.resolve("solution.py"), code);

        // Write test cases as JSON
        List<Map<String, Object>> testData = testCases.stream()
                .map(tc -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("testOrder", tc.getTestOrder());
                    map.put("input", tc.getInput());
                    return map;
                })
                .collect(Collectors.toList());
        Files.writeString(tempDir.resolve("test_cases.json"), objectMapper.writeValueAsString(testData));

        // Copy runner.py from classpath resources
        ClassPathResource runnerResource = new ClassPathResource("docker/runner.py");
        Files.copy(runnerResource.getInputStream(), tempDir.resolve("runner.py"));

        return tempDir;
    }

    /**
     * Runs the Python runner directly on the host (no Docker sandbox).
     */
    private String runPythonDirectly(Path tempDir, int perTestTimeout, int testCount) throws Exception {
        int totalTimeoutSeconds = (perTestTimeout * testCount) + config.getContainerTimeoutBuffer();

        Path runnerPath = tempDir.resolve("runner.py");

        ProcessBuilder pb = new ProcessBuilder(
                findPythonCommand(),
                runnerPath.toAbsolutePath().toString()
        );
        pb.directory(tempDir.toFile());
        pb.environment().put("TEST_TIMEOUT", String.valueOf(perTestTimeout));
        pb.redirectErrorStream(false);

        log.info("Executing Python runner with timeout={}s", totalTimeoutSeconds);

        Process process = pb.start();

        String stdout = new String(process.getInputStream().readAllBytes());
        String stderr = new String(process.getErrorStream().readAllBytes());

        boolean finished = process.waitFor(totalTimeoutSeconds, TimeUnit.SECONDS);

        if (!finished) {
            process.destroyForcibly();
            log.warn("Python execution timed out after {}s", totalTimeoutSeconds);
            throw new RuntimeException("Code execution timed out");
        }

        int exitCode = process.exitValue();

        if (exitCode != 0 && stdout.isBlank()) {
            log.warn("Python runner exited with code {}: {}", exitCode, stderr);
            throw new RuntimeException("Execution error: " + stderr.trim());
        }

        return stdout;
    }

    /**
     * Finds the available Python 3 executable on the host.
     */
    private String findPythonCommand() {
        for (String cmd : List.of("python3", "python")) {
            try {
                ProcessBuilder pb = new ProcessBuilder(cmd, "--version");
                Process p = pb.start();
                if (p.waitFor(5, TimeUnit.SECONDS) && p.exitValue() == 0) {
                    return cmd;
                }
            } catch (Exception ignored) {
            }
        }
        throw new RuntimeException("Python 3 is not available on this system");
    }

    /**
     * Parses the JSON output from runner.py and compares actual vs expected output.
     */
    private List<TestResultResponse> parseAndEvaluateResults(String containerOutput, List<TestCase> testCases) {
        try {
            List<Map<String, Object>> rawResults = objectMapper.readValue(
                    containerOutput.trim(),
                    new TypeReference<>() {}
            );

            // Build a lookup map: testOrder -> TestCase
            Map<Integer, TestCase> testCaseMap = testCases.stream()
                    .collect(Collectors.toMap(TestCase::getTestOrder, tc -> tc));

            List<TestResultResponse> results = new ArrayList<>();

            for (Map<String, Object> raw : rawResults) {
                int testOrder = (int) raw.get("testOrder");
                String actualOutput = ((String) raw.get("stdout")).trim();
                String stderr = (String) raw.get("stderr");
                int executionTimeMs = (int) raw.get("executionTimeMs");
                boolean timedOut = (boolean) raw.get("timedOut");
                int exitCode = (int) raw.get("exitCode");

                TestCase tc = testCaseMap.get(testOrder);
                String expectedOutput = tc.getExpectedOutput().trim();

                // Determine status
                SubmissionStatus status;
                if (timedOut) {
                    status = SubmissionStatus.TIME_LIMIT_EXCEEDED;
                } else if (exitCode != 0) {
                    status = SubmissionStatus.RUNTIME_ERROR;
                } else if (actualOutput.equals(expectedOutput)) {
                    status = SubmissionStatus.ACCEPTED;
                } else {
                    status = SubmissionStatus.WRONG_ANSWER;
                }

                results.add(TestResultResponse.builder()
                        .testOrder(testOrder)
                        .status(status)
                        .executionTimeMs(executionTimeMs)
                        .input(tc.getInput())
                        .expected(expectedOutput)
                        .actual(actualOutput)
                        .stderr(stderr != null && !stderr.isBlank() ? stderr.trim() : null)
                        .build());
            }

            return results;
        } catch (Exception e) {
            log.error("Failed to parse container output: {}", e.getMessage());
            return buildErrorResults(testCases, "Failed to parse execution results");
        }
    }

    /**
     * Builds error results for all test cases when execution completely fails.
     */
    private List<TestResultResponse> buildErrorResults(List<TestCase> testCases, String errorMessage) {
        return testCases.stream()
                .map(tc -> TestResultResponse.builder()
                        .testOrder(tc.getTestOrder())
                        .status(SubmissionStatus.RUNTIME_ERROR)
                        .executionTimeMs(0)
                        .input(tc.getInput())
                        .expected(tc.getExpectedOutput().trim())
                        .actual("")
                        .stderr(errorMessage)
                        .build())
                .toList();
    }

    /**
     * Cleans up the temporary directory after execution.
     */
    private void cleanupTempDir(Path tempDir) {
        if (tempDir == null) return;
        try {
            Files.walk(tempDir)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            log.warn("Failed to delete temp file: {}", path);
                        }
                    });
        } catch (IOException e) {
            log.warn("Failed to cleanup temp directory: {}", tempDir);
        }
    }
}
