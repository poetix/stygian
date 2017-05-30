package com.codepoetics.stygian;

import com.codepoetics.stygian.json.JsonContext;
import com.codepoetics.stygian.json.JsonFlow;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import static com.codepoetics.stygian.java.JavaFlow.describe;
import static com.codepoetics.stygian.java.JavaFlow.runLogging;
import static com.codepoetics.stygian.java.json.JavaJsonFlow.*;

public class JsonContextTest {

    private static final String REQUEST = "REQUEST";
    private static final String CREDENTIALS_CHECK_RESULT = "CREDENTIALS_CHECK_RESULT";
    private static final String WEATHER_RESULT = "WEATHER_RESULT";
    private static final String RESPONSE = "RESPONSE";

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testJsonDocumentFlow() throws IOException, ExecutionException, InterruptedException {
        JsonContext ctx = new JsonContext(REQUEST, toNode(m -> {
            m.put("username", "bob");
            m.put("postcode", "SE6");
        }));

        JsonFlow fetchAndFormatWeather = flow("fetch weather", REQUEST, WEATHER_RESULT, this::fetchWeather)
                .then(flow("format weather", WEATHER_RESULT, RESPONSE, this::formatWeather));

        JsonFlow checkCredentials = flow(
                "check credentials",
                REQUEST,
                CREDENTIALS_CHECK_RESULT,
                this::checkCredentials);

        JsonFlow formatFailure = flow("failure result", REQUEST, RESPONSE, this::failureResult);
        Condition<JsonContext> credentialsAreValid = condition("credentials valid", CREDENTIALS_CHECK_RESULT, this::credentialsOk);

        JsonFlow flow = checkCredentials
                .thenIf(credentialsAreValid, fetchAndFormatWeather)
                .otherwise(formatFailure);

        System.out.println(describe(flow.getFlow().getFlow()));
        runLogging(flow, ctx, System.out::println);
    }

    private JsonNode failureResult(JsonNode request) {
        return toNode("failed", "Credentials for user " + request.get("username").textValue() + " invalid");
    }

    private JsonNode checkCredentials(JsonNode request) {
        return toNode("result", "ok");
    }

    private boolean credentialsOk(JsonNode credentials) {
        return credentials.get("result").textValue().equals("ok");
    }

    private JsonNode fetchWeather(JsonNode request) {
        return toNode("temperature", "26F");
    }

    private JsonNode formatWeather(JsonNode weather) {
        return toNode("success", "temperature: " + weather.get("temperature").textValue());
    }

    private JsonNode toNode(String key, Object value) {
        return toNode(m -> m.put(key, value));
    }

    private JsonNode toNode(Consumer<Map<String, Object>> mapBuilder) {
        Map<String, Object> map = new HashMap<>();
        mapBuilder.accept(map);
        return mapper.valueToTree(map);
    }
}
