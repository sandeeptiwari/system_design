package com.rotatingmind.ratingservice.model;

import java.time.LocalDateTime;
import java.util.List;

public record Survey(Long id, String title, LocalDateTime startDate, LocalDateTime endDate, List<SurveyQuestion> surveyQuestions, boolean completed) {
}
