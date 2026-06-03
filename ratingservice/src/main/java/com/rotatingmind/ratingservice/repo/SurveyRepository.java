package com.rotatingmind.ratingservice.repo;

import com.rotatingmind.ratingservice.model.Survey;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class SurveyRepository {
    private final ConcurrentHashMap<Long, Survey> surveys = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Long> surveyAssigned = new ConcurrentHashMap<>();

    private SurveyRepository() {
    }

    // Holder class for lazy thread-safe singleton initialization
    private static class Holder {
        private static final SurveyRepository INSTANCE = new SurveyRepository();
    }

    // Public accessor for the singleton
    public static SurveyRepository getInstance() {
        return Holder.INSTANCE;
    }

    public Survey save(Survey survey) {
       surveys.put(survey.id(), survey);
       return survey;
    }

    public Optional<Survey> findById(Long id) {
        return Optional.ofNullable(surveys.get(id));
    }

    public List<Survey> findAll() {
        return surveys.values().stream().toList();
    }

    public Optional<Survey> deleteById(Long id) {

        return Optional.ofNullable(
                surveys.remove(id)
        );
    }

    public void assignSurvey(Long surveyId, Long userId) {
        surveyAssigned.put(surveyId, userId);
    }

    public void clear() {
        surveys.clear();
        surveyAssigned.clear();
    }
}
