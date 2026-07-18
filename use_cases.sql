# /*
# Impact of Developer Inactivity on Issue and PR Resolution Times*/
# select
#     i.id as issue_id,
#     i.created_at,
#     i.closed_at,
#     TIMESTAMPDIFF(MINUTE, i.created_at, i.closed_at) as issue_resolution_time,
#     c.author as developer,
#     max(c.commit_date) as last_commit_before_issue
# from
#     project_issue i
# join
#     project_issue_fixing_commits pic on i.id = pic.project_issue_id
# join
#     commit c ON pic.commits_sha = c.sha
# WHERE
#     c.commit_date < i.created_at
# GROUP BY
#     i.id, i.created_at, i.closed_at, c.author;
SELECT 'project' AS table_name, COUNT(*) AS row_count FROM project
UNION ALL
SELECT 'commit' AS table_name, COUNT(*) AS row_count FROM `commit`
UNION ALL
SELECT 'project_issue' AS table_name, COUNT(*) AS row_count FROM project_issue
UNION ALL
SELECT 'project_pull' AS table_name, COUNT(*) AS row_count FROM project_pull
UNION ALL
SELECT 'timeline' AS table_name, COUNT(*) AS row_count FROM timeline
UNION ALL
SELECT 'action' AS table_name, COUNT(*) AS row_count FROM action
UNION ALL
SELECT 'file_changes' AS table_name, COUNT(*) AS row_count FROM file_change
UNION ALL
SELECT 'commit_file_changes' AS table_name, COUNT(*) AS row_count FROM commit_file_changes
UNION ALL
SELECT 'project_issue_fixing_commits' AS table_name, COUNT(*) AS row_count FROM project_issue_fixing_commits
ORDER BY table_name;


SELECT
    'project_issue_fixing_commits' AS relation_table,
    COUNT(*) AS relation_rows,
    COUNT(DISTINCT project_issue_id) AS distinct_left_objects,
    COUNT(DISTINCT fixing_commits_sha) AS distinct_right_objects
FROM project_issue_fixing_commits

UNION ALL

SELECT
    'commit_file_changes' AS relation_table,
    COUNT(*) AS relation_rows,
    COUNT(DISTINCT commit_sha) AS distinct_left_objects,
    COUNT(DISTINCT file_changes_id) AS distinct_right_objects
FROM commit_file_changes;


SELECT
    table_name,
    column_name,
    referenced_table_name,
    referenced_column_name
FROM information_schema.key_column_usage
WHERE table_schema = DATABASE()
  AND referenced_table_name IS NOT NULL
ORDER BY table_name, column_name;


SELECT 'project' AS object_type, COUNT(*) AS count_value FROM project
UNION ALL
SELECT 'commit', COUNT(*) FROM `commit`
UNION ALL
SELECT 'project_issue', COUNT(*) FROM project_issue
UNION ALL
SELECT 'project_pull', COUNT(*) FROM project_pull
UNION ALL
SELECT 'timeline', COUNT(*) FROM timeline
UNION ALL
SELECT 'action', COUNT(*) FROM action
UNION ALL
SELECT 'file_changes', COUNT(*) FROM file_change
UNION ALL
SELECT 'commit_file_changes', COUNT(*) FROM commit_file_changes
UNION ALL
SELECT 'project_issue_fixing_commits', COUNT(*) FROM project_issue_fixing_commits
UNION ALL
SELECT 'foreign_keys', COUNT(*) FROM information_schema.key_column_usage
WHERE table_schema = DATABASE()
  AND referenced_table_name IS NOT NULL;



SELECT table_name, table_rows
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN (
                     'project',
                     'graph',
                     'commit',
                     'project_issue',
                     'project_pull',
                     'timeline',
                     'action',
                     'file_changes',
                     'commit_file_changes',
                     'project_issue_fixing_commits'
    )
ORDER BY table_name;



select * from commit where sha = '00067f1d2e0a46de41ff8d27f1432c30b79e60b6';

WITH
    project_base AS (
        SELECT
            p.project_owner,
            p.project_name
        FROM project p
    ),

    commit_stats AS (
        SELECT
            c.project_owner,
            c.project_name,
            COUNT(DISTINCT c.sha) AS commits_total,
            COUNT(DISTINCT CASE
                               WHEN c.commit_date IS NOT NULL THEN c.sha
                END) AS commits_with_timestamp,
            COUNT(DISTINCT CASE
                               WHEN a.id IS NOT NULL THEN c.sha
                END) AS commits_with_ci,
            COUNT(DISTINCT a.id) AS ci_runs_total
        FROM commit c
                 LEFT JOIN action a
                           ON a.commit_sha = c.sha
        GROUP BY c.project_owner, c.project_name
    ),

    issue_stats AS (
        SELECT
            pi.project_owner,
            pi.project_name,
            COUNT(DISTINCT pi.id, pi.project_name, pi.project_owner) AS issues_total,
            COUNT(DISTINCT CASE
                               WHEN pi.state = 'closed'
                                   THEN pi.id
                END) AS issues_closed,
            COUNT(DISTINCT CASE
                               WHEN pi.created_at IS NOT NULL
                                   AND (pi.closed_at IS NOT NULL OR pi.state <> 'closed')
                                   THEN pi.id
                END) AS issues_with_usable_timestamps,
            COUNT(DISTINCT CASE
                               WHEN pi.fixpr_id IS NOT NULL
                                   OR pi.fix_pr IS NOT NULL
                                   OR pip.project_pull_id IS NOT NULL
                                   OR ppi.project_pull_id IS NOT NULL
                                   OR ppis.project_pull_id IS NOT NULL
                                   THEN pi.id
                END) AS issues_with_pr_link
        FROM project_issue pi
                 LEFT JOIN project_issue_project_pull pip
                           ON  pip.project_issue_id = pi.id
                               AND pip.project_issue_project_name = pi.project_name
                               AND pip.project_issue_project_owner = pi.project_owner
                 LEFT JOIN project_pull_project_issue ppi
                           ON  ppi.project_issue_id = pi.id
                               AND ppi.project_issue_project_name = pi.project_name
                               AND ppi.project_issue_project_owner = pi.project_owner
                 LEFT JOIN project_pull_project_issues ppis
                           ON  ppis.project_issues_id = pi.id
                               AND ppis.project_issues_project_name = pi.project_name
                               AND ppis.project_issues_project_owner = pi.project_owner
        GROUP BY pi.project_owner, pi.project_name
    ),

    pull_stats AS (
        SELECT
            pp.project_owner,
            pp.project_name,
            COUNT(DISTINCT pp.id, pp.project_name, pp.project_owner) AS prs_total,
            COUNT(DISTINCT CASE
                               WHEN pp.state = 'closed'
                                   THEN pp.id
                END) AS prs_closed,
            COUNT(DISTINCT CASE
                               WHEN pp.created_at IS NOT NULL
                                   AND (pp.merged_at IS NOT NULL OR pp.closed_at IS NOT NULL OR pp.state <> 'closed')
                                   THEN pp.id
                END) AS prs_with_usable_timestamps,
            COUNT(DISTINCT CASE
                               WHEN ppc.commits_sha IS NOT NULL
                                   THEN pp.id
                END) AS prs_with_commits
        FROM project_pull pp
                 LEFT JOIN project_pull_commits ppc
                           ON  ppc.project_pull_id = pp.id
                               AND ppc.project_pull_project_name = pp.project_name
                               AND ppc.project_pull_project_owner = pp.project_owner
        GROUP BY pp.project_owner, pp.project_name
    ),

    file_change_stats AS (
        SELECT
            c.project_owner,
            c.project_name,
            COUNT(DISTINCT fc.id) AS file_changes_total,
            COUNT(DISTINCT CASE
                               WHEN fc.patch IS NOT NULL AND fc.patch <> ''
                                   THEN fc.id
                END) AS file_changes_with_patch,
            COUNT(DISTINCT fcl.file_change_id) AS file_changes_with_changed_lines
        FROM commit c
                 JOIN commit_file_changes cfc
                      ON cfc.commit_sha = c.sha
                 JOIN file_change fc
                      ON fc.id = cfc.file_changes_id
                 LEFT JOIN file_change_changed_lines fcl
                           ON fcl.file_change_id = fc.id
        GROUP BY c.project_owner, c.project_name
    ),

    timeline_stats AS (
        SELECT
            project_owner,
            project_name,
            COUNT(DISTINCT timeline_id) AS timelines_total,
            COUNT(DISTINCT CASE
                               WHEN created_at IS NOT NULL THEN timeline_id
                END) AS timelines_with_timestamp
        FROM (
                 SELECT
                     pi.project_owner,
                     pi.project_name,
                     t.id AS timeline_id,
                     t.created_at
                 FROM timeline t
                          JOIN project_issue pi
                               ON  pi.id = t.project_issue_id
                                   AND pi.project_name = t.project_issue_project_name
                                   AND pi.project_owner = t.project_issue_project_owner

                 UNION

                 SELECT
                     pp.project_owner,
                     pp.project_name,
                     t.id AS timeline_id,
                     t.created_at
                 FROM timeline t
                          JOIN project_pull pp
                               ON  pp.id = t.project_pull_id
                                   AND pp.project_name = t.project_pull_project_name
                                   AND pp.project_owner = t.project_pull_project_owner
             ) x
        GROUP BY project_owner, project_name
    ),

    reaction_stats AS (
        SELECT
            project_owner,
            project_name,
            COUNT(DISTINCT reaction_id) AS reactions_total
        FROM (
                 SELECT
                     pi.project_owner,
                     pi.project_name,
                     pi.reaction_id
                 FROM project_issue pi
                 WHERE pi.reaction_id IS NOT NULL

                 UNION

                 SELECT
                     pp.project_owner,
                     pp.project_name,
                     pp.reaction_id
                 FROM project_pull pp
                 WHERE pp.reaction_id IS NOT NULL
             ) x
        GROUP BY project_owner, project_name
    ),

    defect_link_stats AS (
        SELECT
            pi.project_owner,
            pi.project_name,
            COUNT(DISTINCT pifc.project_issue_id, pifc.project_issue_project_name, pifc.project_issue_project_owner)
                AS issues_with_fixing_commits,
            COUNT(DISTINCT pifc.fixing_commits_sha)
                AS fixing_commits_total,
            COUNT(DISTINCT pibic.project_issue_id, pibic.project_issue_project_name, pibic.project_issue_project_owner)
                AS issues_with_candidate_bics,
            COUNT(DISTINCT pibic.bug_introducing_commits_sha)
                AS candidate_bic_commits_total,
            COUNT(DISTINCT
                  CONCAT(
                          pibic.project_issue_id, ':',
                          pibic.project_issue_project_name, ':',
                          pibic.project_issue_project_owner, ':',
                          pibic.bug_introducing_commits_sha
                  )
            ) AS issue_candidate_bic_links_total
        FROM project_issue pi
                 LEFT JOIN project_issue_fixing_commits pifc
                           ON  pifc.project_issue_id = pi.id
                               AND pifc.project_issue_project_name = pi.project_name
                               AND pifc.project_issue_project_owner = pi.project_owner
                 LEFT JOIN project_issue_bug_introducing_commits pibic
                           ON  pibic.project_issue_id = pi.id
                               AND pibic.project_issue_project_name = pi.project_name
                               AND pibic.project_issue_project_owner = pi.project_owner
        GROUP BY pi.project_owner, pi.project_name
    )

SELECT
    pb.project_owner,
    pb.project_name,

    /* Artifact counts */
    COALESCE(cs.commits_total, 0) AS commits_total,
    COALESCE(iss.issues_total, 0) AS issues_total,
    COALESCE(ps.prs_total, 0) AS pull_requests_total,
    COALESCE(fcs.file_changes_total, 0) AS file_changes_total,
    COALESCE(ts.timelines_total, 0) AS timelines_total,
    COALESCE(rs.reactions_total, 0) AS reactions_total,
    COALESCE(cs.ci_runs_total, 0) AS ci_check_runs_total,

    /* PR--commit coverage */
    COALESCE(ps.prs_with_commits, 0) AS prs_with_commits,
    ROUND(
            100.0 * COALESCE(ps.prs_with_commits, 0) / NULLIF(ps.prs_total, 0),
            2
    ) AS pr_commit_coverage_percent,

    /* Issue--PR link coverage */
    COALESCE(iss.issues_closed, 0) AS closed_issues_total,
    COALESCE(iss.issues_with_pr_link, 0) AS issues_with_pr_link,
    ROUND(
            100.0 * COALESCE(iss.issues_with_pr_link, 0) / NULLIF(iss.issues_closed, 0),
            2
    ) AS closed_issue_pr_link_coverage_percent,

    /* CI coverage */
    COALESCE(cs.commits_with_ci, 0) AS commits_with_ci,
    ROUND(
            100.0 * COALESCE(cs.commits_with_ci, 0) / NULLIF(cs.commits_total, 0),
            2
    ) AS commit_ci_coverage_percent,

    /* Timestamp completeness */
    COALESCE(cs.commits_with_timestamp, 0) AS commits_with_timestamp,
    ROUND(
            100.0 * COALESCE(cs.commits_with_timestamp, 0) / NULLIF(cs.commits_total, 0),
            2
    ) AS commit_timestamp_completeness_percent,

    COALESCE(iss.issues_with_usable_timestamps, 0) AS issues_with_usable_timestamps,
    ROUND(
            100.0 * COALESCE(iss.issues_with_usable_timestamps, 0) / NULLIF(iss.issues_total, 0),
            2
    ) AS issue_timestamp_completeness_percent,

    COALESCE(ps.prs_with_usable_timestamps, 0) AS prs_with_usable_timestamps,
    ROUND(
            100.0 * COALESCE(ps.prs_with_usable_timestamps, 0) / NULLIF(ps.prs_total, 0),
            2
    ) AS pr_timestamp_completeness_percent,

    COALESCE(ts.timelines_with_timestamp, 0) AS timelines_with_timestamp,
    ROUND(
            100.0 * COALESCE(ts.timelines_with_timestamp, 0) / NULLIF(ts.timelines_total, 0),
            2
    ) AS timeline_timestamp_completeness_percent,

    /* File-change completeness */
    COALESCE(fcs.file_changes_with_patch, 0) AS file_changes_with_patch,
    ROUND(
            100.0 * COALESCE(fcs.file_changes_with_patch, 0) / NULLIF(fcs.file_changes_total, 0),
            2
    ) AS file_change_patch_coverage_percent,

    COALESCE(fcs.file_changes_with_changed_lines, 0) AS file_changes_with_changed_lines,
    ROUND(
            100.0 * COALESCE(fcs.file_changes_with_changed_lines, 0) / NULLIF(fcs.file_changes_total, 0),
            2
    ) AS changed_line_coverage_percent,

    /* Defect-linking statistics */
    COALESCE(dls.issues_with_fixing_commits, 0) AS issues_with_fixing_commits,
    COALESCE(dls.fixing_commits_total, 0) AS fixing_commits_total,
    COALESCE(dls.issues_with_candidate_bics, 0) AS issues_with_candidate_bics,
    COALESCE(dls.candidate_bic_commits_total, 0) AS candidate_bug_introducing_commits_total,
    COALESCE(dls.issue_candidate_bic_links_total, 0) AS issue_candidate_bic_links_total

FROM project_base pb
         LEFT JOIN commit_stats cs
                   ON cs.project_owner = pb.project_owner
                       AND cs.project_name = pb.project_name
         LEFT JOIN issue_stats iss
                   ON iss.project_owner = pb.project_owner
                       AND iss.project_name = pb.project_name
         LEFT JOIN pull_stats ps
                   ON ps.project_owner = pb.project_owner
                       AND ps.project_name = pb.project_name
         LEFT JOIN file_change_stats fcs
                   ON fcs.project_owner = pb.project_owner
                       AND fcs.project_name = pb.project_name
         LEFT JOIN timeline_stats ts
                   ON ts.project_owner = pb.project_owner
                       AND ts.project_name = pb.project_name
         LEFT JOIN reaction_stats rs
                   ON rs.project_owner = pb.project_owner
                       AND rs.project_name = pb.project_name
         LEFT JOIN defect_link_stats dls
                   ON dls.project_owner = pb.project_owner
                       AND dls.project_name = pb.project_name
WHERE pb.project_owner = 'ansible'
  OR pb.project_owner = 'facebook'

ORDER BY pb.project_owner, pb.project_name;



#PAPER SQL
WITH
    commit_stats AS (
        SELECT
            project_owner,
            project_name,
            COUNT(DISTINCT sha) AS commits_total,
            COUNT(DISTINCT CASE WHEN commit_date IS NOT NULL THEN sha END) AS commits_with_timestamp
        FROM commit
        GROUP BY project_owner, project_name
    ),

    ci_stats AS (
        SELECT
            c.project_owner,
            c.project_name,
            COUNT(DISTINCT a.id) AS ci_check_runs_total,
            COUNT(DISTINCT CASE WHEN a.id IS NOT NULL THEN c.sha END) AS commits_with_ci,
            ROUND(
                    COUNT(DISTINCT a.id) / NULLIF(COUNT(DISTINCT CASE WHEN a.id IS NOT NULL THEN c.sha END), 0),
                    2
            ) AS check_runs_per_ci_commit
        FROM commit c
                 LEFT JOIN action a
                           ON a.commit_sha = c.sha
        GROUP BY c.project_owner, c.project_name
    ),

    issue_stats AS (
        SELECT
            pi.project_owner,
            pi.project_name,
            COUNT(DISTINCT pi.id) AS issues_total,
            COUNT(DISTINCT CASE WHEN pi.state = 'closed' THEN pi.id END) AS closed_issues_total,
            COUNT(DISTINCT CASE
                               WHEN pi.fixpr_id IS NOT NULL
                                   OR pi.fix_pr IS NOT NULL
                                   OR pip.project_pull_id IS NOT NULL
                                   OR ppi.project_pull_id IS NOT NULL
                                   OR ppis.project_pull_id IS NOT NULL
                                   THEN pi.id
                END) AS issues_with_pr_link
        FROM project_issue pi
                 LEFT JOIN project_issue_project_pull pip
                           ON pip.project_issue_id = pi.id
                               AND pip.project_issue_project_name = pi.project_name
                               AND pip.project_issue_project_owner = pi.project_owner
                 LEFT JOIN project_pull_project_issue ppi
                           ON ppi.project_issue_id = pi.id
                               AND ppi.project_issue_project_name = pi.project_name
                               AND ppi.project_issue_project_owner = pi.project_owner
                 LEFT JOIN project_pull_project_issues ppis
                           ON ppis.project_issues_id = pi.id
                               AND ppis.project_issues_project_name = pi.project_name
                               AND ppis.project_issues_project_owner = pi.project_owner
        GROUP BY pi.project_owner, pi.project_name
    ),

    pull_stats AS (
        SELECT
            pp.project_owner,
            pp.project_name,
            COUNT(DISTINCT pp.id) AS pull_requests_total,
            COUNT(DISTINCT CASE WHEN pp.state = 'closed' THEN pp.id END) AS closed_prs_total,
            COUNT(DISTINCT CASE WHEN ppc.commits_sha IS NOT NULL THEN pp.id END) AS prs_with_commits
        FROM project_pull pp
                 LEFT JOIN project_pull_commits ppc
                           ON ppc.project_pull_id = pp.id
                               AND ppc.project_pull_project_name = pp.project_name
                               AND ppc.project_pull_project_owner = pp.project_owner
        GROUP BY pp.project_owner, pp.project_name
    ),

    file_change_stats AS (
        SELECT
            c.project_owner,
            c.project_name,
            COUNT(DISTINCT fc.id) AS file_changes_total,
            COUNT(DISTINCT fcl.file_change_id) AS file_changes_with_changed_lines
        FROM commit c
                 JOIN commit_file_changes cfc
                      ON cfc.commit_sha = c.sha
                 JOIN file_change fc
                      ON fc.id = cfc.file_changes_id
                 LEFT JOIN file_change_changed_lines fcl
                           ON fcl.file_change_id = fc.id
        GROUP BY c.project_owner, c.project_name
    ),

    timeline_stats AS (
        SELECT
            project_owner,
            project_name,
            COUNT(DISTINCT timeline_id) AS timelines_total
        FROM (
                 SELECT
                     pi.project_owner,
                     pi.project_name,
                     t.id AS timeline_id
                 FROM timeline t
                          JOIN project_issue pi
                               ON pi.id = t.project_issue_id
                                   AND pi.project_name = t.project_issue_project_name
                                   AND pi.project_owner = t.project_issue_project_owner

                 UNION

                 SELECT
                     pp.project_owner,
                     pp.project_name,
                     t.id AS timeline_id
                 FROM timeline t
                          JOIN project_pull pp
                               ON pp.id = t.project_pull_id
                                   AND pp.project_name = t.project_pull_project_name
                                   AND pp.project_owner = t.project_pull_project_owner
             ) x
        GROUP BY project_owner, project_name
    ),

    reaction_stats AS (
        SELECT
            project_owner,
            project_name,
            COUNT(DISTINCT reaction_id) AS reactions_total
        FROM (
                 SELECT project_owner, project_name, reaction_id
                 FROM project_issue
                 WHERE reaction_id IS NOT NULL

                 UNION

                 SELECT project_owner, project_name, reaction_id
                 FROM project_pull
                 WHERE reaction_id IS NOT NULL
             ) x
        GROUP BY project_owner, project_name
    ),

    defect_stats AS (
        SELECT
            pi.project_owner,
            pi.project_name,
            COUNT(DISTINCT pifc.project_issue_id) AS issues_with_fixing_commits,
            COUNT(DISTINCT pifc.fixing_commits_sha) AS fixing_commits_total,
            COUNT(DISTINCT pibic.project_issue_id) AS issues_with_candidate_bics,
            COUNT(DISTINCT pibic.bug_introducing_commits_sha) AS candidate_bic_commits_total,
            COUNT(DISTINCT CONCAT(
                    pibic.project_issue_id, ':',
                    pibic.project_issue_project_name, ':',
                    pibic.project_issue_project_owner, ':',
                    pibic.bug_introducing_commits_sha
                           )) AS issue_candidate_bic_links_total
        FROM project_issue pi
                 LEFT JOIN project_issue_fixing_commits pifc
                           ON pifc.project_issue_id = pi.id
                               AND pifc.project_issue_project_name = pi.project_name
                               AND pifc.project_issue_project_owner = pi.project_owner
                 LEFT JOIN project_issue_bug_introducing_commits pibic
                           ON pibic.project_issue_id = pi.id
                               AND pibic.project_issue_project_name = pi.project_name
                               AND pibic.project_issue_project_owner = pi.project_owner
        GROUP BY pi.project_owner, pi.project_name
    )

SELECT
    p.project_owner,
    p.project_name,

    cs.commits_total,
    iss.issues_total,
    ps.pull_requests_total,
    fcs.file_changes_total,
    ts.timelines_total,
    rs.reactions_total,
    cis.ci_check_runs_total,

    ps.prs_with_commits,
    ROUND(100.0 * ps.prs_with_commits / NULLIF(ps.pull_requests_total, 0), 2)
        AS pr_commit_coverage_percent,

    iss.closed_issues_total,
    iss.issues_with_pr_link,
    ROUND(100.0 * iss.issues_with_pr_link / NULLIF(iss.closed_issues_total, 0), 2)
        AS closed_issue_pr_link_coverage_percent,

    cis.commits_with_ci,
    ROUND(100.0 * cis.commits_with_ci / NULLIF(cs.commits_total, 0), 2)
        AS commit_ci_coverage_percent,
    cis.check_runs_per_ci_commit,

    fcs.file_changes_with_changed_lines,
    ROUND(100.0 * fcs.file_changes_with_changed_lines / NULLIF(fcs.file_changes_total, 0), 2)
        AS changed_line_coverage_percent,

    ds.issues_with_fixing_commits,
    ds.fixing_commits_total,
    ds.issues_with_candidate_bics,
    ds.candidate_bic_commits_total,
    ds.issue_candidate_bic_links_total

FROM project p
         LEFT JOIN commit_stats cs
                   ON cs.project_owner = p.project_owner
                       AND cs.project_name = p.project_name
         LEFT JOIN ci_stats cis
                   ON cis.project_owner = p.project_owner
                       AND cis.project_name = p.project_name
         LEFT JOIN issue_stats iss
                   ON iss.project_owner = p.project_owner
                       AND iss.project_name = p.project_name
         LEFT JOIN pull_stats ps
                   ON ps.project_owner = p.project_owner
                       AND ps.project_name = p.project_name
         LEFT JOIN file_change_stats fcs
                   ON fcs.project_owner = p.project_owner
                       AND fcs.project_name = p.project_name
         LEFT JOIN timeline_stats ts
                   ON ts.project_owner = p.project_owner
                       AND ts.project_name = p.project_name
         LEFT JOIN reaction_stats rs
                   ON rs.project_owner = p.project_owner
                       AND rs.project_name = p.project_name
         LEFT JOIN defect_stats ds
                   ON ds.project_owner = p.project_owner
                       AND ds.project_name = p.project_name
WHERE p.project_owner IN ('ansible', 'facebook')
ORDER BY p.project_owner, p.project_name;


select count(*) from project_issue where fix_pr !=0 and project_project_owner='facebook';







SELECT
    pi.project_owner,
    pi.project_name,

    COUNT(DISTINCT pi.id) AS issues_total,

    COUNT(DISTINCT CASE
                       WHEN pi.state = 'closed'
                           THEN pi.id
        END) AS closed_issues_total,

    COUNT(DISTINCT CASE
                       WHEN pi.state = 'closed'
                           AND pi.fix_pr IS NOT NULL
                           AND pi.fix_pr <> 0
                           THEN pi.id
        END) AS closed_issues_with_fixing_pr,

    ROUND(
            100.0 * COUNT(DISTINCT CASE
                                       WHEN pi.state = 'closed'
                                           AND pi.fix_pr IS NOT NULL
                                           AND pi.fix_pr <> 0
                                           THEN pi.id
                END)
                / NULLIF(COUNT(DISTINCT CASE
                                            WHEN pi.state = 'closed'
                                                THEN pi.id
                END), 0),
            2
    ) AS closed_issue_fixing_pr_coverage_percent

FROM project_issue pi
WHERE pi.project_owner IN ('ansible', 'facebook')
GROUP BY
    pi.project_owner,
    pi.project_name
ORDER BY
    pi.project_owner,
    pi.project_name;




WITH
    project_base AS (
        SELECT
            p.project_owner,
            p.project_name
        FROM project p
    ),

    commit_stats AS (
        SELECT
            c.project_owner,
            c.project_name,
            COUNT(DISTINCT c.sha) AS commits_total,
            COUNT(DISTINCT CASE
                               WHEN c.commit_date IS NOT NULL THEN c.sha
                END) AS commits_with_timestamp,
            COUNT(DISTINCT CASE
                               WHEN a.id IS NOT NULL THEN c.sha
                END) AS commits_with_ci,
            COUNT(DISTINCT a.id) AS ci_runs_total
        FROM commit c
                 LEFT JOIN action a
                           ON a.commit_sha = c.sha
        GROUP BY c.project_owner, c.project_name
    ),

    issue_stats AS (
        SELECT
            pi.project_owner,
            pi.project_name,

            COUNT(DISTINCT pi.id, pi.project_name, pi.project_owner) AS issues_total,

            COUNT(DISTINCT CASE
                               WHEN pi.state = 'closed'
                                   THEN pi.id
                END) AS issues_closed,

            COUNT(DISTINCT CASE
                               WHEN pi.created_at IS NOT NULL
                                   AND (pi.closed_at IS NOT NULL OR pi.state <> 'closed')
                                   THEN pi.id
                END) AS issues_with_usable_timestamps,

            COUNT(DISTINCT CASE
                               WHEN pi.state = 'closed'
                                   AND (
                                        (pi.fix_pr IS NOT NULL AND pi.fix_pr <> 0)
                                            OR (pi.fixpr_id IS NOT NULL AND pi.fixpr_id <> 0)
                                            OR pip.project_pull_id IS NOT NULL
                                            OR ppi.project_pull_id IS NOT NULL
                                            OR ppis.project_pull_id IS NOT NULL
                                        )
                                   THEN pi.id
                END) AS closed_issues_with_pr_link

        FROM project_issue pi
                 LEFT JOIN project_issue_project_pull pip
                           ON  pip.project_issue_id = pi.id
                               AND pip.project_issue_project_name = pi.project_name
                               AND pip.project_issue_project_owner = pi.project_owner

                 LEFT JOIN project_pull_project_issue ppi
                           ON  ppi.project_issue_id = pi.id
                               AND ppi.project_issue_project_name = pi.project_name
                               AND ppi.project_issue_project_owner = pi.project_owner

                 LEFT JOIN project_pull_project_issues ppis
                           ON  ppis.project_issues_id = pi.id
                               AND ppis.project_issues_project_name = pi.project_name
                               AND ppis.project_issues_project_owner = pi.project_owner

        GROUP BY pi.project_owner, pi.project_name
    ),

    pull_stats AS (
        SELECT
            pp.project_owner,
            pp.project_name,
            COUNT(DISTINCT pp.id, pp.project_name, pp.project_owner) AS prs_total,
            COUNT(DISTINCT CASE
                               WHEN pp.state = 'closed'
                                   THEN pp.id
                END) AS prs_closed,
            COUNT(DISTINCT CASE
                               WHEN pp.created_at IS NOT NULL
                                   AND (
                                        pp.merged_at IS NOT NULL
                                            OR pp.closed_at IS NOT NULL
                                            OR pp.state <> 'closed'
                                        )
                                   THEN pp.id
                END) AS prs_with_usable_timestamps,
            COUNT(DISTINCT CASE
                               WHEN ppc.commits_sha IS NOT NULL
                                   THEN pp.id
                END) AS prs_with_commits
        FROM project_pull pp
                 LEFT JOIN project_pull_commits ppc
                           ON  ppc.project_pull_id = pp.id
                               AND ppc.project_pull_project_name = pp.project_name
                               AND ppc.project_pull_project_owner = pp.project_owner
        GROUP BY pp.project_owner, pp.project_name
    ),

    file_change_stats AS (
        SELECT
            c.project_owner,
            c.project_name,
            COUNT(DISTINCT fc.id) AS file_changes_total,
            COUNT(DISTINCT CASE
                               WHEN fc.patch IS NOT NULL AND fc.patch <> ''
                                   THEN fc.id
                END) AS file_changes_with_patch,
            COUNT(DISTINCT fcl.file_change_id) AS file_changes_with_changed_lines
        FROM commit c
                 JOIN commit_file_changes cfc
                      ON cfc.commit_sha = c.sha
                 JOIN file_change fc
                      ON fc.id = cfc.file_changes_id
                 LEFT JOIN file_change_changed_lines fcl
                           ON fcl.file_change_id = fc.id
        GROUP BY c.project_owner, c.project_name
    ),

    timeline_stats AS (
        SELECT
            project_owner,
            project_name,
            COUNT(DISTINCT timeline_id) AS timelines_total,
            COUNT(DISTINCT CASE
                               WHEN created_at IS NOT NULL THEN timeline_id
                END) AS timelines_with_timestamp
        FROM (
                 SELECT
                     pi.project_owner,
                     pi.project_name,
                     t.id AS timeline_id,
                     t.created_at
                 FROM timeline t
                          JOIN project_issue pi
                               ON  pi.id = t.project_issue_id
                                   AND pi.project_name = t.project_issue_project_name
                                   AND pi.project_owner = t.project_issue_project_owner

                 UNION

                 SELECT
                     pp.project_owner,
                     pp.project_name,
                     t.id AS timeline_id,
                     t.created_at
                 FROM timeline t
                          JOIN project_pull pp
                               ON  pp.id = t.project_pull_id
                                   AND pp.project_name = t.project_pull_project_name
                                   AND pp.project_owner = t.project_pull_project_owner
             ) x
        GROUP BY project_owner, project_name
    ),

    reaction_stats AS (
        SELECT
            project_owner,
            project_name,
            COUNT(DISTINCT reaction_id) AS reactions_total
        FROM (
                 SELECT
                     pi.project_owner,
                     pi.project_name,
                     pi.reaction_id
                 FROM project_issue pi
                 WHERE pi.reaction_id IS NOT NULL

                 UNION

                 SELECT
                     pp.project_owner,
                     pp.project_name,
                     pp.reaction_id
                 FROM project_pull pp
                 WHERE pp.reaction_id IS NOT NULL
             ) x
        GROUP BY project_owner, project_name
    ),

    defect_link_stats AS (
        SELECT
            pi.project_owner,
            pi.project_name,

            COUNT(DISTINCT
                  pifc.project_issue_id,
                  pifc.project_issue_project_name,
                  pifc.project_issue_project_owner
            ) AS issues_with_fixing_commits,

            COUNT(DISTINCT pifc.fixing_commits_sha) AS fixing_commits_total,

            COUNT(DISTINCT
                  pibic.project_issue_id,
                  pibic.project_issue_project_name,
                  pibic.project_issue_project_owner
            ) AS issues_with_candidate_bics,

            COUNT(DISTINCT pibic.bug_introducing_commits_sha)
                AS candidate_bic_commits_total,

            COUNT(DISTINCT
                  CONCAT(
                          pibic.project_issue_id, ':',
                          pibic.project_issue_project_name, ':',
                          pibic.project_issue_project_owner, ':',
                          pibic.bug_introducing_commits_sha
                  )
            ) AS issue_candidate_bic_links_total

        FROM project_issue pi
                 LEFT JOIN project_issue_fixing_commits pifc
                           ON  pifc.project_issue_id = pi.id
                               AND pifc.project_issue_project_name = pi.project_name
                               AND pifc.project_issue_project_owner = pi.project_owner

                 LEFT JOIN project_issue_bug_introducing_commits pibic
                           ON  pibic.project_issue_id = pi.id
                               AND pibic.project_issue_project_name = pi.project_name
                               AND pibic.project_issue_project_owner = pi.project_owner

        GROUP BY pi.project_owner, pi.project_name
    )

SELECT
    pb.project_owner,
    pb.project_name,

    /* Artifact counts */
    COALESCE(cs.commits_total, 0) AS commits_total,
    COALESCE(iss.issues_total, 0) AS issues_total,
    COALESCE(ps.prs_total, 0) AS pull_requests_total,
    COALESCE(fcs.file_changes_total, 0) AS file_changes_total,
    COALESCE(ts.timelines_total, 0) AS timelines_total,
    COALESCE(rs.reactions_total, 0) AS reactions_total,
    COALESCE(cs.ci_runs_total, 0) AS ci_check_runs_total,

    /* PR--commit coverage */
    COALESCE(ps.prs_with_commits, 0) AS prs_with_commits,
    ROUND(
            100.0 * COALESCE(ps.prs_with_commits, 0) / NULLIF(ps.prs_total, 0),
            2
    ) AS pr_commit_coverage_percent,

    /* Closed issue--PR link coverage */
    COALESCE(iss.issues_closed, 0) AS closed_issues_total,
    COALESCE(iss.closed_issues_with_pr_link, 0) AS closed_issues_with_pr_link,
    ROUND(
            100.0 * COALESCE(iss.closed_issues_with_pr_link, 0) / NULLIF(iss.issues_closed, 0),
            2
    ) AS closed_issue_pr_link_coverage_percent,

    /* CI coverage */
    COALESCE(cs.commits_with_ci, 0) AS commits_with_ci,
    ROUND(
            100.0 * COALESCE(cs.commits_with_ci, 0) / NULLIF(cs.commits_total, 0),
            2
    ) AS commit_ci_coverage_percent,

    CASE
        WHEN COALESCE(cs.commits_with_ci, 0) > 0
            THEN ROUND(1.0 * COALESCE(cs.ci_runs_total, 0) / cs.commits_with_ci, 2)
        ELSE NULL
        END AS ci_runs_per_ci_covered_commit,

    /* Timestamp completeness */
    COALESCE(cs.commits_with_timestamp, 0) AS commits_with_timestamp,
    ROUND(
            100.0 * COALESCE(cs.commits_with_timestamp, 0) / NULLIF(cs.commits_total, 0),
            2
    ) AS commit_timestamp_completeness_percent,

    COALESCE(iss.issues_with_usable_timestamps, 0) AS issues_with_usable_timestamps,
    ROUND(
            100.0 * COALESCE(iss.issues_with_usable_timestamps, 0) / NULLIF(iss.issues_total, 0),
            2
    ) AS issue_timestamp_completeness_percent,

    COALESCE(ps.prs_with_usable_timestamps, 0) AS prs_with_usable_timestamps,
    ROUND(
            100.0 * COALESCE(ps.prs_with_usable_timestamps, 0) / NULLIF(ps.prs_total, 0),
            2
    ) AS pr_timestamp_completeness_percent,

    COALESCE(ts.timelines_with_timestamp, 0) AS timelines_with_timestamp,
    ROUND(
            100.0 * COALESCE(ts.timelines_with_timestamp, 0) / NULLIF(ts.timelines_total, 0),
            2
    ) AS timeline_timestamp_completeness_percent,

    /* File-change completeness */
    COALESCE(fcs.file_changes_with_patch, 0) AS file_changes_with_patch,
    ROUND(
            100.0 * COALESCE(fcs.file_changes_with_patch, 0) / NULLIF(fcs.file_changes_total, 0),
            2
    ) AS file_change_patch_coverage_percent,

    COALESCE(fcs.file_changes_with_changed_lines, 0) AS file_changes_with_changed_lines,
    ROUND(
            100.0 * COALESCE(fcs.file_changes_with_changed_lines, 0) / NULLIF(fcs.file_changes_total, 0),
            2
    ) AS changed_line_coverage_percent,

    /* Defect-linking statistics */
    COALESCE(dls.issues_with_fixing_commits, 0) AS issues_with_fixing_commits,
    COALESCE(dls.fixing_commits_total, 0) AS fixing_commits_total,
    COALESCE(dls.issues_with_candidate_bics, 0) AS issues_with_candidate_bics,
    COALESCE(dls.candidate_bic_commits_total, 0) AS candidate_bug_introducing_commits_total,
    COALESCE(dls.issue_candidate_bic_links_total, 0) AS issue_candidate_bic_links_total

FROM project_base pb
         LEFT JOIN commit_stats cs
                   ON cs.project_owner = pb.project_owner
                       AND cs.project_name = pb.project_name

         LEFT JOIN issue_stats iss
                   ON iss.project_owner = pb.project_owner
                       AND iss.project_name = pb.project_name

         LEFT JOIN pull_stats ps
                   ON ps.project_owner = pb.project_owner
                       AND ps.project_name = pb.project_name

         LEFT JOIN file_change_stats fcs
                   ON fcs.project_owner = pb.project_owner
                       AND fcs.project_name = pb.project_name

         LEFT JOIN timeline_stats ts
                   ON ts.project_owner = pb.project_owner
                       AND ts.project_name = pb.project_name

         LEFT JOIN reaction_stats rs
                   ON rs.project_owner = pb.project_owner
                       AND rs.project_name = pb.project_name

         LEFT JOIN defect_link_stats dls
                   ON dls.project_owner = pb.project_owner
                       AND dls.project_name = pb.project_name

WHERE
    (pb.project_owner = 'ansible' AND pb.project_name = 'ansible')
   OR (pb.project_owner = 'facebook' AND pb.project_name = 'react')

ORDER BY pb.project_owner, pb.project_name;






#RQ2
WITH
    selected_projects AS (
        SELECT
            p.project_owner,
            p.project_name
        FROM project p
        WHERE p.project_owner IN ('ansible', 'facebook')
    ),

    commit_base AS (
        SELECT
            c.project_owner,
            c.project_name,
            c.sha,
            c.commit_date,
            c.in_degree,
            c.out_degree,
            c.is_merge,
            c.merge_count,
            c.min_depth_of_commit_history,
            c.max_depth_of_commit_history,
            c.distance_to_branch_start,
            c.upstream_heads_unique_on_segment,
            c.days_since_last_merge_on_segment,
            c.number_of_branches,
            c.number_of_vertices,
            c.number_of_edges,
            c.average_degree,
            c.num_of_files_changed
        FROM `commit` c
                 JOIN selected_projects sp
                      ON sp.project_owner = c.project_owner
                          AND sp.project_name  = c.project_name
    ),

    churn_by_commit AS (
        SELECT
            cb.project_owner,
            cb.project_name,
            cb.sha,
            COUNT(DISTINCT fc.id) AS files_changed,
            SUM(fc.total_additions) AS total_additions,
            SUM(fc.total_deletions) AS total_deletions,
            SUM(fc.total_changes) AS total_changes,
            AVG(fc.total_changes) AS avg_changes_per_file,
            MAX(fc.total_changes) AS max_changes_in_file,
            COUNT(DISTINCT CASE
                               WHEN fcl.file_change_id IS NOT NULL THEN fc.id
                END) AS file_changes_with_changed_lines
        FROM commit_base cb
                 JOIN commit_file_changes cfc
                      ON cfc.commit_sha = cb.sha
                 JOIN file_change fc
                      ON fc.id = cfc.file_changes_id
                 LEFT JOIN file_change_changed_lines fcl
                           ON fcl.file_change_id = fc.id
        GROUP BY
            cb.project_owner,
            cb.project_name,
            cb.sha
    ),

    file_change_project_stats AS (
        SELECT
            cb.project_owner,
            cb.project_name,
            COUNT(DISTINCT fc.id) AS file_changes_total,
            COUNT(DISTINCT CASE
                               WHEN fc.patch IS NOT NULL AND fc.patch <> '' THEN fc.id
                END) AS file_changes_with_patch,
            COUNT(DISTINCT CASE
                               WHEN fcl.file_change_id IS NOT NULL THEN fc.id
                END) AS file_changes_with_changed_lines
        FROM commit_base cb
                 JOIN commit_file_changes cfc
                      ON cfc.commit_sha = cb.sha
                 JOIN file_change fc
                      ON fc.id = cfc.file_changes_id
                 LEFT JOIN file_change_changed_lines fcl
                           ON fcl.file_change_id = fc.id
        GROUP BY
            cb.project_owner,
            cb.project_name
    ),

    ci_by_commit AS (
        SELECT
            cb.project_owner,
            cb.project_name,
            cb.sha,
            COUNT(a.id) AS total_check_runs,
            SUM(a.passed) AS passed_checks,
            SUM(a.failed) AS failed_checks,
            SUM(a.other) AS other_checks,
            AVG(a.passed_percent) AS avg_passed_percent,
            AVG(a.failed_percent) AS avg_failed_percent,
            AVG(a.other_percent) AS avg_other_percent,
            COUNT(CASE
                      WHEN a.started_at IS NOT NULL
                          AND a.completed_at IS NOT NULL
                          AND TIMESTAMPDIFF(SECOND, a.started_at, a.completed_at) >= 0
                          THEN a.id
                END) AS check_runs_with_duration
        FROM commit_base cb
                 JOIN `action` a
                      ON a.commit_sha = cb.sha
        GROUP BY
            cb.project_owner,
            cb.project_name,
            cb.sha
    ),

    ci_action_project_stats AS (
        SELECT
            cb.project_owner,
            cb.project_name,
            COUNT(a.id) AS ci_check_runs_total,
            COUNT(CASE
                      WHEN a.started_at IS NOT NULL
                          AND a.completed_at IS NOT NULL
                          AND TIMESTAMPDIFF(SECOND, a.started_at, a.completed_at) >= 0
                          THEN a.id
                END) AS ci_check_runs_with_duration
        FROM commit_base cb
                 JOIN `action` a
                      ON a.commit_sha = cb.sha
        GROUP BY
            cb.project_owner,
            cb.project_name
    ),

    issue_process AS (
        SELECT
            pi.project_owner,
            pi.project_name,
            pi.id AS issue_id,
            pi.state,
            pi.created_at,
            pi.closed_at,
            CASE
                WHEN pi.state = 'closed'
                    AND pi.created_at IS NOT NULL
                    AND pi.closed_at IS NOT NULL
                    AND TIMESTAMPDIFF(HOUR, pi.created_at, pi.closed_at) >= 0
                    THEN TIMESTAMPDIFF(HOUR, pi.created_at, pi.closed_at)
                ELSE NULL
                END AS issue_resolution_hours,
            COUNT(DISTINCT pil.labels) AS label_count,
            COUNT(DISTINCT pia.assignees) AS assignee_count,
            COUNT(DISTINCT pit.time_line_id) AS timeline_event_count,
            MAX(CASE
                    WHEN pi.reaction_id IS NOT NULL THEN r.total_count
                    ELSE NULL
                END) AS reaction_count
        FROM project_issue pi
                 JOIN selected_projects sp
                      ON sp.project_owner = pi.project_owner
                          AND sp.project_name  = pi.project_name
                 LEFT JOIN project_issue_labels pil
                           ON pil.project_issue_id = pi.id
                               AND pil.project_issue_project_name = pi.project_name
                               AND pil.project_issue_project_owner = pi.project_owner
                 LEFT JOIN project_issue_assignees pia
                           ON pia.project_issue_id = pi.id
                               AND pia.project_issue_project_name = pi.project_name
                               AND pia.project_issue_project_owner = pi.project_owner
                 LEFT JOIN project_issue_time_line pit
                           ON pit.project_issue_id = pi.id
                               AND pit.project_issue_project_name = pi.project_name
                               AND pit.project_issue_project_owner = pi.project_owner
                 LEFT JOIN reaction r
                           ON r.id = pi.reaction_id
        GROUP BY
            pi.project_owner,
            pi.project_name,
            pi.id,
            pi.state,
            pi.created_at,
            pi.closed_at
    ),

    pull_process AS (
        SELECT
            pp.project_owner,
            pp.project_name,
            pp.id AS pr_id,
            pp.state,
            pp.created_at,
            pp.closed_at,
            pp.merged_at,
            CASE
                WHEN pp.created_at IS NOT NULL
                    AND pp.merged_at IS NOT NULL
                    AND TIMESTAMPDIFF(HOUR, pp.created_at, pp.merged_at) >= 0
                    THEN TIMESTAMPDIFF(HOUR, pp.created_at, pp.merged_at)
                ELSE NULL
                END AS pr_review_hours,
            COUNT(DISTINCT ppl.labels) AS label_count,
            COUNT(DISTINCT ppa.assignees) AS assignee_count,
            COUNT(DISTINCT ppr.reviewers) AS reviewer_count,
            COUNT(DISTINCT ppt.time_line_id) AS timeline_event_count,
            MAX(CASE
                    WHEN pp.reaction_id IS NOT NULL THEN r.total_count
                    ELSE NULL
                END) AS reaction_count
        FROM project_pull pp
                 JOIN selected_projects sp
                      ON sp.project_owner = pp.project_owner
                          AND sp.project_name  = pp.project_name
                 LEFT JOIN project_pull_labels ppl
                           ON ppl.project_pull_id = pp.id
                               AND ppl.project_pull_project_name = pp.project_name
                               AND ppl.project_pull_project_owner = pp.project_owner
                 LEFT JOIN project_pull_assignees ppa
                           ON ppa.project_pull_id = pp.id
                               AND ppa.project_pull_project_name = pp.project_name
                               AND ppa.project_pull_project_owner = pp.project_owner
                 LEFT JOIN project_pull_reviewers ppr
                           ON ppr.project_pull_id = pp.id
                               AND ppr.project_pull_project_name = pp.project_name
                               AND ppr.project_pull_project_owner = pp.project_owner
                 LEFT JOIN project_pull_time_line ppt
                           ON ppt.project_pull_id = pp.id
                               AND ppt.project_pull_project_name = pp.project_name
                               AND ppt.project_pull_project_owner = pp.project_owner
                 LEFT JOIN reaction r
                           ON r.id = pp.reaction_id
        GROUP BY
            pp.project_owner,
            pp.project_name,
            pp.id,
            pp.state,
            pp.created_at,
            pp.closed_at,
            pp.merged_at
    ),

    issue_fix_commits AS (
        SELECT DISTINCT
            pi.project_owner,
            pi.project_name,
            pi.id AS issue_id,
            pifc.fixing_commits_sha AS sha
        FROM project_issue pi
                 JOIN selected_projects sp
                      ON sp.project_owner = pi.project_owner
                          AND sp.project_name  = pi.project_name
                 JOIN project_issue_fixing_commits pifc
                      ON pifc.project_issue_id = pi.id
                          AND pifc.project_issue_project_name = pi.project_name
                          AND pifc.project_issue_project_owner = pi.project_owner
    ),

    issue_fix_agg AS (
        SELECT
            ifc.project_owner,
            ifc.project_name,
            ifc.issue_id,
            COUNT(DISTINCT ifc.sha) AS fix_num_commits,
            AVG(c.min_depth_of_commit_history) AS fix_avg_min_depth,
            AVG(c.max_depth_of_commit_history) AS fix_avg_max_depth,
            AVG(c.distance_to_branch_start) AS fix_avg_fp_distance,
            AVG(c.upstream_heads_unique_on_segment) AS fix_avg_upstream_heads,
            AVG(c.days_since_last_merge_on_segment) AS fix_avg_days_since_merge,
            SUM(ch.total_additions) AS fix_total_additions,
            SUM(ch.total_deletions) AS fix_total_deletions,
            SUM(ch.total_changes) AS fix_total_changes,
            AVG(ch.avg_changes_per_file) AS fix_avg_changes_per_file,
            MAX(ch.max_changes_in_file) AS fix_max_changes_in_file
        FROM issue_fix_commits ifc
                 JOIN `commit` c
                      ON c.sha = ifc.sha
                 LEFT JOIN churn_by_commit ch
                           ON ch.sha = ifc.sha
        GROUP BY
            ifc.project_owner,
            ifc.project_name,
            ifc.issue_id
    ),

    issue_bic_commits AS (
        SELECT DISTINCT
            pi.project_owner,
            pi.project_name,
            pi.id AS issue_id,
            pibic.bug_introducing_commits_sha AS sha
        FROM project_issue pi
                 JOIN selected_projects sp
                      ON sp.project_owner = pi.project_owner
                          AND sp.project_name  = pi.project_name
                 JOIN project_issue_bug_introducing_commits pibic
                      ON pibic.project_issue_id = pi.id
                          AND pibic.project_issue_project_name = pi.project_name
                          AND pibic.project_issue_project_owner = pi.project_owner
    ),

    issue_bic_agg AS (
        SELECT
            ibc.project_owner,
            ibc.project_name,
            ibc.issue_id,
            COUNT(DISTINCT ibc.sha) AS bic_num_commits,
            AVG(c.min_depth_of_commit_history) AS bic_avg_min_depth,
            AVG(c.max_depth_of_commit_history) AS bic_avg_max_depth,
            AVG(c.distance_to_branch_start) AS bic_avg_fp_distance,
            AVG(c.upstream_heads_unique_on_segment) AS bic_avg_upstream_heads,
            AVG(c.days_since_last_merge_on_segment) AS bic_avg_days_since_merge,
            SUM(ch.total_additions) AS bic_total_additions,
            SUM(ch.total_deletions) AS bic_total_deletions,
            SUM(ch.total_changes) AS bic_total_changes,
            AVG(ch.avg_changes_per_file) AS bic_avg_changes_per_file,
            MAX(ch.max_changes_in_file) AS bic_max_changes_in_file
        FROM issue_bic_commits ibc
                 JOIN `commit` c
                      ON c.sha = ibc.sha
                 LEFT JOIN churn_by_commit ch
                           ON ch.sha = ibc.sha
        GROUP BY
            ibc.project_owner,
            ibc.project_name,
            ibc.issue_id
    ),

    pr_bic_commits AS (
        SELECT DISTINCT
            pp.project_owner,
            pp.project_name,
            pp.id AS pr_id,
            ppc.commits_sha AS sha
        FROM project_pull pp
                 JOIN selected_projects sp
                      ON sp.project_owner = pp.project_owner
                          AND sp.project_name  = pp.project_name
                 JOIN project_pull_commits ppc
                      ON ppc.project_pull_id = pp.id
                          AND ppc.project_pull_project_name = pp.project_name
                          AND ppc.project_pull_project_owner = pp.project_owner
                 JOIN project_issue_bug_introducing_commits pibic
                      ON pibic.bug_introducing_commits_sha = ppc.commits_sha
    ),

    pr_bic_agg AS (
        SELECT
            pbc.project_owner,
            pbc.project_name,
            pbc.pr_id,
            COUNT(DISTINCT pbc.sha) AS bic_num_commits,
            AVG(c.min_depth_of_commit_history) AS bic_avg_min_depth,
            AVG(c.max_depth_of_commit_history) AS bic_avg_max_depth,
            AVG(c.distance_to_branch_start) AS bic_avg_fp_distance,
            AVG(c.upstream_heads_unique_on_segment) AS bic_avg_upstream_heads,
            AVG(c.days_since_last_merge_on_segment) AS bic_avg_days_since_merge,
            SUM(ch.total_additions) AS bic_total_additions,
            SUM(ch.total_deletions) AS bic_total_deletions,
            SUM(ch.total_changes) AS bic_total_changes,
            AVG(ch.avg_changes_per_file) AS bic_avg_changes_per_file,
            MAX(ch.max_changes_in_file) AS bic_max_changes_in_file
        FROM pr_bic_commits pbc
                 JOIN `commit` c
                      ON c.sha = pbc.sha
                 LEFT JOIN churn_by_commit ch
                           ON ch.sha = pbc.sha
        GROUP BY
            pbc.project_owner,
            pbc.project_name,
            pbc.pr_id
    )

SELECT *
FROM (
         /* ---------------- RQ2: Commit-graph metric computability ---------------- */

         SELECT
             cb.project_owner,
             cb.project_name,
             1 AS sort_order,
             'Commit-graph metrics' AS metric_family,
             'Core graph metrics: in/out degree, merge flag, min/max depth' AS metric_group,
             'commit' AS eligible_unit,
             COUNT(*) AS eligible_population,
             SUM(CASE
                     WHEN cb.in_degree IS NOT NULL
                         AND cb.out_degree IS NOT NULL
                         AND cb.is_merge IS NOT NULL
                         AND cb.min_depth_of_commit_history IS NOT NULL
                         AND cb.max_depth_of_commit_history IS NOT NULL
                         THEN 1 ELSE 0
                 END) AS computable_observations,
             ROUND(100.0 * SUM(CASE
                                   WHEN cb.in_degree IS NOT NULL
                                       AND cb.out_degree IS NOT NULL
                                       AND cb.is_merge IS NOT NULL
                                       AND cb.min_depth_of_commit_history IS NOT NULL
                                       AND cb.max_depth_of_commit_history IS NOT NULL
                                       THEN 1 ELSE 0
                 END) / NULLIF(COUNT(*), 0), 2) AS computability_percent
         FROM commit_base cb
         GROUP BY cb.project_owner, cb.project_name

         UNION ALL

         SELECT
             cb.project_owner,
             cb.project_name,
             2 AS sort_order,
             'Commit-graph metrics' AS metric_family,
             'Time-aware graph metrics: branch distance, upstream heads, days since merge, branch visibility' AS metric_group,
             'commit' AS eligible_unit,
             COUNT(*) AS eligible_population,
             SUM(CASE
                     WHEN cb.distance_to_branch_start IS NOT NULL
                         AND cb.upstream_heads_unique_on_segment IS NOT NULL
                         AND cb.days_since_last_merge_on_segment IS NOT NULL
                         AND cb.number_of_branches IS NOT NULL
                         THEN 1 ELSE 0
                 END) AS computable_observations,
             ROUND(100.0 * SUM(CASE
                                   WHEN cb.distance_to_branch_start IS NOT NULL
                                       AND cb.upstream_heads_unique_on_segment IS NOT NULL
                                       AND cb.days_since_last_merge_on_segment IS NOT NULL
                                       AND cb.number_of_branches IS NOT NULL
                                       THEN 1 ELSE 0
                 END) / NULLIF(COUNT(*), 0), 2) AS computability_percent
         FROM commit_base cb
         GROUP BY cb.project_owner, cb.project_name

         UNION ALL

         SELECT
             cb.project_owner,
             cb.project_name,
             3 AS sort_order,
             'Commit-graph metrics' AS metric_family,
             'Graph snapshot metrics: vertices, edges, average degree' AS metric_group,
             'commit' AS eligible_unit,
             COUNT(*) AS eligible_population,
             SUM(CASE
                     WHEN cb.number_of_vertices IS NOT NULL
                         AND cb.number_of_edges IS NOT NULL
                         AND cb.average_degree IS NOT NULL
                         THEN 1 ELSE 0
                 END) AS computable_observations,
             ROUND(100.0 * SUM(CASE
                                   WHEN cb.number_of_vertices IS NOT NULL
                                       AND cb.number_of_edges IS NOT NULL
                                       AND cb.average_degree IS NOT NULL
                                       THEN 1 ELSE 0
                 END) / NULLIF(COUNT(*), 0), 2) AS computability_percent
         FROM commit_base cb
         GROUP BY cb.project_owner, cb.project_name

         /* ---------------- RQ2: Patch and churn metric computability ---------------- */

         UNION ALL

         SELECT
             ch.project_owner,
             ch.project_name,
             4 AS sort_order,
             'Patch and churn metrics' AS metric_family,
             'Commit-level churn summary: files, additions, deletions, total changes, per-file change statistics' AS metric_group,
             'commit with file-change records' AS eligible_unit,
             COUNT(*) AS eligible_population,
             SUM(CASE
                     WHEN ch.files_changed IS NOT NULL
                         AND ch.files_changed > 0
                         AND ch.total_additions IS NOT NULL
                         AND ch.total_deletions IS NOT NULL
                         AND ch.total_changes IS NOT NULL
                         AND ch.avg_changes_per_file IS NOT NULL
                         AND ch.max_changes_in_file IS NOT NULL
                         THEN 1 ELSE 0
                 END) AS computable_observations,
             ROUND(100.0 * SUM(CASE
                                   WHEN ch.files_changed IS NOT NULL
                                       AND ch.files_changed > 0
                                       AND ch.total_additions IS NOT NULL
                                       AND ch.total_deletions IS NOT NULL
                                       AND ch.total_changes IS NOT NULL
                                       AND ch.avg_changes_per_file IS NOT NULL
                                       AND ch.max_changes_in_file IS NOT NULL
                                       THEN 1 ELSE 0
                 END) / NULLIF(COUNT(*), 0), 2) AS computability_percent
         FROM churn_by_commit ch
         GROUP BY ch.project_owner, ch.project_name

         UNION ALL

         SELECT
             fcs.project_owner,
             fcs.project_name,
             5 AS sort_order,
             'Patch and churn metrics' AS metric_family,
             'Changed-line indices available for file changes' AS metric_group,
             'file change' AS eligible_unit,
             fcs.file_changes_total AS eligible_population,
             fcs.file_changes_with_changed_lines AS computable_observations,
             ROUND(100.0 * fcs.file_changes_with_changed_lines / NULLIF(fcs.file_changes_total, 0), 2)
                 AS computability_percent
         FROM file_change_project_stats fcs

         UNION ALL

         SELECT
             fcs.project_owner,
             fcs.project_name,
             6 AS sort_order,
             'Patch and churn metrics' AS metric_family,
             'Patch text available for file changes' AS metric_group,
             'file change' AS eligible_unit,
             fcs.file_changes_total AS eligible_population,
             fcs.file_changes_with_patch AS computable_observations,
             ROUND(100.0 * fcs.file_changes_with_patch / NULLIF(fcs.file_changes_total, 0), 2)
                 AS computability_percent
         FROM file_change_project_stats fcs

         /* ---------------- RQ2: CI summary metric computability ---------------- */

         UNION ALL

         SELECT
             ci.project_owner,
             ci.project_name,
             7 AS sort_order,
             'CI summary metrics' AS metric_family,
             'Commit-level CI summary: total, passed, failed, other, pass/fail percentages' AS metric_group,
             'commit with check-run records' AS eligible_unit,
             COUNT(*) AS eligible_population,
             SUM(CASE
                     WHEN ci.total_check_runs IS NOT NULL
                         AND ci.total_check_runs > 0
                         AND ci.passed_checks IS NOT NULL
                         AND ci.failed_checks IS NOT NULL
                         AND ci.other_checks IS NOT NULL
                         AND ci.avg_passed_percent IS NOT NULL
                         AND ci.avg_failed_percent IS NOT NULL
                         AND ci.avg_other_percent IS NOT NULL
                         THEN 1 ELSE 0
                 END) AS computable_observations,
             ROUND(100.0 * SUM(CASE
                                   WHEN ci.total_check_runs IS NOT NULL
                                       AND ci.total_check_runs > 0
                                       AND ci.passed_checks IS NOT NULL
                                       AND ci.failed_checks IS NOT NULL
                                       AND ci.other_checks IS NOT NULL
                                       AND ci.avg_passed_percent IS NOT NULL
                                       AND ci.avg_failed_percent IS NOT NULL
                                       AND ci.avg_other_percent IS NOT NULL
                                       THEN 1 ELSE 0
                 END) / NULLIF(COUNT(*), 0), 2) AS computability_percent
         FROM ci_by_commit ci
         GROUP BY ci.project_owner, ci.project_name

         UNION ALL

         SELECT
             cias.project_owner,
             cias.project_name,
             8 AS sort_order,
             'CI summary metrics' AS metric_family,
             'Check-run duration computable from start and completion timestamps' AS metric_group,
             'check run' AS eligible_unit,
             cias.ci_check_runs_total AS eligible_population,
             cias.ci_check_runs_with_duration AS computable_observations,
             ROUND(100.0 * cias.ci_check_runs_with_duration / NULLIF(cias.ci_check_runs_total, 0), 2)
                 AS computability_percent
         FROM ci_action_project_stats cias

         /* ---------------- RQ2: Process and social metric computability ---------------- */

         UNION ALL

         SELECT
             ip.project_owner,
             ip.project_name,
             9 AS sort_order,
             'Process and social metrics' AS metric_family,
             'Issue resolution time from created_at and closed_at' AS metric_group,
             'closed issue' AS eligible_unit,
             SUM(CASE WHEN ip.state = 'closed' THEN 1 ELSE 0 END) AS eligible_population,
             SUM(CASE WHEN ip.issue_resolution_hours IS NOT NULL THEN 1 ELSE 0 END) AS computable_observations,
             ROUND(100.0 * SUM(CASE WHEN ip.issue_resolution_hours IS NOT NULL THEN 1 ELSE 0 END)
                       / NULLIF(SUM(CASE WHEN ip.state = 'closed' THEN 1 ELSE 0 END), 0), 2)
                 AS computability_percent
         FROM issue_process ip
         GROUP BY ip.project_owner, ip.project_name

         UNION ALL

         SELECT
             pp.project_owner,
             pp.project_name,
             10 AS sort_order,
             'Process and social metrics' AS metric_family,
             'Pull-request review time from created_at and merged_at' AS metric_group,
             'closed pull request' AS eligible_unit,
             SUM(CASE WHEN pp.state = 'closed' THEN 1 ELSE 0 END) AS eligible_population,
             SUM(CASE WHEN pp.pr_review_hours IS NOT NULL THEN 1 ELSE 0 END) AS computable_observations,
             ROUND(100.0 * SUM(CASE WHEN pp.pr_review_hours IS NOT NULL THEN 1 ELSE 0 END)
                       / NULLIF(SUM(CASE WHEN pp.state = 'closed' THEN 1 ELSE 0 END), 0), 2)
                 AS computability_percent
         FROM pull_process pp
         GROUP BY pp.project_owner, pp.project_name

         UNION ALL

         SELECT
             pp.project_owner,
             pp.project_name,
             11 AS sort_order,
             'Process and social metrics' AS metric_family,
             'Reviewer-count variable for pull requests' AS metric_group,
             'pull request' AS eligible_unit,
             COUNT(*) AS eligible_population,
             COUNT(*) AS computable_observations,
             100.00 AS computability_percent
         FROM pull_process pp
         GROUP BY pp.project_owner, pp.project_name

         UNION ALL

         SELECT
             ip.project_owner,
             ip.project_name,
             12 AS sort_order,
             'Process and social metrics' AS metric_family,
             'Issue timeline-event count variable' AS metric_group,
             'issue' AS eligible_unit,
             COUNT(*) AS eligible_population,
             COUNT(*) AS computable_observations,
             100.00 AS computability_percent
         FROM issue_process ip
         GROUP BY ip.project_owner, ip.project_name

         UNION ALL

         SELECT
             pp.project_owner,
             pp.project_name,
             13 AS sort_order,
             'Process and social metrics' AS metric_family,
             'Pull-request timeline-event count variable' AS metric_group,
             'pull request' AS eligible_unit,
             COUNT(*) AS eligible_population,
             COUNT(*) AS computable_observations,
             100.00 AS computability_percent
         FROM pull_process pp
         GROUP BY pp.project_owner, pp.project_name

         /* ---------------- RQ2: Defect-provenance metric computability ---------------- */

         UNION ALL

         SELECT
             ifa.project_owner,
             ifa.project_name,
             14 AS sort_order,
             'Defect-provenance metrics' AS metric_family,
             'Issue-level fixing-commit count' AS metric_group,
             'issue with resolved fixing commits' AS eligible_unit,
             COUNT(*) AS eligible_population,
             SUM(CASE WHEN ifa.fix_num_commits > 0 THEN 1 ELSE 0 END) AS computable_observations,
             ROUND(100.0 * SUM(CASE WHEN ifa.fix_num_commits > 0 THEN 1 ELSE 0 END)
                       / NULLIF(COUNT(*), 0), 2) AS computability_percent
         FROM issue_fix_agg ifa
         GROUP BY ifa.project_owner, ifa.project_name

         UNION ALL

         SELECT
             ifa.project_owner,
             ifa.project_name,
             15 AS sort_order,
             'Defect-provenance metrics' AS metric_family,
             'Issue-level graph/churn summaries over fixing commits' AS metric_group,
             'issue with resolved fixing commits' AS eligible_unit,
             COUNT(*) AS eligible_population,
             SUM(CASE
                     WHEN ifa.fix_num_commits > 0
                         AND ifa.fix_avg_max_depth IS NOT NULL
                         AND ifa.fix_avg_fp_distance IS NOT NULL
                         AND ifa.fix_total_changes IS NOT NULL
                         THEN 1 ELSE 0
                 END) AS computable_observations,
             ROUND(100.0 * SUM(CASE
                                   WHEN ifa.fix_num_commits > 0
                                       AND ifa.fix_avg_max_depth IS NOT NULL
                                       AND ifa.fix_avg_fp_distance IS NOT NULL
                                       AND ifa.fix_total_changes IS NOT NULL
                                       THEN 1 ELSE 0
                 END) / NULLIF(COUNT(*), 0), 2) AS computability_percent
         FROM issue_fix_agg ifa
         GROUP BY ifa.project_owner, ifa.project_name

         UNION ALL

         SELECT
             iba.project_owner,
             iba.project_name,
             16 AS sort_order,
             'Defect-provenance metrics' AS metric_family,
             'Issue-level candidate-BIC count' AS metric_group,
             'issue with candidate BICs' AS eligible_unit,
             COUNT(*) AS eligible_population,
             SUM(CASE WHEN iba.bic_num_commits > 0 THEN 1 ELSE 0 END) AS computable_observations,
             ROUND(100.0 * SUM(CASE WHEN iba.bic_num_commits > 0 THEN 1 ELSE 0 END)
                       / NULLIF(COUNT(*), 0), 2) AS computability_percent
         FROM issue_bic_agg iba
         GROUP BY iba.project_owner, iba.project_name

         UNION ALL

         SELECT
             iba.project_owner,
             iba.project_name,
             17 AS sort_order,
             'Defect-provenance metrics' AS metric_family,
             'Issue-level graph/churn summaries over candidate BICs' AS metric_group,
             'issue with candidate BICs' AS eligible_unit,
             COUNT(*) AS eligible_population,
             SUM(CASE
                     WHEN iba.bic_num_commits > 0
                         AND iba.bic_avg_max_depth IS NOT NULL
                         AND iba.bic_avg_fp_distance IS NOT NULL
                         AND iba.bic_total_changes IS NOT NULL
                         THEN 1 ELSE 0
                 END) AS computable_observations,
             ROUND(100.0 * SUM(CASE
                                   WHEN iba.bic_num_commits > 0
                                       AND iba.bic_avg_max_depth IS NOT NULL
                                       AND iba.bic_avg_fp_distance IS NOT NULL
                                       AND iba.bic_total_changes IS NOT NULL
                                       THEN 1 ELSE 0
                 END) / NULLIF(COUNT(*), 0), 2) AS computability_percent
         FROM issue_bic_agg iba
         GROUP BY iba.project_owner, iba.project_name

         UNION ALL

         SELECT
             pba.project_owner,
             pba.project_name,
             18 AS sort_order,
             'Defect-provenance metrics' AS metric_family,
             'PR-level graph/churn summaries over candidate BICs contained in PRs' AS metric_group,
             'pull request with candidate BICs' AS eligible_unit,
             COUNT(*) AS eligible_population,
             SUM(CASE
                     WHEN pba.bic_num_commits > 0
                         AND pba.bic_avg_max_depth IS NOT NULL
                         AND pba.bic_avg_fp_distance IS NOT NULL
                         AND pba.bic_total_changes IS NOT NULL
                         THEN 1 ELSE 0
                 END) AS computable_observations,
             ROUND(100.0 * SUM(CASE
                                   WHEN pba.bic_num_commits > 0
                                       AND pba.bic_avg_max_depth IS NOT NULL
                                       AND pba.bic_avg_fp_distance IS NOT NULL
                                       AND pba.bic_total_changes IS NOT NULL
                                       THEN 1 ELSE 0
                 END) / NULLIF(COUNT(*), 0), 2) AS computability_percent
         FROM pr_bic_agg pba
         GROUP BY pba.project_owner, pba.project_name
     ) rq2
ORDER BY
    project_owner,
    project_name,
    sort_order;



#RQ2.1

WITH
    selected_projects AS (
        SELECT project_owner, project_name
        FROM project
        WHERE project_owner IN ('ansible', 'facebook')
    ),

    commit_churn AS (
        SELECT
            c.project_owner,
            c.project_name,
            c.sha,

            COUNT(DISTINCT fc.id) AS files_changed,
            SUM(fc.total_additions) AS total_additions,
            SUM(fc.total_deletions) AS total_deletions,
            SUM(fc.total_changes) AS total_changes,
            AVG(fc.total_changes) AS avg_changes_per_file,
            MAX(fc.total_changes) AS max_changes_in_file
        FROM `commit` c
                 JOIN selected_projects sp
                      ON sp.project_owner = c.project_owner
                          AND sp.project_name  = c.project_name
                 JOIN commit_file_changes cfc
                      ON cfc.commit_sha = c.sha
                 JOIN file_change fc
                      ON fc.id = cfc.file_changes_id
        GROUP BY
            c.project_owner,
            c.project_name,
            c.sha
    ),

    issue_fix_rows AS (
        SELECT
            pi.project_owner,
            pi.project_name,
            pi.id AS analysis_id,

            TIMESTAMPDIFF(HOUR, pi.created_at, pi.closed_at) AS target_hours,

            COUNT(DISTINCT c.sha) AS commits_in_row,

            SUM(CASE
                    WHEN c.in_degree IS NOT NULL
                        AND c.out_degree IS NOT NULL
                        AND c.min_depth_of_commit_history IS NOT NULL
                        AND c.max_depth_of_commit_history IS NOT NULL
                        AND c.distance_to_branch_start IS NOT NULL
                        AND c.upstream_heads_unique_on_segment IS NOT NULL
                        AND c.days_since_last_merge_on_segment IS NOT NULL
                        
                        AND c.average_degree IS NOT NULL
                        THEN 1 ELSE 0
                END) AS graph_ready_commits,

            SUM(CASE
                    WHEN cc.total_changes IS NOT NULL
                        AND cc.files_changed IS NOT NULL
                        AND cc.files_changed > 0
                        THEN 1 ELSE 0
                END) AS churn_ready_commits,

            AVG(c.max_depth_of_commit_history - c.min_depth_of_commit_history) AS avg_depth_range,
            AVG(c.distance_to_branch_start) AS avg_fp_distance,
            AVG(c.upstream_heads_unique_on_segment) AS avg_upstream_heads,
            AVG(c.days_since_last_merge_on_segment) AS avg_days_since_merge,
            SUM(cc.total_changes) AS total_changes
        FROM project_issue pi
                 JOIN selected_projects sp
                      ON sp.project_owner = pi.project_owner
                          AND sp.project_name  = pi.project_name
                 JOIN project_issue_fixing_commits pifc
                      ON pifc.project_issue_id = pi.id
                          AND pifc.project_issue_project_name = pi.project_name
                          AND pifc.project_issue_project_owner = pi.project_owner
                 JOIN `commit` c
                      ON c.sha = pifc.fixing_commits_sha
                 LEFT JOIN commit_churn cc
                           ON cc.sha = c.sha
        WHERE pi.state = 'closed'
          AND pi.created_at IS NOT NULL
          AND pi.closed_at IS NOT NULL
        GROUP BY
            pi.project_owner,
            pi.project_name,
            pi.id,
            pi.created_at,
            pi.closed_at
    ),

    issue_bic_rows AS (
        SELECT
            pi.project_owner,
            pi.project_name,
            pi.id AS analysis_id,

            TIMESTAMPDIFF(HOUR, pi.created_at, pi.closed_at) AS target_hours,

            COUNT(DISTINCT c.sha) AS commits_in_row,

            SUM(CASE
                    WHEN c.in_degree IS NOT NULL
                        AND c.out_degree IS NOT NULL
                        AND c.min_depth_of_commit_history IS NOT NULL
                        AND c.max_depth_of_commit_history IS NOT NULL
                        AND c.distance_to_branch_start IS NOT NULL
                        AND c.upstream_heads_unique_on_segment IS NOT NULL
                        AND c.days_since_last_merge_on_segment IS NOT NULL
                        
                        AND c.average_degree IS NOT NULL
                        THEN 1 ELSE 0
                END) AS graph_ready_commits,

            SUM(CASE
                    WHEN cc.total_changes IS NOT NULL
                        AND cc.files_changed IS NOT NULL
                        AND cc.files_changed > 0
                        THEN 1 ELSE 0
                END) AS churn_ready_commits,

            AVG(c.max_depth_of_commit_history - c.min_depth_of_commit_history) AS avg_depth_range,
            AVG(c.distance_to_branch_start) AS avg_fp_distance,
            AVG(c.upstream_heads_unique_on_segment) AS avg_upstream_heads,
            AVG(c.days_since_last_merge_on_segment) AS avg_days_since_merge,
            SUM(cc.total_changes) AS total_changes
        FROM project_issue pi
                 JOIN selected_projects sp
                      ON sp.project_owner = pi.project_owner
                          AND sp.project_name  = pi.project_name
                 JOIN project_issue_bug_introducing_commits pibic
                      ON pibic.project_issue_id = pi.id
                          AND pibic.project_issue_project_name = pi.project_name
                          AND pibic.project_issue_project_owner = pi.project_owner
                 JOIN `commit` c
                      ON c.sha = pibic.bug_introducing_commits_sha
                 LEFT JOIN commit_churn cc
                           ON cc.sha = c.sha
        WHERE pi.state = 'closed'
          AND pi.created_at IS NOT NULL
          AND pi.closed_at IS NOT NULL
        GROUP BY
            pi.project_owner,
            pi.project_name,
            pi.id,
            pi.created_at,
            pi.closed_at
    ),

    pr_bic_commits AS (
        SELECT DISTINCT
            pp.project_owner,
            pp.project_name,
            pp.id AS pr_id,
            ppc.commits_sha AS sha
        FROM project_pull pp
                 JOIN selected_projects sp
                      ON sp.project_owner = pp.project_owner
                          AND sp.project_name  = pp.project_name
                 JOIN project_pull_commits ppc
                      ON ppc.project_pull_id = pp.id
                          AND ppc.project_pull_project_name = pp.project_name
                          AND ppc.project_pull_project_owner = pp.project_owner
                 JOIN project_issue_bug_introducing_commits pibic
                      ON pibic.bug_introducing_commits_sha = ppc.commits_sha
    ),

    pr_bic_rows AS (
        SELECT
            pp.project_owner,
            pp.project_name,
            pp.id AS analysis_id,

            TIMESTAMPDIFF(HOUR, pp.created_at, pp.merged_at) AS target_hours,

            COUNT(DISTINCT c.sha) AS commits_in_row,

            SUM(CASE
                    WHEN c.in_degree IS NOT NULL
                        AND c.out_degree IS NOT NULL
                        AND c.min_depth_of_commit_history IS NOT NULL
                        AND c.max_depth_of_commit_history IS NOT NULL
                        AND c.distance_to_branch_start IS NOT NULL
                        AND c.upstream_heads_unique_on_segment IS NOT NULL
                        AND c.days_since_last_merge_on_segment IS NOT NULL
                        
                        AND c.average_degree IS NOT NULL
                        THEN 1 ELSE 0
                END) AS graph_ready_commits,

            SUM(CASE
                    WHEN cc.total_changes IS NOT NULL
                        AND cc.files_changed IS NOT NULL
                        AND cc.files_changed > 0
                        THEN 1 ELSE 0
                END) AS churn_ready_commits,

            AVG(c.max_depth_of_commit_history - c.min_depth_of_commit_history) AS avg_depth_range,
            AVG(c.distance_to_branch_start) AS avg_fp_distance,
            AVG(c.upstream_heads_unique_on_segment) AS avg_upstream_heads,
            AVG(c.days_since_last_merge_on_segment) AS avg_days_since_merge,
            SUM(cc.total_changes) AS total_changes
        FROM project_pull pp
                 JOIN pr_bic_commits pbc
                      ON pbc.pr_id = pp.id
                          AND pbc.project_name = pp.project_name
                          AND pbc.project_owner = pp.project_owner
                 JOIN `commit` c
                      ON c.sha = pbc.sha
                 LEFT JOIN commit_churn cc
                           ON cc.sha = c.sha
        WHERE pp.state = 'closed'
          AND pp.created_at IS NOT NULL
          AND pp.merged_at IS NOT NULL
        GROUP BY
            pp.project_owner,
            pp.project_name,
            pp.id,
            pp.created_at,
            pp.merged_at
    ),

    all_rows AS (
        SELECT
            project_owner,
            project_name,
            'Issue-level fixing-commit dataset' AS dataset_name,
            'issue' AS analysis_unit,
            analysis_id,
            target_hours,
            commits_in_row,
            graph_ready_commits,
            churn_ready_commits,
            avg_depth_range,
            avg_fp_distance,
            avg_upstream_heads,
            avg_days_since_merge,
            total_changes
        FROM issue_fix_rows

        UNION ALL

        SELECT
            project_owner,
            project_name,
            'Issue-level candidate-BIC dataset' AS dataset_name,
            'issue' AS analysis_unit,
            analysis_id,
            target_hours,
            commits_in_row,
            graph_ready_commits,
            churn_ready_commits,
            avg_depth_range,
            avg_fp_distance,
            avg_upstream_heads,
            avg_days_since_merge,
            total_changes
        FROM issue_bic_rows

        UNION ALL

        SELECT
            project_owner,
            project_name,
            'PR-level candidate-BIC dataset' AS dataset_name,
            'pull request' AS analysis_unit,
            analysis_id,
            target_hours,
            commits_in_row,
            graph_ready_commits,
            churn_ready_commits,
            avg_depth_range,
            avg_fp_distance,
            avg_upstream_heads,
            avg_days_since_merge,
            total_changes
        FROM pr_bic_rows
    )

SELECT
    project_owner,
    project_name,
    dataset_name,
    analysis_unit,

    COUNT(*) AS analysis_rows,

    SUM(CASE WHEN target_hours IS NOT NULL AND target_hours >= 0 THEN 1 ELSE 0 END)
             AS rows_with_target_duration,

    ROUND(
            100.0 * SUM(CASE WHEN target_hours IS NOT NULL AND target_hours >= 0 THEN 1 ELSE 0 END)
                / NULLIF(COUNT(*), 0),
            2
    ) AS target_duration_computability_percent,

    SUM(CASE WHEN graph_ready_commits > 0 THEN 1 ELSE 0 END)
             AS rows_with_graph_summary,

    ROUND(
            100.0 * SUM(CASE WHEN graph_ready_commits > 0 THEN 1 ELSE 0 END)
                / NULLIF(COUNT(*), 0),
            2
    ) AS graph_summary_computability_percent,

    SUM(CASE WHEN graph_ready_commits = commits_in_row THEN 1 ELSE 0 END)
             AS rows_where_all_commits_have_graph_metrics,

    ROUND(
            100.0 * SUM(CASE WHEN graph_ready_commits = commits_in_row THEN 1 ELSE 0 END)
                / NULLIF(COUNT(*), 0),
            2
    ) AS all_commits_graph_complete_percent,

    SUM(CASE WHEN churn_ready_commits > 0 THEN 1 ELSE 0 END)
             AS rows_with_churn_summary,

    ROUND(
            100.0 * SUM(CASE WHEN churn_ready_commits > 0 THEN 1 ELSE 0 END)
                / NULLIF(COUNT(*), 0),
            2
    ) AS churn_summary_computability_percent,

    SUM(CASE WHEN graph_ready_commits > 0 AND churn_ready_commits > 0 THEN 1 ELSE 0 END)
             AS rows_with_graph_and_churn_summary,

    ROUND(
            100.0 * SUM(CASE WHEN graph_ready_commits > 0 AND churn_ready_commits > 0 THEN 1 ELSE 0 END)
                / NULLIF(COUNT(*), 0),
            2
    ) AS graph_churn_summary_computability_percent,

    ROUND(AVG(commits_in_row), 2) AS avg_commits_per_analysis_row,
    ROUND(AVG(graph_ready_commits), 2) AS avg_graph_ready_commits_per_row,
    ROUND(AVG(churn_ready_commits), 2) AS avg_churn_ready_commits_per_row,

    ROUND(AVG(avg_depth_range), 4) AS avg_depth_range,
    ROUND(AVG(avg_fp_distance), 4) AS avg_fp_distance,
    ROUND(AVG(avg_upstream_heads), 4) AS avg_upstream_heads,
    ROUND(AVG(avg_days_since_merge), 4) AS avg_days_since_merge,
    ROUND(AVG(total_changes), 4) AS avg_total_changes
FROM all_rows
GROUP BY
    project_owner,
    project_name,
    dataset_name,
    analysis_unit
ORDER BY
    project_owner,
    project_name,
    dataset_name;



#RQ3

WITH
    commit_churn AS (
        SELECT
            cfc.commit_sha,
            COUNT(DISTINCT fc.id) AS num_files_changed,
            SUM(COALESCE(fc.total_additions, 0)) AS total_additions,
            SUM(COALESCE(fc.total_deletions, 0)) AS total_deletions,
            SUM(COALESCE(fc.total_changes, 0)) AS total_changes,
            AVG(COALESCE(fc.total_changes, 0)) AS avg_changes_per_file,
            MAX(COALESCE(fc.total_changes, 0)) AS max_changes_in_file
        FROM commit_file_changes cfc
                 JOIN file_change fc
                      ON fc.id = cfc.file_changes_id
        GROUP BY
            cfc.commit_sha
    ),

    issue_bic_commit_rows AS (
        SELECT
            pi.project_owner,
            pi.project_name,
            pi.id AS issue_id,
            pi.created_at AS issue_created_at,
            pi.closed_at AS issue_closed_at,

            TIMESTAMPDIFF(HOUR, pi.created_at, pi.closed_at) AS issue_resolution_hours,

            c.sha AS bic_sha,

            c.min_depth_of_commit_history,
            c.max_depth_of_commit_history,
            (
                c.max_depth_of_commit_history
                    - c.min_depth_of_commit_history
                ) AS depth_diff,

            c.distance_to_branch_start,
            c.upstream_heads_unique_on_segment,
            c.days_since_last_merge_on_segment,
            c.in_degree,
            c.out_degree,
            c.number_of_branches,
            c.average_degree,

            CASE
                WHEN c.days_since_last_merge_on_segment IS NULL THEN NULL
                ELSE
                    1.0 * c.distance_to_branch_start
                        / GREATEST(c.days_since_last_merge_on_segment, 1)
                END AS branch_commit_rate,

            cc.num_files_changed,
            cc.total_additions,
            cc.total_deletions,
            cc.total_changes,
            cc.avg_changes_per_file,
            cc.max_changes_in_file,

            CASE
                WHEN cc.num_files_changed > 0
                    THEN 1.0 * cc.total_changes / cc.num_files_changed
                ELSE NULL
                END AS change_density_per_file

        FROM project_issue pi

                 JOIN project_issue_bug_introducing_commits pibic
                      ON pibic.project_issue_id = pi.id
                          AND pibic.project_issue_project_name = pi.project_name
                          AND pibic.project_issue_project_owner = pi.project_owner

                 JOIN `commit` c
                      ON c.sha = pibic.bug_introducing_commits_sha

                 JOIN commit_churn cc
                      ON cc.commit_sha = c.sha

        WHERE pi.project_owner = 'facebook'
          AND pi.state = 'closed'
          AND pi.created_at IS NOT NULL
          AND pi.closed_at IS NOT NULL
          AND TIMESTAMPDIFF(HOUR, pi.created_at, pi.closed_at) >= 0

          /* Only candidate BIC commits associated with a pull request. */
          AND c.pr_id IS NOT NULL
          AND c.pr_id <> 0

          /* Required graph fields. */
          AND c.min_depth_of_commit_history IS NOT NULL
          AND c.max_depth_of_commit_history IS NOT NULL
          AND c.distance_to_branch_start IS NOT NULL
          AND c.upstream_heads_unique_on_segment IS NOT NULL
          AND c.days_since_last_merge_on_segment IS NOT NULL
          AND c.in_degree IS NOT NULL
          AND c.out_degree IS NOT NULL
          
          AND c.average_degree IS NOT NULL

          /* Required churn fields. */
          AND cc.num_files_changed IS NOT NULL
          AND cc.num_files_changed > 0
          AND cc.total_additions IS NOT NULL
          AND cc.total_deletions IS NOT NULL
          AND cc.total_changes IS NOT NULL
          AND cc.avg_changes_per_file IS NOT NULL
          AND cc.max_changes_in_file IS NOT NULL
    )

SELECT
    r.project_owner,
    r.project_name,
    r.issue_id,
    r.issue_created_at,
    r.issue_closed_at,

    /* Targets */
    r.issue_resolution_hours,
    LOG(1 + r.issue_resolution_hours) AS log_issue_resolution_hours,

    /* Deterministic split for QModel Compilation */
    CASE
        WHEN MOD(CRC32(CONCAT(r.project_owner, ':', r.project_name, ':', r.issue_id)), 10) < 8
            THEN 'train'
        ELSE 'validation'
        END AS dataset_split,

    /* Candidate-BIC count */
    COUNT(DISTINCT r.bic_sha) AS bic_num_commits,

    /* Depth and graph-history features */
    AVG(r.min_depth_of_commit_history) AS bic_avg_min_depth,
    AVG(r.max_depth_of_commit_history) AS bic_avg_max_depth,
    AVG(r.depth_diff) AS bic_avg_depth_diff,
    MAX(r.depth_diff) AS bic_max_depth_diff,

    AVG(r.branch_commit_rate) AS bic_avg_branch_commit_rate,
    MAX(r.branch_commit_rate) AS bic_max_branch_commit_rate,

    AVG(r.distance_to_branch_start) AS bic_avg_fp_distance,
    MAX(r.distance_to_branch_start) AS bic_max_fp_distance,

    AVG(r.upstream_heads_unique_on_segment) AS bic_avg_upstream_heads,
    MAX(r.upstream_heads_unique_on_segment) AS bic_max_upstream_heads,

    AVG(r.days_since_last_merge_on_segment) AS bic_avg_days_since_merge,
    MAX(r.days_since_last_merge_on_segment) AS bic_max_days_since_merge,

    AVG(r.in_degree) AS bic_avg_in_degree,
    AVG(r.out_degree) AS bic_avg_out_degree,
    AVG(r.number_of_branches) AS bic_avg_branches,
    AVG(r.average_degree) AS bic_avg_average_degree,

    /* Churn features */
    SUM(r.total_additions) AS bic_total_additions,
    SUM(r.total_deletions) AS bic_total_deletions,
    SUM(r.total_changes) AS bic_total_changes,

    AVG(r.avg_changes_per_file) AS bic_avg_changes_per_file,
    MAX(r.max_changes_in_file) AS bic_max_changes_in_file,
    SUM(r.num_files_changed) AS bic_num_files_changed,

    CASE
        WHEN SUM(r.num_files_changed) > 0
            THEN 1.0 * SUM(r.total_changes) / SUM(r.num_files_changed)
        ELSE NULL
        END AS bic_change_density_per_file

FROM issue_bic_commit_rows r

GROUP BY
    r.project_owner,
    r.project_name,
    r.issue_id,
    r.issue_created_at,
    r.issue_closed_at,
    r.issue_resolution_hours

HAVING COUNT(DISTINCT r.bic_sha) > 0

ORDER BY
    r.project_owner,
    r.project_name,
    r.issue_id;



#RQ3.1
WITH
    commit_churn AS (
        SELECT
            cfc.commit_sha,
            COUNT(DISTINCT fc.id) AS num_files_changed,
            SUM(COALESCE(fc.total_additions, 0)) AS total_additions,
            SUM(COALESCE(fc.total_deletions, 0)) AS total_deletions,
            SUM(COALESCE(fc.total_changes, 0)) AS total_changes,
            AVG(COALESCE(fc.total_changes, 0)) AS avg_changes_per_file,
            MAX(COALESCE(fc.total_changes, 0)) AS max_changes_in_file
        FROM commit_file_changes cfc
                 JOIN file_change fc
                      ON fc.id = cfc.file_changes_id
        GROUP BY cfc.commit_sha
    ),

    issue_base AS (
        SELECT
            pi.project_owner,
            pi.project_name,
            pi.id AS issue_id,
            pi.created_at AS issue_created_at,
            pi.closed_at AS issue_closed_at,
            TIMESTAMPDIFF(HOUR, pi.created_at, pi.closed_at) AS issue_resolution_hours
        FROM project_issue pi
        WHERE pi.project_owner = %(owner)s
          AND pi.state = 'closed'
          AND pi.created_at IS NOT NULL
          AND pi.closed_at IS NOT NULL
          AND TIMESTAMPDIFF(HOUR, pi.created_at, pi.closed_at) >= 0
    ),

    bic_rows AS (
SELECT
    ib.project_owner,
    ib.project_name,
    ib.issue_id,
    c.sha AS bic_sha,

    c.min_depth_of_commit_history,
    c.max_depth_of_commit_history,
    c.max_depth_of_commit_history - c.min_depth_of_commit_history AS depth_diff,
    c.distance_to_branch_start,
    c.upstream_heads_unique_on_segment,
    c.days_since_last_merge_on_segment,
    c.in_degree,
    c.out_degree,
    c.number_of_branches,
    c.average_degree,

    CASE
    WHEN c.days_since_last_merge_on_segment IS NULL THEN NULL
    ELSE 1.0 * c.distance_to_branch_start
    / GREATEST(c.days_since_last_merge_on_segment, 1)
    END AS branch_commit_rate,

    cc.num_files_changed,
    cc.total_additions,
    cc.total_deletions,
    cc.total_changes,
    cc.avg_changes_per_file,
    cc.max_changes_in_file,

    CASE
    WHEN cc.num_files_changed > 0
    THEN 1.0 * cc.total_changes / cc.num_files_changed
    ELSE NULL
    END AS change_density_per_file,

    CASE
    WHEN c.min_depth_of_commit_history IS NOT NULL
    AND c.max_depth_of_commit_history IS NOT NULL
    AND c.distance_to_branch_start IS NOT NULL
    AND c.upstream_heads_unique_on_segment IS NOT NULL
    AND c.days_since_last_merge_on_segment IS NOT NULL
    AND c.in_degree IS NOT NULL
    AND c.out_degree IS NOT NULL
    
    AND c.average_degree IS NOT NULL
    THEN 1 ELSE 0
    END AS graph_ready,

    CASE
    WHEN cc.num_files_changed IS NOT NULL
    AND cc.num_files_changed > 0
    AND cc.total_changes IS NOT NULL
    THEN 1 ELSE 0
    END AS churn_ready

FROM issue_base ib
    LEFT JOIN project_issue_bug_introducing_commits pibic
ON pibic.project_issue_id = ib.issue_id
    AND pibic.project_issue_project_name = ib.project_name
    AND pibic.project_issue_project_owner = ib.project_owner
    LEFT JOIN `commit` c
    ON c.sha = pibic.bug_introducing_commits_sha
    LEFT JOIN commit_churn cc
    ON cc.commit_sha = c.sha
    )

SELECT
    ib.project_owner,
    ib.project_name,
    ib.issue_id,
    ib.issue_created_at,
    ib.issue_closed_at,

    ib.issue_resolution_hours,
    LOG(1 + ib.issue_resolution_hours) AS log_issue_resolution_hours,

    CASE
        WHEN MOD(CRC32(CONCAT(ib.project_owner, ':', ib.project_name, ':', ib.issue_id)), 10) < 8
            THEN 'train'
        ELSE 'validation'
        END AS dataset_split,

    CASE WHEN COUNT(DISTINCT br.bic_sha) > 0 THEN 1 ELSE 0 END AS has_bic_evidence,

    COUNT(DISTINCT br.bic_sha) AS bic_num_commits,
    COALESCE(SUM(br.graph_ready), 0) AS bic_graph_ready_commits,
    COALESCE(SUM(br.churn_ready), 0) AS bic_churn_ready_commits,

    COALESCE(AVG(CASE WHEN br.graph_ready = 1 THEN br.min_depth_of_commit_history END), 0)
        AS bic_avg_min_depth,
    COALESCE(AVG(CASE WHEN br.graph_ready = 1 THEN br.max_depth_of_commit_history END), 0)
        AS bic_avg_max_depth,
    COALESCE(AVG(CASE WHEN br.graph_ready = 1 THEN br.depth_diff END), 0)
        AS bic_avg_depth_diff,
    COALESCE(MAX(CASE WHEN br.graph_ready = 1 THEN br.depth_diff END), 0)
        AS bic_max_depth_diff,

    COALESCE(AVG(CASE WHEN br.graph_ready = 1 THEN br.branch_commit_rate END), 0)
        AS bic_avg_branch_commit_rate,
    COALESCE(MAX(CASE WHEN br.graph_ready = 1 THEN br.branch_commit_rate END), 0)
        AS bic_max_branch_commit_rate,

    COALESCE(AVG(CASE WHEN br.graph_ready = 1 THEN br.distance_to_branch_start END), 0)
        AS bic_avg_fp_distance,
    COALESCE(MAX(CASE WHEN br.graph_ready = 1 THEN br.distance_to_branch_start END), 0)
        AS bic_max_fp_distance,

    COALESCE(AVG(CASE WHEN br.graph_ready = 1 THEN br.upstream_heads_unique_on_segment END), 0)
        AS bic_avg_upstream_heads,
    COALESCE(MAX(CASE WHEN br.graph_ready = 1 THEN br.upstream_heads_unique_on_segment END), 0)
        AS bic_max_upstream_heads,

    COALESCE(AVG(CASE WHEN br.graph_ready = 1 THEN br.days_since_last_merge_on_segment END), 0)
        AS bic_avg_days_since_merge,
    COALESCE(MAX(CASE WHEN br.graph_ready = 1 THEN br.days_since_last_merge_on_segment END), 0)
        AS bic_max_days_since_merge,

    COALESCE(AVG(CASE WHEN br.graph_ready = 1 THEN br.in_degree END), 0)
        AS bic_avg_in_degree,
    COALESCE(AVG(CASE WHEN br.graph_ready = 1 THEN br.out_degree END), 0)
        AS bic_avg_out_degree,
    COALESCE(AVG(CASE WHEN br.graph_ready = 1 THEN br.number_of_branches END), 0)
        AS bic_avg_branches,
    COALESCE(AVG(CASE WHEN br.graph_ready = 1 THEN br.average_degree END), 0)
        AS bic_avg_average_degree,

    COALESCE(SUM(br.total_additions), 0) AS bic_total_additions,
    COALESCE(SUM(br.total_deletions), 0) AS bic_total_deletions,
    COALESCE(SUM(br.total_changes), 0) AS bic_total_changes,

    COALESCE(AVG(CASE WHEN br.churn_ready = 1 THEN br.avg_changes_per_file END), 0)
        AS bic_avg_changes_per_file,
    COALESCE(MAX(CASE WHEN br.churn_ready = 1 THEN br.max_changes_in_file END), 0)
        AS bic_max_changes_in_file,
    COALESCE(SUM(br.num_files_changed), 0) AS bic_num_files_changed,

    CASE
        WHEN COALESCE(SUM(br.num_files_changed), 0) > 0
            THEN 1.0 * COALESCE(SUM(br.total_changes), 0)
            / COALESCE(SUM(br.num_files_changed), 0)
        ELSE 0
        END AS bic_change_density_per_file

FROM issue_base ib
         LEFT JOIN bic_rows br
                   ON br.project_owner = ib.project_owner
                       AND br.project_name = ib.project_name
                       AND br.issue_id = ib.issue_id

GROUP BY
    ib.project_owner,
    ib.project_name,
    ib.issue_id,
    ib.issue_created_at,
    ib.issue_closed_at,
    ib.issue_resolution_hours

ORDER BY
    ib.project_owner,
    ib.project_name,
    ib.issue_id;





#rq3.3
WITH
    commit_churn AS (
        SELECT
            cfc.commit_sha,
            COUNT(DISTINCT fc.id) AS num_files_changed,
            SUM(COALESCE(fc.total_additions, 0)) AS total_additions,
            SUM(COALESCE(fc.total_deletions, 0)) AS total_deletions,
            SUM(COALESCE(fc.total_changes, 0)) AS total_changes,
            AVG(COALESCE(fc.total_changes, 0)) AS avg_changes_per_file,
            MAX(COALESCE(fc.total_changes, 0)) AS max_changes_in_file
        FROM commit_file_changes cfc
                 JOIN file_change fc
                      ON fc.id = cfc.file_changes_id
        GROUP BY
            cfc.commit_sha
    ),

    ci_by_commit AS (
        SELECT
            a.commit_sha,
            COUNT(DISTINCT a.id) AS ci_check_runs,
            SUM(COALESCE(a.total, 0)) AS ci_total_checks,
            SUM(COALESCE(a.passed, 0)) AS ci_passed_checks,
            SUM(COALESCE(a.failed, 0)) AS ci_failed_checks,
            SUM(COALESCE(a.other, 0)) AS ci_other_checks,
            AVG(a.passed_percent) AS ci_avg_passed_percent,
            AVG(a.failed_percent) AS ci_avg_failed_percent,
            AVG(a.other_percent) AS ci_avg_other_percent,
            AVG(
                    CASE
                        WHEN a.started_at IS NOT NULL
                            AND a.completed_at IS NOT NULL
                            AND TIMESTAMPDIFF(SECOND, a.started_at, a.completed_at) >= 0
                            THEN TIMESTAMPDIFF(SECOND, a.started_at, a.completed_at)
                        ELSE NULL
                        END
            ) AS ci_avg_duration_seconds
        FROM action a
        WHERE a.commit_sha IS NOT NULL
        GROUP BY
            a.commit_sha
    ),

    bic_by_commit AS (
        SELECT
            pibic.bug_introducing_commits_sha AS commit_sha,
            COUNT(DISTINCT CONCAT(
                    pibic.project_issue_project_owner, ':',
                    pibic.project_issue_project_name, ':',
                    pibic.project_issue_id
                           )) AS linked_bic_issues
        FROM project_issue_bug_introducing_commits pibic
        GROUP BY
            pibic.bug_introducing_commits_sha
    ),

    pr_process AS (
        SELECT
            pp.project_owner,
            pp.project_name,
            pp.id AS pr_id,

            COUNT(DISTINCT ppl.labels) AS pr_label_count,
            COUNT(DISTINCT ppa.assignees) AS pr_assignee_count,
            COUNT(DISTINCT ppr.reviewers) AS pr_reviewer_count,
            COUNT(DISTINCT ppt.time_line_id) AS pr_timeline_event_count,
            COALESCE(MAX(r.total_count), 0) AS pr_reaction_count

        FROM project_pull pp

                 LEFT JOIN project_pull_labels ppl
                           ON ppl.project_pull_id = pp.id
                               AND ppl.project_pull_project_name = pp.project_name
                               AND ppl.project_pull_project_owner = pp.project_owner

                 LEFT JOIN project_pull_assignees ppa
                           ON ppa.project_pull_id = pp.id
                               AND ppa.project_pull_project_name = pp.project_name
                               AND ppa.project_pull_project_owner = pp.project_owner

                 LEFT JOIN project_pull_reviewers ppr
                           ON ppr.project_pull_id = pp.id
                               AND ppr.project_pull_project_name = pp.project_name
                               AND ppr.project_pull_project_owner = pp.project_owner

                 LEFT JOIN project_pull_time_line ppt
                           ON ppt.project_pull_id = pp.id
                               AND ppt.project_pull_project_name = pp.project_name
                               AND ppt.project_pull_project_owner = pp.project_owner

                 LEFT JOIN reaction r
                           ON r.id = pp.reaction_id

        WHERE pp.project_owner = 'facebook'

        GROUP BY
            pp.project_owner,
            pp.project_name,
            pp.id
    ),

    pr_commit_rows AS (
        SELECT
            pp.project_owner,
            pp.project_name,
            pp.id AS pr_id,
            pp.created_at AS pr_created_at,
            pp.merged_at AS pr_merged_at,

            TIMESTAMPDIFF(HOUR, pp.created_at, pp.merged_at) AS pr_review_hours,

            c.sha AS commit_sha,

            c.min_depth_of_commit_history,
            c.max_depth_of_commit_history,
            (
                c.max_depth_of_commit_history
                    - c.min_depth_of_commit_history
                ) AS depth_diff,

            c.distance_to_branch_start,
            c.upstream_heads_unique_on_segment,
            c.days_since_last_merge_on_segment,
            c.in_degree,
            c.out_degree,
            c.number_of_branches,
            c.average_degree,

            CASE
                WHEN c.days_since_last_merge_on_segment IS NULL THEN NULL
                ELSE
                    1.0 * c.distance_to_branch_start
                        / GREATEST(c.days_since_last_merge_on_segment, 1)
                END AS branch_commit_rate,

            cc.num_files_changed,
            cc.total_additions,
            cc.total_deletions,
            cc.total_changes,
            cc.avg_changes_per_file,
            cc.max_changes_in_file,

            CASE
                WHEN cc.num_files_changed > 0
                    THEN 1.0 * cc.total_changes / cc.num_files_changed
                ELSE NULL
                END AS change_density_per_file,

            ci.ci_check_runs,
            ci.ci_total_checks,
            ci.ci_passed_checks,
            ci.ci_failed_checks,
            ci.ci_other_checks,
            ci.ci_avg_passed_percent,
            ci.ci_avg_failed_percent,
            ci.ci_avg_other_percent,
            ci.ci_avg_duration_seconds,

            COALESCE(bic.linked_bic_issues, 0) AS linked_bic_issues,

            CASE
                WHEN c.min_depth_of_commit_history IS NOT NULL
                    AND c.max_depth_of_commit_history IS NOT NULL
                    AND c.distance_to_branch_start IS NOT NULL
                    AND c.upstream_heads_unique_on_segment IS NOT NULL
                    AND c.days_since_last_merge_on_segment IS NOT NULL
                    AND c.in_degree IS NOT NULL
                    AND c.out_degree IS NOT NULL
                    
                    AND c.average_degree IS NOT NULL
                    THEN 1 ELSE 0
                END AS graph_ready,

            CASE
                WHEN cc.num_files_changed IS NOT NULL
                    AND cc.num_files_changed > 0
                    AND cc.total_changes IS NOT NULL
                    THEN 1 ELSE 0
                END AS churn_ready,

            CASE
                WHEN ci.ci_check_runs IS NOT NULL
                    AND ci.ci_check_runs > 0
                    THEN 1 ELSE 0
                END AS ci_ready

        FROM project_pull pp

                 JOIN project_pull_commits ppc
                      ON ppc.project_pull_id = pp.id
                          AND ppc.project_pull_project_name = pp.project_name
                          AND ppc.project_pull_project_owner = pp.project_owner

                 JOIN `commit` c
                      ON c.sha = ppc.commits_sha

                 LEFT JOIN commit_churn cc
                           ON cc.commit_sha = c.sha

                 LEFT JOIN ci_by_commit ci
                           ON ci.commit_sha = c.sha

                 LEFT JOIN bic_by_commit bic
                           ON bic.commit_sha = c.sha

        WHERE pp.project_owner = 'facebook'
          AND pp.created_at IS NOT NULL
          AND pp.merged_at IS NOT NULL
          AND TIMESTAMPDIFF(HOUR, pp.created_at, pp.merged_at) >= 0
    )

SELECT
    pcr.project_owner,
    pcr.project_name,
    pcr.pr_id,
    pcr.pr_created_at,
    pcr.pr_merged_at,

    /* Targets */
    pcr.pr_review_hours,
    LOG(1 + pcr.pr_review_hours) AS log_pr_review_hours,

    CASE
        WHEN MOD(CRC32(CONCAT(pcr.project_owner, ':', pcr.project_name, ':', pcr.pr_id)), 10) < 8
            THEN 'train'
        ELSE 'validation'
        END AS dataset_split,

    /* Process / social features */
    COALESCE(pp.pr_label_count, 0) AS pr_label_count,
    COALESCE(pp.pr_assignee_count, 0) AS pr_assignee_count,
    COALESCE(pp.pr_reviewer_count, 0) AS pr_reviewer_count,
    COALESCE(pp.pr_timeline_event_count, 0) AS pr_timeline_event_count,
    COALESCE(pp.pr_reaction_count, 0) AS pr_reaction_count,

    /* Evidence counts */
    COUNT(DISTINCT pcr.commit_sha) AS pr_num_commits,
    SUM(pcr.graph_ready) AS pr_graph_ready_commits,
    SUM(pcr.churn_ready) AS pr_churn_ready_commits,
    SUM(pcr.ci_ready) AS pr_ci_ready_commits,

    CASE
        WHEN SUM(CASE WHEN pcr.linked_bic_issues > 0 THEN 1 ELSE 0 END) > 0
            THEN 1
        ELSE 0
        END AS pr_contains_candidate_bic,

    SUM(CASE WHEN pcr.linked_bic_issues > 0 THEN 1 ELSE 0 END)
        AS pr_candidate_bic_commits,

    SUM(pcr.linked_bic_issues) AS pr_candidate_bic_issue_links,

    /* Graph-history features */
    COALESCE(AVG(CASE WHEN pcr.graph_ready = 1 THEN pcr.min_depth_of_commit_history END), 0)
        AS pr_avg_min_depth,

    COALESCE(AVG(CASE WHEN pcr.graph_ready = 1 THEN pcr.max_depth_of_commit_history END), 0)
        AS pr_avg_max_depth,

    COALESCE(AVG(CASE WHEN pcr.graph_ready = 1 THEN pcr.depth_diff END), 0)
        AS pr_avg_depth_diff,

    COALESCE(MAX(CASE WHEN pcr.graph_ready = 1 THEN pcr.depth_diff END), 0)
        AS pr_max_depth_diff,

    COALESCE(AVG(CASE WHEN pcr.graph_ready = 1 THEN pcr.branch_commit_rate END), 0)
        AS pr_avg_branch_commit_rate,

    COALESCE(MAX(CASE WHEN pcr.graph_ready = 1 THEN pcr.branch_commit_rate END), 0)
        AS pr_max_branch_commit_rate,

    COALESCE(AVG(CASE WHEN pcr.graph_ready = 1 THEN pcr.distance_to_branch_start END), 0)
        AS pr_avg_fp_distance,

    COALESCE(MAX(CASE WHEN pcr.graph_ready = 1 THEN pcr.distance_to_branch_start END), 0)
        AS pr_max_fp_distance,

    COALESCE(AVG(CASE WHEN pcr.graph_ready = 1 THEN pcr.upstream_heads_unique_on_segment END), 0)
        AS pr_avg_upstream_heads,

    COALESCE(MAX(CASE WHEN pcr.graph_ready = 1 THEN pcr.upstream_heads_unique_on_segment END), 0)
        AS pr_max_upstream_heads,

    COALESCE(AVG(CASE WHEN pcr.graph_ready = 1 THEN pcr.days_since_last_merge_on_segment END), 0)
        AS pr_avg_days_since_merge,

    COALESCE(MAX(CASE WHEN pcr.graph_ready = 1 THEN pcr.days_since_last_merge_on_segment END), 0)
        AS pr_max_days_since_merge,

    COALESCE(AVG(CASE WHEN pcr.graph_ready = 1 THEN pcr.in_degree END), 0)
        AS pr_avg_in_degree,

    COALESCE(AVG(CASE WHEN pcr.graph_ready = 1 THEN pcr.out_degree END), 0)
        AS pr_avg_out_degree,

    COALESCE(AVG(CASE WHEN pcr.graph_ready = 1 THEN pcr.number_of_branches END), 0)
        AS pr_avg_branches,

    COALESCE(AVG(CASE WHEN pcr.graph_ready = 1 THEN pcr.average_degree END), 0)
        AS pr_avg_average_degree,

    /* Churn features */
    COALESCE(SUM(pcr.total_additions), 0) AS pr_total_additions,
    COALESCE(SUM(pcr.total_deletions), 0) AS pr_total_deletions,
    COALESCE(SUM(pcr.total_changes), 0) AS pr_total_changes,

    COALESCE(AVG(CASE WHEN pcr.churn_ready = 1 THEN pcr.avg_changes_per_file END), 0)
        AS pr_avg_changes_per_file,

    COALESCE(MAX(CASE WHEN pcr.churn_ready = 1 THEN pcr.max_changes_in_file END), 0)
        AS pr_max_changes_in_file,

    COALESCE(SUM(pcr.num_files_changed), 0) AS pr_num_files_changed,

    CASE
        WHEN COALESCE(SUM(pcr.num_files_changed), 0) > 0
            THEN 1.0 * COALESCE(SUM(pcr.total_changes), 0)
            / COALESCE(SUM(pcr.num_files_changed), 0)
        ELSE 0
        END AS pr_change_density_per_file,

    /* Log-scaled count/churn features */
    LOG(1 + COUNT(DISTINCT pcr.commit_sha)) AS log_pr_num_commits,
    LOG(1 + COALESCE(SUM(pcr.total_additions), 0)) AS log_pr_total_additions,
    LOG(1 + COALESCE(SUM(pcr.total_deletions), 0)) AS log_pr_total_deletions,
    LOG(1 + COALESCE(SUM(pcr.total_changes), 0)) AS log_pr_total_changes,
    LOG(1 + COALESCE(SUM(pcr.num_files_changed), 0)) AS log_pr_num_files_changed,

    /* CI features */
    COALESCE(SUM(pcr.ci_check_runs), 0) AS pr_ci_check_runs,
    COALESCE(SUM(pcr.ci_total_checks), 0) AS pr_ci_total_checks,
    COALESCE(SUM(pcr.ci_passed_checks), 0) AS pr_ci_passed_checks,
    COALESCE(SUM(pcr.ci_failed_checks), 0) AS pr_ci_failed_checks,
    COALESCE(SUM(pcr.ci_other_checks), 0) AS pr_ci_other_checks,

    COALESCE(AVG(pcr.ci_avg_passed_percent), 0) AS pr_ci_avg_passed_percent,
    COALESCE(AVG(pcr.ci_avg_failed_percent), 0) AS pr_ci_avg_failed_percent,
    COALESCE(AVG(pcr.ci_avg_other_percent), 0) AS pr_ci_avg_other_percent,
    COALESCE(AVG(pcr.ci_avg_duration_seconds), 0) AS pr_ci_avg_duration_seconds

FROM pr_commit_rows pcr

         LEFT JOIN pr_process pp
                   ON pp.project_owner = pcr.project_owner
                       AND pp.project_name = pcr.project_name
                       AND pp.pr_id = pcr.pr_id

GROUP BY
    pcr.project_owner,
    pcr.project_name,
    pcr.pr_id,
    pcr.pr_created_at,
    pcr.pr_merged_at,
    pcr.pr_review_hours,
    pp.pr_label_count,
    pp.pr_assignee_count,
    pp.pr_reviewer_count,
    pp.pr_timeline_event_count,
    pp.pr_reaction_count

HAVING
    COUNT(DISTINCT pcr.commit_sha) > 0

ORDER BY
    pcr.project_owner,
    pcr.project_name,
    pcr.pr_id;



select * from project_pull_commits where project_pull_id=1483


select failed, failed_percent, other_percent, passed_percent, result, status, commit_sha from action where  passed_percent!=0 and other_percent!=0 and failed_percent!=0


describe action;



#RQ3.4
WITH
    commit_churn AS (
        SELECT
            cfc.commit_sha,
            COUNT(DISTINCT fc.id) AS num_files_changed,
            SUM(COALESCE(fc.total_additions, 0)) AS total_additions,
            SUM(COALESCE(fc.total_deletions, 0)) AS total_deletions,
            SUM(COALESCE(fc.total_changes, 0)) AS total_changes,
            AVG(COALESCE(fc.total_changes, 0)) AS avg_changes_per_file,
            MAX(COALESCE(fc.total_changes, 0)) AS max_changes_in_file
        FROM commit_file_changes cfc
                 JOIN file_change fc
                      ON fc.id = cfc.file_changes_id
        GROUP BY
            cfc.commit_sha
    ),

    ci_by_commit AS (
        SELECT
            a.commit_sha,

            /* Count only CI rows that contain real parsed CI totals. */
            COUNT(DISTINCT CASE
                               WHEN a.total > 0 THEN a.id
                               ELSE NULL
                END) AS ci_check_runs,

            /* Sum only non-zero CI summary rows. */
            SUM(CASE
                    WHEN a.total > 0 THEN a.total
                    ELSE 0
                END) AS ci_total_checks,

            SUM(CASE
                    WHEN a.total > 0 THEN a.passed
                    ELSE 0
                END) AS ci_passed_checks,

            SUM(CASE
                    WHEN a.total > 0 THEN a.failed
                    ELSE 0
                END) AS ci_failed_checks,

            SUM(CASE
                    WHEN a.total > 0 THEN a.other
                    ELSE 0
                END) AS ci_other_checks,

            /* Average percentages only over real CI summary rows. */
            AVG(CASE
                    WHEN a.total > 0 THEN a.passed_percent
                    ELSE NULL
                END) AS ci_avg_passed_percent,

            AVG(CASE
                    WHEN a.total > 0 THEN a.failed_percent
                    ELSE NULL
                END) AS ci_avg_failed_percent,

            AVG(CASE
                    WHEN a.total > 0 THEN a.other_percent
                    ELSE NULL
                END) AS ci_avg_other_percent,

            /* Average duration only for real CI rows with valid timestamps. */
            AVG(
                    CASE
                        WHEN a.total > 0
                            AND a.started_at IS NOT NULL
                            AND a.completed_at IS NOT NULL
                            AND TIMESTAMPDIFF(SECOND, a.started_at, a.completed_at) >= 0
                            THEN TIMESTAMPDIFF(SECOND, a.started_at, a.completed_at)
                        ELSE NULL
                        END
            ) AS ci_avg_duration_seconds

        FROM action a
        WHERE a.commit_sha IS NOT NULL
        GROUP BY
            a.commit_sha
    ),

    bic_by_commit AS (
        SELECT
            pibic.bug_introducing_commits_sha AS commit_sha,
            COUNT(DISTINCT CONCAT(
                    pibic.project_issue_project_owner, ':',
                    pibic.project_issue_project_name, ':',
                    pibic.project_issue_id
                           )) AS linked_bic_issues
        FROM project_issue_bug_introducing_commits pibic
        GROUP BY
            pibic.bug_introducing_commits_sha
    ),

    pr_process AS (
        SELECT
            pp.project_owner,
            pp.project_name,
            pp.id AS pr_id,

            COUNT(DISTINCT ppl.labels) AS pr_label_count,
            COUNT(DISTINCT ppa.assignees) AS pr_assignee_count,
            COUNT(DISTINCT ppr.reviewers) AS pr_reviewer_count,
            COUNT(DISTINCT ppt.time_line_id) AS pr_timeline_event_count,
            COALESCE(MAX(r.total_count), 0) AS pr_reaction_count

        FROM project_pull pp

                 LEFT JOIN project_pull_labels ppl
                           ON ppl.project_pull_id = pp.id
                               AND ppl.project_pull_project_name = pp.project_name
                               AND ppl.project_pull_project_owner = pp.project_owner

                 LEFT JOIN project_pull_assignees ppa
                           ON ppa.project_pull_id = pp.id
                               AND ppa.project_pull_project_name = pp.project_name
                               AND ppa.project_pull_project_owner = pp.project_owner

                 LEFT JOIN project_pull_reviewers ppr
                           ON ppr.project_pull_id = pp.id
                               AND ppr.project_pull_project_name = pp.project_name
                               AND ppr.project_pull_project_owner = pp.project_owner

                 LEFT JOIN project_pull_time_line ppt
                           ON ppt.project_pull_id = pp.id
                               AND ppt.project_pull_project_name = pp.project_name
                               AND ppt.project_pull_project_owner = pp.project_owner

                 LEFT JOIN reaction r
                           ON r.id = pp.reaction_id

        WHERE pp.project_owner = 'facebook'

        GROUP BY
            pp.project_owner,
            pp.project_name,
            pp.id
    ),

    pr_commit_rows AS (
        SELECT
            pp.project_owner,
            pp.project_name,
            pp.id AS pr_id,
            pp.created_at AS pr_created_at,
            pp.merged_at AS pr_merged_at,

            TIMESTAMPDIFF(HOUR, pp.created_at, pp.merged_at) AS pr_review_hours,

            c.sha AS commit_sha,

            c.min_depth_of_commit_history,
            c.max_depth_of_commit_history,
            (
                c.max_depth_of_commit_history
                    - c.min_depth_of_commit_history
                ) AS depth_diff,

            c.distance_to_branch_start,
            c.upstream_heads_unique_on_segment,
            c.days_since_last_merge_on_segment,
            c.in_degree,
            c.out_degree,
            c.number_of_branches,
            c.average_degree,

            CASE
                WHEN c.days_since_last_merge_on_segment IS NULL THEN NULL
                ELSE
                    1.0 * c.distance_to_branch_start
                        / GREATEST(c.days_since_last_merge_on_segment, 1)
                END AS branch_commit_rate,

            cc.num_files_changed,
            cc.total_additions,
            cc.total_deletions,
            cc.total_changes,
            cc.avg_changes_per_file,
            cc.max_changes_in_file,

            CASE
                WHEN cc.num_files_changed > 0
                    THEN 1.0 * cc.total_changes / cc.num_files_changed
                ELSE NULL
                END AS change_density_per_file,

            ci.ci_check_runs,
            ci.ci_total_checks,
            ci.ci_passed_checks,
            ci.ci_failed_checks,
            ci.ci_other_checks,
            ci.ci_avg_passed_percent,
            ci.ci_avg_failed_percent,
            ci.ci_avg_other_percent,
            ci.ci_avg_duration_seconds,

            COALESCE(bic.linked_bic_issues, 0) AS linked_bic_issues,

            CASE
                WHEN c.min_depth_of_commit_history IS NOT NULL
                    AND c.max_depth_of_commit_history IS NOT NULL
                    AND c.distance_to_branch_start IS NOT NULL
                    AND c.upstream_heads_unique_on_segment IS NOT NULL
                    AND c.days_since_last_merge_on_segment IS NOT NULL
                    AND c.in_degree IS NOT NULL
                    AND c.out_degree IS NOT NULL
                    
                    AND c.average_degree IS NOT NULL
                    THEN 1 ELSE 0
                END AS graph_ready,

            CASE
                WHEN cc.num_files_changed IS NOT NULL
                    AND cc.num_files_changed > 0
                    AND cc.total_changes IS NOT NULL
                    THEN 1 ELSE 0
                END AS churn_ready,

            CASE
                WHEN ci.ci_check_runs IS NOT NULL
                    AND ci.ci_check_runs > 0
                    THEN 1 ELSE 0
                END AS ci_ready

        FROM project_pull pp

                 JOIN project_pull_commits ppc
                      ON ppc.project_pull_id = pp.id
                          AND ppc.project_pull_project_name = pp.project_name
                          AND ppc.project_pull_project_owner = pp.project_owner

                 JOIN `commit` c
                      ON c.sha = ppc.commits_sha

                 LEFT JOIN commit_churn cc
                           ON cc.commit_sha = c.sha

                 LEFT JOIN ci_by_commit ci
                           ON ci.commit_sha = c.sha

                 LEFT JOIN bic_by_commit bic
                           ON bic.commit_sha = c.sha

        WHERE pp.project_owner = 'facebook'
          AND pp.created_at IS NOT NULL
          AND pp.merged_at IS NOT NULL
          AND TIMESTAMPDIFF(HOUR, pp.created_at, pp.merged_at) >= 0
    )

SELECT
    pcr.project_owner,
    pcr.project_name,
    pcr.pr_id,
    pcr.pr_created_at,
    pcr.pr_merged_at,

    /* Targets */
    pcr.pr_review_hours,
    LOG(1 + pcr.pr_review_hours) AS log_pr_review_hours,

    CASE
        WHEN MOD(CRC32(CONCAT(pcr.project_owner, ':', pcr.project_name, ':', pcr.pr_id)), 10) < 8
            THEN 'train'
        ELSE 'validation'
        END AS dataset_split,

    /* Process / social features */
    COALESCE(pp.pr_label_count, 0) AS pr_label_count,
    COALESCE(pp.pr_assignee_count, 0) AS pr_assignee_count,
    COALESCE(pp.pr_reviewer_count, 0) AS pr_reviewer_count,
    COALESCE(pp.pr_timeline_event_count, 0) AS pr_timeline_event_count,
    COALESCE(pp.pr_reaction_count, 0) AS pr_reaction_count,

    /* Evidence counts */
    COUNT(DISTINCT pcr.commit_sha) AS pr_num_commits,
    SUM(pcr.graph_ready) AS pr_graph_ready_commits,
    SUM(pcr.churn_ready) AS pr_churn_ready_commits,
    SUM(pcr.ci_ready) AS pr_ci_ready_commits,

    CASE
        WHEN SUM(CASE WHEN pcr.linked_bic_issues > 0 THEN 1 ELSE 0 END) > 0
            THEN 1
        ELSE 0
        END AS pr_contains_candidate_bic,

    SUM(CASE WHEN pcr.linked_bic_issues > 0 THEN 1 ELSE 0 END)
        AS pr_candidate_bic_commits,

    SUM(pcr.linked_bic_issues) AS pr_candidate_bic_issue_links,

    /* Graph-history features */
    COALESCE(AVG(CASE WHEN pcr.graph_ready = 1 THEN pcr.min_depth_of_commit_history END), 0)
        AS pr_avg_min_depth,

    COALESCE(AVG(CASE WHEN pcr.graph_ready = 1 THEN pcr.max_depth_of_commit_history END), 0)
        AS pr_avg_max_depth,

    COALESCE(AVG(CASE WHEN pcr.graph_ready = 1 THEN pcr.depth_diff END), 0)
        AS pr_avg_depth_diff,

    COALESCE(MAX(CASE WHEN pcr.graph_ready = 1 THEN pcr.depth_diff END), 0)
        AS pr_max_depth_diff,

    COALESCE(AVG(CASE WHEN pcr.graph_ready = 1 THEN pcr.branch_commit_rate END), 0)
        AS pr_avg_branch_commit_rate,

    COALESCE(MAX(CASE WHEN pcr.graph_ready = 1 THEN pcr.branch_commit_rate END), 0)
        AS pr_max_branch_commit_rate,

    COALESCE(AVG(CASE WHEN pcr.graph_ready = 1 THEN pcr.distance_to_branch_start END), 0)
        AS pr_avg_fp_distance,

    COALESCE(MAX(CASE WHEN pcr.graph_ready = 1 THEN pcr.distance_to_branch_start END), 0)
        AS pr_max_fp_distance,

    COALESCE(AVG(CASE WHEN pcr.graph_ready = 1 THEN pcr.upstream_heads_unique_on_segment END), 0)
        AS pr_avg_upstream_heads,

    COALESCE(MAX(CASE WHEN pcr.graph_ready = 1 THEN pcr.upstream_heads_unique_on_segment END), 0)
        AS pr_max_upstream_heads,

    COALESCE(AVG(CASE WHEN pcr.graph_ready = 1 THEN pcr.days_since_last_merge_on_segment END), 0)
        AS pr_avg_days_since_merge,

    COALESCE(MAX(CASE WHEN pcr.graph_ready = 1 THEN pcr.days_since_last_merge_on_segment END), 0)
        AS pr_max_days_since_merge,

    COALESCE(AVG(CASE WHEN pcr.graph_ready = 1 THEN pcr.in_degree END), 0)
        AS pr_avg_in_degree,

    COALESCE(AVG(CASE WHEN pcr.graph_ready = 1 THEN pcr.out_degree END), 0)
        AS pr_avg_out_degree,

    COALESCE(AVG(CASE WHEN pcr.graph_ready = 1 THEN pcr.number_of_branches END), 0)
        AS pr_avg_branches,

    COALESCE(AVG(CASE WHEN pcr.graph_ready = 1 THEN pcr.average_degree END), 0)
        AS pr_avg_average_degree,

    /* Churn features */
    COALESCE(SUM(pcr.total_additions), 0) AS pr_total_additions,
    COALESCE(SUM(pcr.total_deletions), 0) AS pr_total_deletions,
    COALESCE(SUM(pcr.total_changes), 0) AS pr_total_changes,

    COALESCE(AVG(CASE WHEN pcr.churn_ready = 1 THEN pcr.avg_changes_per_file END), 0)
        AS pr_avg_changes_per_file,

    COALESCE(MAX(CASE WHEN pcr.churn_ready = 1 THEN pcr.max_changes_in_file END), 0)
        AS pr_max_changes_in_file,

    COALESCE(SUM(pcr.num_files_changed), 0) AS pr_num_files_changed,

    CASE
        WHEN COALESCE(SUM(pcr.num_files_changed), 0) > 0
            THEN 1.0 * COALESCE(SUM(pcr.total_changes), 0)
            / COALESCE(SUM(pcr.num_files_changed), 0)
        ELSE 0
        END AS pr_change_density_per_file,

    /* Log-scaled count/churn features */
    LOG(1 + COUNT(DISTINCT pcr.commit_sha)) AS log_pr_num_commits,
    LOG(1 + COALESCE(SUM(pcr.total_additions), 0)) AS log_pr_total_additions,
    LOG(1 + COALESCE(SUM(pcr.total_deletions), 0)) AS log_pr_total_deletions,
    LOG(1 + COALESCE(SUM(pcr.total_changes), 0)) AS log_pr_total_changes,
    LOG(1 + COALESCE(SUM(pcr.num_files_changed), 0)) AS log_pr_num_files_changed,

    /* CI features */
    COALESCE(SUM(pcr.ci_check_runs), 0) AS pr_ci_check_runs,
    COALESCE(SUM(pcr.ci_total_checks), 0) AS pr_ci_total_checks,
    COALESCE(SUM(pcr.ci_passed_checks), 0) AS pr_ci_passed_checks,
    COALESCE(SUM(pcr.ci_failed_checks), 0) AS pr_ci_failed_checks,
    COALESCE(SUM(pcr.ci_other_checks), 0) AS pr_ci_other_checks,

    COALESCE(AVG(pcr.ci_avg_passed_percent), 0) AS pr_ci_avg_passed_percent,
    COALESCE(AVG(pcr.ci_avg_failed_percent), 0) AS pr_ci_avg_failed_percent,
    COALESCE(AVG(pcr.ci_avg_other_percent), 0) AS pr_ci_avg_other_percent,
    COALESCE(AVG(pcr.ci_avg_duration_seconds), 0) AS pr_ci_avg_duration_seconds

FROM pr_commit_rows pcr

         LEFT JOIN pr_process pp
                   ON pp.project_owner = pcr.project_owner
                       AND pp.project_name = pcr.project_name
                       AND pp.pr_id = pcr.pr_id

GROUP BY
    pcr.project_owner,
    pcr.project_name,
    pcr.pr_id,
    pcr.pr_created_at,
    pcr.pr_merged_at,
    pcr.pr_review_hours,
    pp.pr_label_count,
    pp.pr_assignee_count,
    pp.pr_reviewer_count,
    pp.pr_timeline_event_count,
    pp.pr_reaction_count

HAVING
    COUNT(DISTINCT pcr.commit_sha) > 0

ORDER BY
    pcr.project_owner,
    pcr.project_name,
    pcr.pr_id;




#RQ3.5
WITH
    commit_churn AS (
        SELECT
            cfc.commit_sha,
            COUNT(DISTINCT fc.id) AS num_files_changed,
            SUM(COALESCE(fc.total_additions, 0)) AS total_additions,
            SUM(COALESCE(fc.total_deletions, 0)) AS total_deletions,
            SUM(COALESCE(fc.total_changes, 0)) AS total_changes,
            AVG(COALESCE(fc.total_changes, 0)) AS avg_changes_per_file,
            MAX(COALESCE(fc.total_changes, 0)) AS max_changes_in_file
        FROM commit_file_changes cfc
                 JOIN file_change fc
                      ON fc.id = cfc.file_changes_id
        GROUP BY
            cfc.commit_sha
    ),

    ci_action_first_nonzero AS (
        SELECT
            x.*
        FROM (
                 SELECT
                     a.*,
                     ROW_NUMBER() OVER (
                         PARTITION BY a.commit_sha
                         ORDER BY a.id ASC
                         ) AS rn
                 FROM action a
                 WHERE a.commit_sha IS NOT NULL
                   AND a.total > 0
             ) x
        WHERE x.rn = 1
    ),

    ci_by_commit AS (
        SELECT
            a.commit_sha,

            1 AS ci_check_runs,

            a.total AS ci_total_checks,
            a.passed AS ci_passed_checks,
            a.failed AS ci_failed_checks,
            a.other AS ci_other_checks,

            a.passed_percent AS ci_avg_passed_percent,
            a.failed_percent AS ci_avg_failed_percent,
            a.other_percent AS ci_avg_other_percent,

            CASE
                WHEN a.started_at IS NOT NULL
                    AND a.completed_at IS NOT NULL
                    AND TIMESTAMPDIFF(SECOND, a.started_at, a.completed_at) >= 0
                    THEN TIMESTAMPDIFF(SECOND, a.started_at, a.completed_at)
                ELSE NULL
                END AS ci_avg_duration_seconds

        FROM ci_action_first_nonzero a
    ),

    bic_by_commit AS (
        SELECT
            pibic.bug_introducing_commits_sha AS commit_sha,
            COUNT(DISTINCT CONCAT(
                    pibic.project_issue_project_owner, ':',
                    pibic.project_issue_project_name, ':',
                    pibic.project_issue_id
                           )) AS linked_bic_issues
        FROM project_issue_bug_introducing_commits pibic
        GROUP BY
            pibic.bug_introducing_commits_sha
    ),

    pr_process AS (
        SELECT
            pp.project_owner,
            pp.project_name,
            pp.id AS pr_id,

            COUNT(DISTINCT ppl.labels) AS pr_label_count,
            COUNT(DISTINCT ppa.assignees) AS pr_assignee_count,
            COUNT(DISTINCT ppr.reviewers) AS pr_reviewer_count,
            COUNT(DISTINCT ppt.time_line_id) AS pr_timeline_event_count,
            COALESCE(MAX(r.total_count), 0) AS pr_reaction_count

        FROM project_pull pp

                 LEFT JOIN project_pull_labels ppl
                           ON ppl.project_pull_id = pp.id
                               AND ppl.project_pull_project_name = pp.project_name
                               AND ppl.project_pull_project_owner = pp.project_owner

                 LEFT JOIN project_pull_assignees ppa
                           ON ppa.project_pull_id = pp.id
                               AND ppa.project_pull_project_name = pp.project_name
                               AND ppa.project_pull_project_owner = pp.project_owner

                 LEFT JOIN project_pull_reviewers ppr
                           ON ppr.project_pull_id = pp.id
                               AND ppr.project_pull_project_name = pp.project_name
                               AND ppr.project_pull_project_owner = pp.project_owner

                 LEFT JOIN project_pull_time_line ppt
                           ON ppt.project_pull_id = pp.id
                               AND ppt.project_pull_project_name = pp.project_name
                               AND ppt.project_pull_project_owner = pp.project_owner

                 LEFT JOIN reaction r
                           ON r.id = pp.reaction_id

        WHERE pp.project_owner = 'facebook'

GROUP BY
    pp.project_owner,
    pp.project_name,
    pp.id
    ),

    pr_commit_rows AS (
SELECT
    pp.project_owner,
    pp.project_name,
    pp.id AS pr_id,
    pp.created_at AS pr_created_at,
    pp.merged_at AS pr_merged_at,

    TIMESTAMPDIFF(HOUR, pp.created_at, pp.merged_at) AS pr_review_hours,

    c.sha AS commit_sha,

    c.min_depth_of_commit_history,
    c.max_depth_of_commit_history,
    (
    c.max_depth_of_commit_history
    - c.min_depth_of_commit_history
    ) AS depth_diff,

    c.distance_to_branch_start,
    c.upstream_heads_unique_on_segment,
    c.days_since_last_merge_on_segment,
    c.in_degree,
    c.out_degree,
    c.average_degree,

    CASE
    WHEN c.days_since_last_merge_on_segment IS NULL THEN NULL
    ELSE
    1.0 * c.distance_to_branch_start
    / GREATEST(c.days_since_last_merge_on_segment, 1)
    END AS branch_commit_rate,

    cc.num_files_changed,
    cc.total_additions,
    cc.total_deletions,
    cc.total_changes,
    cc.avg_changes_per_file,
    cc.max_changes_in_file,

    CASE
    WHEN cc.num_files_changed > 0
    THEN 1.0 * cc.total_changes / cc.num_files_changed
    ELSE NULL
    END AS change_density_per_file,

    ci.ci_check_runs,
    ci.ci_total_checks,
    ci.ci_passed_checks,
    ci.ci_failed_checks,
    ci.ci_other_checks,
    ci.ci_avg_passed_percent,
    ci.ci_avg_failed_percent,
    ci.ci_avg_other_percent,
    ci.ci_avg_duration_seconds,

    COALESCE(bic.linked_bic_issues, 0) AS linked_bic_issues,

    CASE
    WHEN c.min_depth_of_commit_history IS NOT NULL
    AND c.max_depth_of_commit_history IS NOT NULL
    AND c.distance_to_branch_start IS NOT NULL
    AND c.upstream_heads_unique_on_segment IS NOT NULL
    AND c.days_since_last_merge_on_segment IS NOT NULL
    AND c.in_degree IS NOT NULL
    AND c.out_degree IS NOT NULL
    
    AND c.average_degree IS NOT NULL
    THEN 1 ELSE 0
    END AS graph_ready,

    CASE
    WHEN cc.num_files_changed IS NOT NULL
    AND cc.num_files_changed > 0
    AND cc.total_changes IS NOT NULL
    THEN 1 ELSE 0
    END AS churn_ready,

    CASE
    WHEN ci.ci_check_runs IS NOT NULL
    AND ci.ci_check_runs > 0
    THEN 1 ELSE 0
    END AS ci_ready

FROM project_pull pp

    JOIN project_pull_commits ppc
ON ppc.project_pull_id = pp.id
    AND ppc.project_pull_project_name = pp.project_name
    AND ppc.project_pull_project_owner = pp.project_owner

    JOIN `commit` c
    ON c.sha = ppc.commits_sha

    LEFT JOIN commit_churn cc
    ON cc.commit_sha = c.sha

    LEFT JOIN ci_by_commit ci
    ON ci.commit_sha = c.sha

    LEFT JOIN bic_by_commit bic
    ON bic.commit_sha = c.sha

WHERE pp.project_owner = 'facebook'
  AND pp.created_at IS NOT NULL
  AND pp.merged_at IS NOT NULL
  AND TIMESTAMPDIFF(HOUR, pp.created_at, pp.merged_at) >= 0
    )

SELECT
    pcr.project_owner,
    pcr.project_name,
    pcr.pr_id,
    pcr.pr_created_at,
    pcr.pr_merged_at,

    /* Targets */
    pcr.pr_review_hours,
    LOG(1 + pcr.pr_review_hours) AS log_pr_review_hours,

    CASE
        WHEN MOD(CRC32(CONCAT(pcr.project_owner, ':', pcr.project_name, ':', pcr.pr_id)), 10) < 8
            THEN 'train'
        ELSE 'validation'
        END AS dataset_split,

    /* Process / social features */
    COALESCE(pp.pr_label_count, 0) AS pr_label_count,
    COALESCE(pp.pr_assignee_count, 0) AS pr_assignee_count,
    COALESCE(pp.pr_reviewer_count, 0) AS pr_reviewer_count,
    COALESCE(pp.pr_timeline_event_count, 0) AS pr_timeline_event_count,
    COALESCE(pp.pr_reaction_count, 0) AS pr_reaction_count,

    /* Evidence counts */
    COUNT(DISTINCT pcr.commit_sha) AS pr_num_commits,
    SUM(pcr.graph_ready) AS pr_graph_ready_commits,
    SUM(pcr.churn_ready) AS pr_churn_ready_commits,
    SUM(pcr.ci_ready) AS pr_ci_ready_commits,

    CASE
        WHEN SUM(CASE WHEN pcr.linked_bic_issues > 0 THEN 1 ELSE 0 END) > 0
            THEN 1
        ELSE 0
        END AS pr_contains_candidate_bic,

    SUM(CASE WHEN pcr.linked_bic_issues > 0 THEN 1 ELSE 0 END)
        AS pr_candidate_bic_commits,

    SUM(pcr.linked_bic_issues) AS pr_candidate_bic_issue_links,

    /* Graph-history features */
    COALESCE(AVG(CASE WHEN pcr.graph_ready = 1 THEN pcr.min_depth_of_commit_history END), 0)
        AS pr_avg_min_depth,

    COALESCE(AVG(CASE WHEN pcr.graph_ready = 1 THEN pcr.max_depth_of_commit_history END), 0)
        AS pr_avg_max_depth,

    COALESCE(AVG(CASE WHEN pcr.graph_ready = 1 THEN pcr.depth_diff END), 0)
        AS pr_avg_depth_diff,

    COALESCE(MAX(CASE WHEN pcr.graph_ready = 1 THEN pcr.depth_diff END), 0)
        AS pr_max_depth_diff,

    COALESCE(AVG(CASE WHEN pcr.graph_ready = 1 THEN pcr.branch_commit_rate END), 0)
        AS pr_avg_branch_commit_rate,

    COALESCE(MAX(CASE WHEN pcr.graph_ready = 1 THEN pcr.branch_commit_rate END), 0)
        AS pr_max_branch_commit_rate,

    COALESCE(AVG(CASE WHEN pcr.graph_ready = 1 THEN pcr.distance_to_branch_start END), 0)
        AS pr_avg_fp_distance,

    COALESCE(MAX(CASE WHEN pcr.graph_ready = 1 THEN pcr.distance_to_branch_start END), 0)
        AS pr_max_fp_distance,

    COALESCE(AVG(CASE WHEN pcr.graph_ready = 1 THEN pcr.upstream_heads_unique_on_segment END), 0)
        AS pr_avg_upstream_heads,

    COALESCE(MAX(CASE WHEN pcr.graph_ready = 1 THEN pcr.upstream_heads_unique_on_segment END), 0)
        AS pr_max_upstream_heads,

    COALESCE(AVG(CASE WHEN pcr.graph_ready = 1 THEN pcr.days_since_last_merge_on_segment END), 0)
        AS pr_avg_days_since_merge,

    COALESCE(MAX(CASE WHEN pcr.graph_ready = 1 THEN pcr.days_since_last_merge_on_segment END), 0)
        AS pr_max_days_since_merge,

    COALESCE(AVG(CASE WHEN pcr.graph_ready = 1 THEN pcr.in_degree END), 0)
        AS pr_avg_in_degree,

    COALESCE(AVG(CASE WHEN pcr.graph_ready = 1 THEN pcr.out_degree END), 0)
        AS pr_avg_out_degree,

    COALESCE(AVG(CASE WHEN pcr.graph_ready = 1 THEN pcr.number_of_branches END), 0)
        AS pr_avg_branches,

    COALESCE(AVG(CASE WHEN pcr.graph_ready = 1 THEN pcr.average_degree END), 0)
        AS pr_avg_average_degree,

    /* Churn features */
    COALESCE(SUM(pcr.total_additions), 0) AS pr_total_additions,
    COALESCE(SUM(pcr.total_deletions), 0) AS pr_total_deletions,
    COALESCE(SUM(pcr.total_changes), 0) AS pr_total_changes,

    COALESCE(AVG(CASE WHEN pcr.churn_ready = 1 THEN pcr.avg_changes_per_file END), 0)
        AS pr_avg_changes_per_file,

    COALESCE(MAX(CASE WHEN pcr.churn_ready = 1 THEN pcr.max_changes_in_file END), 0)
        AS pr_max_changes_in_file,

    COALESCE(SUM(pcr.num_files_changed), 0) AS pr_num_files_changed,

    CASE
        WHEN COALESCE(SUM(pcr.num_files_changed), 0) > 0
            THEN 1.0 * COALESCE(SUM(pcr.total_changes), 0)
            / COALESCE(SUM(pcr.num_files_changed), 0)
        ELSE 0
        END AS pr_change_density_per_file,

    /* Log-scaled count/churn features */
    LOG(1 + COUNT(DISTINCT pcr.commit_sha)) AS log_pr_num_commits,
    LOG(1 + COALESCE(SUM(pcr.total_additions), 0)) AS log_pr_total_additions,
    LOG(1 + COALESCE(SUM(pcr.total_deletions), 0)) AS log_pr_total_deletions,
    LOG(1 + COALESCE(SUM(pcr.total_changes), 0)) AS log_pr_total_changes,
    LOG(1 + COALESCE(SUM(pcr.num_files_changed), 0)) AS log_pr_num_files_changed,

    /* CI features */
    COALESCE(SUM(pcr.ci_check_runs), 0) AS pr_ci_check_runs,
    COALESCE(SUM(pcr.ci_total_checks), 0) AS pr_ci_total_checks,
    COALESCE(SUM(pcr.ci_passed_checks), 0) AS pr_ci_passed_checks,
    COALESCE(SUM(pcr.ci_failed_checks), 0) AS pr_ci_failed_checks,
    COALESCE(SUM(pcr.ci_other_checks), 0) AS pr_ci_other_checks,

    COALESCE(AVG(pcr.ci_avg_passed_percent), 0) AS pr_ci_avg_passed_percent,
    COALESCE(AVG(pcr.ci_avg_failed_percent), 0) AS pr_ci_avg_failed_percent,
    COALESCE(AVG(pcr.ci_avg_other_percent), 0) AS pr_ci_avg_other_percent,
    COALESCE(AVG(pcr.ci_avg_duration_seconds), 0) AS pr_ci_avg_duration_seconds

FROM pr_commit_rows pcr

         LEFT JOIN pr_process pp
                   ON pp.project_owner = pcr.project_owner
                       AND pp.project_name = pcr.project_name
                       AND pp.pr_id = pcr.pr_id

GROUP BY
    pcr.project_owner,
    pcr.project_name,
    pcr.pr_id,
    pcr.pr_created_at,
    pcr.pr_merged_at,
    pcr.pr_review_hours,
    pp.pr_label_count,
    pp.pr_assignee_count,
    pp.pr_reviewer_count,
    pp.pr_timeline_event_count,
    pp.pr_reaction_count

HAVING
    COUNT(DISTINCT pcr.commit_sha) > 0

ORDER BY
    pcr.project_owner,
    pcr.project_name,
    pcr.pr_id;


#RQ3.6
SET SESSION group_concat_max_len = 1000000;

WITH
    commit_churn AS (
        SELECT
            cfc.commit_sha,
            COUNT(DISTINCT fc.id) AS num_files_changed,
            SUM(COALESCE(fc.total_additions, 0)) AS total_additions,
            SUM(COALESCE(fc.total_deletions, 0)) AS total_deletions,
            SUM(COALESCE(fc.total_changes, 0)) AS total_changes,
            AVG(COALESCE(fc.total_changes, 0)) AS avg_changes_per_file,
            MAX(COALESCE(fc.total_changes, 0)) AS max_changes_in_file
        FROM commit_file_changes cfc
                 JOIN file_change fc
                      ON fc.id = cfc.file_changes_id
        GROUP BY
            cfc.commit_sha
    ),

    ci_action_first_nonzero AS (
        SELECT
            x.*
        FROM (
                 SELECT
                     a.*,
                     ROW_NUMBER() OVER (
                         PARTITION BY a.commit_sha
                         ORDER BY a.id ASC
                         ) AS rn
                 FROM action a
                 WHERE a.commit_sha IS NOT NULL
                   AND a.total > 0
             ) x
        WHERE x.rn = 1
    ),

    ci_by_commit AS (
        SELECT
            a.commit_sha,

            1 AS ci_check_runs,

            a.total AS ci_total_checks,
            a.passed AS ci_passed_checks,
            a.failed AS ci_failed_checks,
            a.other AS ci_other_checks,

            a.passed_percent AS ci_avg_passed_percent,
            a.failed_percent AS ci_avg_failed_percent,
            a.other_percent AS ci_avg_other_percent,

            CASE
                WHEN a.started_at IS NOT NULL
                    AND a.completed_at IS NOT NULL
                    AND TIMESTAMPDIFF(SECOND, a.started_at, a.completed_at) >= 0
                    THEN TIMESTAMPDIFF(SECOND, a.started_at, a.completed_at)
                ELSE NULL
                END AS ci_avg_duration_seconds

        FROM ci_action_first_nonzero a
    ),

    bic_by_commit AS (
        SELECT
            pibic.bug_introducing_commits_sha AS commit_sha,
            COUNT(DISTINCT CONCAT(
                    pibic.project_issue_project_owner, ':',
                    pibic.project_issue_project_name, ':',
                    pibic.project_issue_id
                           )) AS linked_bic_issues
        FROM project_issue_bug_introducing_commits pibic
        GROUP BY
            pibic.bug_introducing_commits_sha
    ),

    pr_process AS (
        SELECT
            pp.project_owner,
            pp.project_name,
            pp.id AS pr_id,

            COUNT(DISTINCT ppl.labels) AS pr_label_count,
            COUNT(DISTINCT ppa.assignees) AS pr_assignee_count,
            COUNT(DISTINCT ppr.reviewers) AS pr_reviewer_count,
            COUNT(DISTINCT ppt.time_line_id) AS pr_timeline_event_count,
            COALESCE(MAX(r.total_count), 0) AS pr_reaction_count

        FROM project_pull pp

                 LEFT JOIN project_pull_labels ppl
                           ON ppl.project_pull_id = pp.id
                               AND ppl.project_pull_project_name = pp.project_name
                               AND ppl.project_pull_project_owner = pp.project_owner

                 LEFT JOIN project_pull_assignees ppa
                           ON ppa.project_pull_id = pp.id
                               AND ppa.project_pull_project_name = pp.project_name
                               AND ppa.project_pull_project_owner = pp.project_owner

                 LEFT JOIN project_pull_reviewers ppr
                           ON ppr.project_pull_id = pp.id
                               AND ppr.project_pull_project_name = pp.project_name
                               AND ppr.project_pull_project_owner = pp.project_owner

                 LEFT JOIN project_pull_time_line ppt
                           ON ppt.project_pull_id = pp.id
                               AND ppt.project_pull_project_name = pp.project_name
                               AND ppt.project_pull_project_owner = pp.project_owner

                 LEFT JOIN reaction r
                           ON r.id = pp.reaction_id

        WHERE pp.project_owner = 'facebook'

GROUP BY
    pp.project_owner,
    pp.project_name,
    pp.id
    ),

    pr_commit_rows AS (
SELECT
    pp.project_owner,
    pp.project_name,
    pp.id AS pr_id,
    pp.created_at AS pr_created_at,
    pp.merged_at AS pr_merged_at,

    TIMESTAMPDIFF(HOUR, pp.created_at, pp.merged_at) AS pr_review_hours,

    c.sha AS commit_sha,

    c.min_depth_of_commit_history,
    c.max_depth_of_commit_history,
    (
    c.max_depth_of_commit_history
    - c.min_depth_of_commit_history
    ) AS depth_diff,

    c.distance_to_branch_start,
    c.upstream_heads_unique_on_segment,
    c.days_since_last_merge_on_segment,
    c.in_degree,
    c.out_degree,
    c.number_of_branches,
    c.average_degree,

    CASE
    WHEN c.days_since_last_merge_on_segment IS NULL THEN NULL
    ELSE
    1.0 * c.distance_to_branch_start
    / GREATEST(c.days_since_last_merge_on_segment, 1)
    END AS branch_commit_rate,

    cc.num_files_changed,
    cc.total_additions,
    cc.total_deletions,
    cc.total_changes,
    cc.avg_changes_per_file,
    cc.max_changes_in_file,

    CASE
    WHEN cc.num_files_changed > 0
    THEN 1.0 * cc.total_changes / cc.num_files_changed
    ELSE NULL
    END AS change_density_per_file,

    ci.ci_check_runs,
    ci.ci_total_checks,
    ci.ci_passed_checks,
    ci.ci_failed_checks,
    ci.ci_other_checks,
    ci.ci_avg_passed_percent,
    ci.ci_avg_failed_percent,
    ci.ci_avg_other_percent,
    ci.ci_avg_duration_seconds,

    COALESCE(bic.linked_bic_issues, 0) AS linked_bic_issues,

    CASE
    WHEN c.min_depth_of_commit_history IS NOT NULL
    AND c.max_depth_of_commit_history IS NOT NULL
    AND c.distance_to_branch_start IS NOT NULL
    AND c.upstream_heads_unique_on_segment IS NOT NULL
    AND c.days_since_last_merge_on_segment IS NOT NULL
    AND c.in_degree IS NOT NULL
    AND c.out_degree IS NOT NULL
    
    AND c.average_degree IS NOT NULL
    THEN 1 ELSE 0
    END AS graph_ready,

    CASE
    WHEN cc.num_files_changed IS NOT NULL
    AND cc.num_files_changed > 0
    AND cc.total_changes IS NOT NULL
    THEN 1 ELSE 0
    END AS churn_ready,

    CASE
    WHEN ci.ci_check_runs IS NOT NULL
    AND ci.ci_check_runs > 0
    THEN 1 ELSE 0
    END AS ci_ready

FROM project_pull pp

    JOIN project_pull_commits ppc
ON ppc.project_pull_id = pp.id
    AND ppc.project_pull_project_name = pp.project_name
    AND ppc.project_pull_project_owner = pp.project_owner

    JOIN `commit` c
    ON c.sha = ppc.commits_sha

    LEFT JOIN commit_churn cc
    ON cc.commit_sha = c.sha

    LEFT JOIN ci_by_commit ci
    ON ci.commit_sha = c.sha

    LEFT JOIN bic_by_commit bic
    ON bic.commit_sha = c.sha

WHERE pp.project_owner = 'facebook'
  AND pp.created_at IS NOT NULL
  AND pp.merged_at IS NOT NULL
  AND TIMESTAMPDIFF(HOUR, pp.created_at, pp.merged_at) >= 0
    )

SELECT
    pcr.project_owner,
    pcr.project_name,
    pcr.pr_id,
    pcr.pr_created_at,
    pcr.pr_merged_at,

    /* Targets */
    pcr.pr_review_hours,
    LOG(1 + pcr.pr_review_hours) AS log_pr_review_hours,

    CASE
        WHEN MOD(CRC32(CONCAT(pcr.project_owner, ':', pcr.project_name, ':', pcr.pr_id)), 10) < 8
            THEN 'train'
        ELSE 'validation'
        END AS dataset_split,

    /* Process / social features */
    COALESCE(pp.pr_label_count, 0) AS pr_label_count,
    COALESCE(pp.pr_assignee_count, 0) AS pr_assignee_count,
    COALESCE(pp.pr_reviewer_count, 0) AS pr_reviewer_count,
    COALESCE(pp.pr_timeline_event_count, 0) AS pr_timeline_event_count,
    COALESCE(pp.pr_reaction_count, 0) AS pr_reaction_count,

    /* Evidence counts */
    COUNT(DISTINCT pcr.commit_sha) AS pr_num_commits,

    GROUP_CONCAT(
            DISTINCT pcr.commit_sha
            ORDER BY pcr.commit_sha
            SEPARATOR ', '
    ) AS pr_commit_shas,

    GROUP_CONCAT(
            DISTINCT CASE
                         WHEN pcr.ci_ready = 1 THEN pcr.commit_sha
                         ELSE NULL
        END
            ORDER BY pcr.commit_sha
            SEPARATOR ', '
    ) AS pr_ci_commit_shas,

    SUM(pcr.graph_ready) AS pr_graph_ready_commits,
    SUM(pcr.churn_ready) AS pr_churn_ready_commits,
    SUM(pcr.ci_ready) AS pr_ci_ready_commits,

    CASE
        WHEN SUM(CASE WHEN pcr.linked_bic_issues > 0 THEN 1 ELSE 0 END) > 0
            THEN 1
        ELSE 0
        END AS pr_contains_candidate_bic,

    SUM(CASE WHEN pcr.linked_bic_issues > 0 THEN 1 ELSE 0 END)
        AS pr_candidate_bic_commits,

    GROUP_CONCAT(
            DISTINCT CASE
                         WHEN pcr.linked_bic_issues > 0 THEN pcr.commit_sha
                         ELSE NULL
        END
            ORDER BY pcr.commit_sha
            SEPARATOR ', '
    ) AS pr_candidate_bic_commit_shas,

    SUM(pcr.linked_bic_issues) AS pr_candidate_bic_issue_links,

    /* Graph-history features */
    COALESCE(AVG(CASE WHEN pcr.graph_ready = 1 THEN pcr.min_depth_of_commit_history END), 0)
        AS pr_avg_min_depth,

    COALESCE(AVG(CASE WHEN pcr.graph_ready = 1 THEN pcr.max_depth_of_commit_history END), 0)
        AS pr_avg_max_depth,

    COALESCE(AVG(CASE WHEN pcr.graph_ready = 1 THEN pcr.depth_diff END), 0)
        AS pr_avg_depth_diff,

    COALESCE(MAX(CASE WHEN pcr.graph_ready = 1 THEN pcr.depth_diff END), 0)
        AS pr_max_depth_diff,

    COALESCE(AVG(CASE WHEN pcr.graph_ready = 1 THEN pcr.branch_commit_rate END), 0)
        AS pr_avg_branch_commit_rate,

    COALESCE(MAX(CASE WHEN pcr.graph_ready = 1 THEN pcr.branch_commit_rate END), 0)
        AS pr_max_branch_commit_rate,

    COALESCE(AVG(CASE WHEN pcr.graph_ready = 1 THEN pcr.distance_to_branch_start END), 0)
        AS pr_avg_fp_distance,

    COALESCE(MAX(CASE WHEN pcr.graph_ready = 1 THEN pcr.distance_to_branch_start END), 0)
        AS pr_max_fp_distance,

    COALESCE(AVG(CASE WHEN pcr.graph_ready = 1 THEN pcr.upstream_heads_unique_on_segment END), 0)
        AS pr_avg_upstream_heads,

    COALESCE(MAX(CASE WHEN pcr.graph_ready = 1 THEN pcr.upstream_heads_unique_on_segment END), 0)
        AS pr_max_upstream_heads,

    COALESCE(AVG(CASE WHEN pcr.graph_ready = 1 THEN pcr.days_since_last_merge_on_segment END), 0)
        AS pr_avg_days_since_merge,

    COALESCE(MAX(CASE WHEN pcr.graph_ready = 1 THEN pcr.days_since_last_merge_on_segment END), 0)
        AS pr_max_days_since_merge,

    COALESCE(AVG(CASE WHEN pcr.graph_ready = 1 THEN pcr.in_degree END), 0)
        AS pr_avg_in_degree,

    COALESCE(AVG(CASE WHEN pcr.graph_ready = 1 THEN pcr.out_degree END), 0)
        AS pr_avg_out_degree,

    COALESCE(AVG(CASE WHEN pcr.graph_ready = 1 THEN pcr.number_of_branches END), 0)
        AS pr_avg_branches,

    COALESCE(AVG(CASE WHEN pcr.graph_ready = 1 THEN pcr.average_degree END), 0)
        AS pr_avg_average_degree,

    /* Churn features */
    COALESCE(SUM(pcr.total_additions), 0) AS pr_total_additions,
    COALESCE(SUM(pcr.total_deletions), 0) AS pr_total_deletions,
    COALESCE(SUM(pcr.total_changes), 0) AS pr_total_changes,

    COALESCE(AVG(CASE WHEN pcr.churn_ready = 1 THEN pcr.avg_changes_per_file END), 0)
        AS pr_avg_changes_per_file,

    COALESCE(MAX(CASE WHEN pcr.churn_ready = 1 THEN pcr.max_changes_in_file END), 0)
        AS pr_max_changes_in_file,

    COALESCE(SUM(pcr.num_files_changed), 0) AS pr_num_files_changed,

    CASE
        WHEN COALESCE(SUM(pcr.num_files_changed), 0) > 0
            THEN 1.0 * COALESCE(SUM(pcr.total_changes), 0)
            / COALESCE(SUM(pcr.num_files_changed), 0)
        ELSE 0
        END AS pr_change_density_per_file,

    /* Log-scaled count/churn features */
    LOG(1 + COUNT(DISTINCT pcr.commit_sha)) AS log_pr_num_commits,
    LOG(1 + COALESCE(SUM(pcr.total_additions), 0)) AS log_pr_total_additions,
    LOG(1 + COALESCE(SUM(pcr.total_deletions), 0)) AS log_pr_total_deletions,
    LOG(1 + COALESCE(SUM(pcr.total_changes), 0)) AS log_pr_total_changes,
    LOG(1 + COALESCE(SUM(pcr.num_files_changed), 0)) AS log_pr_num_files_changed,

    /* CI features */
    COALESCE(SUM(pcr.ci_check_runs), 0) AS pr_ci_check_runs,
    COALESCE(SUM(pcr.ci_total_checks), 0) AS pr_ci_total_checks,
    COALESCE(SUM(pcr.ci_passed_checks), 0) AS pr_ci_passed_checks,
    COALESCE(SUM(pcr.ci_failed_checks), 0) AS pr_ci_failed_checks,
    COALESCE(SUM(pcr.ci_other_checks), 0) AS pr_ci_other_checks,

    COALESCE(AVG(pcr.ci_avg_passed_percent), 0) AS pr_ci_avg_passed_percent,
    COALESCE(AVG(pcr.ci_avg_failed_percent), 0) AS pr_ci_avg_failed_percent,
    COALESCE(AVG(pcr.ci_avg_other_percent), 0) AS pr_ci_avg_other_percent,
    COALESCE(AVG(pcr.ci_avg_duration_seconds), 0) AS pr_ci_avg_duration_seconds

FROM pr_commit_rows pcr

         LEFT JOIN pr_process pp
                   ON pp.project_owner = pcr.project_owner
                       AND pp.project_name = pcr.project_name
                       AND pp.pr_id = pcr.pr_id

GROUP BY
    pcr.project_owner,
    pcr.project_name,
    pcr.pr_id,
    pcr.pr_created_at,
    pcr.pr_merged_at,
    pcr.pr_review_hours,
    pp.pr_label_count,
    pp.pr_assignee_count,
    pp.pr_reviewer_count,
    pp.pr_timeline_event_count,
    pp.pr_reaction_count

HAVING
    COUNT(DISTINCT pcr.commit_sha) > 0

ORDER BY
    pcr.project_owner,
    pcr.project_name,
    pcr.pr_id;



select count(*) from action where  passed_percent>0


select * from commit where sha='7513996f20e34070141aa605fe282ca6986915a0'


#RQ3.7
    WITH
    commit_churn AS (
        SELECT
            cfc.commit_sha,
            COUNT(DISTINCT fc.id) AS num_files_changed,
            SUM(COALESCE(fc.total_additions, 0)) AS total_additions,
            SUM(COALESCE(fc.total_deletions, 0)) AS total_deletions,
            SUM(COALESCE(fc.total_changes, 0)) AS total_changes,
            AVG(COALESCE(fc.total_changes, 0)) AS avg_changes_per_file,
            MAX(COALESCE(fc.total_changes, 0)) AS max_changes_in_file
        FROM commit_file_changes cfc
        JOIN file_change fc
          ON fc.id = cfc.file_changes_id
        GROUP BY
            cfc.commit_sha
    ),

    ci_result_rows AS (
        SELECT
            a.commit_sha,
            LOWER(TRIM(a.result)) AS result_norm,
            a.started_at,
            a.completed_at
        FROM action a
        WHERE a.commit_sha IS NOT NULL
          AND a.result IS NOT NULL
          AND TRIM(a.result) <> ''
    ),

    ci_by_commit AS (
        SELECT
            r.commit_sha,

            COUNT(*) AS ci_check_runs,
            COUNT(*) AS ci_total_checks,

            SUM(CASE
                WHEN r.result_norm = 'success'
                THEN 1 ELSE 0
            END) AS ci_passed_checks,

            SUM(CASE
                WHEN r.result_norm IN (
                    'failure',
                    'failed',
                    'timed_out',
                    'startup_failure',
                    'action_required'
                )
                THEN 1 ELSE 0
            END) AS ci_failed_checks,

            SUM(CASE
                WHEN r.result_norm = 'cancelled'
                THEN 1 ELSE 0
            END) AS ci_cancelled_checks,

            SUM(CASE
                WHEN r.result_norm NOT IN (
                    'success',
                    'failure',
                    'failed',
                    'timed_out',
                    'startup_failure',
                    'action_required',
                    'cancelled'
                )
                THEN 1 ELSE 0
            END) AS ci_other_checks,

            ROUND(
                100.0 * SUM(CASE
                    WHEN r.result_norm = 'success'
                    THEN 1 ELSE 0
                END) / NULLIF(COUNT(*), 0),
                2
            ) AS ci_avg_passed_percent,

            ROUND(
                100.0 * SUM(CASE
                    WHEN r.result_norm IN (
                        'failure',
                        'failed',
                        'timed_out',
                        'startup_failure',
                        'action_required'
                    )
                    THEN 1 ELSE 0
                END) / NULLIF(COUNT(*), 0),
                2
            ) AS ci_avg_failed_percent,

            ROUND(
                100.0 * SUM(CASE
                    WHEN r.result_norm = 'cancelled'
                    THEN 1 ELSE 0
                END) / NULLIF(COUNT(*), 0),
                2
            ) AS ci_avg_cancelled_percent,

            ROUND(
                100.0 * SUM(CASE
                    WHEN r.result_norm NOT IN (
                        'success',
                        'failure',
                        'failed',
                        'timed_out',
                        'startup_failure',
                        'action_required',
                        'cancelled'
                    )
                    THEN 1 ELSE 0
                END) / NULLIF(COUNT(*), 0),
                2
            ) AS ci_avg_other_percent,

            AVG(
                CASE
                    WHEN r.started_at IS NOT NULL
                     AND r.completed_at IS NOT NULL
                     AND TIMESTAMPDIFF(SECOND, r.started_at, r.completed_at) >= 0
                    THEN TIMESTAMPDIFF(SECOND, r.started_at, r.completed_at)
                    ELSE NULL
                END
            ) AS ci_avg_duration_seconds

        FROM ci_result_rows r
        GROUP BY
            r.commit_sha
    ),

    bic_by_commit AS (
        SELECT
            pibic.bug_introducing_commits_sha AS commit_sha,
            COUNT(DISTINCT CONCAT(
                pibic.project_issue_project_owner, ':',
                pibic.project_issue_project_name, ':',
                pibic.project_issue_id
            )) AS linked_bic_issues
        FROM project_issue_bug_introducing_commits pibic
        GROUP BY
            pibic.bug_introducing_commits_sha
    ),

    pr_process AS (
        SELECT
            pp.project_owner,
            pp.project_name,
            pp.id AS pr_id,

            COUNT(DISTINCT ppl.labels) AS pr_label_count,
            COUNT(DISTINCT ppa.assignees) AS pr_assignee_count,
            COUNT(DISTINCT ppr.reviewers) AS pr_reviewer_count,
            COUNT(DISTINCT ppt.time_line_id) AS pr_timeline_event_count,
            COALESCE(MAX(r.total_count), 0) AS pr_reaction_count

        FROM project_pull pp

        LEFT JOIN project_pull_labels ppl
          ON ppl.project_pull_id = pp.id
         AND ppl.project_pull_project_name = pp.project_name
         AND ppl.project_pull_project_owner = pp.project_owner

        LEFT JOIN project_pull_assignees ppa
          ON ppa.project_pull_id = pp.id
         AND ppa.project_pull_project_name = pp.project_name
         AND ppa.project_pull_project_owner = pp.project_owner

        LEFT JOIN project_pull_reviewers ppr
          ON ppr.project_pull_id = pp.id
         AND ppr.project_pull_project_name = pp.project_name
         AND ppr.project_pull_project_owner = pp.project_owner

        LEFT JOIN project_pull_time_line ppt
          ON ppt.project_pull_id = pp.id
         AND ppt.project_pull_project_name = pp.project_name
         AND ppt.project_pull_project_owner = pp.project_owner

        LEFT JOIN reaction r
          ON r.id = pp.reaction_id

        WHERE pp.project_owner = 'ansible'

        GROUP BY
            pp.project_owner,
            pp.project_name,
            pp.id
    ),

    pr_commit_rows AS (
        SELECT
            pp.project_owner,
            pp.project_name,
            pp.id AS pr_id,
            pp.created_at AS pr_created_at,
            pp.merged_at AS pr_merged_at,

            TIMESTAMPDIFF(HOUR, pp.created_at, pp.merged_at) AS pr_review_hours,

            c.sha AS commit_sha,

            c.min_depth_of_commit_history,
            c.max_depth_of_commit_history,
            (
                c.max_depth_of_commit_history
                - c.min_depth_of_commit_history
            ) AS depth_diff,

            c.distance_to_branch_start,
            c.upstream_heads_unique_on_segment,
            c.days_since_last_merge_on_segment,
            c.in_degree,
            c.out_degree,
            c.number_of_branches,
            c.average_degree,

            CASE
                WHEN c.days_since_last_merge_on_segment IS NULL THEN NULL
                ELSE
                    1.0 * c.distance_to_branch_start
                    / GREATEST(c.days_since_last_merge_on_segment, 1)
            END AS branch_commit_rate,

            cc.num_files_changed,
            cc.total_additions,
            cc.total_deletions,
            cc.total_changes,
            cc.avg_changes_per_file,
            cc.max_changes_in_file,

            CASE
                WHEN cc.num_files_changed > 0
                    THEN 1.0 * cc.total_changes / cc.num_files_changed
                ELSE NULL
            END AS change_density_per_file,

            ci.ci_check_runs,
            ci.ci_total_checks,
            ci.ci_passed_checks,
            ci.ci_failed_checks,
            ci.ci_cancelled_checks,
            ci.ci_other_checks,
            ci.ci_avg_passed_percent,
            ci.ci_avg_failed_percent,
            ci.ci_avg_cancelled_percent,
            ci.ci_avg_other_percent,
            ci.ci_avg_duration_seconds,

            COALESCE(bic.linked_bic_issues, 0) AS linked_bic_issues,

            CASE
                WHEN c.min_depth_of_commit_history IS NOT NULL
                 AND c.max_depth_of_commit_history IS NOT NULL
                 AND c.distance_to_branch_start IS NOT NULL
                 AND c.upstream_heads_unique_on_segment IS NOT NULL
                 AND c.days_since_last_merge_on_segment IS NOT NULL
                 AND c.in_degree IS NOT NULL
                 AND c.out_degree IS NOT NULL
                 
                 AND c.average_degree IS NOT NULL
                THEN 1 ELSE 0
            END AS graph_ready,

            CASE
                WHEN cc.num_files_changed IS NOT NULL
                 AND cc.num_files_changed > 0
                 AND cc.total_changes IS NOT NULL
                THEN 1 ELSE 0
            END AS churn_ready,

            CASE
                WHEN ci.ci_check_runs IS NOT NULL
                 AND ci.ci_check_runs > 0
                THEN 1 ELSE 0
            END AS ci_ready

        FROM project_pull pp

        JOIN project_pull_commits ppc
          ON ppc.project_pull_id = pp.id
         AND ppc.project_pull_project_name = pp.project_name
         AND ppc.project_pull_project_owner = pp.project_owner

        JOIN `commit` c
          ON c.sha = ppc.commits_sha

        LEFT JOIN commit_churn cc
          ON cc.commit_sha = c.sha

        LEFT JOIN ci_by_commit ci
          ON ci.commit_sha = c.sha

        LEFT JOIN bic_by_commit bic
          ON bic.commit_sha = c.sha

        WHERE pp.project_owner = 'ansible'
          AND pp.created_at IS NOT NULL
          AND pp.merged_at IS NOT NULL
          AND TIMESTAMPDIFF(HOUR, pp.created_at, pp.merged_at) >= 0
    )

SELECT
    pcr.project_owner,
    pcr.project_name,
    pcr.pr_id,
    pcr.pr_created_at,
    pcr.pr_merged_at,

    /* Targets */
    pcr.pr_review_hours,
    LOG(1 + pcr.pr_review_hours) AS log_pr_review_hours,

    CASE
        WHEN MOD(CRC32(CONCAT(pcr.project_owner, ':', pcr.project_name, ':', pcr.pr_id)), 10) < 8
            THEN 'train'
        ELSE 'validation'
        END AS dataset_split,

    /* Process / social features */
    COALESCE(pp.pr_label_count, 0) AS pr_label_count,
    COALESCE(pp.pr_assignee_count, 0) AS pr_assignee_count,
    COALESCE(pp.pr_reviewer_count, 0) AS pr_reviewer_count,
    COALESCE(pp.pr_timeline_event_count, 0) AS pr_timeline_event_count,
    COALESCE(pp.pr_reaction_count, 0) AS pr_reaction_count,

    /* Evidence counts */
    COUNT(DISTINCT pcr.commit_sha) AS pr_num_commits,
    SUM(pcr.graph_ready) AS pr_graph_ready_commits,
    SUM(pcr.churn_ready) AS pr_churn_ready_commits,
    SUM(pcr.ci_ready) AS pr_ci_ready_commits,

    CASE
        WHEN SUM(CASE WHEN pcr.linked_bic_issues > 0 THEN 1 ELSE 0 END) > 0
            THEN 1
        ELSE 0
        END AS pr_contains_candidate_bic,

    SUM(CASE WHEN pcr.linked_bic_issues > 0 THEN 1 ELSE 0 END)
        AS pr_candidate_bic_commits,

    SUM(pcr.linked_bic_issues) AS pr_candidate_bic_issue_links,

    /* Graph-history features */
    COALESCE(AVG(CASE WHEN pcr.graph_ready = 1 THEN pcr.min_depth_of_commit_history END), 0)
        AS pr_avg_min_depth,

    COALESCE(AVG(CASE WHEN pcr.graph_ready = 1 THEN pcr.max_depth_of_commit_history END), 0)
        AS pr_avg_max_depth,

    COALESCE(AVG(CASE WHEN pcr.graph_ready = 1 THEN pcr.depth_diff END), 0)
        AS pr_avg_depth_diff,

    COALESCE(MAX(CASE WHEN pcr.graph_ready = 1 THEN pcr.depth_diff END), 0)
        AS pr_max_depth_diff,

    COALESCE(AVG(CASE WHEN pcr.graph_ready = 1 THEN pcr.branch_commit_rate END), 0)
        AS pr_avg_branch_commit_rate,

    COALESCE(MAX(CASE WHEN pcr.graph_ready = 1 THEN pcr.branch_commit_rate END), 0)
        AS pr_max_branch_commit_rate,

    COALESCE(AVG(CASE WHEN pcr.graph_ready = 1 THEN pcr.distance_to_branch_start END), 0)
        AS pr_avg_fp_distance,

    COALESCE(MAX(CASE WHEN pcr.graph_ready = 1 THEN pcr.distance_to_branch_start END), 0)
        AS pr_max_fp_distance,

    COALESCE(AVG(CASE WHEN pcr.graph_ready = 1 THEN pcr.upstream_heads_unique_on_segment END), 0)
        AS pr_avg_upstream_heads,

    COALESCE(MAX(CASE WHEN pcr.graph_ready = 1 THEN pcr.upstream_heads_unique_on_segment END), 0)
        AS pr_max_upstream_heads,

    COALESCE(AVG(CASE WHEN pcr.graph_ready = 1 THEN pcr.days_since_last_merge_on_segment END), 0)
        AS pr_avg_days_since_merge,

    COALESCE(MAX(CASE WHEN pcr.graph_ready = 1 THEN pcr.days_since_last_merge_on_segment END), 0)
        AS pr_max_days_since_merge,

    COALESCE(AVG(CASE WHEN pcr.graph_ready = 1 THEN pcr.in_degree END), 0)
        AS pr_avg_in_degree,

    COALESCE(AVG(CASE WHEN pcr.graph_ready = 1 THEN pcr.out_degree END), 0)
        AS pr_avg_out_degree,

    COALESCE(AVG(CASE WHEN pcr.graph_ready = 1 THEN pcr.number_of_branches END), 0)
        AS pr_avg_branches,

    COALESCE(AVG(CASE WHEN pcr.graph_ready = 1 THEN pcr.average_degree END), 0)
        AS pr_avg_average_degree,

    /* Churn features */
    COALESCE(SUM(pcr.total_additions), 0) AS pr_total_additions,
    COALESCE(SUM(pcr.total_deletions), 0) AS pr_total_deletions,
    COALESCE(SUM(pcr.total_changes), 0) AS pr_total_changes,

    COALESCE(AVG(CASE WHEN pcr.churn_ready = 1 THEN pcr.avg_changes_per_file END), 0)
        AS pr_avg_changes_per_file,

    COALESCE(MAX(CASE WHEN pcr.churn_ready = 1 THEN pcr.max_changes_in_file END), 0)
        AS pr_max_changes_in_file,

    COALESCE(SUM(pcr.num_files_changed), 0) AS pr_num_files_changed,

    CASE
        WHEN COALESCE(SUM(pcr.num_files_changed), 0) > 0
            THEN 1.0 * COALESCE(SUM(pcr.total_changes), 0)
            / COALESCE(SUM(pcr.num_files_changed), 0)
        ELSE 0
        END AS pr_change_density_per_file,

    /* Log-scaled count/churn features */
    LOG(1 + COUNT(DISTINCT pcr.commit_sha)) AS log_pr_num_commits,
    LOG(1 + COALESCE(SUM(pcr.total_additions), 0)) AS log_pr_total_additions,
    LOG(1 + COALESCE(SUM(pcr.total_deletions), 0)) AS log_pr_total_deletions,
    LOG(1 + COALESCE(SUM(pcr.total_changes), 0)) AS log_pr_total_changes,
    LOG(1 + COALESCE(SUM(pcr.num_files_changed), 0)) AS log_pr_num_files_changed,

    /* CI features computed from action.result values */
    COALESCE(SUM(pcr.ci_check_runs), 0) AS pr_ci_check_runs,
    COALESCE(SUM(pcr.ci_total_checks), 0) AS pr_ci_total_checks,
    COALESCE(SUM(pcr.ci_passed_checks), 0) AS pr_ci_passed_checks,
    COALESCE(SUM(pcr.ci_failed_checks), 0) AS pr_ci_failed_checks,
    COALESCE(SUM(pcr.ci_cancelled_checks), 0) AS pr_ci_cancelled_checks,
    COALESCE(SUM(pcr.ci_other_checks), 0) AS pr_ci_other_checks,

    CASE
        WHEN COALESCE(SUM(pcr.ci_total_checks), 0) > 0
            THEN ROUND(
                100.0 * COALESCE(SUM(pcr.ci_passed_checks), 0)
                    / COALESCE(SUM(pcr.ci_total_checks), 0),
                2
                 )
        ELSE 0
        END AS pr_ci_avg_passed_percent,

    CASE
        WHEN COALESCE(SUM(pcr.ci_total_checks), 0) > 0
            THEN ROUND(
                100.0 * COALESCE(SUM(pcr.ci_failed_checks), 0)
                    / COALESCE(SUM(pcr.ci_total_checks), 0),
                2
                 )
        ELSE 0
        END AS pr_ci_avg_failed_percent,

    CASE
        WHEN COALESCE(SUM(pcr.ci_total_checks), 0) > 0
            THEN ROUND(
                100.0 * COALESCE(SUM(pcr.ci_cancelled_checks), 0)
                    / COALESCE(SUM(pcr.ci_total_checks), 0),
                2
                 )
        ELSE 0
        END AS pr_ci_avg_cancelled_percent,

    CASE
        WHEN COALESCE(SUM(pcr.ci_total_checks), 0) > 0
            THEN ROUND(
                100.0 * COALESCE(SUM(pcr.ci_other_checks), 0)
                    / COALESCE(SUM(pcr.ci_total_checks), 0),
                2
                 )
        ELSE 0
        END AS pr_ci_avg_other_percent,

    COALESCE(AVG(pcr.ci_avg_duration_seconds), 0) AS pr_ci_avg_duration_seconds

FROM pr_commit_rows pcr

         LEFT JOIN pr_process pp
                   ON pp.project_owner = pcr.project_owner
                       AND pp.project_name = pcr.project_name
                       AND pp.pr_id = pcr.pr_id

GROUP BY
    pcr.project_owner,
    pcr.project_name,
    pcr.pr_id,
    pcr.pr_created_at,
    pcr.pr_merged_at,
    pcr.pr_review_hours,
    pp.pr_label_count,
    pp.pr_assignee_count,
    pp.pr_reviewer_count,
    pp.pr_timeline_event_count,
    pp.pr_reaction_count

HAVING
    COUNT(DISTINCT pcr.commit_sha) > 0

ORDER BY
    pcr.project_owner,
    pcr.project_name,
    pcr.pr_id;



#RQ3.8
WITH
    commit_churn AS (
        SELECT
            cfc.commit_sha,
            COUNT(DISTINCT fc.id) AS num_files_changed,
            SUM(COALESCE(fc.total_additions, 0)) AS total_additions,
            SUM(COALESCE(fc.total_deletions, 0)) AS total_deletions,
            SUM(COALESCE(fc.total_changes, 0)) AS total_changes,
            AVG(COALESCE(fc.total_changes, 0)) AS avg_changes_per_file,
            MAX(COALESCE(fc.total_changes, 0)) AS max_changes_in_file
        FROM commit_file_changes cfc
                 JOIN file_change fc
                      ON fc.id = cfc.file_changes_id
        GROUP BY
            cfc.commit_sha
    ),

    ci_result_rows AS (
        SELECT
            a.commit_sha,
            LOWER(TRIM(a.result)) AS result_norm,
            a.started_at,
            a.completed_at
        FROM action a
        WHERE a.commit_sha IS NOT NULL
          AND a.result IS NOT NULL
          AND TRIM(a.result) <> ''
    ),

    ci_by_commit AS (
        SELECT
            r.commit_sha,

            COUNT(*) AS ci_check_runs,
            COUNT(*) AS ci_total_checks,

            SUM(CASE
                    WHEN r.result_norm = 'success'
                        THEN 1 ELSE 0
                END) AS ci_passed_checks,

            SUM(CASE
                    WHEN r.result_norm IN (
                                           'failure',
                                           'failed',
                                           'timed_out',
                                           'startup_failure',
                                           'action_required'
                        )
                        THEN 1 ELSE 0
                END) AS ci_failed_checks,

            SUM(CASE
                    WHEN r.result_norm = 'cancelled'
                        THEN 1 ELSE 0
                END) AS ci_cancelled_checks,

            SUM(CASE
                    WHEN r.result_norm NOT IN (
                                               'success',
                                               'failure',
                                               'failed',
                                               'timed_out',
                                               'startup_failure',
                                               'action_required',
                                               'cancelled'
                        )
                        THEN 1 ELSE 0
                END) AS ci_other_checks,

            AVG(
                    CASE
                        WHEN r.started_at IS NOT NULL
                            AND r.completed_at IS NOT NULL
                            AND TIMESTAMPDIFF(SECOND, r.started_at, r.completed_at) >= 0
                            THEN TIMESTAMPDIFF(SECOND, r.started_at, r.completed_at)
                        ELSE NULL
                        END
            ) AS ci_avg_duration_seconds

        FROM ci_result_rows r
        GROUP BY
            r.commit_sha
    ),

    bic_by_commit AS (
        SELECT
            pibic.bug_introducing_commits_sha AS commit_sha,
            COUNT(DISTINCT CONCAT(
                    pibic.project_issue_project_owner, ':',
                    pibic.project_issue_project_name, ':',
                    pibic.project_issue_id
                           )) AS linked_bic_issues
        FROM project_issue_bug_introducing_commits pibic
        GROUP BY
            pibic.bug_introducing_commits_sha
    ),

    pr_process AS (
        SELECT
            pp.project_owner,
            pp.project_name,
            pp.id AS pr_id,

            COUNT(DISTINCT ppl.labels) AS pr_label_count,
            COUNT(DISTINCT ppa.assignees) AS pr_assignee_count,
            COUNT(DISTINCT ppr.reviewers) AS pr_reviewer_count,
            COUNT(DISTINCT ppt.time_line_id) AS pr_timeline_event_count,
            COALESCE(MAX(r.total_count), 0) AS pr_reaction_count

        FROM project_pull pp

                 LEFT JOIN project_pull_labels ppl
                           ON ppl.project_pull_id = pp.id
                               AND ppl.project_pull_project_name = pp.project_name
                               AND ppl.project_pull_project_owner = pp.project_owner

                 LEFT JOIN project_pull_assignees ppa
                           ON ppa.project_pull_id = pp.id
                               AND ppa.project_pull_project_name = pp.project_name
                               AND ppa.project_pull_project_owner = pp.project_owner

                 LEFT JOIN project_pull_reviewers ppr
                           ON ppr.project_pull_id = pp.id
                               AND ppr.project_pull_project_name = pp.project_name
                               AND ppr.project_pull_project_owner = pp.project_owner

                 LEFT JOIN project_pull_time_line ppt
                           ON ppt.project_pull_id = pp.id
                               AND ppt.project_pull_project_name = pp.project_name
                               AND ppt.project_pull_project_owner = pp.project_owner

                 LEFT JOIN reaction r
                           ON r.id = pp.reaction_id

        WHERE pp.project_owner = 'facebook'
          AND pp.project_name = 'react'

GROUP BY
    pp.project_owner,
    pp.project_name,
    pp.id
    ),

    pr_commit_rows AS (
SELECT
    pp.project_owner,
    pp.project_name,
    pp.id AS pr_id,
    pp.created_at AS pr_created_at,
    pp.merged_at AS pr_merged_at,

    TIMESTAMPDIFF(SECOND, pp.created_at, pp.merged_at) / 3600.0
    AS pr_review_hours,

    c.sha AS commit_sha,

    c.min_depth_of_commit_history,
    c.max_depth_of_commit_history,

    (
    c.max_depth_of_commit_history
    - c.min_depth_of_commit_history
    ) AS depth_diff,

    c.distance_to_branch_start,
    c.upstream_heads_unique_on_segment,
    c.days_since_last_merge_on_segment,
    c.in_degree,
    c.out_degree,
    c.number_of_branches,
    c.average_degree,

    CASE
    WHEN c.days_since_last_merge_on_segment IS NULL
    OR c.distance_to_branch_start IS NULL
    THEN NULL
    ELSE
    1.0 * c.distance_to_branch_start
    / GREATEST(c.days_since_last_merge_on_segment, 1)
    END AS branch_commit_rate,

    cc.num_files_changed,
    cc.total_additions,
    cc.total_deletions,
    cc.total_changes,
    cc.avg_changes_per_file,
    cc.max_changes_in_file,

    CASE
    WHEN cc.num_files_changed > 0
    THEN 1.0 * cc.total_changes / cc.num_files_changed
    ELSE NULL
    END AS change_density_per_file,

    ci.ci_check_runs,
    ci.ci_total_checks,
    ci.ci_passed_checks,
    ci.ci_failed_checks,
    ci.ci_cancelled_checks,
    ci.ci_other_checks,
    ci.ci_avg_duration_seconds,

    COALESCE(bic.linked_bic_issues, 0) AS linked_bic_issues,

    CASE
    WHEN c.min_depth_of_commit_history IS NOT NULL
    AND c.max_depth_of_commit_history IS NOT NULL
    AND c.distance_to_branch_start IS NOT NULL
    AND c.upstream_heads_unique_on_segment IS NOT NULL
    AND c.days_since_last_merge_on_segment IS NOT NULL
    AND c.in_degree IS NOT NULL
    AND c.out_degree IS NOT NULL
    
    AND c.average_degree IS NOT NULL
    THEN 1 ELSE 0
    END AS graph_ready,

    CASE
    WHEN cc.num_files_changed IS NOT NULL
    AND cc.num_files_changed > 0
    AND cc.total_changes IS NOT NULL
    THEN 1 ELSE 0
    END AS churn_ready,

    CASE
    WHEN ci.ci_check_runs IS NOT NULL
    AND ci.ci_check_runs > 0
    THEN 1 ELSE 0
    END AS ci_ready

FROM project_pull pp

    JOIN project_pull_commits ppc
ON ppc.project_pull_id = pp.id
    AND ppc.project_pull_project_name = pp.project_name
    AND ppc.project_pull_project_owner = pp.project_owner

    JOIN `commit` c
    ON c.sha = ppc.commits_sha

    LEFT JOIN commit_churn cc
    ON cc.commit_sha = c.sha

    LEFT JOIN ci_by_commit ci
    ON ci.commit_sha = c.sha

    LEFT JOIN bic_by_commit bic
    ON bic.commit_sha = c.sha

WHERE pp.project_owner = 'facebook'
  AND pp.project_name = 'react'
  AND pp.created_at IS NOT NULL
  AND pp.merged_at IS NOT NULL
  AND TIMESTAMPDIFF(SECOND, pp.created_at, pp.merged_at) >= 0
    )

SELECT
    pcr.project_owner,
    pcr.project_name,
    pcr.pr_id,
    pcr.pr_created_at,
    pcr.pr_merged_at,

    /* Target variables */
    pcr.pr_review_hours,
    LOG(1 + pcr.pr_review_hours) AS log_pr_review_hours,

    CASE
        WHEN MOD(CRC32(CONCAT(pcr.project_owner, ':', pcr.project_name, ':', pcr.pr_id)), 10) < 8
            THEN 'train'
        ELSE 'validation'
        END AS dataset_split,

    /* Process / social features */
    COALESCE(pp.pr_label_count, 0) AS pr_label_count,
    COALESCE(pp.pr_assignee_count, 0) AS pr_assignee_count,
    COALESCE(pp.pr_reviewer_count, 0) AS pr_reviewer_count,
    COALESCE(pp.pr_timeline_event_count, 0) AS pr_timeline_event_count,
    COALESCE(pp.pr_reaction_count, 0) AS pr_reaction_count,

    /* Evidence-count features */
    COUNT(DISTINCT pcr.commit_sha) AS pr_num_commits,
    SUM(pcr.graph_ready) AS pr_graph_ready_commits,
    SUM(pcr.churn_ready) AS pr_churn_ready_commits,
    SUM(pcr.ci_ready) AS pr_ci_ready_commits,

    ROUND(
            1.0 * SUM(pcr.graph_ready) / NULLIF(COUNT(DISTINCT pcr.commit_sha), 0),
            4
    ) AS pr_graph_ready_fraction,

    ROUND(
            1.0 * SUM(pcr.churn_ready) / NULLIF(COUNT(DISTINCT pcr.commit_sha), 0),
            4
    ) AS pr_churn_ready_fraction,

    ROUND(
            1.0 * SUM(pcr.ci_ready) / NULLIF(COUNT(DISTINCT pcr.commit_sha), 0),
            4
    ) AS pr_ci_ready_fraction,

    /* Candidate defect-provenance features */
    CASE
        WHEN SUM(CASE WHEN pcr.linked_bic_issues > 0 THEN 1 ELSE 0 END) > 0
            THEN 1
        ELSE 0
        END AS pr_contains_candidate_bic,

    SUM(CASE WHEN pcr.linked_bic_issues > 0 THEN 1 ELSE 0 END)
        AS pr_candidate_bic_commits,

    SUM(pcr.linked_bic_issues) AS pr_candidate_bic_issue_links,

    /* Graph-history features */
    AVG(CASE WHEN pcr.graph_ready = 1 THEN pcr.min_depth_of_commit_history END)
        AS pr_avg_min_depth,

    AVG(CASE WHEN pcr.graph_ready = 1 THEN pcr.max_depth_of_commit_history END)
        AS pr_avg_max_depth,

    AVG(CASE WHEN pcr.graph_ready = 1 THEN pcr.depth_diff END)
        AS pr_avg_depth_diff,

    MAX(CASE WHEN pcr.graph_ready = 1 THEN pcr.depth_diff END)
        AS pr_max_depth_diff,

    AVG(CASE WHEN pcr.graph_ready = 1 THEN pcr.branch_commit_rate END)
        AS pr_avg_branch_commit_rate,

    MAX(CASE WHEN pcr.graph_ready = 1 THEN pcr.branch_commit_rate END)
        AS pr_max_branch_commit_rate,

    AVG(CASE WHEN pcr.graph_ready = 1 THEN pcr.distance_to_branch_start END)
        AS pr_avg_fp_distance,

    MAX(CASE WHEN pcr.graph_ready = 1 THEN pcr.distance_to_branch_start END)
        AS pr_max_fp_distance,

    AVG(CASE WHEN pcr.graph_ready = 1 THEN pcr.upstream_heads_unique_on_segment END)
        AS pr_avg_upstream_heads,

    MAX(CASE WHEN pcr.graph_ready = 1 THEN pcr.upstream_heads_unique_on_segment END)
        AS pr_max_upstream_heads,

    AVG(CASE WHEN pcr.graph_ready = 1 THEN pcr.days_since_last_merge_on_segment END)
        AS pr_avg_days_since_merge,

    MAX(CASE WHEN pcr.graph_ready = 1 THEN pcr.days_since_last_merge_on_segment END)
        AS pr_max_days_since_merge,

    AVG(CASE WHEN pcr.graph_ready = 1 THEN pcr.in_degree END)
        AS pr_avg_in_degree,

    AVG(CASE WHEN pcr.graph_ready = 1 THEN pcr.out_degree END)
        AS pr_avg_out_degree,

    AVG(CASE WHEN pcr.graph_ready = 1 THEN pcr.number_of_branches END)
        AS pr_avg_branches,

    AVG(CASE WHEN pcr.graph_ready = 1 THEN pcr.average_degree END)
        AS pr_avg_average_degree,

    /* Churn features */
    SUM(COALESCE(pcr.total_additions, 0)) AS pr_total_additions,
    SUM(COALESCE(pcr.total_deletions, 0)) AS pr_total_deletions,
    SUM(COALESCE(pcr.total_changes, 0)) AS pr_total_changes,

    AVG(CASE WHEN pcr.churn_ready = 1 THEN pcr.avg_changes_per_file END)
        AS pr_avg_changes_per_file,

    MAX(CASE WHEN pcr.churn_ready = 1 THEN pcr.max_changes_in_file END)
        AS pr_max_changes_in_file,

    SUM(COALESCE(pcr.num_files_changed, 0)) AS pr_num_files_changed,

    CASE
        WHEN SUM(COALESCE(pcr.num_files_changed, 0)) > 0
            THEN
            1.0 * SUM(COALESCE(pcr.total_changes, 0))
                / SUM(COALESCE(pcr.num_files_changed, 0))
        ELSE NULL
        END AS pr_change_density_per_file,

    /* Log-scaled count/churn features */
    LOG(1 + COUNT(DISTINCT pcr.commit_sha)) AS log_pr_num_commits,
    LOG(1 + SUM(COALESCE(pcr.total_additions, 0))) AS log_pr_total_additions,
    LOG(1 + SUM(COALESCE(pcr.total_deletions, 0))) AS log_pr_total_deletions,
    LOG(1 + SUM(COALESCE(pcr.total_changes, 0))) AS log_pr_total_changes,
    LOG(1 + SUM(COALESCE(pcr.num_files_changed, 0))) AS log_pr_num_files_changed,

    /* CI features computed from action.result values */
    SUM(COALESCE(pcr.ci_check_runs, 0)) AS pr_ci_check_runs,
    SUM(COALESCE(pcr.ci_total_checks, 0)) AS pr_ci_total_checks,
    SUM(COALESCE(pcr.ci_passed_checks, 0)) AS pr_ci_passed_checks,
    SUM(COALESCE(pcr.ci_failed_checks, 0)) AS pr_ci_failed_checks,
    SUM(COALESCE(pcr.ci_cancelled_checks, 0)) AS pr_ci_cancelled_checks,
    SUM(COALESCE(pcr.ci_other_checks, 0)) AS pr_ci_other_checks,

    CASE
        WHEN SUM(COALESCE(pcr.ci_total_checks, 0)) > 0
            THEN ROUND(
                100.0 * SUM(COALESCE(pcr.ci_passed_checks, 0))
                    / SUM(COALESCE(pcr.ci_total_checks, 0)),
                2
                 )
        ELSE NULL
        END AS pr_ci_passed_percent,

    CASE
        WHEN SUM(COALESCE(pcr.ci_total_checks, 0)) > 0
            THEN ROUND(
                100.0 * SUM(COALESCE(pcr.ci_failed_checks, 0))
                    / SUM(COALESCE(pcr.ci_total_checks, 0)),
                2
                 )
        ELSE NULL
        END AS pr_ci_failed_percent,

    CASE
        WHEN SUM(COALESCE(pcr.ci_total_checks, 0)) > 0
            THEN ROUND(
                100.0 * SUM(COALESCE(pcr.ci_cancelled_checks, 0))
                    / SUM(COALESCE(pcr.ci_total_checks, 0)),
                2
                 )
        ELSE NULL
        END AS pr_ci_cancelled_percent,

    CASE
        WHEN SUM(COALESCE(pcr.ci_total_checks, 0)) > 0
            THEN ROUND(
                100.0 * SUM(COALESCE(pcr.ci_other_checks, 0))
                    / SUM(COALESCE(pcr.ci_total_checks, 0)),
                2
                 )
        ELSE NULL
        END AS pr_ci_other_percent,

    AVG(CASE WHEN pcr.ci_ready = 1 THEN pcr.ci_avg_duration_seconds END)
        AS pr_ci_avg_duration_seconds

FROM pr_commit_rows pcr

         LEFT JOIN pr_process pp
                   ON pp.project_owner = pcr.project_owner
                       AND pp.project_name = pcr.project_name
                       AND pp.pr_id = pcr.pr_id

GROUP BY
    pcr.project_owner,
    pcr.project_name,
    pcr.pr_id,
    pcr.pr_created_at,
    pcr.pr_merged_at,
    pcr.pr_review_hours,
    pp.pr_label_count,
    pp.pr_assignee_count,
    pp.pr_reviewer_count,
    pp.pr_timeline_event_count,
    pp.pr_reaction_count

HAVING
    COUNT(DISTINCT pcr.commit_sha) > 0

ORDER BY
    pcr.project_owner,
    pcr.project_name,
    pcr.pr_id;




#misc
WITH target_projects AS (
    SELECT 'ansible' AS project_owner, 'ansible' AS project_name
    UNION ALL
    SELECT 'facebook' AS project_owner, 'react' AS project_name
),

     commit_counts AS (
         SELECT
             c.project_owner,
             c.project_name,
             COUNT(DISTINCT c.sha) AS commits
         FROM `commit` c
                  JOIN target_projects tp
                       ON tp.project_owner = c.project_owner
                           AND tp.project_name  = c.project_name
         GROUP BY c.project_owner, c.project_name
     ),

     pr_counts AS (
         SELECT
             pp.project_owner,
             pp.project_name,
             COUNT(DISTINCT pp.id) AS pull_requests
         FROM project_pull pp
                  JOIN target_projects tp
                       ON tp.project_owner = pp.project_owner
                           AND tp.project_name  = pp.project_name
         GROUP BY pp.project_owner, pp.project_name
     ),

     issue_counts AS (
         SELECT
             pi.project_owner,
             pi.project_name,
             COUNT(DISTINCT pi.id) AS issues
         FROM project_issue pi
                  JOIN target_projects tp
                       ON tp.project_owner = pi.project_owner
                           AND tp.project_name  = pi.project_name
         GROUP BY pi.project_owner, pi.project_name
     ),

     timeline_counts AS (
         SELECT
             t.project_owner,
             t.project_name,
             COUNT(DISTINCT t.id) AS timeline_events
         FROM (
                  SELECT
                      tl.id,
                      tl.project_pull_project_owner AS project_owner,
                      tl.project_pull_project_name  AS project_name
                  FROM timeline tl
                  WHERE tl.project_pull_id IS NOT NULL

                  UNION

                  SELECT
                      tl.id,
                      tl.project_issue_project_owner AS project_owner,
                      tl.project_issue_project_name  AS project_name
                  FROM timeline tl
                  WHERE tl.project_issue_id IS NOT NULL
              ) t
                  JOIN target_projects tp
                       ON tp.project_owner = t.project_owner
                           AND tp.project_name  = t.project_name
         GROUP BY t.project_owner, t.project_name
     ),

     file_change_counts AS (
         SELECT
             c.project_owner,
             c.project_name,
             COUNT(DISTINCT fc.id) AS file_changes
         FROM `commit` c
                  JOIN commit_file_changes cfc
                       ON cfc.commit_sha = c.sha
                  JOIN file_change fc
                       ON fc.id = cfc.file_changes_id
                  JOIN target_projects tp
                       ON tp.project_owner = c.project_owner
                           AND tp.project_name  = c.project_name
         GROUP BY c.project_owner, c.project_name
     ),

     ci_counts AS (
         SELECT
             c.project_owner,
             c.project_name,

             /* Number of persisted CI/action records. */
             COUNT(DISTINCT a.id) AS ci_action_records,

             /* Number of underlying check runs if action.total stores the run count. */
             COALESCE(SUM(a.total), 0) AS ci_check_runs
         FROM `commit` c
                  JOIN action a
                       ON a.commit_sha = c.sha
                  JOIN target_projects tp
                       ON tp.project_owner = c.project_owner
                           AND tp.project_name  = c.project_name
         GROUP BY c.project_owner, c.project_name
     ),

     pr_issue_links AS (
         SELECT
             pppi.project_pull_project_owner AS project_owner,
             pppi.project_pull_project_name  AS project_name,
             pppi.project_pull_id            AS pr_id,
             pppi.project_issue_id           AS issue_id
         FROM project_pull_project_issue pppi

         UNION

         SELECT
             pppi2.project_pull_project_owner AS project_owner,
             pppi2.project_pull_project_name  AS project_name,
             pppi2.project_pull_id            AS pr_id,
             pppi2.project_issues_id          AS issue_id
         FROM project_pull_project_issues pppi2

         UNION

         SELECT
             pipp.project_pull_project_owner AS project_owner,
             pipp.project_pull_project_name  AS project_name,
             pipp.project_pull_id            AS pr_id,
             pipp.project_issue_id           AS issue_id
         FROM project_issue_project_pull pipp

         UNION

         SELECT
             pi.fixpr_project_owner AS project_owner,
             pi.fixpr_project_name  AS project_name,
             pi.fixpr_id            AS pr_id,
             pi.id                  AS issue_id
         FROM project_issue pi
         WHERE pi.fixpr_id IS NOT NULL
     ),

     pr_issue_link_counts AS (
         SELECT
             l.project_owner,
             l.project_name,
             COUNT(DISTINCT CONCAT(l.project_owner, '/', l.project_name, '#PR', l.pr_id, '-ISSUE', l.issue_id))
                 AS linked_pr_issue_pairs
         FROM pr_issue_links l
                  JOIN target_projects tp
                       ON tp.project_owner = l.project_owner
                           AND tp.project_name  = l.project_name
         GROUP BY l.project_owner, l.project_name
     ),

     fixing_commit_counts AS (
         SELECT
             pifc.project_issue_project_owner AS project_owner,
             pifc.project_issue_project_name  AS project_name,
             COUNT(DISTINCT pifc.fixing_commits_sha) AS linked_bug_fixing_commits
         FROM project_issue_fixing_commits pifc
                  JOIN target_projects tp
                       ON tp.project_owner = pifc.project_issue_project_owner
                           AND tp.project_name  = pifc.project_issue_project_name
         GROUP BY
             pifc.project_issue_project_owner,
             pifc.project_issue_project_name
     ),

     bic_counts AS (
         SELECT
             pibic.project_issue_project_owner AS project_owner,
             pibic.project_issue_project_name  AS project_name,
             COUNT(DISTINCT pibic.bug_introducing_commits_sha) AS candidate_bug_introducing_commits
         FROM project_issue_bug_introducing_commits pibic
                  JOIN target_projects tp
                       ON tp.project_owner = pibic.project_issue_project_owner
                           AND tp.project_name  = pibic.project_issue_project_name
         GROUP BY
             pibic.project_issue_project_owner,
             pibic.project_issue_project_name
     ),

     pr_modeling_flags AS (
         SELECT
             pp.project_owner,
             pp.project_name,
             pp.id AS pr_id,
             pp.state,
             pp.created_at,
             pp.merged_at,
             TIMESTAMPDIFF(SECOND, pp.created_at, pp.merged_at) AS review_seconds,

             COUNT(DISTINCT ppc.commits_sha) AS linked_commits,

             COUNT(DISTINCT CASE
                                WHEN c.max_depth_of_commit_history IS NOT NULL
                                    AND c.max_depth_of_commit_history > 0
                                    THEN c.sha
                 END) AS graph_ready_commits,

             COUNT(DISTINCT cfc.file_changes_id) AS linked_file_changes
         FROM project_pull pp
                  LEFT JOIN project_pull_commits ppc
                            ON ppc.project_pull_id            = pp.id
                                AND ppc.project_pull_project_name  = pp.project_name
                                AND ppc.project_pull_project_owner = pp.project_owner
                  LEFT JOIN `commit` c
                            ON c.sha = ppc.commits_sha
                  LEFT JOIN commit_file_changes cfc
                            ON cfc.commit_sha = c.sha
                  JOIN target_projects tp
                       ON tp.project_owner = pp.project_owner
                           AND tp.project_name  = pp.project_name
         GROUP BY
             pp.project_owner,
             pp.project_name,
             pp.id,
             pp.state,
             pp.created_at,
             pp.merged_at
     ),

     modeling_counts AS (
         SELECT
             project_owner,
             project_name,
             COUNT(*) AS modeling_rows
         FROM pr_modeling_flags
         WHERE state = 'closed'
           AND created_at IS NOT NULL
           AND merged_at IS NOT NULL
           AND review_seconds > 0
           AND linked_commits > 0
           AND graph_ready_commits > 0
           AND linked_file_changes > 0
         GROUP BY project_owner, project_name
     )

SELECT
    CONCAT(tp.project_owner, '/', tp.project_name) AS project,

    COALESCE(cc.commits, 0) AS commits,
    COALESCE(pc.pull_requests, 0) AS pull_requests,
    COALESCE(ic.issues, 0) AS issues,
    COALESCE(tc.timeline_events, 0) AS timeline_events,
    COALESCE(fcc.file_changes, 0) AS file_changes,

    /* Use this if the paper reports persisted CI/action entities. */
    COALESCE(cic.ci_action_records, 0) AS ci_action_records,

    /* Use this if the paper reports underlying CI check runs. */
    COALESCE(cic.ci_check_runs, 0) AS ci_check_runs,

    COALESCE(pilc.linked_pr_issue_pairs, 0) AS linked_pr_issue_pairs,
    COALESCE(fixc.linked_bug_fixing_commits, 0) AS linked_bug_fixing_commits,
    COALESCE(bicc.candidate_bug_introducing_commits, 0) AS candidate_bug_introducing_commits,
    COALESCE(mc.modeling_rows, 0) AS modeling_rows

FROM target_projects tp
         LEFT JOIN commit_counts cc
                   ON cc.project_owner = tp.project_owner
                       AND cc.project_name  = tp.project_name
         LEFT JOIN pr_counts pc
                   ON pc.project_owner = tp.project_owner
                       AND pc.project_name  = tp.project_name
         LEFT JOIN issue_counts ic
                   ON ic.project_owner = tp.project_owner
                       AND ic.project_name  = tp.project_name
         LEFT JOIN timeline_counts tc
                   ON tc.project_owner = tp.project_owner
                       AND tc.project_name  = tp.project_name
         LEFT JOIN file_change_counts fcc
                   ON fcc.project_owner = tp.project_owner
                       AND fcc.project_name  = tp.project_name
         LEFT JOIN ci_counts cic
                   ON cic.project_owner = tp.project_owner
                       AND cic.project_name  = tp.project_name
         LEFT JOIN pr_issue_link_counts pilc
                   ON pilc.project_owner = tp.project_owner
                       AND pilc.project_name  = tp.project_name
         LEFT JOIN fixing_commit_counts fixc
                   ON fixc.project_owner = tp.project_owner
                       AND fixc.project_name  = tp.project_name
         LEFT JOIN bic_counts bicc
                   ON bicc.project_owner = tp.project_owner
                       AND bicc.project_name  = tp.project_name
         LEFT JOIN modeling_counts mc
                   ON mc.project_owner = tp.project_owner
                       AND mc.project_name  = tp.project_name
ORDER BY project;



#misc1

WITH target_projects AS (
    SELECT 'ansible' AS project_owner, 'ansible' AS project_name
    UNION ALL
    SELECT 'facebook' AS project_owner, 'react' AS project_name
),

     pr_modeling_flags AS (
         SELECT
             pp.project_owner,
             pp.project_name,
             pp.id AS pr_id,
             pp.state,
             pp.created_at,
             pp.merged_at,
             TIMESTAMPDIFF(SECOND, pp.created_at, pp.merged_at) AS review_seconds,

             COUNT(DISTINCT ppc.commits_sha) AS linked_commits,

             COUNT(DISTINCT CASE
                                WHEN c.max_depth_of_commit_history IS NOT NULL
                                    AND c.max_depth_of_commit_history > 0
                                    THEN c.sha
                 END) AS graph_ready_commits,

             COUNT(DISTINCT cfc.file_changes_id) AS linked_file_changes
         FROM project_pull pp
                  LEFT JOIN project_pull_commits ppc
                            ON ppc.project_pull_id            = pp.id
                                AND ppc.project_pull_project_name  = pp.project_name
                                AND ppc.project_pull_project_owner = pp.project_owner
                  LEFT JOIN `commit` c
                            ON c.sha = ppc.commits_sha
                  LEFT JOIN commit_file_changes cfc
                            ON cfc.commit_sha = c.sha
                  JOIN target_projects tp
                       ON tp.project_owner = pp.project_owner
                           AND tp.project_name  = pp.project_name
         GROUP BY
             pp.project_owner,
             pp.project_name,
             pp.id,
             pp.state,
             pp.created_at,
             pp.merged_at
     ),

     classified AS (
         SELECT
             project_owner,
             project_name,
             pr_id,
             CASE
                 WHEN state <> 'closed' OR state IS NULL
                     THEN 'excluded: PR not closed'
                 WHEN created_at IS NULL
                     THEN 'excluded: missing created_at'
                 WHEN merged_at IS NULL
                     THEN 'excluded: not merged or missing merged_at'
                 WHEN review_seconds IS NULL OR review_seconds <= 0
                     THEN 'excluded: non-positive review time'
                 WHEN linked_commits = 0
                     THEN 'excluded: no linked commits'
                 WHEN graph_ready_commits = 0
                     THEN 'excluded: no graph-ready commits'
                 WHEN linked_file_changes = 0
                     THEN 'excluded: no linked file changes'
                 ELSE 'retained for modeling'
                 END AS modeling_status
         FROM pr_modeling_flags
     )

SELECT
    CONCAT(project_owner, '/', project_name) AS project,
    modeling_status,
    COUNT(*) AS pull_requests
FROM classified
GROUP BY
    project_owner,
    project_name,
    modeling_status
ORDER BY
    project,
    CASE modeling_status
        WHEN 'retained for modeling' THEN 0
        WHEN 'excluded: PR not closed' THEN 1
        WHEN 'excluded: missing created_at' THEN 2
        WHEN 'excluded: not merged or missing merged_at' THEN 3
        WHEN 'excluded: non-positive review time' THEN 4
        WHEN 'excluded: no linked commits' THEN 5
        WHEN 'excluded: no graph-ready commits' THEN 6
        WHEN 'excluded: no linked file changes' THEN 7
        ELSE 99
        END;



#misc2

WITH target_projects AS (
    SELECT 'ansible' AS project_owner, 'ansible' AS project_name
    UNION ALL
    SELECT 'facebook' AS project_owner, 'react' AS project_name
),

     pr_modeling_flags AS (
         SELECT
             pp.project_owner,
             pp.project_name,
             pp.id AS pr_id,
             pp.state,
             pp.created_at,
             pp.merged_at,
             TIMESTAMPDIFF(SECOND, pp.created_at, pp.merged_at) AS review_seconds,

             COUNT(DISTINCT ppc.commits_sha) AS linked_commits,

             COUNT(DISTINCT CASE
                                WHEN c.max_depth_of_commit_history IS NOT NULL
                                    AND c.max_depth_of_commit_history > 0
                                    THEN c.sha
                 END) AS graph_ready_commits,

             COUNT(DISTINCT cfc.file_changes_id) AS linked_file_changes
         FROM project_pull pp
                  LEFT JOIN project_pull_commits ppc
                            ON ppc.project_pull_id            = pp.id
                                AND ppc.project_pull_project_name  = pp.project_name
                                AND ppc.project_pull_project_owner = pp.project_owner
                  LEFT JOIN `commit` c
                            ON c.sha = ppc.commits_sha
                  LEFT JOIN commit_file_changes cfc
                            ON cfc.commit_sha = c.sha
                  JOIN target_projects tp
                       ON tp.project_owner = pp.project_owner
                           AND tp.project_name  = pp.project_name
         GROUP BY
             pp.project_owner,
             pp.project_name,
             pp.id,
             pp.state,
             pp.created_at,
             pp.merged_at
     )

SELECT
    CONCAT(project_owner, '/', project_name) AS project,

    COUNT(*) AS total_prs,

    SUM(CASE WHEN state <> 'closed' OR state IS NULL THEN 1 ELSE 0 END)
                                             AS not_closed_prs,

    SUM(CASE WHEN created_at IS NULL THEN 1 ELSE 0 END)
                                             AS missing_created_at_prs,

    SUM(CASE WHEN merged_at IS NULL THEN 1 ELSE 0 END)
                                             AS missing_merged_at_or_unmerged_prs,

    SUM(CASE WHEN review_seconds IS NULL OR review_seconds <= 0 THEN 1 ELSE 0 END)
                                             AS non_positive_review_time_prs,

    SUM(CASE WHEN linked_commits = 0 THEN 1 ELSE 0 END)
                                             AS prs_without_linked_commits,

    SUM(CASE WHEN graph_ready_commits = 0 THEN 1 ELSE 0 END)
                                             AS prs_without_graph_ready_commits,

    SUM(CASE WHEN linked_file_changes = 0 THEN 1 ELSE 0 END)
                                             AS prs_without_linked_file_changes,

    SUM(CASE
            WHEN state = 'closed'
                AND created_at IS NOT NULL
                AND merged_at IS NOT NULL
                AND review_seconds > 0
                AND linked_commits > 0
                AND graph_ready_commits > 0
                AND linked_file_changes > 0
                THEN 1 ELSE 0
        END) AS retained_for_modeling

FROM pr_modeling_flags
GROUP BY project_owner, project_name
ORDER BY project;





#RQ2.x

WITH
    selected_projects AS (
        SELECT project_owner, project_name
        FROM project
        WHERE project_owner IN ('ansible', 'facebook')
    ),

    commit_churn AS (
        SELECT
            c.project_owner,
            c.project_name,
            c.sha,

            COUNT(DISTINCT fc.id) AS files_changed,
            SUM(fc.total_additions) AS total_additions,
            SUM(fc.total_deletions) AS total_deletions,
            SUM(fc.total_changes) AS total_changes,
            AVG(fc.total_changes) AS avg_changes_per_file,
            MAX(fc.total_changes) AS max_changes_in_file
        FROM `commit` c
                 JOIN selected_projects sp
                      ON sp.project_owner = c.project_owner
                          AND sp.project_name  = c.project_name
                 JOIN commit_file_changes cfc
                      ON cfc.commit_sha = c.sha
                 JOIN file_change fc
                      ON fc.id = cfc.file_changes_id
        GROUP BY
            c.project_owner,
            c.project_name,
            c.sha
    ),

    issue_fix_rows AS (
        SELECT
            pi.project_owner,
            pi.project_name,
            pi.id AS analysis_id,

            TIMESTAMPDIFF(HOUR, pi.created_at, pi.closed_at) AS target_hours,

            COUNT(DISTINCT c.sha) AS commits_in_row,

            SUM(CASE
                    WHEN c.in_degree IS NOT NULL
                        AND c.out_degree IS NOT NULL
                        AND c.min_depth_of_commit_history IS NOT NULL
                        AND c.max_depth_of_commit_history IS NOT NULL
                        AND c.distance_to_branch_start IS NOT NULL
                        AND c.upstream_heads_unique_on_segment IS NOT NULL
                        AND c.days_since_last_merge_on_segment IS NOT NULL
                        
                        AND c.average_degree IS NOT NULL
                        THEN 1 ELSE 0
                END) AS graph_ready_commits,

            SUM(CASE
                    WHEN cc.total_changes IS NOT NULL
                        AND cc.files_changed IS NOT NULL
                        AND cc.files_changed > 0
                        THEN 1 ELSE 0
                END) AS churn_ready_commits,

            AVG(c.max_depth_of_commit_history - c.min_depth_of_commit_history) AS avg_depth_range,
            AVG(c.distance_to_branch_start) AS avg_fp_distance,
            AVG(c.upstream_heads_unique_on_segment) AS avg_upstream_heads,
            AVG(c.days_since_last_merge_on_segment) AS avg_days_since_merge,
            SUM(cc.total_changes) AS total_changes
        FROM project_issue pi
                 JOIN selected_projects sp
                      ON sp.project_owner = pi.project_owner
                          AND sp.project_name  = pi.project_name
                 JOIN project_issue_fixing_commits pifc
                      ON pifc.project_issue_id = pi.id
                          AND pifc.project_issue_project_name = pi.project_name
                          AND pifc.project_issue_project_owner = pi.project_owner
                 JOIN `commit` c
                      ON c.sha = pifc.fixing_commits_sha
                 LEFT JOIN commit_churn cc
                           ON cc.sha = c.sha
        WHERE pi.state = 'closed'
          AND pi.created_at IS NOT NULL
          AND pi.closed_at IS NOT NULL
        GROUP BY
            pi.project_owner,
            pi.project_name,
            pi.id,
            pi.created_at,
            pi.closed_at
    ),

    issue_bic_rows AS (
        SELECT
            pi.project_owner,
            pi.project_name,
            pi.id AS analysis_id,

            TIMESTAMPDIFF(HOUR, pi.created_at, pi.closed_at) AS target_hours,

            COUNT(DISTINCT c.sha) AS commits_in_row,

            SUM(CASE
                    WHEN c.in_degree IS NOT NULL
                        AND c.out_degree IS NOT NULL
                        AND c.min_depth_of_commit_history IS NOT NULL
                        AND c.max_depth_of_commit_history IS NOT NULL
                        AND c.distance_to_branch_start IS NOT NULL
                        AND c.upstream_heads_unique_on_segment IS NOT NULL
                        AND c.days_since_last_merge_on_segment IS NOT NULL
                        
                        AND c.average_degree IS NOT NULL
                        THEN 1 ELSE 0
                END) AS graph_ready_commits,

            SUM(CASE
                    WHEN cc.total_changes IS NOT NULL
                        AND cc.files_changed IS NOT NULL
                        AND cc.files_changed > 0
                        THEN 1 ELSE 0
                END) AS churn_ready_commits,

            AVG(c.max_depth_of_commit_history - c.min_depth_of_commit_history) AS avg_depth_range,
            AVG(c.distance_to_branch_start) AS avg_fp_distance,
            AVG(c.upstream_heads_unique_on_segment) AS avg_upstream_heads,
            AVG(c.days_since_last_merge_on_segment) AS avg_days_since_merge,
            SUM(cc.total_changes) AS total_changes
        FROM project_issue pi
                 JOIN selected_projects sp
                      ON sp.project_owner = pi.project_owner
                          AND sp.project_name  = pi.project_name
                 JOIN project_issue_bug_introducing_commits pibic
                      ON pibic.project_issue_id = pi.id
                          AND pibic.project_issue_project_name = pi.project_name
                          AND pibic.project_issue_project_owner = pi.project_owner
                 JOIN `commit` c
                      ON c.sha = pibic.bug_introducing_commits_sha
                 LEFT JOIN commit_churn cc
                           ON cc.sha = c.sha
        WHERE pi.state = 'closed'
          AND pi.created_at IS NOT NULL
          AND pi.closed_at IS NOT NULL
        GROUP BY
            pi.project_owner,
            pi.project_name,
            pi.id,
            pi.created_at,
            pi.closed_at
    ),

    pr_bic_commits AS (
        SELECT DISTINCT
            pp.project_owner,
            pp.project_name,
            pp.id AS pr_id,
            ppc.commits_sha AS sha
        FROM project_pull pp
                 JOIN selected_projects sp
                      ON sp.project_owner = pp.project_owner
                          AND sp.project_name  = pp.project_name
                 JOIN project_pull_commits ppc
                      ON ppc.project_pull_id = pp.id
                          AND ppc.project_pull_project_name = pp.project_name
                          AND ppc.project_pull_project_owner = pp.project_owner
                 JOIN project_issue_bug_introducing_commits pibic
                      ON pibic.bug_introducing_commits_sha = ppc.commits_sha
    ),

    pr_bic_rows AS (
        SELECT
            pp.project_owner,
            pp.project_name,
            pp.id AS analysis_id,

            TIMESTAMPDIFF(HOUR, pp.created_at, pp.merged_at) AS target_hours,

            COUNT(DISTINCT c.sha) AS commits_in_row,

            SUM(CASE
                    WHEN c.in_degree IS NOT NULL
                        AND c.out_degree IS NOT NULL
                        AND c.min_depth_of_commit_history IS NOT NULL
                        AND c.max_depth_of_commit_history IS NOT NULL
                        AND c.distance_to_branch_start IS NOT NULL
                        AND c.upstream_heads_unique_on_segment IS NOT NULL
                        AND c.days_since_last_merge_on_segment IS NOT NULL
                        
                        AND c.average_degree IS NOT NULL
                        THEN 1 ELSE 0
                END) AS graph_ready_commits,

            SUM(CASE
                    WHEN cc.total_changes IS NOT NULL
                        AND cc.files_changed IS NOT NULL
                        AND cc.files_changed > 0
                        THEN 1 ELSE 0
                END) AS churn_ready_commits,

            AVG(c.max_depth_of_commit_history - c.min_depth_of_commit_history) AS avg_depth_range,
            AVG(c.distance_to_branch_start) AS avg_fp_distance,
            AVG(c.upstream_heads_unique_on_segment) AS avg_upstream_heads,
            AVG(c.days_since_last_merge_on_segment) AS avg_days_since_merge,
            SUM(cc.total_changes) AS total_changes
        FROM project_pull pp
                 JOIN pr_bic_commits pbc
                      ON pbc.pr_id = pp.id
                          AND pbc.project_name = pp.project_name
                          AND pbc.project_owner = pp.project_owner
                 JOIN `commit` c
                      ON c.sha = pbc.sha
                 LEFT JOIN commit_churn cc
                           ON cc.sha = c.sha
        WHERE pp.state = 'closed'
          AND pp.created_at IS NOT NULL
          AND pp.merged_at IS NOT NULL
        GROUP BY
            pp.project_owner,
            pp.project_name,
            pp.id,
            pp.created_at,
            pp.merged_at
    ),

    all_rows AS (
        SELECT
            project_owner,
            project_name,
            'Issue-level fixing-commit dataset' AS dataset_name,
            'issue' AS analysis_unit,
            analysis_id,
            target_hours,
            commits_in_row,
            graph_ready_commits,
            churn_ready_commits,
            avg_depth_range,
            avg_fp_distance,
            avg_upstream_heads,
            avg_days_since_merge,
            total_changes
        FROM issue_fix_rows

        UNION ALL

        SELECT
            project_owner,
            project_name,
            'Issue-level candidate-BIC dataset' AS dataset_name,
            'issue' AS analysis_unit,
            analysis_id,
            target_hours,
            commits_in_row,
            graph_ready_commits,
            churn_ready_commits,
            avg_depth_range,
            avg_fp_distance,
            avg_upstream_heads,
            avg_days_since_merge,
            total_changes
        FROM issue_bic_rows

        UNION ALL

        SELECT
            project_owner,
            project_name,
            'PR-level candidate-BIC dataset' AS dataset_name,
            'pull request' AS analysis_unit,
            analysis_id,
            target_hours,
            commits_in_row,
            graph_ready_commits,
            churn_ready_commits,
            avg_depth_range,
            avg_fp_distance,
            avg_upstream_heads,
            avg_days_since_merge,
            total_changes
        FROM pr_bic_rows
    )

SELECT
    project_owner,
    project_name,
    dataset_name,
    analysis_unit,

    COUNT(*) AS analysis_rows,

    SUM(CASE WHEN target_hours IS NOT NULL AND target_hours >= 0 THEN 1 ELSE 0 END)
             AS rows_with_target_duration,

    ROUND(
            100.0 * SUM(CASE WHEN target_hours IS NOT NULL AND target_hours >= 0 THEN 1 ELSE 0 END)
                / NULLIF(COUNT(*), 0),
            2
    ) AS target_duration_computability_percent,

    SUM(CASE WHEN graph_ready_commits > 0 THEN 1 ELSE 0 END)
             AS rows_with_graph_summary,

    ROUND(
            100.0 * SUM(CASE WHEN graph_ready_commits > 0 THEN 1 ELSE 0 END)
                / NULLIF(COUNT(*), 0),
            2
    ) AS graph_summary_computability_percent,

    SUM(CASE WHEN graph_ready_commits = commits_in_row THEN 1 ELSE 0 END)
             AS rows_where_all_commits_have_graph_metrics,

    ROUND(
            100.0 * SUM(CASE WHEN graph_ready_commits = commits_in_row THEN 1 ELSE 0 END)
                / NULLIF(COUNT(*), 0),
            2
    ) AS all_commits_graph_complete_percent,

    SUM(CASE WHEN churn_ready_commits > 0 THEN 1 ELSE 0 END)
             AS rows_with_churn_summary,

    ROUND(
            100.0 * SUM(CASE WHEN churn_ready_commits > 0 THEN 1 ELSE 0 END)
                / NULLIF(COUNT(*), 0),
            2
    ) AS churn_summary_computability_percent,

    SUM(CASE WHEN graph_ready_commits > 0 AND churn_ready_commits > 0 THEN 1 ELSE 0 END)
             AS rows_with_graph_and_churn_summary,

    ROUND(
            100.0 * SUM(CASE WHEN graph_ready_commits > 0 AND churn_ready_commits > 0 THEN 1 ELSE 0 END)
                / NULLIF(COUNT(*), 0),
            2
    ) AS graph_churn_summary_computability_percent,

    ROUND(AVG(commits_in_row), 2) AS avg_commits_per_analysis_row,
    ROUND(AVG(graph_ready_commits), 2) AS avg_graph_ready_commits_per_row,
    ROUND(AVG(churn_ready_commits), 2) AS avg_churn_ready_commits_per_row,

    ROUND(AVG(avg_depth_range), 4) AS avg_depth_range,
    ROUND(AVG(avg_fp_distance), 4) AS avg_fp_distance,
    ROUND(AVG(avg_upstream_heads), 4) AS avg_upstream_heads,
    ROUND(AVG(avg_days_since_merge), 4) AS avg_days_since_merge,
    ROUND(AVG(total_changes), 4) AS avg_total_changes
FROM all_rows
GROUP BY
    project_owner,
    project_name,
    dataset_name,
    analysis_unit
ORDER BY
    project_owner,
    project_name,
    dataset_name;



#RQ555
WITH
    selected_projects AS (
        SELECT project_owner, project_name
        FROM project
        WHERE project_owner IN ('ansible', 'facebook')
    ),

    commit_churn AS (
        SELECT
            c.project_owner,
            c.project_name,
            c.sha,

            COUNT(DISTINCT fc.id) AS files_changed,
            SUM(fc.total_additions) AS total_additions,
            SUM(fc.total_deletions) AS total_deletions,
            SUM(fc.total_changes) AS total_changes,
            AVG(fc.total_changes) AS avg_changes_per_file,
            MAX(fc.total_changes) AS max_changes_in_file
        FROM `commit` c
                 JOIN selected_projects sp
                      ON sp.project_owner = c.project_owner
                          AND sp.project_name  = c.project_name
                 JOIN commit_file_changes cfc
                      ON cfc.commit_sha = c.sha
                 JOIN file_change fc
                      ON fc.id = cfc.file_changes_id
        GROUP BY
            c.project_owner,
            c.project_name,
            c.sha
    ),

    issue_fix_rows AS (
        SELECT
            pi.project_owner,
            pi.project_name,
            pi.id AS analysis_id,

            TIMESTAMPDIFF(HOUR, pi.created_at, pi.closed_at) AS target_hours,

            COUNT(DISTINCT c.sha) AS commits_in_row,

            SUM(CASE
                    WHEN c.in_degree IS NOT NULL
                        AND c.out_degree IS NOT NULL
                        AND c.min_depth_of_commit_history IS NOT NULL
                        AND c.max_depth_of_commit_history IS NOT NULL
                        AND c.distance_to_branch_start IS NOT NULL
                        AND c.upstream_heads_unique_on_segment IS NOT NULL
                        AND c.days_since_last_merge_on_segment IS NOT NULL
                        AND c.number_of_branches IS NOT NULL
                        AND c.average_degree IS NOT NULL
                        THEN 1 ELSE 0
                END) AS graph_ready_commits,

            SUM(CASE
                    WHEN cc.total_changes IS NOT NULL
                        AND cc.files_changed IS NOT NULL
                        AND cc.files_changed > 0
                        THEN 1 ELSE 0
                END) AS churn_ready_commits,

            AVG(c.max_depth_of_commit_history - c.min_depth_of_commit_history) AS avg_depth_range,
            AVG(c.distance_to_branch_start) AS avg_fp_distance,
            AVG(c.upstream_heads_unique_on_segment) AS avg_upstream_heads,
            AVG(c.days_since_last_merge_on_segment) AS avg_days_since_merge,
            SUM(cc.total_changes) AS total_changes
        FROM project_issue pi
                 JOIN selected_projects sp
                      ON sp.project_owner = pi.project_owner
                          AND sp.project_name  = pi.project_name
                 JOIN project_issue_fixing_commits pifc
                      ON pifc.project_issue_id = pi.id
                          AND pifc.project_issue_project_name = pi.project_name
                          AND pifc.project_issue_project_owner = pi.project_owner
                 JOIN `commit` c
                      ON c.sha = pifc.fixing_commits_sha
                 LEFT JOIN commit_churn cc
                           ON cc.sha = c.sha
        WHERE pi.state = 'closed'
          AND pi.created_at IS NOT NULL
          AND pi.closed_at IS NOT NULL
        GROUP BY
            pi.project_owner,
            pi.project_name,
            pi.id,
            pi.created_at,
            pi.closed_at
    ),

    issue_bic_rows AS (
        SELECT
            pi.project_owner,
            pi.project_name,
            pi.id AS analysis_id,

            TIMESTAMPDIFF(HOUR, pi.created_at, pi.closed_at) AS target_hours,

            COUNT(DISTINCT c.sha) AS commits_in_row,

            SUM(CASE
                    WHEN c.in_degree IS NOT NULL
                        AND c.out_degree IS NOT NULL
                        AND c.min_depth_of_commit_history IS NOT NULL
                        AND c.max_depth_of_commit_history IS NOT NULL
                        AND c.distance_to_branch_start IS NOT NULL
                        AND c.upstream_heads_unique_on_segment IS NOT NULL
                        AND c.days_since_last_merge_on_segment IS NOT NULL
                        AND c.number_of_branches IS NOT NULL
                        AND c.average_degree IS NOT NULL
                        THEN 1 ELSE 0
                END) AS graph_ready_commits,

            SUM(CASE
                    WHEN cc.total_changes IS NOT NULL
                        AND cc.files_changed IS NOT NULL
                        AND cc.files_changed > 0
                        THEN 1 ELSE 0
                END) AS churn_ready_commits,

            AVG(c.max_depth_of_commit_history - c.min_depth_of_commit_history) AS avg_depth_range,
            AVG(c.distance_to_branch_start) AS avg_fp_distance,
            AVG(c.upstream_heads_unique_on_segment) AS avg_upstream_heads,
            AVG(c.days_since_last_merge_on_segment) AS avg_days_since_merge,
            SUM(cc.total_changes) AS total_changes
        FROM project_issue pi
                 JOIN selected_projects sp
                      ON sp.project_owner = pi.project_owner
                          AND sp.project_name  = pi.project_name
                 JOIN project_issue_bug_introducing_commits pibic
                      ON pibic.project_issue_id = pi.id
                          AND pibic.project_issue_project_name = pi.project_name
                          AND pibic.project_issue_project_owner = pi.project_owner
                 JOIN `commit` c
                      ON c.sha = pibic.bug_introducing_commits_sha
                 LEFT JOIN commit_churn cc
                           ON cc.sha = c.sha
        WHERE pi.state = 'closed'
          AND pi.created_at IS NOT NULL
          AND pi.closed_at IS NOT NULL
        GROUP BY
            pi.project_owner,
            pi.project_name,
            pi.id,
            pi.created_at,
            pi.closed_at
    ),

    pr_bic_commits AS (
        SELECT DISTINCT
            pp.project_owner,
            pp.project_name,
            pp.id AS pr_id,
            ppc.commits_sha AS sha
        FROM project_pull pp
                 JOIN selected_projects sp
                      ON sp.project_owner = pp.project_owner
                          AND sp.project_name  = pp.project_name
                 JOIN project_pull_commits ppc
                      ON ppc.project_pull_id = pp.id
                          AND ppc.project_pull_project_name = pp.project_name
                          AND ppc.project_pull_project_owner = pp.project_owner
                 JOIN project_issue_bug_introducing_commits pibic
                      ON pibic.bug_introducing_commits_sha = ppc.commits_sha
    ),

    pr_bic_rows AS (
        SELECT
            pp.project_owner,
            pp.project_name,
            pp.id AS analysis_id,

            TIMESTAMPDIFF(HOUR, pp.created_at, pp.merged_at) AS target_hours,

            COUNT(DISTINCT c.sha) AS commits_in_row,

            SUM(CASE
                    WHEN c.in_degree IS NOT NULL
                        AND c.out_degree IS NOT NULL
                        AND c.min_depth_of_commit_history IS NOT NULL
                        AND c.max_depth_of_commit_history IS NOT NULL
                        AND c.distance_to_branch_start IS NOT NULL
                        AND c.upstream_heads_unique_on_segment IS NOT NULL
                        AND c.days_since_last_merge_on_segment IS NOT NULL
                        AND c.number_of_branches IS NOT NULL
                        AND c.average_degree IS NOT NULL
                        THEN 1 ELSE 0
                END) AS graph_ready_commits,

            SUM(CASE
                    WHEN cc.total_changes IS NOT NULL
                        AND cc.files_changed IS NOT NULL
                        AND cc.files_changed > 0
                        THEN 1 ELSE 0
                END) AS churn_ready_commits,

            AVG(c.max_depth_of_commit_history - c.min_depth_of_commit_history) AS avg_depth_range,
            AVG(c.distance_to_branch_start) AS avg_fp_distance,
            AVG(c.upstream_heads_unique_on_segment) AS avg_upstream_heads,
            AVG(c.days_since_last_merge_on_segment) AS avg_days_since_merge,
            SUM(cc.total_changes) AS total_changes
        FROM project_pull pp
                 JOIN pr_bic_commits pbc
                      ON pbc.pr_id = pp.id
                          AND pbc.project_name = pp.project_name
                          AND pbc.project_owner = pp.project_owner
                 JOIN `commit` c
                      ON c.sha = pbc.sha
                 LEFT JOIN commit_churn cc
                           ON cc.sha = c.sha
        WHERE pp.state = 'closed'
          AND pp.created_at IS NOT NULL
          AND pp.merged_at IS NOT NULL
        GROUP BY
            pp.project_owner,
            pp.project_name,
            pp.id,
            pp.created_at,
            pp.merged_at
    ),

    all_rows AS (
        SELECT
            project_owner,
            project_name,
            'Issue-level fixing-commit dataset' AS dataset_name,
            'issue' AS analysis_unit,
            analysis_id,
            target_hours,
            commits_in_row,
            graph_ready_commits,
            churn_ready_commits,
            avg_depth_range,
            avg_fp_distance,
            avg_upstream_heads,
            avg_days_since_merge,
            total_changes
        FROM issue_fix_rows

        UNION ALL

        SELECT
            project_owner,
            project_name,
            'Issue-level candidate-BIC dataset' AS dataset_name,
            'issue' AS analysis_unit,
            analysis_id,
            target_hours,
            commits_in_row,
            graph_ready_commits,
            churn_ready_commits,
            avg_depth_range,
            avg_fp_distance,
            avg_upstream_heads,
            avg_days_since_merge,
            total_changes
        FROM issue_bic_rows

        UNION ALL

        SELECT
            project_owner,
            project_name,
            'PR-level candidate-BIC dataset' AS dataset_name,
            'pull request' AS analysis_unit,
            analysis_id,
            target_hours,
            commits_in_row,
            graph_ready_commits,
            churn_ready_commits,
            avg_depth_range,
            avg_fp_distance,
            avg_upstream_heads,
            avg_days_since_merge,
            total_changes
        FROM pr_bic_rows
    )

SELECT
    project_owner,
    project_name,
    dataset_name,
    analysis_unit,

    COUNT(*) AS analysis_rows,

    SUM(CASE WHEN target_hours IS NOT NULL AND target_hours >= 0 THEN 1 ELSE 0 END)
             AS rows_with_target_duration,

    ROUND(
            100.0 * SUM(CASE WHEN target_hours IS NOT NULL AND target_hours >= 0 THEN 1 ELSE 0 END)
                / NULLIF(COUNT(*), 0),
            2
    ) AS target_duration_computability_percent,

    SUM(CASE WHEN graph_ready_commits > 0 THEN 1 ELSE 0 END)
             AS rows_with_graph_summary,

    ROUND(
            100.0 * SUM(CASE WHEN graph_ready_commits > 0 THEN 1 ELSE 0 END)
                / NULLIF(COUNT(*), 0),
            2
    ) AS graph_summary_computability_percent,

    SUM(CASE WHEN graph_ready_commits = commits_in_row THEN 1 ELSE 0 END)
             AS rows_where_all_commits_have_graph_metrics,

    ROUND(
            100.0 * SUM(CASE WHEN graph_ready_commits = commits_in_row THEN 1 ELSE 0 END)
                / NULLIF(COUNT(*), 0),
            2
    ) AS all_commits_graph_complete_percent,

    SUM(CASE WHEN churn_ready_commits > 0 THEN 1 ELSE 0 END)
             AS rows_with_churn_summary,

    ROUND(
            100.0 * SUM(CASE WHEN churn_ready_commits > 0 THEN 1 ELSE 0 END)
                / NULLIF(COUNT(*), 0),
            2
    ) AS churn_summary_computability_percent,

    SUM(CASE WHEN graph_ready_commits > 0 AND churn_ready_commits > 0 THEN 1 ELSE 0 END)
             AS rows_with_graph_and_churn_summary,

    ROUND(
            100.0 * SUM(CASE WHEN graph_ready_commits > 0 AND churn_ready_commits > 0 THEN 1 ELSE 0 END)
                / NULLIF(COUNT(*), 0),
            2
    ) AS graph_churn_summary_computability_percent,

    ROUND(AVG(commits_in_row), 2) AS avg_commits_per_analysis_row,
    ROUND(AVG(graph_ready_commits), 2) AS avg_graph_ready_commits_per_row,
    ROUND(AVG(churn_ready_commits), 2) AS avg_churn_ready_commits_per_row,

    ROUND(AVG(avg_depth_range), 4) AS avg_depth_range,
    ROUND(AVG(avg_fp_distance), 4) AS avg_fp_distance,
    ROUND(AVG(avg_upstream_heads), 4) AS avg_upstream_heads,
    ROUND(AVG(avg_days_since_merge), 4) AS avg_days_since_merge,
    ROUND(AVG(total_changes), 4) AS avg_total_changes
FROM all_rows
GROUP BY
    project_owner,
    project_name,
    dataset_name,
    analysis_unit
ORDER BY
    project_owner,
    project_name,
    dataset_name;




#CI
WITH
    commit_churn AS (
        SELECT
            cfc.commit_sha,
            COUNT(DISTINCT fc.id) AS num_files_changed,
            SUM(COALESCE(fc.total_additions, 0)) AS total_additions,
            SUM(COALESCE(fc.total_deletions, 0)) AS total_deletions,
            SUM(COALESCE(fc.total_changes, 0)) AS total_changes,
            AVG(COALESCE(fc.total_changes, 0)) AS avg_changes_per_file,
            MAX(COALESCE(fc.total_changes, 0)) AS max_changes_in_file
        FROM commit_file_changes cfc
                 JOIN file_change fc
                      ON fc.id = cfc.file_changes_id
        GROUP BY
            cfc.commit_sha
    ),

    ci_result_rows AS (
        SELECT
            a.commit_sha,
            LOWER(TRIM(a.result)) AS result_norm,
            a.started_at,
            a.completed_at
        FROM action a
        WHERE a.commit_sha IS NOT NULL
          AND a.result IS NOT NULL
          AND TRIM(a.result) <> ''
    ),

    ci_by_commit AS (
        SELECT
            r.commit_sha,

            COUNT(*) AS ci_check_runs,
            COUNT(*) AS ci_total_checks,

            SUM(CASE
                    WHEN r.result_norm = 'success'
                        THEN 1 ELSE 0
                END) AS ci_passed_checks,

            SUM(CASE
                    WHEN r.result_norm IN (
                                           'failure',
                                           'failed',
                                           'timed_out',
                                           'startup_failure',
                                           'action_required'
                        )
                        THEN 1 ELSE 0
                END) AS ci_failed_checks,

            SUM(CASE
                    WHEN r.result_norm = 'cancelled'
                        THEN 1 ELSE 0
                END) AS ci_cancelled_checks,

            SUM(CASE
                    WHEN r.result_norm NOT IN (
                                               'success',
                                               'failure',
                                               'failed',
                                               'timed_out',
                                               'startup_failure',
                                               'action_required',
                                               'cancelled'
                        )
                        THEN 1 ELSE 0
                END) AS ci_other_checks,

            AVG(
                    CASE
                        WHEN r.started_at IS NOT NULL
                            AND r.completed_at IS NOT NULL
                            AND TIMESTAMPDIFF(SECOND, r.started_at, r.completed_at) >= 0
                            THEN TIMESTAMPDIFF(SECOND, r.started_at, r.completed_at)
                        ELSE NULL
                        END
            ) AS ci_avg_duration_hours

        FROM ci_result_rows r
        GROUP BY
            r.commit_sha
    ),

    bic_by_commit AS (
        SELECT
            pibic.bug_introducing_commits_sha AS commit_sha,
            COUNT(DISTINCT CONCAT(
                    pibic.project_issue_project_owner, ':',
                    pibic.project_issue_project_name, ':',
                    pibic.project_issue_id
                           )) AS linked_bic_issues
        FROM project_issue_bug_introducing_commits pibic
        GROUP BY
            pibic.bug_introducing_commits_sha
    ),

    pr_process AS (
        SELECT
            pp.project_owner,
            pp.project_name,
            pp.id AS pr_id,

            COUNT(DISTINCT ppl.labels) AS pr_label_count,
            COUNT(DISTINCT ppa.assignees) AS pr_assignee_count,
            COUNT(DISTINCT ppr.reviewers) AS pr_reviewer_count,
            COUNT(DISTINCT ppt.time_line_id) AS pr_timeline_event_count,
            COALESCE(MAX(r.total_count), 0) AS pr_reaction_count

        FROM project_pull pp

                 LEFT JOIN project_pull_labels ppl
                           ON ppl.project_pull_id = pp.id
                               AND ppl.project_pull_project_name = pp.project_name
                               AND ppl.project_pull_project_owner = pp.project_owner

                 LEFT JOIN project_pull_assignees ppa
                           ON ppa.project_pull_id = pp.id
                               AND ppa.project_pull_project_name = pp.project_name
                               AND ppa.project_pull_project_owner = pp.project_owner

                 LEFT JOIN project_pull_reviewers ppr
                           ON ppr.project_pull_id = pp.id
                               AND ppr.project_pull_project_name = pp.project_name
                               AND ppr.project_pull_project_owner = pp.project_owner

                 LEFT JOIN project_pull_time_line ppt
                           ON ppt.project_pull_id = pp.id
                               AND ppt.project_pull_project_name = pp.project_name
                               AND ppt.project_pull_project_owner = pp.project_owner

                 LEFT JOIN reaction r
                           ON r.id = pp.reaction_id

        WHERE pp.project_owner = 'ansible'

GROUP BY
    pp.project_owner,
    pp.project_name,
    pp.id
    ),

    pr_commit_rows AS (
SELECT
    pp.project_owner,
    pp.project_name,
    pp.id AS pr_id,
    pp.created_at AS pr_created_at,
    pp.merged_at AS pr_merged_at,

    TIMESTAMPDIFF(SECOND, pp.created_at, pp.merged_at) / 3600.0
    AS pr_review_hours,

    c.sha AS commit_sha,

    c.min_depth_of_commit_history,
    c.max_depth_of_commit_history,

    (
    c.max_depth_of_commit_history
    - c.min_depth_of_commit_history
    ) AS depth_diff,

    c.distance_to_branch_start,
    c.upstream_heads_unique_on_segment,
    c.days_since_last_merge_on_segment,
    c.in_degree,
    c.out_degree,
    c.number_of_branches,
    c.average_degree,

    CASE
    WHEN c.days_since_last_merge_on_segment IS NULL
    OR c.distance_to_branch_start IS NULL
    THEN NULL
    ELSE
    1.0 * c.distance_to_branch_start
    / GREATEST(c.days_since_last_merge_on_segment, 1)
    END AS branch_commit_rate,

    cc.num_files_changed,
    cc.total_additions,
    cc.total_deletions,
    cc.total_changes,
    cc.avg_changes_per_file,
    cc.max_changes_in_file,

    CASE
    WHEN cc.num_files_changed > 0
    THEN 1.0 * cc.total_changes / cc.num_files_changed
    ELSE NULL
    END AS change_density_per_file,

    ci.ci_check_runs,
    ci.ci_total_checks,
    ci.ci_passed_checks,
    ci.ci_failed_checks,
    ci.ci_cancelled_checks,
    ci.ci_other_checks,
    ci.ci_avg_duration_hours,

    COALESCE(bic.linked_bic_issues, 0) AS linked_bic_issues,

    CASE
    WHEN c.min_depth_of_commit_history IS NOT NULL
    AND c.max_depth_of_commit_history IS NOT NULL
    AND c.distance_to_branch_start IS NOT NULL
    AND c.upstream_heads_unique_on_segment IS NOT NULL
    AND c.days_since_last_merge_on_segment IS NOT NULL
    AND c.in_degree IS NOT NULL
    AND c.out_degree IS NOT NULL
    AND c.number_of_branches IS NOT NULL
    AND c.average_degree IS NOT NULL
    THEN 1 ELSE 0
    END AS graph_ready,

    CASE
    WHEN cc.num_files_changed IS NOT NULL
    AND cc.num_files_changed > 0
    AND cc.total_changes IS NOT NULL
    THEN 1 ELSE 0
    END AS churn_ready,

    CASE
    WHEN ci.ci_check_runs IS NOT NULL
    AND ci.ci_check_runs > 0
    THEN 1 ELSE 0
    END AS ci_ready

FROM project_pull pp

    JOIN project_pull_commits ppc
ON ppc.project_pull_id = pp.id
    AND ppc.project_pull_project_name = pp.project_name
    AND ppc.project_pull_project_owner = pp.project_owner

    JOIN `commit` c
    ON c.sha = ppc.commits_sha

    LEFT JOIN commit_churn cc
    ON cc.commit_sha = c.sha

    LEFT JOIN ci_by_commit ci
    ON ci.commit_sha = c.sha

    LEFT JOIN bic_by_commit bic
    ON bic.commit_sha = c.sha

WHERE pp.project_owner = 'ansible'
  AND pp.created_at IS NOT NULL
  AND pp.merged_at IS NOT NULL
  AND TIMESTAMPDIFF(SECOND, pp.created_at, pp.merged_at) >= 0
    )

SELECT
    pcr.project_owner,
    pcr.project_name,
    pcr.pr_id,
    pcr.pr_created_at,
    pcr.pr_merged_at,

    /* Target variables */
    pcr.pr_review_hours,
    LOG(1 + pcr.pr_review_hours) AS log_pr_review_hours,

    CASE
        WHEN MOD(CRC32(CONCAT(pcr.project_owner, ':', pcr.project_name, ':', pcr.pr_id)), 10) < 8
            THEN 'train'
        ELSE 'validation'
        END AS dataset_split,

    /* Process / social features */
    COALESCE(pp.pr_label_count, 0) AS pr_label_count,
    COALESCE(pp.pr_assignee_count, 0) AS pr_assignee_count,
    COALESCE(pp.pr_reviewer_count, 0) AS pr_reviewer_count,
    COALESCE(pp.pr_timeline_event_count, 0) AS pr_timeline_event_count,
    COALESCE(pp.pr_reaction_count, 0) AS pr_reaction_count,

    /* Evidence-count features */
    COUNT(DISTINCT pcr.commit_sha) AS pr_num_commits,
    SUM(pcr.graph_ready) AS pr_graph_ready_commits,
    SUM(pcr.churn_ready) AS pr_churn_ready_commits,
    SUM(pcr.ci_ready) AS pr_ci_ready_commits,

    ROUND(
            1.0 * SUM(pcr.graph_ready) / NULLIF(COUNT(DISTINCT pcr.commit_sha), 0),
            4
    ) AS pr_graph_ready_fraction,

    ROUND(
            1.0 * SUM(pcr.churn_ready) / NULLIF(COUNT(DISTINCT pcr.commit_sha), 0),
            4
    ) AS pr_churn_ready_fraction,

    ROUND(
            1.0 * SUM(pcr.ci_ready) / NULLIF(COUNT(DISTINCT pcr.commit_sha), 0),
            4
    ) AS pr_ci_ready_fraction,

    /* Candidate defect-provenance features */
    CASE
        WHEN SUM(CASE WHEN pcr.linked_bic_issues > 0 THEN 1 ELSE 0 END) > 0
            THEN 1
        ELSE 0
        END AS pr_contains_candidate_bic,

    SUM(CASE WHEN pcr.linked_bic_issues > 0 THEN 1 ELSE 0 END)
        AS pr_candidate_bic_commits,

    SUM(pcr.linked_bic_issues) AS pr_candidate_bic_issue_links,

    /* Graph-history features */
    AVG(CASE WHEN pcr.graph_ready = 1 THEN pcr.min_depth_of_commit_history END)
        AS pr_avg_min_depth,

    AVG(CASE WHEN pcr.graph_ready = 1 THEN pcr.max_depth_of_commit_history END)
        AS pr_avg_max_depth,

    AVG(CASE WHEN pcr.graph_ready = 1 THEN pcr.depth_diff END)
        AS pr_avg_depth_diff,

    MAX(CASE WHEN pcr.graph_ready = 1 THEN pcr.depth_diff END)
        AS pr_max_depth_diff,

    AVG(CASE WHEN pcr.graph_ready = 1 THEN pcr.branch_commit_rate END)
        AS pr_avg_branch_commit_rate,

    MAX(CASE WHEN pcr.graph_ready = 1 THEN pcr.branch_commit_rate END)
        AS pr_max_branch_commit_rate,

    AVG(CASE WHEN pcr.graph_ready = 1 THEN pcr.distance_to_branch_start END)
        AS pr_avg_fp_distance,

    MAX(CASE WHEN pcr.graph_ready = 1 THEN pcr.distance_to_branch_start END)
        AS pr_max_fp_distance,

    AVG(CASE WHEN pcr.graph_ready = 1 THEN pcr.upstream_heads_unique_on_segment END)
        AS pr_avg_upstream_heads,

    MAX(CASE WHEN pcr.graph_ready = 1 THEN pcr.upstream_heads_unique_on_segment END)
        AS pr_max_upstream_heads,

    AVG(CASE WHEN pcr.graph_ready = 1 THEN pcr.days_since_last_merge_on_segment END)
        AS pr_avg_days_since_merge,

    MAX(CASE WHEN pcr.graph_ready = 1 THEN pcr.days_since_last_merge_on_segment END)
        AS pr_max_days_since_merge,

    AVG(CASE WHEN pcr.graph_ready = 1 THEN pcr.in_degree END)
        AS pr_avg_in_degree,

    AVG(CASE WHEN pcr.graph_ready = 1 THEN pcr.out_degree END)
        AS pr_avg_out_degree,

    AVG(CASE WHEN pcr.graph_ready = 1 THEN pcr.number_of_branches END)
        AS pr_avg_branches,

    AVG(CASE WHEN pcr.graph_ready = 1 THEN pcr.average_degree END)
        AS pr_avg_average_degree,

    /* Churn features */
    SUM(COALESCE(pcr.total_additions, 0)) AS pr_total_additions,
    SUM(COALESCE(pcr.total_deletions, 0)) AS pr_total_deletions,
    SUM(COALESCE(pcr.total_changes, 0)) AS pr_total_changes,

    AVG(CASE WHEN pcr.churn_ready = 1 THEN pcr.avg_changes_per_file END)
        AS pr_avg_changes_per_file,

    MAX(CASE WHEN pcr.churn_ready = 1 THEN pcr.max_changes_in_file END)
        AS pr_max_changes_in_file,

    SUM(COALESCE(pcr.num_files_changed, 0)) AS pr_num_files_changed,

    CASE
        WHEN SUM(COALESCE(pcr.num_files_changed, 0)) > 0
            THEN
            1.0 * SUM(COALESCE(pcr.total_changes, 0))
                / SUM(COALESCE(pcr.num_files_changed, 0))
        ELSE NULL
        END AS pr_change_density_per_file,

    /* Log-scaled count/churn features */
    LOG(1 + COUNT(DISTINCT pcr.commit_sha)) AS log_pr_num_commits,
    LOG(1 + SUM(COALESCE(pcr.total_additions, 0))) AS log_pr_total_additions,
    LOG(1 + SUM(COALESCE(pcr.total_deletions, 0))) AS log_pr_total_deletions,
    LOG(1 + SUM(COALESCE(pcr.total_changes, 0))) AS log_pr_total_changes,
    LOG(1 + SUM(COALESCE(pcr.num_files_changed, 0))) AS log_pr_num_files_changed,

    /* CI features computed from action.result values */
    SUM(COALESCE(pcr.ci_check_runs, 0)) AS pr_ci_check_runs,
    SUM(COALESCE(pcr.ci_total_checks, 0)) AS pr_ci_total_checks,
    SUM(COALESCE(pcr.ci_passed_checks, 0)) AS pr_ci_passed_checks,
    SUM(COALESCE(pcr.ci_failed_checks, 0)) AS pr_ci_failed_checks,
    SUM(COALESCE(pcr.ci_cancelled_checks, 0)) AS pr_ci_cancelled_checks,
    SUM(COALESCE(pcr.ci_other_checks, 0)) AS pr_ci_other_checks,

    CASE
        WHEN SUM(COALESCE(pcr.ci_total_checks, 0)) > 0
            THEN ROUND(
                100.0 * SUM(COALESCE(pcr.ci_passed_checks, 0))
                    / SUM(COALESCE(pcr.ci_total_checks, 0)),
                2
                 )
        ELSE NULL
        END AS pr_ci_passed_percent,

    CASE
        WHEN SUM(COALESCE(pcr.ci_total_checks, 0)) > 0
            THEN ROUND(
                100.0 * SUM(COALESCE(pcr.ci_failed_checks, 0))
                    / SUM(COALESCE(pcr.ci_total_checks, 0)),
                2
                 )
        ELSE NULL
        END AS pr_ci_failed_percent,

    CASE
        WHEN SUM(COALESCE(pcr.ci_total_checks, 0)) > 0
            THEN ROUND(
                100.0 * SUM(COALESCE(pcr.ci_cancelled_checks, 0))
                    / SUM(COALESCE(pcr.ci_total_checks, 0)),
                2
                 )
        ELSE NULL
        END AS pr_ci_cancelled_percent,

    CASE
        WHEN SUM(COALESCE(pcr.ci_total_checks, 0)) > 0
            THEN ROUND(
                100.0 * SUM(COALESCE(pcr.ci_other_checks, 0))
                    / SUM(COALESCE(pcr.ci_total_checks, 0)),
                2
                 )
        ELSE NULL
        END AS pr_ci_other_percent,

    AVG(CASE WHEN pcr.ci_ready = 1 THEN pcr.ci_avg_duration_hours END)
        AS pr_ci_avg_duration_hours

FROM pr_commit_rows pcr

         LEFT JOIN pr_process pp
                   ON pp.project_owner = pcr.project_owner
                       AND pp.project_name = pcr.project_name
                       AND pp.pr_id = pcr.pr_id

GROUP BY
    pcr.project_owner,
    pcr.project_name,
    pcr.pr_id,
    pcr.pr_created_at,
    pcr.pr_merged_at,
    pcr.pr_review_hours,
    pp.pr_label_count,
    pp.pr_assignee_count,
    pp.pr_reviewer_count,
    pp.pr_timeline_event_count,
    pp.pr_reaction_count

HAVING
    COUNT(DISTINCT pcr.commit_sha) > 0

ORDER BY
    pcr.project_owner,
    pcr.project_name,
    pcr.pr_id;



select distinct (action.result) from action;



select * from action where commit_sha='5d0063e4bf42280a519365742414a43db664605b'




#rrr
    WITH
    selected_projects AS (
        SELECT project_owner, project_name
        FROM project
        WHERE project_owner IN ('ansible', 'facebook')
    ),

    commit_churn AS (
        SELECT
            c.project_owner,
            c.project_name,
            c.sha,

            COUNT(DISTINCT fc.id) AS files_changed,
            SUM(fc.total_additions) AS total_additions,
            SUM(fc.total_deletions) AS total_deletions,
            SUM(fc.total_changes) AS total_changes,
            AVG(fc.total_changes) AS avg_changes_per_file,
            MAX(fc.total_changes) AS max_changes_in_file
        FROM `commit` c
                 JOIN selected_projects sp
                      ON sp.project_owner = c.project_owner
                          AND sp.project_name  = c.project_name
                 JOIN commit_file_changes cfc
                      ON cfc.commit_sha = c.sha
                 JOIN file_change fc
                      ON fc.id = cfc.file_changes_id
        GROUP BY
            c.project_owner,
            c.project_name,
            c.sha
    ),

    issue_fix_rows AS (
        SELECT
            pi.project_owner,
            pi.project_name,
            pi.id AS analysis_id,

            TIMESTAMPDIFF(HOUR, pi.created_at, pi.closed_at) AS target_hours,

            COUNT(DISTINCT c.sha) AS commits_in_row,

            SUM(CASE
                    WHEN c.in_degree IS NOT NULL
                        AND c.out_degree IS NOT NULL
                        AND c.min_depth_of_commit_history IS NOT NULL
                        AND c.max_depth_of_commit_history IS NOT NULL
                        AND c.distance_to_branch_start IS NOT NULL
                        AND c.upstream_heads_unique_on_segment IS NOT NULL
                        AND c.days_since_last_merge_on_segment IS NOT NULL
                        AND c.number_of_branches IS NOT NULL
                        AND c.average_degree IS NOT NULL
                        THEN 1 ELSE 0
                END) AS graph_ready_commits,

            SUM(CASE
                    WHEN cc.total_changes IS NOT NULL
                        AND cc.files_changed IS NOT NULL
                        AND cc.files_changed > 0
                        THEN 1 ELSE 0
                END) AS churn_ready_commits,

            AVG(c.max_depth_of_commit_history - c.min_depth_of_commit_history) AS avg_depth_range,
            AVG(c.distance_to_branch_start) AS avg_fp_distance,
            AVG(c.upstream_heads_unique_on_segment) AS avg_upstream_heads,
            AVG(c.days_since_last_merge_on_segment) AS avg_days_since_merge,
            SUM(cc.total_changes) AS total_changes
        FROM project_issue pi
                 JOIN selected_projects sp
                      ON sp.project_owner = pi.project_owner
                          AND sp.project_name  = pi.project_name
                 JOIN project_issue_fixing_commits pifc
                      ON pifc.project_issue_id = pi.id
                          AND pifc.project_issue_project_name = pi.project_name
                          AND pifc.project_issue_project_owner = pi.project_owner
                 JOIN `commit` c
                      ON c.sha = pifc.fixing_commits_sha
                 LEFT JOIN commit_churn cc
                           ON cc.sha = c.sha
        WHERE pi.state = 'closed'
          AND pi.created_at IS NOT NULL
          AND pi.closed_at IS NOT NULL
        GROUP BY
            pi.project_owner,
            pi.project_name,
            pi.id,
            pi.created_at,
            pi.closed_at
    ),

    issue_bic_rows AS (
        SELECT
            pi.project_owner,
            pi.project_name,
            pi.id AS analysis_id,

            TIMESTAMPDIFF(HOUR, pi.created_at, pi.closed_at) AS target_hours,

            COUNT(DISTINCT c.sha) AS commits_in_row,

            SUM(CASE
                    WHEN c.in_degree IS NOT NULL
                        AND c.out_degree IS NOT NULL
                        AND c.min_depth_of_commit_history IS NOT NULL
                        AND c.max_depth_of_commit_history IS NOT NULL
                        AND c.distance_to_branch_start IS NOT NULL
                        AND c.upstream_heads_unique_on_segment IS NOT NULL
                        AND c.days_since_last_merge_on_segment IS NOT NULL
                        AND c.number_of_branches IS NOT NULL
                        AND c.average_degree IS NOT NULL
                        THEN 1 ELSE 0
                END) AS graph_ready_commits,

            SUM(CASE
                    WHEN cc.total_changes IS NOT NULL
                        AND cc.files_changed IS NOT NULL
                        AND cc.files_changed > 0
                        THEN 1 ELSE 0
                END) AS churn_ready_commits,

            AVG(c.max_depth_of_commit_history - c.min_depth_of_commit_history) AS avg_depth_range,
            AVG(c.distance_to_branch_start) AS avg_fp_distance,
            AVG(c.upstream_heads_unique_on_segment) AS avg_upstream_heads,
            AVG(c.days_since_last_merge_on_segment) AS avg_days_since_merge,
            SUM(cc.total_changes) AS total_changes
        FROM project_issue pi
                 JOIN selected_projects sp
                      ON sp.project_owner = pi.project_owner
                          AND sp.project_name  = pi.project_name
                 JOIN project_issue_bug_introducing_commits pibic
                      ON pibic.project_issue_id = pi.id
                          AND pibic.project_issue_project_name = pi.project_name
                          AND pibic.project_issue_project_owner = pi.project_owner
                 JOIN `commit` c
                      ON c.sha = pibic.bug_introducing_commits_sha
                 LEFT JOIN commit_churn cc
                           ON cc.sha = c.sha
        WHERE pi.state = 'closed'
          AND pi.created_at IS NOT NULL
          AND pi.closed_at IS NOT NULL
        GROUP BY
            pi.project_owner,
            pi.project_name,
            pi.id,
            pi.created_at,
            pi.closed_at
    ),

    pr_bic_commits AS (
        SELECT DISTINCT
            pp.project_owner,
            pp.project_name,
            pp.id AS pr_id,
            ppc.commits_sha AS sha
        FROM project_pull pp
                 JOIN selected_projects sp
                      ON sp.project_owner = pp.project_owner
                          AND sp.project_name  = pp.project_name
                 JOIN project_pull_commits ppc
                      ON ppc.project_pull_id = pp.id
                          AND ppc.project_pull_project_name = pp.project_name
                          AND ppc.project_pull_project_owner = pp.project_owner
                 JOIN project_issue_bug_introducing_commits pibic
                      ON pibic.bug_introducing_commits_sha = ppc.commits_sha
    ),

    pr_bic_rows AS (
        SELECT
            pp.project_owner,
            pp.project_name,
            pp.id AS analysis_id,

            TIMESTAMPDIFF(HOUR, pp.created_at, pp.merged_at) AS target_hours,

            COUNT(DISTINCT c.sha) AS commits_in_row,

            SUM(CASE
                    WHEN c.in_degree IS NOT NULL
                        AND c.out_degree IS NOT NULL
                        AND c.min_depth_of_commit_history IS NOT NULL
                        AND c.max_depth_of_commit_history IS NOT NULL
                        AND c.distance_to_branch_start IS NOT NULL
                        AND c.upstream_heads_unique_on_segment IS NOT NULL
                        AND c.days_since_last_merge_on_segment IS NOT NULL
                        AND c.number_of_branches IS NOT NULL
                        AND c.average_degree IS NOT NULL
                        THEN 1 ELSE 0
                END) AS graph_ready_commits,

            SUM(CASE
                    WHEN cc.total_changes IS NOT NULL
                        AND cc.files_changed IS NOT NULL
                        AND cc.files_changed > 0
                        THEN 1 ELSE 0
                END) AS churn_ready_commits,

            AVG(c.max_depth_of_commit_history - c.min_depth_of_commit_history) AS avg_depth_range,
            AVG(c.distance_to_branch_start) AS avg_fp_distance,
            AVG(c.upstream_heads_unique_on_segment) AS avg_upstream_heads,
            AVG(c.days_since_last_merge_on_segment) AS avg_days_since_merge,
            SUM(cc.total_changes) AS total_changes
        FROM project_pull pp
                 JOIN pr_bic_commits pbc
                      ON pbc.pr_id = pp.id
                          AND pbc.project_name = pp.project_name
                          AND pbc.project_owner = pp.project_owner
                 JOIN `commit` c
                      ON c.sha = pbc.sha
                 LEFT JOIN commit_churn cc
                           ON cc.sha = c.sha
        WHERE pp.state = 'closed'
          AND pp.created_at IS NOT NULL
          AND pp.merged_at IS NOT NULL
        GROUP BY
            pp.project_owner,
            pp.project_name,
            pp.id,
            pp.created_at,
            pp.merged_at
    ),

    all_rows AS (
        SELECT
            project_owner,
            project_name,
            'Issue-level fixing-commit dataset' AS dataset_name,
            'issue' AS analysis_unit,
            analysis_id,
            target_hours,
            commits_in_row,
            graph_ready_commits,
            churn_ready_commits,
            avg_depth_range,
            avg_fp_distance,
            avg_upstream_heads,
            avg_days_since_merge,
            total_changes
        FROM issue_fix_rows

        UNION ALL

        SELECT
            project_owner,
            project_name,
            'Issue-level candidate-BIC dataset' AS dataset_name,
            'issue' AS analysis_unit,
            analysis_id,
            target_hours,
            commits_in_row,
            graph_ready_commits,
            churn_ready_commits,
            avg_depth_range,
            avg_fp_distance,
            avg_upstream_heads,
            avg_days_since_merge,
            total_changes
        FROM issue_bic_rows

        UNION ALL

        SELECT
            project_owner,
            project_name,
            'PR-level candidate-BIC dataset' AS dataset_name,
            'pull request' AS analysis_unit,
            analysis_id,
            target_hours,
            commits_in_row,
            graph_ready_commits,
            churn_ready_commits,
            avg_depth_range,
            avg_fp_distance,
            avg_upstream_heads,
            avg_days_since_merge,
            total_changes
        FROM pr_bic_rows
    )

SELECT
    project_owner,
    project_name,
    dataset_name,
    analysis_unit,

    COUNT(*) AS analysis_rows,

    SUM(CASE WHEN target_hours IS NOT NULL AND target_hours >= 0 THEN 1 ELSE 0 END)
             AS rows_with_target_duration,

    ROUND(
            100.0 * SUM(CASE WHEN target_hours IS NOT NULL AND target_hours >= 0 THEN 1 ELSE 0 END)
                / NULLIF(COUNT(*), 0),
            2
    ) AS target_duration_computability_percent,

    SUM(CASE WHEN graph_ready_commits > 0 THEN 1 ELSE 0 END)
             AS rows_with_graph_summary,

    ROUND(
            100.0 * SUM(CASE WHEN graph_ready_commits > 0 THEN 1 ELSE 0 END)
                / NULLIF(COUNT(*), 0),
            2
    ) AS graph_summary_computability_percent,

    SUM(CASE WHEN graph_ready_commits = commits_in_row THEN 1 ELSE 0 END)
             AS rows_where_all_commits_have_graph_metrics,

    ROUND(
            100.0 * SUM(CASE WHEN graph_ready_commits = commits_in_row THEN 1 ELSE 0 END)
                / NULLIF(COUNT(*), 0),
            2
    ) AS all_commits_graph_complete_percent,

    SUM(CASE WHEN churn_ready_commits > 0 THEN 1 ELSE 0 END)
             AS rows_with_churn_summary,

    ROUND(
            100.0 * SUM(CASE WHEN churn_ready_commits > 0 THEN 1 ELSE 0 END)
                / NULLIF(COUNT(*), 0),
            2
    ) AS churn_summary_computability_percent,

    SUM(CASE WHEN graph_ready_commits > 0 AND churn_ready_commits > 0 THEN 1 ELSE 0 END)
             AS rows_with_graph_and_churn_summary,

    ROUND(
            100.0 * SUM(CASE WHEN graph_ready_commits > 0 AND churn_ready_commits > 0 THEN 1 ELSE 0 END)
                / NULLIF(COUNT(*), 0),
            2
    ) AS graph_churn_summary_computability_percent,

    ROUND(AVG(commits_in_row), 2) AS avg_commits_per_analysis_row,
    ROUND(AVG(graph_ready_commits), 2) AS avg_graph_ready_commits_per_row,
    ROUND(AVG(churn_ready_commits), 2) AS avg_churn_ready_commits_per_row,

    ROUND(AVG(avg_depth_range), 4) AS avg_depth_range,
    ROUND(AVG(avg_fp_distance), 4) AS avg_fp_distance,
    ROUND(AVG(avg_upstream_heads), 4) AS avg_upstream_heads,
    ROUND(AVG(avg_days_since_merge), 4) AS avg_days_since_merge,
    ROUND(AVG(total_changes), 4) AS avg_total_changes
FROM all_rows
GROUP BY
    project_owner,
    project_name,
    dataset_name,
    analysis_unit
ORDER BY
    project_owner,
    project_name,
    dataset_name;


select count(*) from file_change


GRANT SELECT, SHOW VIEW, TRIGGER, EVENT
    ON `qmodel_demo`.*
    TO 'root'@'%';