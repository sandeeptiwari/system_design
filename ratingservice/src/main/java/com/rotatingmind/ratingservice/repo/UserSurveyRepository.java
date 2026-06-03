package com.rotatingmind.ratingservice.repo;

import com.rotatingmind.ratingservice.model.Response;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class UserSurveyRepository {
    private final ConcurrentHashMap<Long, Response> userSurveys = new ConcurrentHashMap<>();

    private UserSurveyRepository() {
    }

    // Holder class for lazy thread-safe singleton initialization
    private static class Holder {
        private static final UserSurveyRepository INSTANCE = new UserSurveyRepository();
    }

    // Public accessor for the singleton
    public static UserSurveyRepository getInstance() {
        return UserSurveyRepository.Holder.INSTANCE;
    }

    public Response save(Response response) {
        userSurveys.put(response.id(), response);
        return response;
    }

    public Optional<Response> findById(Long id) {
        return Optional.ofNullable(userSurveys.get(id));
    }

    public List<Response> findAll() {
        return userSurveys.values().stream().toList();
    }

    public Optional<Response> deleteById(Long id) {

        return Optional.ofNullable(
                userSurveys.remove(id)
        );
    }

    public List<Response> findBySurveyId(Long surveyId) {
        return userSurveys.values().stream().filter(response -> response.surveyId().equals(surveyId)).toList();
    }

    public void clear() {
        userSurveys.clear();
    }
}
