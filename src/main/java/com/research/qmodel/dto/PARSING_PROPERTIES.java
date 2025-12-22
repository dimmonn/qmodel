package com.research.qmodel.dto;

public enum PARSING_PROPERTIES {
    URL("url"),
    FILES("files"),
    ADDITIONS("additions"),
    DELETIONS("deletions"),
    CHANGES("changes"),
    FILENAME("filename"),
    STATUS("status"),
    PATCH("patch"),
    SHA("sha"),
    COMMIT("commit"),
    AUTHOR("author"),
    DATE("date"),
    WORKFLOW_RUNS("workflow_runs"),
    REACTIONS("reactions"),
    TOTAL_COUNT("total_count"),
    PLUS_ONE("+1"),
    MINUS_ONE("-1"),
    LAUGH("laugh"),
    HOORAY("hooray"),
    CONFUSED("confused"),
    HEART("heart"),
    ROCKET("rocket"),
    EYES("eyes");


    private final String key;

    PARSING_PROPERTIES(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }
}
