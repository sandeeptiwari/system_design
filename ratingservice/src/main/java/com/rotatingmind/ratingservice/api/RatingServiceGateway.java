package com.rotatingmind.ratingservice.api;

import com.rotatingmind.ratingservice.exception.UnauthorizedException;
import com.rotatingmind.ratingservice.model.*;
import com.rotatingmind.ratingservice.service.*;

public class RatingServiceGateway {

    private final RatingService ratingService;
    private final UserService userService;
    private final QuestionService questionService;
    private final UserSurveyService userSurveyService;

    public RatingServiceGateway() {
        this.userService = new UserServiceImpl();
        this.ratingService = new RatingServiceImpl();
        this.questionService = new QuestionServiceImpl();
        this.userSurveyService = new UserSurveyServiceImpl();
    }

    public Survey createSurvey(Survey survey, Long userId) {
        var user = userService.getUserById(userId);
        if (user.roles().contains(Role.ADMIN)) {
          return ratingService.createSurvey(survey);
        }
        throw new UnauthorizedException(
                "Only admin can create survey"
        );
    }

    public Survey getSurvey(Long surveyId) {
        return ratingService.getSurvey(surveyId);
    }


    public void addQuestion() {}

    public void addOption() {}

    public Result getSurveyRating(Long surveyId) {
        return ratingService.calculateSurveyRating(surveyId);
    }

    public User createUsers(User user) {
        return userService.createUser(user);
    }

    public Question createQuestion(Question q) {
        return questionService.createQuestion(q);
    }

    public void submitSurvey(Response response) {
        userSurveyService.submitSurvey(response);
    }

    public void assignSurvey(long surveyId, long userId) {
        ratingService.assignSurvey(surveyId, userId);
    }
}
