package com.myorg.infrastructure;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;

public class DataAyalysisWorkloadApp {
    public static void main(final String[] args) {
        App app = new App();

        new DataAyalysisWorkloadStack(app, "DataAyalysisWorkloadStack",
                StackProps.builder()
                .env(Environment.builder()
                        .account(System.getenv("CDK_DEFAULT_ACCOUNT"))
                        .region(System.getenv("CDK_DEFAULT_REGION"))
                        .account("438221642870")
                        .region("us-east-1")
                        .build())
                .build());
        app.synth();
    }
}

