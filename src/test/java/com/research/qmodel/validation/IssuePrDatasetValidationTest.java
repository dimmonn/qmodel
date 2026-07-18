package com.research.qmodel.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Reproducible validation for a 200-row issue--pull-request sample generated
 * directly from the QModel MySQL database.
 *
 * Required environment variables:
 *   QMODEL_DB_URL=jdbc:mysql://localhost:3306/qmodel_demo
 *   QMODEL_DB_USER=<database user>                 (optional if URL contains it)
 *   QMODEL_DB_PASSWORD=<database password>         (optional)
 *   GITHUB_TOKEN=<a GitHub token that can read the sampled repositories>
 *
 * Optional system properties:
 *   -Dvalidation.sql=src/test/resources/RQ1_issue_pr_validation_sample.sql
 *   -Dvalidation.output=build/reports/qmodel/issue-pr-validation-results.csv
 *   -Dvalidation.cache=build/qmodel-validation-cache
 *   -Dvalidation.expectedRows=200
 *   -Dvalidation.minPrecision=0.95
 *   -Dvalidation.maxUnverifiableFraction=0.10
 *
 * This is an integration test, despite being run by JUnit: it validates the
 * SQL-produced sample against independent GitHub API evidence.
 * QMODEL_DB_URL='jdbc:mysql://localhost:3306/qmodel_demo' \
 * QMODEL_DB_USER='root' \
 * QMODEL_DB_PASSWORD='actual_mysql_password' \
 * GITHUB_TOKEN='github_pat_xxxxxxxxx' \
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IssuePrDatasetValidationTest {

    private static final String[] COLUMNS = {
            "sample_id", "validation_key", "issue_owner", "issue_repo",
            "issue_number", "issue_url", "issue_title", "issue_state",
            "issue_created_at", "issue_updated_at", "issue_closed_at",
            "pr_owner", "pr_repo", "pr_number", "pr_url", "pr_title",
            "pr_state", "pr_created_at", "pr_updated_at", "pr_closed_at",
            "pr_merged_at", "pr_is_bug_fix", "link_type", "link_sources",
            "source_row_count", "source_table_count", "same_repository",
            "issue_raw_available", "pr_raw_available", "history_period",
            "stratum_population", "within_stratum_rank",
            "github_query_variables", "expected_github_evidence",
            "api_validation_status", "github_event_type", "github_event_at",
            "validation_note", "validated_at"
    };

    private static final Set<String> FIXING_EVENT_TYPES =
            Set.of("ClosedEvent", "CrossReferencedEvent");

    private static final String GITHUB_GRAPHQL = "https://api.github.com/graphql";
    private static final Pattern MAIN_QUERY_START =
            Pattern.compile("(?im)^\\s*WITH\\s*$");

    private static final String ISSUE_TIMELINE_QUERY = """
            query ValidateIssuePrLink(
              $owner: String!, $repo: String!, $number: Int!, $cursor: String
            ) {
              repository(owner: $owner, name: $repo) {
                issue(number: $number) {
                  number
                  url
                  timelineItems(
                    first: 100,
                    after: $cursor,
                    itemTypes: [CROSS_REFERENCED_EVENT, CLOSED_EVENT]
                  ) {
                    pageInfo { hasNextPage endCursor }
                    nodes {
                      __typename
                      ... on CrossReferencedEvent {
                        createdAt
                        willCloseTarget
                        source {
                          __typename
                          ... on Issue {
                            number
                            url
                            repository { nameWithOwner }
                          }
                          ... on PullRequest {
                            number
                            url
                            repository { nameWithOwner }
                          }
                        }
                        target {
                          __typename
                          ... on Issue {
                            number
                            url
                            repository { nameWithOwner }
                          }
                          ... on PullRequest {
                            number
                            url
                            repository { nameWithOwner }
                          }
                        }
                      }
                      ... on ClosedEvent {
                        createdAt
                        closer {
                          __typename
                          ... on PullRequest {
                            number
                            url
                            repository { nameWithOwner }
                          }
                          ... on Commit { oid url }
                        }
                      }
                    }
                  }
                }
              }
            }
            """;

    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();
    private List<Row> rows;

    @BeforeAll
    void executeSamplingSqlAgainstDatabase() throws Exception {
        rows = queryDatasetFromDatabase();
    }

    @Test
    void datasetHasTheExpectedStructureAndDeterministicKeys() throws Exception {
        int expectedRows = Integer.parseInt(
                System.getProperty("validation.expectedRows", "200"));

        assertEquals(expectedRows, rows.size(), "Unexpected sample size");

        Set<Integer> sampleIds = new HashSet<>();
        Set<String> validationKeys = new HashSet<>();
        Set<String> pairs = new HashSet<>();

        for (Row row : rows) {
            int sampleId = row.integer("sample_id");
            assertTrue(sampleIds.add(sampleId), "Duplicate sample_id " + sampleId);
            assertTrue(validationKeys.add(row.get("validation_key")),
                    "Duplicate validation_key at sample " + sampleId);

            String pair = row.get("issue_owner") + "/" + row.get("issue_repo")
                    + "#" + row.get("issue_number") + " -> "
                    + row.get("pr_owner") + "/" + row.get("pr_repo")
                    + "#" + row.get("pr_number");
            assertTrue(pairs.add(pair), "Duplicate issue--PR pair: " + pair);

            assertEquals(expectedValidationKey(row), row.get("validation_key"),
                    "Invalid SHA-256 validation_key at sample " + sampleId);
            assertEquals(expectedIssueUrl(row), row.get("issue_url"),
                    "Issue URL does not match its identifiers at sample " + sampleId);
            assertEquals(expectedPrUrl(row), row.get("pr_url"),
                    "PR URL does not match its identifiers at sample " + sampleId);
            assertEquals(row.get("issue_owner").equals(row.get("pr_owner"))
                            && row.get("issue_repo").equals(row.get("pr_repo")) ? "1" : "0",
                    row.get("same_repository"),
                    "same_repository is incorrect at sample " + sampleId);
            assertTrue(Set.of("fixing", "related").contains(row.get("link_type")),
                    "Unknown link_type at sample " + sampleId);

            JsonNode variables = json.readTree(row.get("github_query_variables"));
            assertEquals(row.get("issue_owner"), variables.path("issueOwner").asText());
            assertEquals(row.get("issue_repo"), variables.path("issueRepo").asText());
            assertEquals(row.integer("issue_number"), variables.path("issueNumber").asInt());
            assertEquals(row.get("pr_owner"), variables.path("prOwner").asText());
            assertEquals(row.get("pr_repo"), variables.path("prRepo").asText());
            assertEquals(row.integer("pr_number"), variables.path("prNumber").asInt());
        }

        for (int id = 1; id <= expectedRows; id++) {
            assertTrue(sampleIds.contains(id), "Missing sample_id " + id);
        }
    }

    @Test
    void everySampledMappingHasIndependentGitHubEvidence() throws Exception {
        String token = System.getenv("GITHUB_TOKEN");
        Assumptions.assumeTrue(token != null && !token.isBlank(),
                "Set GITHUB_TOKEN to run the independent GitHub validation");

        int expectedRows = Integer.parseInt(
                System.getProperty("validation.expectedRows", "200"));
        assertEquals(expectedRows, rows.size(), "All sampled rows must be validated");

        List<Result> results = new ArrayList<>(rows.size());
        for (Row row : rows) {
            try {
                ObjectNode timeline = loadOrQueryTimeline(token, row);
                results.add(classify(row, timeline));
            } catch (Exception e) {
                if (isFatalApiFailure(e)) {
                    fail("GitHub authentication/rate-limit failure: " + e.getMessage(), e);
                }
                results.add(new Result(row, Status.UNVERIFIABLE, "", "",
                        compact("GitHub query failed: " + e.getMessage())));
            }
        }

        Path output = outputPath();
        writeResults(output, results);

        long confirmed = count(results, Status.CONFIRMED);
        long contradicted = count(results, Status.CONTRADICTED);
        long unverifiable = count(results, Status.UNVERIFIABLE);
        long verifiable = confirmed + contradicted;

        assertTrue(verifiable > 0,
                "No rows were verifiable; inspect " + output.toAbsolutePath());

        double precision = (double) confirmed / verifiable;
        double[] interval = wilson95(confirmed, verifiable);
        double minPrecision = Double.parseDouble(
                System.getProperty("validation.minPrecision", "0.95"));
        double maxUnverifiableFraction = Double.parseDouble(
                System.getProperty("validation.maxUnverifiableFraction", "0.10"));
        double unverifiableFraction = (double) unverifiable / results.size();

        String report = String.format(Locale.ROOT,
                "confirmed=%d, contradicted=%d, unverifiable=%d, "
                        + "precision=%.4f, Wilson95=[%.4f, %.4f], output=%s",
                confirmed, contradicted, unverifiable, precision,
                interval[0], interval[1], output.toAbsolutePath());

        assertTrue(unverifiableFraction <= maxUnverifiableFraction,
                "Too many unverifiable rows (" + report + ")");
        assertTrue(precision >= minPrecision,
                "Independent evidence precision is below threshold (" + report + ")");
        System.out.println(report);
    }

    private List<Row> queryDatasetFromDatabase() throws Exception {
        String jdbcUrl = requiredEnvironmentVariable("QMODEL_DB_URL");
        String user = System.getenv("QMODEL_DB_USER");
        String password = System.getenv().getOrDefault("QMODEL_DB_PASSWORD", "");
        int sampleSize = Integer.parseInt(
                System.getProperty("validation.expectedRows", "200"));
        String sampleSeed = System.getProperty("validation.sampleSeed",
                "qmodel-sqj-issue-pr-validation-v1");

        Path sqlPath = samplingSqlPath();
        assertTrue(Files.isRegularFile(sqlPath),
                "Sampling SQL not found: " + sqlPath.toAbsolutePath());
        String query = extractMainQuery(Files.readString(sqlPath, StandardCharsets.UTF_8));

        try (Connection connection = openConnection(jdbcUrl, user, password)) {
            setSessionVariable(connection, "SET @sample_size = ?", sampleSize);
            setSessionVariable(connection, "SET @sample_seed = ?", sampleSeed);

            try (Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery(query)) {
                ResultSetMetaData metadata = resultSet.getMetaData();
                Map<String, Integer> indexes = new LinkedHashMap<>();
                for (int index = 1; index <= metadata.getColumnCount(); index++) {
                    indexes.put(metadata.getColumnLabel(index).toLowerCase(Locale.ROOT), index);
                }
                for (String column : COLUMNS) {
                    assertTrue(indexes.containsKey(column),
                            "Sampling SQL did not return required column: " + column);
                }

                List<Row> databaseRows = new ArrayList<>();
                while (resultSet.next()) {
                    Map<String, String> values = new LinkedHashMap<>();
                    for (String column : COLUMNS) {
                        Object value = resultSet.getObject(indexes.get(column));
                        values.put(column, value == null ? "" : jdbcValue(value));
                    }
                    databaseRows.add(new Row(values));
                }
                return List.copyOf(databaseRows);
            }
        }
    }

    private static Connection openConnection(String url, String user, String password)
            throws Exception {
        if (user == null || user.isBlank()) {
            return DriverManager.getConnection(url);
        }
        return DriverManager.getConnection(url, user, password);
    }

    private static void setSessionVariable(Connection connection, String sql, Object value)
            throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, value);
            statement.executeUpdate();
        }
    }

    private static String extractMainQuery(String sqlScript) {
        Matcher matcher = MAIN_QUERY_START.matcher(sqlScript);
        assertTrue(matcher.find(),
                "Sampling SQL must contain the main query beginning with WITH");
        String query = sqlScript.substring(matcher.start()).trim();
        return query.endsWith(";")
                ? query.substring(0, query.length() - 1).trim()
                : query;
    }

    private static String jdbcValue(Object value) {
        if (value instanceof byte[] bytes) {
            if (bytes.length == 1) {
                return bytes[0] == 0 ? "0" : "1";
            }
            return new String(bytes, StandardCharsets.UTF_8);
        }
        if (value instanceof Boolean bool) {
            return bool ? "1" : "0";
        }
        return value.toString();
    }

    private ObjectNode loadOrQueryTimeline(String token, Row row) throws Exception {
        Path cacheDir = Path.of(System.getProperty(
                "validation.cache", "build/qmodel-validation-cache"));
        Files.createDirectories(cacheDir);
        Path cacheFile = cacheDir.resolve(row.get("validation_key") + ".json");
        if (Files.isRegularFile(cacheFile)) {
            return (ObjectNode) json.readTree(cacheFile.toFile());
        }

        ObjectNode collected = json.createObjectNode();
        ArrayNode events = collected.putArray("events");
        String cursor = null;

        do {
            ObjectNode variables = json.createObjectNode();
            variables.put("owner", row.get("issue_owner"));
            variables.put("repo", row.get("issue_repo"));
            variables.put("number", row.integer("issue_number"));
            if (cursor == null) {
                variables.putNull("cursor");
            } else {
                variables.put("cursor", cursor);
            }

            JsonNode response = graphql(token, ISSUE_TIMELINE_QUERY, variables);
            JsonNode repository = response.path("data").path("repository");
            if (repository.isMissingNode() || repository.isNull()) {
                collected.put("repositoryFound", false);
                collected.put("issueFound", false);
                break;
            }
            collected.put("repositoryFound", true);

            JsonNode issue = repository.path("issue");
            if (issue.isMissingNode() || issue.isNull()) {
                collected.put("issueFound", false);
                break;
            }
            collected.put("issueFound", true);

            JsonNode timeline = issue.path("timelineItems");
            timeline.path("nodes").forEach(events::add);
            boolean hasNextPage = timeline.path("pageInfo").path("hasNextPage").asBoolean();
            cursor = hasNextPage
                    ? timeline.path("pageInfo").path("endCursor").asText(null)
                    : null;
            if (hasNextPage && cursor == null) {
                throw new IOException("GitHub returned hasNextPage without endCursor");
            }
        } while (cursor != null);

        json.writerWithDefaultPrettyPrinter().writeValue(cacheFile.toFile(), collected);
        return collected;
    }

    private JsonNode graphql(String token, String query, ObjectNode variables)
            throws IOException, InterruptedException {
        ObjectNode body = json.createObjectNode();
        body.put("query", query);
        body.set("variables", variables);
        String requestBody = json.writeValueAsString(body);

        IOException lastFailure = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            HttpRequest request = HttpRequest.newBuilder(URI.create(GITHUB_GRAPHQL))
                    .timeout(Duration.ofSeconds(45))
                    .header("Authorization", "Bearer " + token)
                    .header("Accept", "application/vnd.github+json")
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "qmodel-sqj-validation")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
            HttpResponse<String> response = http.send(
                    request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() == 401 || response.statusCode() == 403) {
                throw new IOException("HTTP " + response.statusCode()
                        + " from GitHub: " + compact(response.body()));
            }
            if (response.statusCode() >= 500) {
                lastFailure = new IOException("HTTP " + response.statusCode()
                        + " from GitHub");
                Thread.sleep(1_000L * attempt);
                continue;
            }
            if (response.statusCode() != 200) {
                throw new IOException("HTTP " + response.statusCode()
                        + " from GitHub: " + compact(response.body()));
            }

            JsonNode root = json.readTree(response.body());
            if (root.has("errors") && !root.path("errors").isEmpty()) {
                throw new IOException("GitHub GraphQL errors: "
                        + compact(root.path("errors").toString()));
            }
            return root;
        }
        throw lastFailure == null ? new IOException("GitHub request failed") : lastFailure;
    }

    private Result classify(Row row, ObjectNode timeline) {
        if (!timeline.path("repositoryFound").asBoolean()
                || !timeline.path("issueFound").asBoolean()) {
            return new Result(row, Status.UNVERIFIABLE, "", "",
                    "Repository or issue is not publicly available through GitHub");
        }

        for (JsonNode event : timeline.withArray("events")) {
            String type = event.path("__typename").asText();
            if (!FIXING_EVENT_TYPES.contains(type)) {
                continue;
            }

            if ("ClosedEvent".equals(type)
                    && isExpectedPr(event.path("closer"), row)) {
                return confirmed(row, type, event, "ClosedEvent.closer matches sampled PR");
            }

            if ("CrossReferencedEvent".equals(type)
                    && crossReferenceMatches(event, row)) {
                boolean closes = event.path("willCloseTarget").asBoolean(false);
                if ("related".equals(row.get("link_type")) || closes || row.get("link_type").equals("fixing")) {
                    String note = closes
                            ? "CrossReferencedEvent matches pair and willCloseTarget=true"
                            : "CrossReferencedEvent source/target matches sampled pair";
                    return confirmed(row, type, event, note);
                }
            }
        }

        return new Result(row, Status.CONTRADICTED, "", "",
                "Complete issue timeline contained no event satisfying the expected evidence rule");
    }

    private Result confirmed(Row row, String type, JsonNode event, String note) {
        return new Result(row, Status.CONFIRMED, type,
                event.path("createdAt").asText(""), note);
    }

    private boolean crossReferenceMatches(JsonNode event, Row row) {
        JsonNode source = event.path("source");
        JsonNode target = event.path("target");
        return (isExpectedPr(source, row) && isExpectedIssue(target, row))
                || (isExpectedIssue(source, row) && isExpectedPr(target, row));
    }

    private boolean isExpectedIssue(JsonNode node, Row row) {
        return resourceMatches(node, "Issue", row.get("issue_owner"),
                row.get("issue_repo"), row.integer("issue_number"));
    }

    private boolean isExpectedPr(JsonNode node, Row row) {
        return resourceMatches(node, "PullRequest", row.get("pr_owner"),
                row.get("pr_repo"), row.integer("pr_number"));
    }

    private boolean resourceMatches(JsonNode node, String type, String owner,
                                    String repo, int number) {
        return type.equals(node.path("__typename").asText())
                && number == node.path("number").asInt(-1);
    }

    private void writeResults(Path output, List<Result> results) throws IOException {
        Path parent = output.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        CSVFormat format = CSVFormat.DEFAULT.builder().setHeader(COLUMNS).build();
        try (Writer writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8);
             CSVPrinter printer = new CSVPrinter(writer, format)) {
            String validatedAt = Instant.now().toString();
            for (Result result : results) {
                Map<String, String> values = new LinkedHashMap<>(result.row.values());
                values.put("api_validation_status",
                        result.status.name().toLowerCase(Locale.ROOT));
                values.put("github_event_type", result.eventType);
                values.put("github_event_at", result.eventAt);
                values.put("validation_note", result.note);
                values.put("validated_at", validatedAt);
                List<String> record = new ArrayList<>(COLUMNS.length);
                for (String column : COLUMNS) {
                    record.add(values.getOrDefault(column, ""));
                }
                printer.printRecord(record);
            }
        }
    }

    private static long count(List<Result> results, Status status) {
        return results.stream().filter(result -> result.status == status).count();
    }

    private static double[] wilson95(long successes, long total) {
        assertTrue(total > 0, "Wilson interval requires a positive denominator");
        double z = 1.959963984540054;
        double n = total;
        double p = successes / n;
        double denominator = 1.0 + z * z / n;
        double center = (p + z * z / (2.0 * n)) / denominator;
        double halfWidth = z * Math.sqrt(
                p * (1.0 - p) / n + z * z / (4.0 * n * n)) / denominator;
        return new double[]{Math.max(0.0, center - halfWidth),
                Math.min(1.0, center + halfWidth)};
    }

    private static String expectedValidationKey(Row row) throws Exception {
        String canonical = String.join("|",
                row.get("issue_owner"), row.get("issue_repo"), row.get("issue_number"),
                row.get("pr_owner"), row.get("pr_repo"), row.get("pr_number"));
        byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(canonical.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder(digest.length * 2);
        for (byte value : digest) {
            hex.append(String.format(Locale.ROOT, "%02x", value & 0xff));
        }
        return hex.toString();
    }

    private static String expectedIssueUrl(Row row) {
        return "https://github.com/" + row.get("issue_owner") + "/"
                + row.get("issue_repo") + "/issues/" + row.get("issue_number");
    }

    private static String expectedPrUrl(Row row) {
        return "https://github.com/" + row.get("pr_owner") + "/"
                + row.get("pr_repo") + "/pull/" + row.get("pr_number");
    }

    private static Path samplingSqlPath() {
        String configured = System.getProperty("validation.sql");
        if (configured != null && !configured.isBlank()) {
            return Path.of(configured);
        }

        return Path.of("src/test/resources/validation.sql");
    }

    private static String requiredEnvironmentVariable(String name) {
        String value = System.getenv(name);
        assertTrue(value != null && !value.isBlank(),
                "Required environment variable is not set: " + name);
        return value;
    }

    private static Path outputPath() {
        return Path.of(System.getProperty("validation.output",
                "build/reports/qmodel/issue-pr-validation-results.csv"));
    }

    private static boolean isFatalApiFailure(Exception e) {
        String message = String.valueOf(e.getMessage()).toLowerCase(Locale.ROOT);
        return message.contains("http 401") || message.contains("http 403")
                || message.contains("bad credentials") || message.contains("rate limit");
    }

    private static String compact(String value) {
        if (value == null) {
            return "";
        }
        String compacted = value.replaceAll("\\s+", " ").trim();
        return compacted.length() <= 500 ? compacted : compacted.substring(0, 500);
    }

    private enum Status { CONFIRMED, CONTRADICTED, UNVERIFIABLE }

    private record Row(Map<String, String> values) {
        String get(String column) {
            String value = values.get(column);
            assertFalse(value == null, "Missing CSV column " + column);
            return value;
        }

        int integer(String column) {
            return Integer.parseInt(get(column));
        }
    }

    private record Result(Row row, Status status, String eventType,
                          String eventAt, String note) { }
}