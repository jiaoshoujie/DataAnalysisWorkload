package com.myorg.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.myorg.utils.TestUtils;
import org.junit.jupiter.api.Test;
import software.amazon.awscdk.App;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.apigateway.RestApi;
import software.amazon.awscdk.services.kinesis.Stream;
import software.amazon.awscdk.services.kinesisfirehose.CfnDeliveryStream;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.s3.Bucket;

import java.io.IOException;

import static com.myorg.utils.TestUtils.toCloudFormationJson;
import static org.junit.jupiter.api.Assertions.*;

public class DataAyalysisWorkloadTest {

    @Test
    public void testStack() {
        // GIVEN
        App app = new App();
        StackProps stackProps = StackProps.builder().build();

        // WHEN
        DataAyalysisWorkloadStack stack = new DataAyalysisWorkloadStack(app, "TestStack", stackProps);

        // THEN
        assertNotNull(stack);
    }

    @Test
    public void testRestApi() {
        // GIVEN
        App app = new App();
        StackProps stackProps = StackProps.builder().build();

        // WHEN
        DataAyalysisWorkloadStack stack = new DataAyalysisWorkloadStack(app, "TestStack", stackProps);
        RestApi dataCollectionAPI = (RestApi) stack.getNode().findChild("APIGateway");
        // THEN
        assertEquals(1, dataCollectionAPI.getMethods().size());
        assertEquals("POST", dataCollectionAPI.getMethods().get(0).getHttpMethod());
        assertEquals("Client Tracking Data Collection API", dataCollectionAPI.getRestApiName());
    }

    @Test
    public void testLambdaFunction() {
        // GIVEN
        App app = new App();
        StackProps stackProps = StackProps.builder().build();

        // WHEN
        DataAyalysisWorkloadStack stack = new DataAyalysisWorkloadStack(app, "TestStack", stackProps);
        Function clientTrackingDataHandler = (Function) stack.getNode().findChild("ClientTrackingDataHandler");

        // THEN
        assertEquals("x86_64", clientTrackingDataHandler.getArchitecture().getName());
        assertEquals(Runtime.JAVA_8, clientTrackingDataHandler.getRuntime());
        assertEquals(30, clientTrackingDataHandler.getTimeout().toSeconds().intValue());
    }

    @Test
    public void testKinesisStream() {
        // GIVEN
        App app = new App();
        StackProps stackProps = StackProps.builder().build();

        // WHEN
        DataAyalysisWorkloadStack stack = new DataAyalysisWorkloadStack(app, "TestStack", stackProps);
        Stream dataStreamBus = (Stream) stack.getNode().findChild("KinesisStream");

        // THEN
        assertNotNull(dataStreamBus);
    }

    @Test
    public void testKinesisFirehose() {
        // GIVEN
        App app = new App();
        StackProps stackProps = StackProps.builder().build();

        // WHEN
        DataAyalysisWorkloadStack stack = new DataAyalysisWorkloadStack(app, "TestStack", stackProps);
        CfnDeliveryStream deliveryStream = (CfnDeliveryStream) stack.getNode().findChild("KinesisFirehose");
        // THEN
        assertEquals("client-tracking-data-delivery", deliveryStream.getDeliveryStreamName());
        assertEquals("KinesisStreamAsSource", deliveryStream.getDeliveryStreamType());

    }

    @Test
    public void testS3Bucket() {
        // GIVEN
        App app = new App();
        StackProps stackProps = StackProps.builder().build();

        // WHEN
        DataAyalysisWorkloadStack stack = new DataAyalysisWorkloadStack(app, "TestStack", stackProps);
        Bucket dataLakeBucket = (Bucket) stack.getNode().findChild("S3Bucket");
        // THEN
        assertNotNull(dataLakeBucket);
    }

    @Test
    public void testRole() throws IOException {
        // GIVEN
        App app = new App();
        StackProps stackProps = StackProps.builder().build();

        // WHEN
        DataAyalysisWorkloadStack stack = new DataAyalysisWorkloadStack(app, "TestStack", stackProps);

        // THEN
        JsonNode actualStack = toCloudFormationJson(app, stack).path("Resources");
        JsonNode expectedStack = TestUtils.fromFileResource(getClass().getResource("../../../expectedStack.json"))
                .path("Resources");
        final String type = "AWS::IAM::Role";
        JsonNode actual = TestUtils.getJsonNode(actualStack, type);
        System.out.println(actualStack);
        JsonNode expected = TestUtils.getJsonNode(expectedStack, type);
        String[] keys = {"ManagedPolicyArns", "AssumeRolePolicyDocument"};
        for (String key : keys) {
            assertEquals(actual.get(key), expected.get(key));
        }
    }

    @Test
    public void testPermission() throws IOException {
        // GIVEN
        App app = new App();
        StackProps stackProps = StackProps.builder().build();

        // WHEN
        DataAyalysisWorkloadStack stack = new DataAyalysisWorkloadStack(app, "TestStack", stackProps);

        // THEN
        JsonNode actualStack = toCloudFormationJson(app, stack).path("Resources");
        JsonNode expectedStack = TestUtils.fromFileResource(getClass().getResource("../../../expectedStack.json"))
                .path("Resources");
        final String type = "AWS::Lambda::Permission";
        JsonNode actual = TestUtils.getJsonNode(actualStack, type);
        JsonNode expected = TestUtils.getJsonNode(expectedStack, type);
        String[] keys = {"ManagedPolicyArns", "AssumeRolePolicyDocument"};
        for (String key : keys) {
            assertEquals(actual.get(key), expected.get(key));
        }
    }
}
