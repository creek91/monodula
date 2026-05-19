package com.github.monodula.maven.strategy;

public interface SplitStrategy {
    String getName();

    SplitResult split(SplitContext context);
}
