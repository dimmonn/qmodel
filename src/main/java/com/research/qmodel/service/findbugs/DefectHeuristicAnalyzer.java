package com.research.qmodel.service.findbugs;

import org.apache.commons.lang3.StringUtils;

public class DefectHeuristicAnalyzer {
  public boolean isPotentiallyDefectivePatch(String patch) {
    if (patch == null || patch.isBlank()) return false;

    String[] lines = patch.split("\n");
    for (String line : lines) {
      line = line.trim();

      // Skip hunk headers and unchanged lines
      if (line.startsWith("@@") || line.startsWith(" ")) continue;

      // Analyze REMOVED lines for suspicious deletions
      if (line.startsWith("-")) {
        String removed = line.substring(1).trim().toLowerCase();
        if (StringUtils.containsIgnoreCase(removed,"null")
            || StringUtils.containsIgnoreCase(removed,"assert")
            || StringUtils.containsIgnoreCase(removed,"validate")) {
          return true; // Possibly removed validation/check
        }
        if (StringUtils.containsIgnoreCase(removed,"if") && StringUtils.containsIgnoreCase(removed,"(") && StringUtils.containsIgnoreCase(removed,")")) {
          return true; // Removed a conditional block
        }
      }

      // Analyze ADDED lines for suspicious additions
      if (line.startsWith("+")) {
        String added = line.substring(1).trim().toLowerCase();
        if (StringUtils.containsIgnoreCase(added, "todo")
            || StringUtils.containsIgnoreCase(added, "fixme")) {
          return true;
        }
        if (StringUtils.containsIgnoreCase(added,"system.out") || StringUtils.containsIgnoreCase(added,"printstacktrace")) {
          return true;
        }
        if (added.matches(".*catch\\s*\\(.*\\)\\s*\\{\\s*\\}")) {
          return true; // empty catch block
        }
      }
    }

    return false; // No signals detected
  }

  public static void main(String[] args) {
    boolean potentialDefect =
        new DefectHeuristicAnalyzer()
            .isPotentiallyDefectivePatch(
                "@@ -12,4 +12,6 @@ public static void main(String[] args) {\n"
                    + "         SpringApplication.run(TestApplication.class, args);\n"
                    + "     }\n"
                    + " \n"
                    + "+    void aa() {\n"
                    + "+    }\n"
                    + " }");
    System.out.println(potentialDefect);
  }
}
