package com.research.qmodel.graph;

import java.util.*;

/**
 * Concrete example: A 10-commit DAG with two branches and one merge.
 * 
 * Visual structure:
 * 
 *     A ---> B ---> C ---> D ---> E (main)
 *            ^
 *            |
 *     F ---> G ---> H ---> M (feature, merged at M)
 *                          |
 *                          v
 *                    I ---> J (after merge)
 * 
 * This demonstrates:
 * - Linear history (A-B-C-D-E)
 * - Branching from B
 * - Feature branch (F-G-H)
 * - Merge commit (M with 2 parents: H and D)
 * - Continued history (I-J after merge)
 */
public class GraphMetricsExample {

    static class ExampleCommit {
        String sha;
        long timestamp;  // seconds since epoch
        String[] parentShas;  // empty array for root
        String label;  // for display

        ExampleCommit(String sha, long timestamp, String label, String... parentShas) {
            this.sha = sha;
            this.timestamp = timestamp;
            this.parentShas = parentShas;
            this.label = label;
        }

        int parentCount() {
            return parentShas.length;
        }

        boolean isMerge() {
            return parentCount() > 1;
        }
    }

    static class MetricsResult {
        String sha;
        String label;
        int inDegree;
        int outDegree;
        int minDepth;
        int maxDepth;
        boolean isMerge;
        int distToBranchStart;
        int branchCount;
        int upstreamUnique;
        int daysSinceLastMerge;

        @Override
        public String toString() {
            return String.format(
                    "%-6s | in:%-2d out:%-2d | depth:%-2d-%-2d | dist:%-2d | branchCnt:%-2d | upstream:%-2d | daysSinceMerge:%-3d | merge:%s",
                    label,
                    inDegree, outDegree,
                    minDepth, maxDepth,
                    distToBranchStart,
                    branchCount,
                    upstreamUnique,
                    daysSinceLastMerge,
                    isMerge ? "Y" : "N"
            );
        }
    }

    public static void main(String[] args) {
        // Define the example DAG
        long now = System.currentTimeMillis() / 1000;  // current time in seconds

        ExampleCommit[] commits = {
                // Main branch (linear)
                new ExampleCommit("aaa111", now - 10000, "A"),
                new ExampleCommit("bbb222", now - 9000, "B", "aaa111"),
                new ExampleCommit("ccc333", now - 8000, "C", "bbb222"),
                new ExampleCommit("ddd444", now - 7000, "D", "ccc333"),
                new ExampleCommit("eee555", now - 6000, "E", "ddd444"),

                // Feature branch (from B)
                new ExampleCommit("fff666", now - 8500, "F", "bbb222"),
                new ExampleCommit("ggg777", now - 7500, "G", "fff666"),
                new ExampleCommit("hhh888", now - 6500, "H", "ggg777"),

                // Merge commit (H + D) and after
                new ExampleCommit("mmm999", now - 5500, "M", "hhh888", "ddd444"),  // MERGE
                new ExampleCommit("iii000", now - 4500, "I", "mmm999"),
                new ExampleCommit("jjj111", now - 3500, "J", "iii000"),
        };

        // Build the graph manually
        Map<String, ExampleCommit> commitMap = new HashMap<>();
        Map<String, Integer> inDegree = new HashMap<>();
        Map<String, Integer> outDegree = new HashMap<>();
        Map<String, Set<String>> parents = new HashMap<>();
        Map<String, Set<String>> children = new HashMap<>();

        for (ExampleCommit c : commits) {
            commitMap.put(c.sha, c);
            inDegree.put(c.sha, 0);
            outDegree.put(c.sha, 0);
            parents.put(c.sha, new HashSet<>());
            children.put(c.sha, new HashSet<>());
        }

        // Add edges
        for (ExampleCommit c : commits) {
            for (String p : c.parentShas) {
                parents.get(c.sha).add(p);
                children.get(p).add(c.sha);
                outDegree.put(p, outDegree.get(p) + 1);
                inDegree.put(c.sha, inDegree.get(c.sha) + 1);
            }
        }

        // Compute depths (DP)
        Map<String, Integer> minDepth = new HashMap<>();
        Map<String, Integer> maxDepth = new HashMap<>();

        for (ExampleCommit c : commits) {
            if (c.parentCount() == 0) {
                minDepth.put(c.sha, 0);
                maxDepth.put(c.sha, 0);
            } else {
                int mn = Integer.MAX_VALUE;
                int mx = Integer.MIN_VALUE;
                for (String p : c.parentShas) {
                    int pmn = minDepth.get(p) + 1;
                    int pmx = maxDepth.get(p) + 1;
                    mn = Math.min(mn, pmn);
                    mx = Math.max(mx, pmx);
                }
                minDepth.put(c.sha, mn);
                maxDepth.put(c.sha, mx);
            }
        }

        // Compute first-parent continuity
        Map<String, String> firstParent = new HashMap<>();
        Map<String, Integer> distToBranchStart = new HashMap<>();

        for (ExampleCommit c : commits) {
            if (c.parentCount() == 0) {
                firstParent.put(c.sha, null);
                distToBranchStart.put(c.sha, 0);
            } else if (c.parentCount() == 1) {
                String p = c.parentShas[0];
                firstParent.put(c.sha, p);
                int parentOut = children.get(p).size();
                if (parentOut > 1) {
                    // Parent is a branch point
                    distToBranchStart.put(c.sha, 0);
                } else {
                    distToBranchStart.put(c.sha, distToBranchStart.get(p) + 1);
                }
            } else {
                // Multiple parents: choose first by some rule (we'll use the first in array)
                String chosen = c.parentShas[0];
                firstParent.put(c.sha, chosen);
                // Merge commits reset distance
                distToBranchStart.put(c.sha, 0);
            }
        }

        // Compute days since last merge (simplified)
        Map<String, Integer> daysSinceLastMerge = new HashMap<>();
        Map<String, Long> lastMergeTime = new HashMap<>();

        for (ExampleCommit c : commits) {
            String seg = c.sha;  // simplified segment tracking
            // Find segment start by following first-parent
            for (ExampleCommit candidate : commits) {
                if (candidate.sha.equals(c.sha)) {
                    seg = c.sha;
                    break;
                }
            }

            long lastMerge = lastMergeTime.getOrDefault(seg, Long.MIN_VALUE);
            if (lastMerge == Long.MIN_VALUE) {
                daysSinceLastMerge.put(c.sha, 0);
            } else {
                long deltaSec = Math.max(0, c.timestamp - lastMerge);
                int days = (int) (deltaSec / (24 * 60 * 60));
                daysSinceLastMerge.put(c.sha, days);
            }

            if (c.isMerge()) {
                lastMergeTime.put(seg, c.timestamp);
            }
        }

        // Compute branch count (time-aware, number of branches that can reach this commit)
        // For simplicity: count heads reachable from this commit
        Map<String, Integer> branchCount = new HashMap<>();
        String[] heads = {"eee555", "jjj111"};  // E and J are the head commits
        for (ExampleCommit c : commits) {
            int cnt = 0;
            for (String head : heads) {
                if (canReach(c.sha, head, parents)) {
                    cnt++;
                }
            }
            branchCount.put(c.sha, cnt);
        }

        // Compute upstream unique (simplified)
        Map<String, Integer> upstreamUnique = new HashMap<>();
        for (ExampleCommit c : commits) {
            if (c.isMerge()) {
                upstreamUnique.put(c.sha, c.parentCount() - 1);  // number of non-first-parent sources
            } else {
                String p = firstParent.get(c.sha);
                int val = (p == null) ? 0 : upstreamUnique.getOrDefault(p, 0);
                upstreamUnique.put(c.sha, val);
            }
        }

        // Print results
        System.out.println("========== EXAMPLE DAG STRUCTURE ==========");
        System.out.println();
        System.out.println("      aaa111 (A) ---+");
        System.out.println("         |          |");
        System.out.println("         v          |");
        System.out.println("      bbb222 (B)    |");
        System.out.println("       /  \\         |");
        System.out.println("      /    \\        |");
        System.out.println("     |      |       |");
        System.out.println("  fff666    ccc333  |");
        System.out.println("  (F)       (C)     |");
        System.out.println("    |         |     |");
        System.out.println("    v         v     |");
        System.out.println("  ggg777    ddd444  |");
        System.out.println("  (G)       (D)     |");
        System.out.println("    |         |     |");
        System.out.println("    v         v     |");
        System.out.println("  hhh888    eee555  |");
        System.out.println("  (H)       (E) <---+");
        System.out.println("    \\       /");
        System.out.println("     \\     /");
        System.out.println("      mmm999 (M) [MERGE]");
        System.out.println("         |");
        System.out.println("         v");
        System.out.println("      iii000 (I)");
        System.out.println("         |");
        System.out.println("         v");
        System.out.println("      jjj111 (J)");
        System.out.println();

        System.out.println("========== COMPUTED METRICS PER COMMIT ==========");
        System.out.println();
        System.out.println("Columns: label | in/out | depth (min-max) | dist to branch start | branch count | upstream | days since merge | is merge");
        System.out.println("─".repeat(145));

        for (ExampleCommit c : commits) {
            MetricsResult res = new MetricsResult();
            res.sha = c.sha;
            res.label = c.label;
            res.inDegree = inDegree.get(c.sha);
            res.outDegree = outDegree.get(c.sha);
            res.minDepth = minDepth.get(c.sha);
            res.maxDepth = maxDepth.get(c.sha);
            res.isMerge = c.isMerge();
            res.distToBranchStart = distToBranchStart.get(c.sha);
            res.branchCount = branchCount.get(c.sha);
            res.upstreamUnique = upstreamUnique.get(c.sha);
            res.daysSinceLastMerge = daysSinceLastMerge.get(c.sha);

            System.out.println(res);
        }

        System.out.println();
        System.out.println("========== EXPLANATION ==========");
        System.out.println();
        System.out.println("inDegree:           Number of parents (0=root, 1=normal, 2+=merge)");
        System.out.println("outDegree:          Number of children");
        System.out.println("minDepth:           Shortest path to any root commit");
        System.out.println("maxDepth:           Longest path to any root commit");
        System.out.println("distToBranchStart:  Distance along first-parent chain since last branch point");
        System.out.println("branchCount:        Number of branch heads that can reach this commit");
        System.out.println("upstreamUnique:     Distinct non-first-parent sources merged before");
        System.out.println("daysSinceLastMerge: Days elapsed since last merge in first-parent segment");
        System.out.println("isMerge:            Y if this commit has multiple parents");
        System.out.println();
        System.out.println("Key observations:");
        System.out.println("- A, B are branch points (outDegree > 1)");
        System.out.println("- M is a merge commit (inDegree = 2)");
        System.out.println("- E and J are leaf commits (outDegree = 0)");
        System.out.println("- All commits reachable from J, only some from E (F,G,H,M,I,J can reach J)");
        System.out.println("- distToBranchStart resets at branch points and merges");
        System.out.println("- Merges increment upstreamUnique for downstream commits");
    }

    static boolean canReach(String from, String to, Map<String, Set<String>> parents) {
        if (from.equals(to)) return true;
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new LinkedList<>(parents.get(to));
        while (!queue.isEmpty()) {
            String cur = queue.poll();
            if (visited.contains(cur)) continue;
            visited.add(cur);
            if (cur.equals(from)) return true;
            queue.addAll(parents.get(cur));
        }
        return false;
    }
}

