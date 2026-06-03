package com.rotatingmind.ratingservice.model;

import java.util.List;

public record Response(Long id, Long surveyId, List<Answer> answers, Long userId) {
}
