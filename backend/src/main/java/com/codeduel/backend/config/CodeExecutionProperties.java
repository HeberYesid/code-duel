package com.codeduel.backend.config;

import com.codeduel.backend.model.enums.DifficultyLevel;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "code-execution")
@Getter
@Setter
public class CodeExecutionProperties {

    private DockerConfig docker = new DockerConfig();
    private Map<DifficultyLevel, Integer> timeouts = Map.of(
            DifficultyLevel.EASY, 3,
            DifficultyLevel.MEDIUM, 5,
            DifficultyLevel.HARD, 10
    );
    private int containerTimeoutBuffer = 5;

    /**
     * Returns the per-test timeout in seconds for a given difficulty.
     */
    public int getTimeoutForDifficulty(DifficultyLevel difficulty) {
        return timeouts.getOrDefault(difficulty, 5);
    }

    @Getter
    @Setter
    public static class DockerConfig {
        private String image = "python:3.12-alpine";
        private String memoryLimit = "128m";
        private String cpuLimit = "0.5";
        private int pidsLimit = 50;
    }
}
