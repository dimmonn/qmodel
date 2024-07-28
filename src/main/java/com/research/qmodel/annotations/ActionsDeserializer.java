package com.research.qmodel.annotations;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.research.qmodel.model.AGraph;
import com.research.qmodel.model.Actions;
import com.research.qmodel.model.Commit;
import com.research.qmodel.model.FileChange;
import com.research.qmodel.repos.AGraphRepository;
import com.research.qmodel.service.BasicQueryService;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.swing.*;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class ActionsDeserializer extends JsonDeserializer<Actions> {
    @Override
    public Actions deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        Actions actions = new Actions();
        String workflowRuns = node.path("workflow_runs").toString();
        actions.setAllActions(workflowRuns);
        return actions;
    }


}