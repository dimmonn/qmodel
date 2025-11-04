package com.research.qmodel.annotations;

import com.research.qmodel.dto.Project;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface FilterUrl {
    String PROJECT_REGEX = "^https:\\/\\/api\\.github\\.com\\/repos\\/([^\\/]+)\\/([^\\/]+).*";
    String ID_REGEX = ".*/(\\d+)$";
    default Project parseToProject(String url) {
        Pattern pattern = Pattern.compile(PROJECT_REGEX);
        Matcher matcher = pattern.matcher(url);
        if (matcher.matches()) {
            String projectOwner = matcher.group(1);
            String projectName = matcher.group(2);
            return new Project(projectOwner, projectName);
        }
        return null;
    }
    default Long parseToID(String url) {
        Pattern pattern = Pattern.compile(ID_REGEX);
        Matcher matcher = pattern.matcher(url);
        if (matcher.matches()) {
            String foundID = matcher.group(1);
            return Long.parseLong(foundID);
        }
        return null;
    }
}
