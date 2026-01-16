package com.example.demo.dto;

public record TextEditRequest(
        int page,
        float x,
        float y,
        String text,
        float fontSize
) {}
