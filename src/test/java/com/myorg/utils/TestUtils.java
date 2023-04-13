package com.myorg.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awscdk.App;
import software.amazon.awscdk.Stack;

import java.io.IOException;
import java.net.URL;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.StreamSupport;

public class TestUtils {
    private static ObjectMapper JSON = new ObjectMapper();

    public static JsonNode fromFileResource(final URL fileResource) throws IOException {
        return JSON.readTree(fileResource);
    }

    public static JsonNode toCloudFormationJson(final App app, final Stack stack) throws IOException {
        JsonNode n =
                JSON.valueToTree(
                        app.synth().getStackByName(stack.getStackName()).getTemplate());
        System.out.println(JSON.writerWithDefaultPrettyPrinter().writeValueAsString(n));
        return n;
    }

    public static JsonNode getJsonNode(JsonNode stackJson, String type) {
        Iterable<JsonNode> iterable = stackJson::elements;
        Optional<JsonNode> maybe =
                StreamSupport.stream(iterable.spliterator(), false)
                        .filter(jn -> jn.get("Type").textValue().equals(type))
                        .findFirst();
        return maybe.map(jn -> jn.path("Properties")).orElseThrow(NoSuchElementException::new);
    }
}