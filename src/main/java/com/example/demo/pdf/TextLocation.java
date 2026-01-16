package com.example.demo.pdf;

public record TextLocation(
        int page,
        float x,
        float y,
        float width,
        float height,
        String text
) {}
