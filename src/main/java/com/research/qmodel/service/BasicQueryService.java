package com.research.qmodel.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.research.qmodel.model.AGraph;
import com.research.qmodel.model.Commit;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
public class BasicQueryService extends GitHubIssuesFetcher {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private static final Logger LOGGER = LoggerFactory.getLogger(BasicQueryService.class);
    @Value("${app.mode:prod}")
    private String mode;

    @Value("${qmodel.api.key}")
    private String[] apiKey;

    @Value("${app.page_size:100}")
    private int PAGE_SIZE;

    @Value("${app.base_url:https://api.github.com/}")
    private String BASE_URL;

    @Value("${app.demo_max_pages:3}")
    private int DEMO_MAX_PAGES;

    @Value("${app.demo_max_rows:1500}")
    private int DEMO_MAX_ROWS;

    @Value("${app.hard_max_pages:100000}")
    private int HARD_MAX_PAGES;

    public BasicQueryService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public <T> T retrieveMetrics(String url, TypeReference<T> targetType, String urlCondition, String pageCondition, boolean withCursor) {

        final List<JsonNode> all = new ArrayList<>();
        if (withCursor) {
            JsonNode body = getRowDataWithCursor(url);
            all.addAll(rowsFromBody(body));
            return objectMapper.convertValue(all, targetType);
        }

        int pageNumber = 1;
        int totalRows = 0;

        while (true) {
            PageChunk page = getRowDataPage(url, BASE_URL, pageNumber, PAGE_SIZE, urlCondition, pageCondition, false);
            if (page.rows.isEmpty()) break;
            all.addAll(page.rows);
            totalRows += page.rows.size();

            LOGGER.info("page={} rows={} totalRows={} hasNext={}", pageNumber, page.rows.size(), totalRows, page.hasNext);

            if (!shouldContinue(page.hasNext, pageNumber, totalRows)) break;
            pageNumber++;
        }

        return objectMapper.convertValue(all, targetType);
    }

    public <T> T retrievemetricsObject(String url, TypeReference<T> targetType, String urlCondition, String pageCondition) {

        List<JsonNode> container = new ArrayList<>();
        JsonNode body = getRowData(url, BASE_URL, 1, PAGE_SIZE, urlCondition, pageCondition, true);
        if (body != null && !body.isEmpty()) {
            container.add(objectMapper.convertValue(body, new TypeReference<JsonNode>() {
            }));
        }
        return objectMapper.convertValue(container, targetType);
    }

    public <T> void retrieveMetricsWithStreaming(String url, TypeReference<T> targetType, String urlCondition, String pageCondition, boolean withCursor, int flushEvery, Consumer<T> batchConsumer) {

        final int FLUSH_EVERY = Math.max(1, flushEvery);
        final List<JsonNode> buffer = new ArrayList<>(FLUSH_EVERY + 16);

        final Runnable flush = saveEntriesAndCleanupCache(targetType, batchConsumer, buffer);

        if (withCursor) {
            JsonNode body = getRowDataWithCursor(url);
            List<JsonNode> rows = rowsFromBody(body);
            int i = 0;
            while (i < rows.size()) {
                int end = Math.min(i + FLUSH_EVERY, rows.size());
                buffer.addAll(rows.subList(i, end));
                flush.run();
                i = end;
            }
            return;
        }

        int pageNumber = 1;
        int totalRows = 0;

        while (true) {
            PageChunk page = getRowDataPage(url, BASE_URL, pageNumber, PAGE_SIZE, urlCondition, pageCondition, false);
            if (page.rows.isEmpty()) break;

            buffer.addAll(page.rows);
            totalRows += page.rows.size();

            if (buffer.size() >= FLUSH_EVERY) {
                flush.run();
            }
            LOGGER.info("stream page={} pageRows={} buffer={} totalRows={} hasNext={}", pageNumber, page.rows.size(), buffer.size(), totalRows, page.hasNext);

            if (!shouldContinue(page.hasNext, pageNumber, totalRows)) break;
            pageNumber++;
        }

        flush.run();
    }

    private <T> Runnable saveEntriesAndCleanupCache(TypeReference<T> targetType, Consumer<T> batchConsumer, List<JsonNode> buffer) {
        final Runnable flush = () -> {
            if (buffer.isEmpty()) return;
            try {
                T batch = objectMapper.convertValue(buffer, targetType);
                batchConsumer.accept(batch);
            } catch (Exception ex) {
                LOGGER.error("Batch convert/persist failed: {}", ex.getMessage(), ex);
            } finally {
                buffer.clear();
                ArrayList<?> arr = (ArrayList<?>) buffer;
                try {
                    arr.trimToSize();
                } catch (Exception e) {
                    LOGGER.error("trimToSize failed: {}", e.getMessage(), e);
                }
            }
        };
        return flush;
    }

    public AGraph retrieveForks(String owner, String repo, Set<String> forks) {
        AGraph merged = null;
        for (String fork : forks) {
            String[] nm = fork.split("/");
            JsonNode branches = retrieveMetrics("%s" + String.format("repos/%s/%s/branches", nm[0], nm[1]) + "%s", new TypeReference<>() {
            }, "?", "&", false);

            for (JsonNode br : branches) {
                String commitId = StringUtils.substringAfterLast(br.path("commit").path("url").asText(), "/");
                AGraph ag = retrievemetricsObject("%s" + String.format("repos/%s/%s/commits/%s", owner, repo, commitId) + "%s", new TypeReference<>() {
                }, "?", "?");
                if (merged == null) {
                    merged = ag;
                } else if (ag != null && ag.getCommits() != null) {
                    for (Commit c : ag.getCommits()) merged.addCommit(c);
                }
            }
        }
        return merged;
    }

    public AGraph retrieveCommitBySha(String owner, String repo, String sha) {
        return retrievemetricsObject("%s" + String.format("repos/%s/%s/commits/%s", owner, repo, sha) + "%s", new TypeReference<>() {
        }, "?", "&");
    }

    public String getHtmlData(String url) {
        RequestEntity<Void> req = newGetRequest(url);
        if (req == null) return null;
        try {
            ResponseEntity<String> resp = restTemplate.exchange(req, String.class);
            return resp.getBody();
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            getNextKey(true);
            return null;
        }
    }

    private boolean isDemo() {
        return "demo".equalsIgnoreCase(mode);
    }

    private static final class PageChunk {
        final List<JsonNode> rows;
        final boolean hasNext;

        PageChunk(List<JsonNode> rows, boolean hasNext) {
            this.rows = rows;
            this.hasNext = hasNext;
        }
    }

    private static List<JsonNode> rowsFromBody(JsonNode body) {
        if (body == null || body.isNull()) return List.of();
        if (body.isArray()) {
            List<JsonNode> out = new ArrayList<>(body.size());
            body.forEach(out::add);
            return out;
        }
        return List.of(body);
    }

    private static boolean hasNextFromLink(HttpHeaders headers) {
        String link = headers.getFirst("Link");
        if (link == null) return false;
        for (String part : link.split(",")) {
            String seg = part.trim().toLowerCase(Locale.ROOT);
            if (seg.contains("rel=\"next\"")) return true;
        }
        return false;
    }


    private static boolean inferHasNextBySize(int pageSize, List<JsonNode> rows) {
        return rows.size() == pageSize;
    }

    private boolean shouldContinue(boolean hasNext, int pageNumber, int totalRows) {
        if (!hasNext) return false;
        if (isDemo()) {
            if (pageNumber >= DEMO_MAX_PAGES) return false;
            return totalRows < DEMO_MAX_ROWS;
        } else {
            return pageNumber < HARD_MAX_PAGES;
        }
    }

    private PageChunk getRowDataPage(String url, String baseUrl, int pageNumber, int pageSize, String urlCondition, String pageCondition, boolean skipPages) {

        final String apiUrl = skipPages ? String.format(url, baseUrl, "") : String.format(url, baseUrl, urlCondition + "page=" + pageNumber + pageCondition + "per_page=" + pageSize);

        ResponseEntity<JsonNode> resp = retrieveRowData(apiUrl);
        if (resp == null) return new PageChunk(List.of(), false);

        List<JsonNode> rows = rowsFromBody(resp.getBody());
        boolean hasNext = hasNextFromLink(resp.getHeaders());
        if (!hasNext && !resp.getHeaders().containsKey("Link")) {
            hasNext = inferHasNextBySize(pageSize, rows);
        }
        return new PageChunk(rows, hasNext);
    }

    public JsonNode getRowData(String url, String baseUrl, int pageNumber, int pageSize, String urlCondition, String pageCondition, boolean skipPages) {

        PageChunk pg = getRowDataPage(url, baseUrl, pageNumber, pageSize, urlCondition, pageCondition, skipPages);
        return objectMapper.valueToTree(pg.rows);
    }

    public JsonNode getRowDataWithCursor(String urlTemplate) {
        String full = parseToCommitUrl(urlTemplate);
        ResponseEntity<JsonNode> resp = retrieveRowData(full);
        return resp == null ? null : resp.getBody();
    }

    private RequestEntity<Void> newGetRequest(String url) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", getNextKey(false));
            return new RequestEntity<>(headers, HttpMethod.GET, new URI(url));
        } catch (Exception e) {
            LOGGER.error("newGetRequest failed: {}", e.getMessage(), e);
            return null;
        }
    }

    private ResponseEntity<JsonNode> retrieveRowData(String url) {
        try {
            RequestEntity<Void> req = newGetRequest(url);
            if (req == null) return null;
            return restTemplate.exchange(req, JsonNode.class);
        } catch (Exception first) {
            return restoreStateIfFailed(url);
        }
    }

    private ResponseEntity<JsonNode> restoreStateIfFailed(String url) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", getNextKey(true));
            RequestEntity<Void> req = new RequestEntity<>(headers, HttpMethod.GET, new URI(url));
            return restTemplate.exchange(req, JsonNode.class);
        } catch (Exception second) {
            LOGGER.error("GET failed for {}: {}", url, second.getMessage(), second);
            return null;
        }
    }

    String parseToCommitUrl(String urlFormat) {
        return String.format(urlFormat, BASE_URL, "");
    }

    public Set<String> getForks(String owner, String repo) {
        List<Map<String, JsonNode>> forks = retrieveMetrics("%s" + String.format("repos/%s/%s/forks", owner, repo) + "%s", new TypeReference<>() {
        }, "?", "&", false);
        return forks.parallelStream().map(o -> o.get("full_name").asText()).collect(Collectors.toSet());
    }
}
