package com.research.qmodel.annotations;

import java.util.HashSet;
import java.util.Set;

public interface ChangePatchProcessor {

  default Set<Integer> getChangedLineNumbers(String patch) {
    Set<Integer> changedLines = new HashSet<>();

    String[] lines = patch.split("\n");
    int originalLineStart = 0;
    int newLineStart = 0;
    for (String line : lines) {
      if (line.startsWith("@@")) {
        String[] parts = line.split(" ");
        String originalRange = parts[1];
        String newRange = parts[2];

        String[] originalParts = originalRange.split(",");
        originalLineStart = Integer.parseInt(originalParts[0].substring(1));

        String[] newParts = newRange.split(",");
        newLineStart = Integer.parseInt(newParts[0].substring(1));
      } else if (line.startsWith("-")) {
        int removedLineNumber = originalLineStart++;
        changedLines.add(removedLineNumber);
      } else if (line.startsWith("+")) {
        int addedLineNumber = newLineStart++;
        changedLines.add(addedLineNumber);
      } else {
        originalLineStart++;
        newLineStart++;
      }
    }

    return changedLines;
  }
}
