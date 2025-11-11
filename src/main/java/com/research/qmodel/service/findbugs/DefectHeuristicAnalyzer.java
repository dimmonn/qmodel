package com.research.qmodel.service.findbugs;

import org.apache.commons.lang3.StringUtils;

public class DefectHeuristicAnalyzer {
  public boolean isPotentiallyDefectivePatch(String patch) {
    if (patch == null || patch.isBlank()) return false;

    String[] lines = patch.split("\n");
    for (String line : lines) {
      line = line.trim();

      if (line.startsWith("@@") || line.startsWith(" ")) continue;

      if (line.startsWith("-")) {
        String removed = line.substring(1).trim().toLowerCase();
        if (StringUtils.containsIgnoreCase(removed,"null")
            || StringUtils.containsIgnoreCase(removed,"assert")
            || StringUtils.containsIgnoreCase(removed,"validate")) {
          return true;
        }
        if (StringUtils.containsIgnoreCase(removed,"if") && StringUtils.containsIgnoreCase(removed,"(") && StringUtils.containsIgnoreCase(removed,")")) {
          return true;
        }
      }

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
          return true;
        }
      }
    }

    return false;
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
