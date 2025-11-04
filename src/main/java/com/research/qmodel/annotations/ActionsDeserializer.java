package com.research.qmodel.annotations;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.research.qmodel.model.Actions;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static com.research.qmodel.dto.PARSING_PROPERTIES.WORKFLOW_RUNS;

@Component
public class ActionsDeserializer extends JsonDeserializer<Actions> {
  @Override
  public Actions deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
      throws IOException {
    JsonNode node = jsonParser.getCodec().readTree(jsonParser);
    Actions actions = new Actions();
    String workflowRuns = node.path(WORKFLOW_RUNS.key()).toString();
    actions.setAllActions(workflowRuns);
    return actions;
  }
}
