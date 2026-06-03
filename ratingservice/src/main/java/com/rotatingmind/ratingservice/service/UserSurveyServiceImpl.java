package com.rotatingmind.ratingservice.service;

import com.rotatingmind.ratingservice.model.Response;
import com.rotatingmind.ratingservice.repo.UserSurveyRepository;

public class UserSurveyServiceImpl implements UserSurveyService {
   private UserSurveyRepository userSurveyRepository = UserSurveyRepository.getInstance();

    @Override
    public void submitSurvey(Response response) {
        userSurveyRepository.save(response);
    }
}
