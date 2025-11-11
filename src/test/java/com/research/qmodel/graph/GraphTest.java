package com.research.qmodel.graph;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import com.research.qmodel.repos.CommitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

import static graphql.Assert.assertTrue;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class GraphTest {
    @Autowired
    private CommitRepository commitRepository;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private ResourceLoader resourceLoader;

    @Test
    void buildGraphTest() throws Exception {
        Resource resource = resourceLoader.getResource("classpath:graph.json");
        String testGraphRaw = Files.readString(resource.getFile().toPath(), StandardCharsets.UTF_8);
        Map<String, Vertex> testGraph = mapper.readValue(testGraphRaw, new TypeReference<>() {
        });

        ResultActions resultActions = this.mockMvc.perform(get("/api/graph/data").param("owner", "dimmonn").param("repo", "test")).andDo(print()).andExpect(status().isOk());
        String realGraphRaw = resultActions.andReturn().getResponse().getContentAsString();
        Map<String, Vertex> realGraph = mapper.readValue(realGraphRaw, new TypeReference<>() {
        });
        testGraph.forEach((k, v) -> {
            assertTrue(realGraph.containsKey(k));
            assertTrue(realGraph.containsValue(v));
        });
    }
}