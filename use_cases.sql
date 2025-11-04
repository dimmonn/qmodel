/*
Impact of Developer Inactivity on Issue and PR Resolution Times*/
select
    i.id as issue_id,
    i.created_at,
    i.closed_at,
    TIMESTAMPDIFF(MINUTE, i.created_at, i.closed_at) as issue_resolution_time,
    c.author as developer,
    max(c.commit_date) as last_commit_before_issue
from
    project_issue i
join
    project_issue_fixing_commits pic on i.id = pic.project_issue_id
join
    commit c ON pic.commits_sha = c.sha
WHERE
    c.commit_date < i.created_at
GROUP BY
    i.id, i.created_at, i.closed_at, c.author;
