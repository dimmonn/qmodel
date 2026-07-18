/*
Database storage and table-size report.

Database: MySQL 8+
Schema:   qmodel_demo

Warning: exact COUNT(*) scans every InnoDB table and may take some time.
Run this report against the frozen research database snapshot.
*/

SET @schema_name := 'qmodel_demo';
SET SESSION group_concat_max_len = 1000000;


/* =========================================================
   1. Database environment
   ========================================================= */

SELECT
    VERSION()                         AS mysql_version,
    @schema_name                      AS database_name,
    @@character_set_server            AS server_character_set,
    @@collation_server                AS server_collation,
    @@innodb_page_size                AS innodb_page_size_bytes,
    @@max_allowed_packet              AS max_allowed_packet_bytes;


/* =========================================================
   2. Calculate exact row counts
   ========================================================= */

DROP TEMPORARY TABLE IF EXISTS exact_table_rows;

SELECT GROUP_CONCAT(
               CONCAT(
                       'SELECT ',
                       QUOTE(table_name),
                       ' AS table_name, COUNT(*) AS exact_rows ',
                       'FROM `',
                       REPLACE(table_schema, '`', '``'),
                       '`.`',
                       REPLACE(table_name, '`', '``'),
                       '`'
               )
               SEPARATOR ' UNION ALL '
       )
INTO @row_count_query
FROM information_schema.tables
WHERE table_schema = @schema_name
  AND table_type = 'BASE TABLE';

SET @row_count_query = CONCAT(
        'CREATE TEMPORARY TABLE exact_table_rows AS ',
        @row_count_query
                       );

PREPARE row_count_statement FROM @row_count_query;
EXECUTE row_count_statement;
DEALLOCATE PREPARE row_count_statement;


/* =========================================================
   3. Overall database requirements
   ========================================================= */

SELECT
    @schema_name AS database_name,

    COUNT(*) AS table_count,

    SUM(r.exact_rows) AS exact_total_rows,

    ROUND(
            SUM(t.data_length) / POWER(1024, 2),
            2
    ) AS data_size_mb,

    ROUND(
            SUM(t.index_length) / POWER(1024, 2),
            2
    ) AS index_size_mb,

    ROUND(
            SUM(t.data_length + t.index_length) / POWER(1024, 2),
            2
    ) AS total_size_mb,

    ROUND(
            SUM(t.data_length + t.index_length) / POWER(1024, 3),
            3
    ) AS total_size_gb,

    ROUND(
            SUM(t.data_free) / POWER(1024, 2),
            2
    ) AS allocated_free_space_mb,

    ROUND(
            2 * SUM(t.data_length + t.index_length) / POWER(1024, 3),
            3
    ) AS database_plus_one_backup_gb,

    ROUND(
            SUM(t.data_length + t.index_length)
                / NULLIF(SUM(r.exact_rows), 0),
            2
    ) AS average_bytes_per_row

FROM information_schema.tables t
         JOIN exact_table_rows r
              ON r.table_name = t.table_name
WHERE t.table_schema = @schema_name
  AND t.table_type = 'BASE TABLE';


/* =========================================================
   4. Per-table storage statistics
   ========================================================= */

SELECT
    t.table_name,

    r.exact_rows,

    t.engine,

    t.row_format,

    ROUND(
            t.data_length / POWER(1024, 2),
            2
    ) AS data_size_mb,

    ROUND(
            t.index_length / POWER(1024, 2),
            2
    ) AS index_size_mb,

    ROUND(
            (t.data_length + t.index_length) / POWER(1024, 2),
            2
    ) AS total_size_mb,

    ROUND(
            100.0 * (t.data_length + t.index_length)
                / NULLIF(
                    SUM(t.data_length + t.index_length) OVER (),
                    0
                  ),
            2
    ) AS percentage_of_database,

    ROUND(
            (t.data_length + t.index_length)
                / NULLIF(r.exact_rows, 0),
            2
    ) AS average_bytes_per_row,

    ROUND(
            t.data_free / POWER(1024, 2),
            2
    ) AS allocated_free_space_mb,

    t.create_time,
    t.update_time

FROM information_schema.tables t
         JOIN exact_table_rows r
              ON r.table_name = t.table_name
WHERE t.table_schema = @schema_name
  AND t.table_type = 'BASE TABLE'
ORDER BY
    t.data_length + t.index_length DESC;


/* =========================================================
   5. Largest tables
   ========================================================= */

SELECT
    t.table_name,
    r.exact_rows,

    ROUND(
            (t.data_length + t.index_length) / POWER(1024, 2),
            2
    ) AS total_size_mb,

    ROUND(
            t.data_length / POWER(1024, 2),
            2
    ) AS data_size_mb,

    ROUND(
            t.index_length / POWER(1024, 2),
            2
    ) AS index_size_mb

FROM information_schema.tables t
         JOIN exact_table_rows r
              ON r.table_name = t.table_name
WHERE t.table_schema = @schema_name
  AND t.table_type = 'BASE TABLE'
ORDER BY
    t.data_length + t.index_length DESC
LIMIT 15;


/* =========================================================
   6. Tables with the most rows
   ========================================================= */

SELECT
    t.table_name,
    r.exact_rows,

    ROUND(
            (t.data_length + t.index_length) / POWER(1024, 2),
            2
    ) AS total_size_mb,

    ROUND(
            (t.data_length + t.index_length)
                / NULLIF(r.exact_rows, 0),
            2
    ) AS average_bytes_per_row

FROM information_schema.tables t
         JOIN exact_table_rows r
              ON r.table_name = t.table_name
WHERE t.table_schema = @schema_name
  AND t.table_type = 'BASE TABLE'
ORDER BY
    r.exact_rows DESC
LIMIT 15;



