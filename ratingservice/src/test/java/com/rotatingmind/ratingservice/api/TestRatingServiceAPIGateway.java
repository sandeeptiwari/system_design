package com.rotatingmind.ratingservice.api;


import com.rotatingmind.ratingservice.exception.UnauthorizedException;
import com.rotatingmind.ratingservice.model.*;
import com.rotatingmind.ratingservice.repo.QuestionRepository;
import com.rotatingmind.ratingservice.repo.SurveyRepository;
import com.rotatingmind.ratingservice.repo.UserRepository;
import com.rotatingmind.ratingservice.repo.UserSurveyRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class TestRatingServiceAPIGateway {

    private RatingServiceGateway ratingServiceGateway;

    @BeforeEach
    void setup() {
        clearInMemoryRepositories();
        ratingServiceGateway = new RatingServiceGateway();
        createUsers().stream().forEach(u -> ratingServiceGateway.createUsers(u));
        createQuestions().stream().forEach(q -> ratingServiceGateway.createQuestion(q));
        //createOptions().stream().forEach(q -> ratingServiceGateway.createQuestion(q));
    }

    private void clearInMemoryRepositories() {
        UserSurveyRepository.getInstance().clear();
        SurveyRepository.getInstance().clear();
        QuestionRepository.getInstance().clear();
        UserRepository.getInstance().clear();
    }

    @Test
    @DisplayName("Should Admin create a new survey")
    public void testCreateSurvey() {
        Survey survey = createEmployeeSurvey();

        ratingServiceGateway.createSurvey(survey, 4L);

        Survey createdSurvey =
                ratingServiceGateway.getSurvey(1001L);

        assertNotNull(createdSurvey);
        assertEquals(1001L, createdSurvey.id());
    }

    @Test
    @DisplayName("Should throw exception if role is not Admin")
    public void testCreateSurvey_Role_Is_Not_Admin() {
        Survey survey = createEmployeeSurvey();

        UnauthorizedException exception =
                assertThrows(
                        UnauthorizedException.class,
                        () -> ratingServiceGateway.createSurvey(
                                survey,
                                2L // non-admin user
                        )
                );

        assertEquals(
                "Only admin can create survey",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Should admin be able to see correct average rating for the survey")
    public void testAdminSeeCorrectAverageRatingForTheSurvey() {
        Survey survey = createEmployeeSurvey();
        ratingServiceGateway.createSurvey(survey, 4L);

        Survey createdSurvey =
                ratingServiceGateway.getSurvey(1001L);

        ratingServiceGateway.assignSurvey(createdSurvey.id(), 2L);

        //Attend the survey
        ratingServiceGateway.submitSurvey(
                createResponse(
                        1L,
                        createdSurvey.id(),
                        2L,
                        602L,
                        701L,
                        803L
                )
        );

        var result = ratingServiceGateway.getSurveyRating(createdSurvey.id());
        Assertions.assertNotNull(result);
        assertEquals(createdSurvey.id(), result.surveyId());
        assertEquals(4.0, result.averageRating(), 0.0001);
        assertEquals(3, result.questionResults().size());

        Map<Long, Double> avgByQuestion = toAvgMap(result.questionResults());
        assertEquals(4.0, avgByQuestion.get(401L), 0.0001);
        assertEquals(3.0, avgByQuestion.get(402L), 0.0001);
        assertEquals(5.0, avgByQuestion.get(403L), 0.0001);
    }

    @Test
    @DisplayName("Should calculate overall and question averages across multiple responses")
    public void testSurveyRating_MultipleResponses() {
        Survey survey = createEmployeeSurvey();
        ratingServiceGateway.createSurvey(survey, 4L);
        ratingServiceGateway.assignSurvey(survey.id(), 2L);
        ratingServiceGateway.assignSurvey(survey.id(), 3L);

        ratingServiceGateway.submitSurvey(
                createResponse(
                        1L,
                        survey.id(),
                        2L,
                        602L,
                        701L,
                        803L
                )
        );
        ratingServiceGateway.submitSurvey(
                createResponse(
                        2L,
                        survey.id(),
                        3L,
                        603L,
                        700L,
                        802L
                )
        );

        Result result = ratingServiceGateway.getSurveyRating(survey.id());

        assertEquals(3.6667, result.averageRating(), 0.0001);
        Map<Long, Double> avgByQuestion = toAvgMap(result.questionResults());
        assertEquals(4.5, avgByQuestion.get(401L), 0.0001);
        assertEquals(2.0, avgByQuestion.get(402L), 0.0001);
        assertEquals(4.5, avgByQuestion.get(403L), 0.0001);
    }

    @Test
    @DisplayName("Should return zero averages when survey has no responses")
    public void testSurveyRating_NoResponses() {
        Survey survey = createEmployeeSurvey();
        ratingServiceGateway.createSurvey(survey, 4L);

        Result result = ratingServiceGateway.getSurveyRating(survey.id());

        assertEquals(0.0, result.averageRating(), 0.0001);
        assertEquals(3, result.questionResults().size());
        result.questionResults().forEach(qr -> assertEquals(0.0, qr.avgWeightage(), 0.0001));
    }

    @Test
    @DisplayName("Should ignore answers for questions outside the survey")
    public void testSurveyRating_IgnoresNonSurveyQuestionAnswers() {
        Survey survey = createPartialSurvey();
        ratingServiceGateway.createSurvey(survey, 4L);
        ratingServiceGateway.assignSurvey(survey.id(), 2L);

        Answer validAnswer1 = new Answer(501L, 401L, 602L);
        Answer validAnswer2 = new Answer(502L, 402L, 703L);
        Answer ignoredAnswer = new Answer(503L, 403L, 803L);

        Response response = new Response(
                1L,
                survey.id(),
                List.of(validAnswer1, validAnswer2, ignoredAnswer),
                2L
        );
        ratingServiceGateway.submitSurvey(response);

        Result result = ratingServiceGateway.getSurveyRating(survey.id());

        assertEquals(4.5, result.averageRating(), 0.0001);
        assertEquals(2, result.questionResults().size());
        Map<Long, Double> avgByQuestion = toAvgMap(result.questionResults());
        assertEquals(4.0, avgByQuestion.get(401L), 0.0001);
        assertEquals(5.0, avgByQuestion.get(402L), 0.0001);
        assertNull(avgByQuestion.get(403L));
        Map<Long, Double> avgByQuestion2 = toAvgMap(result.questionResults());

    }


    // ----------------------------------------------------
    // Reusable Test Data
    // ----------------------------------------------------

    private Survey createEmployeeSurvey() {

        List<SurveyQuestion> surveyQuestions = List.of(
                new SurveyQuestion(1001L, 401L, 1),
                new SurveyQuestion(1001L, 402L, 2),
                new SurveyQuestion(1001L, 403L, 3)
        );

        return new Survey(
                1001L,
                "Employee Satisfaction Survey",
                LocalDate.now().atStartOfDay(),
                LocalDate.now().plusDays(15).atStartOfDay(),
                surveyQuestions,
                false
        );
    }

    private Survey createPartialSurvey() {
        List<SurveyQuestion> surveyQuestions = List.of(
                new SurveyQuestion(1002L, 401L, 1),
                new SurveyQuestion(1002L, 402L, 2)
        );
        return new Survey(
                1002L,
                "Pulse Survey",
                LocalDate.now().atStartOfDay(),
                LocalDate.now().plusDays(7).atStartOfDay(),
                surveyQuestions,
                false
        );
    }

    private Response createResponse(
            Long responseId,
            Long surveyId,
            Long userId,
            Long question401OptionId,
            Long question402OptionId,
            Long question403OptionId
    ) {
        Answer answer1 = new Answer(responseId * 10 + 1, 401L, question401OptionId);
        Answer answer2 = new Answer(responseId * 10 + 2, 402L, question402OptionId);
        Answer answer3 = new Answer(responseId * 10 + 3, 403L, question403OptionId);
        return new Response(
                responseId,
                surveyId,
                List.of(answer1, answer2, answer3),
                userId
        );
    }

    private Map<Long, Double> toAvgMap(List<QuestionResult> questionResults) {
        return questionResults.stream()
                .collect(Collectors.toMap(
                        QuestionResult::questionId,
                        QuestionResult::avgWeightage
                ));
    }

    private List<Question> createQuestions() {

        Question question1 = new Question(
                401L,
                "How satisfied are you with your work-life balance?",
                createOptions(
                        401L,
                        600L
                )
        );

        Question question2 = new Question(
                402L,
                "How do you rate team collaboration?",
                createOptions(
                        402L,
                        700L
                )
        );

        Question question3 = new Question(
                403L,
                "How satisfied are you with career growth opportunities?",
                createOptions(
                        403L,
                        800L
                )
        );

        return List.of(
                question1,
                question2,
                question3
        );
    }

    private List<Option> createOptions(
            Long questionId,
            Long startingOptionId
    ) {

        return List.of(

                new Option(
                        startingOptionId,
                        "Poor",
                        questionId,
                        1
                ),

                new Option(
                        startingOptionId + 1,
                        "Average",
                        questionId,
                        3
                ),

                new Option(
                        startingOptionId + 2,
                        "Good",
                        questionId,
                        4
                ),

                new Option(
                        startingOptionId + 3,
                        "Excellent",
                        questionId,
                        5
                )
        );
    }

    private List<User> createUsers() {

        User admin = new User(
                1L,
                "Sandeep",
                "Tiwari",
                "sandeep@rotatingmind.com",
                List.of(Role.ADMIN)
        );

        User user1 = new User(
                2L,
                "Rahul",
                "Sharma",
                "rahul@gmail.com",
                List.of(Role.USER)
        );

        User user2 = new User(
                3L,
                "Priya",
                "Verma",
                "priya@gmail.com",
                List.of(Role.USER)
        );

        User adminAndUser = new User(
                4L,
                "Amit",
                "Kapoor",
                "amit@company.com",
                List.of(Role.ADMIN, Role.USER)
        );

        return List.of(
                admin,
                user1,
                user2,
                adminAndUser
        );
    }

}
