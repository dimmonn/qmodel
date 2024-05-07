package com.research.qmodel.model;

public enum GITHUB_DEFINITION {
    MERGE("Merge pull request #");
    private String value;

    GITHUB_DEFINITION(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
