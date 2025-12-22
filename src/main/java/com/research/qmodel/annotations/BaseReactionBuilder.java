package com.research.qmodel.annotations;

import com.fasterxml.jackson.databind.JsonNode;
import com.research.qmodel.model.BaseMetric;
import com.research.qmodel.model.Reaction;

import static com.research.qmodel.dto.PARSING_PROPERTIES.*;

public interface BaseReactionBuilder {
    default void setupReaction(JsonNode rowData, JsonNode node, BaseMetric baseMetric) {
        if (rowData.get(REACTIONS.key()) != null
                && (node.path(REACTIONS.key()).path(TOTAL_COUNT.key()).asInt() != 0
                || node.path(REACTIONS.key()).path(PLUS_ONE.key()).asInt() != 0
                || node.path(REACTIONS.key()).path(MINUS_ONE.key()).asInt() != 0
                || node.path(REACTIONS.key()).path(LAUGH.key()).asInt() != 0
                || node.path(REACTIONS.key()).path(HOORAY.key()).asInt() != 0
                || node.path(REACTIONS.key()).path(CONFUSED.key()).asInt() != 0
                || node.path(REACTIONS.key()).path(HEART.key()).asInt() != 0
                || node.path(REACTIONS.key()).path(ROCKET.key()).asInt() != 0
                || node.path(REACTIONS.key()).path(EYES.key()).asInt() != 0)) {
            Reaction reaction = new Reaction();
            reaction.setUrl(node.path(REACTIONS.key()).path(URL.key()).asText());
            reaction.setTotalCount(node.path(REACTIONS.key()).path(TOTAL_COUNT.key()).asInt());
            reaction.setTotalCount(node.path(REACTIONS.key()).path(PLUS_ONE.key()).asInt());
            reaction.setTotalCount(node.path(REACTIONS.key()).path(MINUS_ONE.key()).asInt());
            reaction.setTotalCount(node.path(REACTIONS.key()).path(LAUGH.key()).asInt());
            reaction.setTotalCount(node.path(REACTIONS.key()).path(HOORAY.key()).asInt());
            reaction.setTotalCount(node.path(REACTIONS.key()).path(CONFUSED.key()).asInt());
            reaction.setTotalCount(node.path(REACTIONS.key()).path(HEART.key()).asInt());
            reaction.setTotalCount(node.path(REACTIONS.key()).path(ROCKET.key()).asInt());
            reaction.setTotalCount(node.path(REACTIONS.key()).path(EYES.key()).asInt());
            baseMetric.setReaction(reaction);
        }
    }
}
