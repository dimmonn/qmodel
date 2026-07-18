
/*
Purpose
-------
Prepare a deterministic sample of issue-pull-request relations for independent
validation against the GitHub GraphQL API.

Database: MySQL 8+
Output:   At most @sample_size rows (200 by default), ready for CSV/Parquet
          export and API validation.

Important
---------
This query samples QModel-positive mappings, so the later API check estimates
mapping precision. Estimating recall requires the reverse experiment: start
from a sample of GitHub-confirmed issue-PR links and check whether QModel
contains them.
*/

SET @sample_size := 200;
SET @sample_seed := 'qmodel-sqj-issue-pr-validation-v1';

WITH
/*
Collect every schema path that can express an issue-PR relationship.
is_fixing = 1 is reserved for the direct fixing-PR fields on project_issue.
The ORM join tables are retained as related links unless the application stores
more specific provenance elsewhere.
*/
    raw_links AS (
        SELECT
            x.project_issue_project_owner AS issue_owner,
            x.project_issue_project_name  AS issue_repo,
            x.project_issue_id            AS issue_number,
            x.project_pull_project_owner  AS pr_owner,
            x.project_pull_project_name   AS pr_repo,
            x.project_pull_id             AS pr_number,
            0                             AS is_fixing,
            'project_issue_project_pull'  AS link_source
        FROM qmodel_demo.project_issue_project_pull x

        UNION ALL

        SELECT
            x.project_issue_project_owner,
            x.project_issue_project_name,
            x.project_issue_id,
            x.project_pull_project_owner,
            x.project_pull_project_name,
            x.project_pull_id,
            0,
            'project_pull_project_issue'
        FROM qmodel_demo.project_pull_project_issue x

        UNION ALL

        SELECT
            x.project_issues_project_owner,
            x.project_issues_project_name,
            x.project_issues_id,
            x.project_pull_project_owner,
            x.project_pull_project_name,
            x.project_pull_id,
            0,
            'project_pull_project_issues'
        FROM qmodel_demo.project_pull_project_issues x

        UNION ALL

        SELECT
            i.project_owner,
            i.project_name,
            i.id,
            i.fixpr_project_owner,
            i.fixpr_project_name,
            i.fixpr_id,
            1,
            'project_issue.fixpr_fk'
        FROM qmodel_demo.project_issue i
        WHERE i.fixpr_id IS NOT NULL
          AND i.fixpr_project_owner IS NOT NULL
          AND i.fixpr_project_name IS NOT NULL

        UNION ALL

        /* Legacy same-repository fixing-PR identifier. */
        SELECT
            i.project_owner,
            i.project_name,
            i.id,
            i.project_owner,
            i.project_name,
            i.fix_pr,
            1,
            'project_issue.fix_pr_legacy'
        FROM qmodel_demo.project_issue i
        WHERE i.fix_pr IS NOT NULL
    ),

/*
Collapse duplicate paths to one canonical issue-PR pair, while retaining every
table through which the relationship was observed.
*/
    canonical_links AS (
        SELECT
            issue_owner,
            issue_repo,
            issue_number,
            pr_owner,
            pr_repo,
            pr_number,
            MAX(is_fixing) AS is_fixing,
            COUNT(*) AS source_row_count,
            COUNT(DISTINCT link_source) AS source_table_count,
            GROUP_CONCAT(
                    DISTINCT link_source
                    ORDER BY link_source
                    SEPARATOR ','
            ) AS link_sources
        FROM raw_links
        WHERE issue_owner IS NOT NULL
          AND issue_repo IS NOT NULL
          AND issue_number IS NOT NULL
          AND pr_owner IS NOT NULL
          AND pr_repo IS NOT NULL
          AND pr_number IS NOT NULL
        GROUP BY
            issue_owner,
            issue_repo,
            issue_number,
            pr_owner,
            pr_repo,
            pr_number
    ),

/*
Join the canonical keys back to their artifacts. This excludes dangling legacy
identifiers from the API-validation sample but exposes useful integrity flags.
Remove the two owner predicates to validate every mined project.
*/
    eligible_links AS (
        SELECT
            SHA2(
                    CONCAT_WS(
                            '|',
                            c.issue_owner,
                            c.issue_repo,
                            c.issue_number,
                            c.pr_owner,
                            c.pr_repo,
                            c.pr_number
                    ),
                    256
            ) AS validation_key,

            c.issue_owner,
            c.issue_repo,
            c.issue_number,
            c.pr_owner,
            c.pr_repo,
            c.pr_number,

            CASE WHEN c.is_fixing = 1 THEN 'fixing' ELSE 'related' END
              AS link_type,
            c.link_sources,
            c.source_row_count,
            c.source_table_count,

            i.title      AS issue_title,
            i.state      AS issue_state,
            i.created_at AS issue_created_at,
            i.updated_at AS issue_updated_at,
            i.closed_at  AS issue_closed_at,

            p.title      AS pr_title,
            p.state      AS pr_state,
            p.created_at AS pr_created_at,
            p.updated_at AS pr_updated_at,
            p.closed_at  AS pr_closed_at,
            p.merged_at  AS pr_merged_at,
            p.is_bug_fix AS pr_is_bug_fix,

            CASE
                WHEN c.issue_owner = c.pr_owner
                    AND c.issue_repo  = c.pr_repo
                    THEN 1 ELSE 0
                END AS same_repository,

            CASE WHEN i.raw_issue IS NOT NULL THEN 1 ELSE 0 END
                AS issue_raw_available,
            CASE WHEN p.raw_pull IS NOT NULL THEN 1 ELSE 0 END
                AS pr_raw_available,

            COALESCE(i.created_at, p.created_at) AS history_timestamp,

            CONCAT(
                    'https://github.com/',
                    c.issue_owner, '/', c.issue_repo,
                    '/issues/', c.issue_number
            ) AS issue_url,

            CONCAT(
                    'https://github.com/',
                    c.pr_owner, '/', c.pr_repo,
                    '/pull/', c.pr_number
            ) AS pr_url,

            JSON_OBJECT(
                    'issueOwner',  c.issue_owner,
                    'issueRepo',   c.issue_repo,
                    'issueNumber', c.issue_number,
                    'prOwner',     c.pr_owner,
                    'prRepo',      c.pr_repo,
                    'prNumber',    c.pr_number
            ) AS github_query_variables,

            CASE
                WHEN c.is_fixing = 1 THEN
                    'ClosedEvent.closer matches PR OR CrossReferencedEvent.willCloseTarget=true'
                ELSE
                    'CrossReferencedEvent source/target matches issue and PR'
                END AS expected_github_evidence

        FROM canonical_links c
                 JOIN qmodel_demo.project_issue i
                      ON i.project_owner = c.issue_owner
                          AND i.project_name  = c.issue_repo
                          AND i.id            = c.issue_number
                 JOIN qmodel_demo.project_pull p
                      ON p.project_owner = c.pr_owner
                          AND p.project_name  = c.pr_repo
                          AND p.id            = c.pr_number
        WHERE c.issue_owner IN ('ansible', 'facebook')
    ),

/* Divide each repository's eligible links into older and recent halves. */
    history_buckets AS (
        SELECT
            e.*,
            NTILE(2) OVER (
                PARTITION BY e.issue_owner, e.issue_repo
                ORDER BY
                    e.history_timestamp,
                    e.issue_number,
                    e.pr_owner,
                    e.pr_repo,
                    e.pr_number
                ) AS history_bucket
        FROM eligible_links e
    ),

/* Deterministically randomize within project x period x link-type strata. */
    within_strata AS (
        SELECT
            h.*,
            CASE WHEN h.history_bucket = 1 THEN 'older' ELSE 'recent' END
                  AS history_period,
            COUNT(*) OVER (
                PARTITION BY
                    h.issue_owner,
                    h.issue_repo,
                    h.history_bucket,
                    h.link_type
                ) AS stratum_population,
            ROW_NUMBER() OVER (
                PARTITION BY
                    h.issue_owner,
                    h.issue_repo,
                    h.history_bucket,
                    h.link_type
                ORDER BY
                    SHA2(
                            CONCAT_WS('|', @sample_seed, h.validation_key),
                            256
                    ),
                    h.validation_key
                ) AS within_stratum_rank
        FROM history_buckets h
    ),

/*
Round-robin ordering balances the sample across non-empty strata. If one
stratum has fewer rows, the other strata automatically fill the remainder.
*/
    sample_order AS (
        SELECT
            s.*,
            ROW_NUMBER() OVER (
                ORDER BY
                    s.within_stratum_rank,
                    SHA2(
                            CONCAT_WS(
                                    '|',
                                    @sample_seed,
                                    'stratum',
                                    s.issue_owner,
                                    s.issue_repo,
                                    s.history_bucket,
                                    s.link_type
                            ),
                            256
                    ),
                    s.validation_key
                ) AS validation_sample_rank
        FROM within_strata s
    )

SELECT
    validation_sample_rank AS sample_id,
    validation_key,

    issue_owner,
    issue_repo,
    issue_number,
    issue_url,
    issue_title,
    issue_state,
    issue_created_at,
    issue_updated_at,
    issue_closed_at,

    pr_owner,
    pr_repo,
    pr_number,
    pr_url,
    pr_title,
    pr_state,
    pr_created_at,
    pr_updated_at,
    pr_closed_at,
    pr_merged_at,
    pr_is_bug_fix,

    link_type,
    link_sources,
    source_row_count,
    source_table_count,
    same_repository,
    issue_raw_available,
    pr_raw_available,
    history_period,
    stratum_population,
    within_stratum_rank,

    github_query_variables,
    expected_github_evidence,

    /* Columns to be populated by the independent API-validation runner. */
    CAST(NULL AS CHAR(16))   AS api_validation_status,
    CAST(NULL AS CHAR(64))   AS github_event_type,
    CAST(NULL AS DATETIME)   AS github_event_at,
    CAST(NULL AS CHAR(1024)) AS validation_note,
    CAST(NULL AS DATETIME)   AS validated_at

FROM sample_order
WHERE validation_sample_rank <= @sample_size
ORDER BY validation_sample_rank;
