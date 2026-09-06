package com.resumeai.util;

import java.util.Set;

public final class SkillDictionary {

    private SkillDictionary() {}

    public static final Set<String> KNOWN_SKILLS = Set.of(
            "java", "python", "javascript", "typescript", "c++", "c#", "go", "rust", "kotlin", "swift",
            "spring", "spring boot", "django", "flask", "fastapi", "express", "node.js", "nestjs",
            "react", "angular", "vue", "next.js", "redux", "html", "css", "tailwind", "bootstrap",
            "sql", "postgresql", "mysql", "mongodb", "redis", "elasticsearch", "cassandra", "dynamodb",
            "aws", "azure", "gcp", "docker", "kubernetes", "terraform", "ansible", "jenkins", "github actions",
            "ci/cd", "microservices", "rest", "restful api", "graphql", "grpc", "kafka", "rabbitmq",
            "git", "linux", "bash", "nginx", "junit", "jest", "cypress", "selenium", "pytest",
            "machine learning", "deep learning", "tensorflow", "pytorch", "pandas", "numpy", "scikit-learn",
            "hibernate", "jpa", "maven", "gradle", "webpack", "vite", "figma", "agile", "scrum", "jira",
            "oauth", "jwt", "websocket", "load balancing", "system design", "data structures", "algorithms"
    );

    public static boolean isKnownSkill(String candidate) {
        return KNOWN_SKILLS.contains(candidate.trim().toLowerCase());
    }
}

