package com.rotatingmind.ratingservice.service;

import com.rotatingmind.ratingservice.model.Response;
import com.rotatingmind.ratingservice.model.Result;
import com.rotatingmind.ratingservice.model.Survey;

public interface RatingService {

    Survey createSurvey(Survey survey);

    Survey getSurvey(Long surveyId);

    void assignSurvey(Long surveyId, Long userId);

    Result calculateSurveyRating(Long surveyId);
}
