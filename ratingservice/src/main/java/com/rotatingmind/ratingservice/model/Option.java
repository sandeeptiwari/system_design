package com.rotatingmind.ratingservice.model;

public record Option(Long id, String optionTxt, Long questionId, int weight) {
}
