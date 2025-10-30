package com.research.qmodel.annotations;

import com.fasterxml.jackson.databind.JsonNode;
import com.research.qmodel.model.BaseMetric;
import com.research.qmodel.model.Reaction;

public interface BaseReactionBuilder {
  default void setupReaction(JsonNode rowData, JsonNode node, BaseMetric baseMetric) {
    if (rowData.get("reactions") != null
        && (node.path("reactions").path("total_count").asInt() != 0
        || node.path("reactions").path("+1").asInt() != 0
        || node.path("reactions").path("-1").asInt() != 0
        || node.path("reactions").path("laugh").asInt() != 0
        || node.path("reactions").path("hooray").asInt() != 0
        || node.path("reactions").path("confused").asInt() != 0
        || node.path("reactions").path("heart").asInt() != 0
        || node.path("reactions").path("rocket").asInt() != 0
        || node.path("reactions").path("eyes").asInt() != 0)) {
      Reaction reaction = new Reaction();
      reaction.setUrl(node.path("reactions").path("url").asText());
      reaction.setTotalCount(node.path("reactions").path("total_count").asInt());
      reaction.setTotalCount(node.path("reactions").path("+1").asInt());
      reaction.setTotalCount(node.path("reactions").path("-1").asInt());
      reaction.setTotalCount(node.path("reactions").path("laugh").asInt());
      reaction.setTotalCount(node.path("reactions").path("hooray").asInt());
      reaction.setTotalCount(node.path("reactions").path("confused").asInt());
      reaction.setTotalCount(node.path("reactions").path("heart").asInt());
      reaction.setTotalCount(node.path("reactions").path("rocket").asInt());
      reaction.setTotalCount(node.path("reactions").path("eyes").asInt());
      baseMetric.setReaction(reaction);
    }
  }
}
