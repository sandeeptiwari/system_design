package com.rotatingmind.ratingservice.model;

import java.util.List;

public record Result(Long id, Long surveyId, double averageRating, List<QuestionResult> questionResults) {
}
