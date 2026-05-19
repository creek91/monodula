package com.github.monodula.maven.strategy;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AddContextTest {

    @Test
    void simpleModuleName() {
        AddContext ctx = new AddContext("payment", "com.example", "my-project", "1.0.0-SNAPSHOT");
        assertThat(ctx.getModuleName()).isEqualTo("payment");
        assertThat(ctx.getPackageSegment()).isEqualTo("payment");
        assertThat(ctx.getClassName()).isEqualTo("Payment");
    }

    @Test
    void hyphenatedModuleName() {
        AddContext ctx =
                new AddContext("payment-gateway", "com.example", "my-project", "1.0.0-SNAPSHOT");
        assertThat(ctx.getPackageSegment()).isEqualTo("paymentgateway");
        assertThat(ctx.getClassName()).isEqualTo("PaymentGateway");
    }

    @Test
    void multiHyphenModuleName() {
        AddContext ctx = new AddContext("a-b-c", "com.example", "my-project", "1.0.0-SNAPSHOT");
        assertThat(ctx.getPackageSegment()).isEqualTo("abc");
        assertThat(ctx.getClassName()).isEqualTo("ABC");
    }

    @Test
    void singleLetterModule() {
        AddContext ctx = new AddContext("p", "com.example", "my-project", "1.0.0-SNAPSHOT");
        assertThat(ctx.getPackageSegment()).isEqualTo("p");
        assertThat(ctx.getClassName()).isEqualTo("P");
    }

    @Test
    void basePackageFromGroupIdAndHyphenatedArtifactId() {
        AddContext ctx = new AddContext("payment", "com.example", "my-project", "1.0.0-SNAPSHOT");
        assertThat(ctx.getBasePackage()).isEqualTo("com.example.myproject");
    }

    @Test
    void basePackageArtifactIdNoHyphen() {
        AddContext ctx = new AddContext("payment", "com.example", "myproject", "1.0.0-SNAPSHOT");
        assertThat(ctx.getBasePackage()).isEqualTo("com.example.myproject");
    }
}
