package com.research.qmodel.service.findbugs;

public class DefectHeuristicAnalyzer {
  public boolean isPotentialDefect(String patch) {
    if (patch == null || patch.isEmpty()) return false;

    String[] lines = patch.split("\n");
    int riskScore = 0;

    for (String line : lines) {
      // Only examine removed lines (original code)
      if (line.startsWith("-")) {
        String stripped = line.substring(1).trim();

        // Heuristic patterns
        if (stripped.matches(".*\\b(if|else|for|while|switch)\\b.*")) {
          riskScore += 2; // control-flow change
        }
        if (stripped.matches(".*\\btry\\b.*") || stripped.matches(".*\\bcatch\\b.*")) {
          riskScore += 2; // removed exception handling
        }
        if (stripped.matches(".*==\\s*null.*") || stripped.matches(".*!=\\s*null.*")) {
          riskScore += 2; // removed null check
        }
        if (stripped.matches(".*assert.*")) {
          riskScore += 2; // removed assertion
        }
        if (stripped.matches(".*(FIXME|TODO|hack|workaround).*")) {
          riskScore += 3; // developer warning
        }
        if (stripped.matches(".*System\\.out\\.println.*") || stripped.matches(".*logger\\..*")) {
          riskScore -= 1; // likely not defect
        }
        if (stripped.matches(".*(\\{|\\})\\s*")) {
          riskScore += 1; // structural change
        }
      }

      // Detect function signature changes (less accurate)
      if (line.startsWith("-")
          && line.matches(
              "-\\s*(public|private|protected)?\\s*(static)?\\s*\\w+\\s+\\w+\\s*\\(.*\\).*")) {
        riskScore += 2;
      }
    }

    return riskScore >= 3;
  }
  public static void main(String[] args){
    boolean potentialDefect =
        new DefectHeuristicAnalyzer()
            .isPotentialDefect(
                "here is how patch might look like\n"
                    + "@@ -12,4 +12,6 @@ public static void main(String[] args) {\n"
                    + "         SpringApplication.run(TestApplication.class, args);\n"
                    + "     }\n"
                    + " \n"
                    + "+    void aa() {\n"
                    + "+    }\n"
                    + " }");
    System.out.println(potentialDefect);
  }
}
