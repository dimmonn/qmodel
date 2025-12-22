package com.research.qmodel.annotations;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.research.qmodel.model.Action;
import com.research.qmodel.model.Commit;
import com.research.qmodel.model.CommitID;
import com.research.qmodel.repos.CommitRepository;
import com.research.qmodel.service.DataPersistance;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.research.qmodel.dto.PARSING_PROPERTIES.WORKFLOW_RUNS;

@Component
public class ActionsDeserializer extends JsonDeserializer<List<Action>> {
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    private final Logger LOGGER = LoggerFactory.getLogger(ActionsDeserializer.class);

    @Override
    public List<Action> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
            throws IOException {
        JsonNode nodes = jsonParser.getCodec().readTree(jsonParser);
        if (nodes.get(0).path("check_runs").isEmpty()) {
            return null;
        }
        List<Action> actions = new ArrayList<>();
        for (JsonNode node : nodes) {
            JsonNode runs = node.path("check_runs");
            for (JsonNode run : runs) {
                Action action = new Action();
                String status = run.path("status").asText();
                String result = run.path("conclusion").asText();
                String startedAt = run.path("started_at").asText();
                String completedAt = run.path("completed_at").asText();
                String title = run.path("output").path("title").asText();
                String summary = run.path("output").path("summary").asText();
                String text = run.path("output").path("text").asText();
                if (StringUtils.isNotEmpty(text)) {
                    int failed = getInt(text, "Failed:\\s*(\\d+)");
                    int passed = getInt(text, "Passed:\\s*([0-9,]+)");
                    int other = getInt(text, "Other:\\s*([0-9,]+)");
                    int total = getInt(text, "Total:\\s*([0-9,]+)");
                    double failedPercent = getDouble(text, "Failed:\\s*\\d+\\s*\\(([^%]+)%");
                    double passedPercent = getDouble(text, "Passed:\\s*[0-9,]+\\s*\\(([^%]+)%");
                    double otherPercent = getDouble(text, "Other:\\s*[0-9,]+\\s*\\(([^%]+)%");
                    action.setFailed(failed);
                    action.setPassed(passed);
                    action.setOther(other);
                    action.setTotal(total);
                    action.setFailedPercent(failedPercent);
                    action.setPassedPercent(passedPercent);
                    action.setOtherPercent(otherPercent);
                }

                String name = run.path("name").asText();
                String description = run.path("app").path("description").asText();
                String events = run.path("app").path("events").toString();
                try {
                    action.setStartedAt(dateFormat.parse(startedAt));
                    action.setCompletedAt(dateFormat.parse(completedAt));
                } catch (ParseException e) {
                    LOGGER.error("Failed to parse time", e);
                }
                action.setStatus(status);
                action.setResult(result);
                action.setTitle(title);
                action.setSummary(summary);
                action.setText(text);
                action.setName(name);
                action.setDescription(description);
                action.setEvents(events);
                actions.add(action);
            }

        }
        return actions;
    }

    private int getInt(String text, String regex) {
        Matcher m = Pattern.compile(regex).matcher(text);
        return m.find() ? Integer.parseInt(m.group(1).replace(",", "")) : 0;
    }

    private double getDouble(String text, String regex) {
        Matcher m = Pattern.compile(regex).matcher(text);
        return m.find() ? Double.parseDouble(m.group(1)) : 0.0;
    }
}
