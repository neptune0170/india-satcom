package com.indiasatcom.taskmanagement.interfaces.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.indiasatcom.taskmanagement.domain.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@TestPropertySource(properties = "spring.main.web-application-type=servlet")
class TaskControllerIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(TaskControllerIntegrationTest.class);

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TaskRepository taskRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        // The InMemoryTaskRepository is a singleton bean shared across tests; reset it
        // so each test starts from a known empty state.
        taskRepository.findAll(null, 0, Integer.MAX_VALUE)
                .forEach(t -> taskRepository.deleteById(t.getId()));
    }

    private final LocalDate future = LocalDate.now().plusDays(7);

    @Test
    void createTask_returns201WithGeneratedId() throws Exception {
        String body = """
                { "title": "Write docs", "description": "API docs", "status": "PENDING", "due_date": "%s" }
                """.formatted(future);
        log.info("Input: POST /tasks body={}", body.trim());

        MvcResult result = mockMvc.perform(post("/tasks").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.title", equalTo("Write docs")))
                .andExpect(jsonPath("$.status", equalTo("PENDING")))
                .andExpect(jsonPath("$.due_date", equalTo(future.toString())))
                .andReturn();
        log.info("Result: status={}, body={}",
                result.getResponse().getStatus(), result.getResponse().getContentAsString());
    }

    @Test
    void createTask_missingTitle_returns400() throws Exception {
        String body = """
                { "description": "no title", "due_date": "%s" }
                """.formatted(future);
        log.info("Input: POST /tasks body={} (no title)", body.trim());

        MvcResult result = mockMvc.perform(post("/tasks").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", equalTo("Validation failed")))
                .andReturn();
        log.info("Result: status={}, body={}",
                result.getResponse().getStatus(), result.getResponse().getContentAsString());
    }

    @Test
    void createTask_missingDueDate_returns400() throws Exception {
        String body = """
                { "title": "no date" }
                """;
        log.info("Input: POST /tasks body={} (no due_date)", body.trim());

        MvcResult result = mockMvc.perform(post("/tasks").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andReturn();
        log.info("Result: status={}, body={}",
                result.getResponse().getStatus(), result.getResponse().getContentAsString());
    }

    @Test
    void createTask_pastDueDate_returns400() throws Exception {
        LocalDate past = LocalDate.now().minusDays(1);
        String body = """
                { "title": "past", "due_date": "%s" }
                """.formatted(past);
        log.info("Input: POST /tasks body={} (past date)", body.trim());

        MvcResult result = mockMvc.perform(post("/tasks").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", org.hamcrest.Matchers.containsString("past")))
                .andReturn();
        log.info("Result: status={}, body={}",
                result.getResponse().getStatus(), result.getResponse().getContentAsString());
    }

    @Test
    void getTask_returnsTaskWhenExists() throws Exception {
        String id = createTaskAndReturnId("Read book", future);
        log.info("Input: GET /tasks/{}", id);

        MvcResult result = mockMvc.perform(get("/tasks/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", equalTo(id)))
                .andExpect(jsonPath("$.title", equalTo("Read book")))
                .andReturn();
        log.info("Result: status={}, body={}",
                result.getResponse().getStatus(), result.getResponse().getContentAsString());
    }

    @Test
    void getTask_returns404WhenMissing() throws Exception {
        log.info("Input: GET /tasks/does-not-exist");

        MvcResult result = mockMvc.perform(get("/tasks/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", org.hamcrest.Matchers.containsString("does-not-exist")))
                .andReturn();
        log.info("Result: status={}, body={}",
                result.getResponse().getStatus(), result.getResponse().getContentAsString());
    }

    @Test
    void updateTask_appliesPartialChanges() throws Exception {
        String id = createTaskAndReturnId("Original", future);
        String body = """
                { "title": "Updated", "status": "IN_PROGRESS" }
                """;
        log.info("Input: PUT /tasks/{} body={}", id, body.trim());

        MvcResult result = mockMvc.perform(put("/tasks/" + id).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", equalTo("Updated")))
                .andExpect(jsonPath("$.status", equalTo("IN_PROGRESS")))
                .andReturn();
        log.info("Result: status={}, body={}",
                result.getResponse().getStatus(), result.getResponse().getContentAsString());
    }

    @Test
    void updateTask_returns404WhenMissing() throws Exception {
        String body = "{ \"title\": \"x\" }";
        log.info("Input: PUT /tasks/missing body={}", body);

        MvcResult result = mockMvc.perform(put("/tasks/missing")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound())
                .andReturn();
        log.info("Result: status={}, body={}",
                result.getResponse().getStatus(), result.getResponse().getContentAsString());
    }

    @Test
    void deleteTask_returns204WhenExists() throws Exception {
        String id = createTaskAndReturnId("To delete", future);
        log.info("Input: DELETE /tasks/{}", id);

        MvcResult del = mockMvc.perform(delete("/tasks/" + id)).andExpect(status().isNoContent()).andReturn();
        log.info("Result: status={} (no body)", del.getResponse().getStatus());

        log.info("Input: GET /tasks/{} (after delete)", id);
        MvcResult get = mockMvc.perform(get("/tasks/" + id)).andExpect(status().isNotFound()).andReturn();
        log.info("Result: status={}, body={}",
                get.getResponse().getStatus(), get.getResponse().getContentAsString());
    }

    @Test
    void deleteTask_returns404WhenMissing() throws Exception {
        log.info("Input: DELETE /tasks/missing");

        MvcResult result = mockMvc.perform(delete("/tasks/missing"))
                .andExpect(status().isNotFound())
                .andReturn();
        log.info("Result: status={}, body={}",
                result.getResponse().getStatus(), result.getResponse().getContentAsString());
    }

    @Test
    void listTasks_returnsAllSortedByDueDateWithPaginationAndFilter() throws Exception {
        createTaskAndReturnId("late", future.plusDays(10));
        createTaskAndReturnId("soon", future);
        String doneId = createTaskAndReturnId("middle", future.plusDays(5));
        mockMvc.perform(put("/tasks/" + doneId).contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"status\": \"DONE\" }"))
                .andExpect(status().isOk());
        log.info("Setup: 3 tasks created (late, soon, middle); 'middle' marked DONE");

        log.info("Input: GET /tasks (no params)");
        MvcResult all = mockMvc.perform(get("/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].title", equalTo("soon")))
                .andExpect(jsonPath("$[1].title", equalTo("middle")))
                .andExpect(jsonPath("$[2].title", equalTo("late")))
                .andReturn();
        log.info("Result: status={}, body={}",
                all.getResponse().getStatus(), all.getResponse().getContentAsString());

        log.info("Input: GET /tasks?status=DONE");
        MvcResult filtered = mockMvc.perform(get("/tasks").param("status", "DONE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", equalTo("middle")))
                .andReturn();
        log.info("Result: status={}, body={}",
                filtered.getResponse().getStatus(), filtered.getResponse().getContentAsString());

        log.info("Input: GET /tasks?page=0&size=2");
        MvcResult page0 = mockMvc.perform(get("/tasks").param("page", "0").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andReturn();
        log.info("Result: status={}, body={}",
                page0.getResponse().getStatus(), page0.getResponse().getContentAsString());

        log.info("Input: GET /tasks?page=1&size=2");
        MvcResult page1 = mockMvc.perform(get("/tasks").param("page", "1").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andReturn();
        log.info("Result: status={}, body={}",
                page1.getResponse().getStatus(), page1.getResponse().getContentAsString());
    }

    private String createTaskAndReturnId(String title, LocalDate due) throws Exception {
        String body = """
                { "title": "%s", "due_date": "%s" }
                """.formatted(title, due);

        MvcResult result = mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("id").asText();
    }
}
