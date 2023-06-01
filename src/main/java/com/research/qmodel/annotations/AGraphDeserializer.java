package com.research.qmodel.annotations;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.research.qmodel.model.AGraph;
import com.research.qmodel.model.Commit;
import com.research.qmodel.model.Project;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class AGraphDeserializer extends JsonDeserializer<AGraph> {

    @Override
    public AGraph deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        AGraph aGraph = new AGraph();
        aGraph.setGraph(node.toString());
        for (JsonNode rawCommit : node) {
            if (rawCommit.get("commit") != null && rawCommit.get("commit").get("author") != null && rawCommit.get("commit").get("author").get("date") != null) {
                Commit commit = new Commit();
                DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT;
                Instant instant = Instant.from(formatter.parse( rawCommit.get("commit").get("author").get("date").asText()));
                Date date = Date.from(instant);
                commit.setCommitDate(date);
                aGraph.addCoommit(commit);
            }
        }

        return aGraph;
    }
}