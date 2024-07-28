package com.research.qmodel.graph;

import org.eclipse.jgit.lib.ProgressMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SimpleProgressMonitor implements ProgressMonitor {
    private final Logger LOGGER = LoggerFactory.getLogger(SimpleProgressMonitor.class);
    private int totalTasks = 0;
    private int completedTasks = 0;

    @Override
    public void start(int totalTasks) {
        this.totalTasks = totalTasks;
        LOGGER.info("Starting clone with {} tasks", totalTasks);
    }

    @Override
    public void beginTask(String title, int totalWork) {
        LOGGER.info("Starting task: {}", title);
    }

    @Override
    public void update(int completed) {
        completedTasks += completed;
        LOGGER.info("Progress: {}/{}", completedTasks, totalTasks);
    }

    @Override
    public void endTask() {
        LOGGER.info("Task completed");
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public void showDuration(boolean b) {

    }
}
