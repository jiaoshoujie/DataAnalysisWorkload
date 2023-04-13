package com.myorg.infrastructure;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.apigateway.LambdaIntegration;
import software.amazon.awscdk.services.apigateway.RestApi;
import software.amazon.awscdk.services.iam.*;
import software.amazon.awscdk.services.kinesis.Stream;
import software.amazon.awscdk.services.kinesis.StreamProps;
import software.amazon.awscdk.services.kinesisfirehose.CfnDeliveryStream;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.s3.Bucket;
import software.constructs.Construct;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * We want to build a data analysis workload on AWS.
 * This project is used to build the infrastructure for the data collection part.
 * Client -> API Gateway -> Lambda -> Kinesis -> Firehose -> S3
 */
public class DataAyalysisWorkloadStack extends Stack {

    private static final String KINESIS_NAME = "client-tracking-data-stream";

    public DataAyalysisWorkloadStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public DataAyalysisWorkloadStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        /**
         * Step 1 : Prepare components
         */
        RestApi dataCollectionAPI = RestApi.Builder.create(this, "APIGateway")
                .restApiName("Client Tracking Data Collection API")
                .description("This API is used to collect tracking data reported by client")
                .build();

        Function clientTrackingDataHandler = Function.Builder.create(this, "ClientTrackingDataHandler")
                .code(Code.fromAsset("./asset/data-collection-lambda.jar"))
                .handler("com.myorg.lambda.TrackingDataHandler")
                .runtime(Runtime.JAVA_8).memorySize(1024)
                .timeout(Duration.seconds(30))
                .build();

        Stream dataStreamBus = new Stream(this, "KinesisStream", StreamProps.builder()
                .streamName(KINESIS_NAME)
                .build());

        CfnDeliveryStream deliveryStream = CfnDeliveryStream.Builder.create(this, "KinesisFirehose")
                .deliveryStreamName("client-tracking-data-delivery")
                .build();

        Bucket dataLakeBucket = Bucket.Builder.create(this, "S3Bucket")
                .publicReadAccess(false)
                .removalPolicy(RemovalPolicy.DESTROY)
                .bucketName("our-company-data-lake")
                .build();

        /**
         * Step 2 : Create roles and set up access permissions
         */
        PolicyStatement statement = PolicyStatement.Builder.create()
                .effect(Effect.ALLOW)
                .actions(Arrays.asList(new String[]{"kinesis:DescribeStream"}))
                .resources(Arrays.asList(dataStreamBus.getStreamArn())).build();
        Map<String, PolicyDocument> policyDocumentMap = new HashMap<>();
        policyDocumentMap.put("firehose-kinesis-policy", PolicyDocument.Builder.create()
                .statements(Arrays.asList(new PolicyStatement[]{statement})).build());

        Role firehoseRole = Role.Builder.create(this, "FirehoseRole")
                .roleName("kinesis-firehose-s3")
                .assumedBy(new ServicePrincipal("firehose.amazonaws.com"))
                .inlinePolicies(policyDocumentMap)
                .build();

        /**
         * Step 3 : Connect components
         */
        clientTrackingDataHandler.addEnvironment("KINESIS_NAME", KINESIS_NAME);
        LambdaIntegration handlerIngeration = LambdaIntegration.Builder.create(clientTrackingDataHandler).build();
        dataCollectionAPI.getRoot().addResource("track").addMethod("POST", handlerIngeration);

        deliveryStream.setDeliveryStreamType("KinesisStreamAsSource");
        deliveryStream.setKinesisStreamSourceConfiguration(CfnDeliveryStream.KinesisStreamSourceConfigurationProperty.builder()
                .kinesisStreamArn(dataStreamBus.getStreamArn())
                .roleArn(firehoseRole.getRoleArn())
                .build());
        deliveryStream.setS3DestinationConfiguration(CfnDeliveryStream.S3DestinationConfigurationProperty.builder()
                .bucketArn(dataLakeBucket.getBucketArn())
                .roleArn(firehoseRole.getRoleArn())
                .prefix("client-tracking-data/")
                .build());
    }

}
