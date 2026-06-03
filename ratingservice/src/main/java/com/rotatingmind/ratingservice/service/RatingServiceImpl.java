package com.rotatingmind.ratingservice.service;

import com.rotatingmind.ratingservice.model.*;
import com.rotatingmind.ratingservice.repo.QuestionRepository;
import com.rotatingmind.ratingservice.repo.SurveyRepository;
import com.rotatingmind.ratingservice.repo.UserSurveyRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RatingServiceImpl implements RatingService {

    private SurveyRepository surveyRepository = SurveyRepository.getInstance();
    private UserSurveyRepository userSurveyRepository = UserSurveyRepository.getInstance();
    private QuestionRepository questionRepository = QuestionRepository.getInstance();

    @Override
    public Survey createSurvey(Survey survey) {
        return surveyRepository.save(survey);
    }

    @Override
    public Survey getSurvey(Long surveyId) {
        return surveyRepository.findById(surveyId).orElseThrow(() -> new RuntimeException("Survey not found"));
    }

    @Override
    public void assignSurvey(Long surveyId, Long userId) {
        surveyRepository.assignSurvey(surveyId, userId);
    }

    @Override
    public Result calculateSurveyRating(Long surveyId) {
        Survey survey = checkSurvey(surveyId);
        Set<Long> surveyQuestionIds = getSurveyQuestionIds(survey);
        List<Answer> answers = getAnswers(surveyId, surveyQuestionIds);
        Map<Long, Question> questions = getQuestions(surveyQuestionIds);

        Map<Long, List<Answer>> answersByQuestion = groupAnswersByQuestion(answers);

        List<QuestionResult> questionResults = calculateQuestionResults(
                survey,
                answersByQuestion,
                questions
        );

        double surveyAverage = calculateSurveyAverage(questionResults);

        return buildResult(
                survey,
                surveyAverage,
                questionResults
        );
    }

    private List<QuestionResult> calculateQuestionResults(
            Survey survey,
            Map<Long, List<Answer>> answersByQuestion,
            Map<Long, Question> questions
    ) {

        List<QuestionResult> results = new ArrayList<>();

        for (SurveyQuestion surveyQuestion : survey.surveyQuestions()) {
            Long questionId = surveyQuestion.questionId();
            List<Answer> answers = answersByQuestion.getOrDefault(
                    questionId,
                    Collections.emptyList()
            );
            Question question = questions.get(questionId);

            double avg = calculateQuestionAverage(question, answers);

            results.add(new QuestionResult(9000L, questionId, avg));
        }

        return results;
    }

    private double calculateQuestionAverage(Question question, List<Answer> answers) {
        if (answers.isEmpty()) {
            return 0.0;
        }
        return answers.stream()
                .mapToInt(answer -> {
                    Option option = findOption(question, answer.optionId());
                    return option.weight();
                })
                .average()
                .orElse(0.0);
    }

    private Option findOption(
            Question question,
            Long optionId
    ) {

        return question.options()
                .stream()
                .filter(o -> o.id().equals(optionId))
                .findFirst()
                .orElseThrow(() ->
                        new RuntimeException(
                                "Option not found"
                        ));
    }

    private double calculateSurveyAverage(
            List<QuestionResult> questionResults
    ) {

        return questionResults.stream()
                .mapToDouble(QuestionResult::avgWeightage)
                .average()
                .orElse(0.0);
    }

    private Result buildResult(
            Survey survey,
            double surveyAverage,
            List<QuestionResult> questionResults
    ) {

        return new Result(
                9001L,  // id
                survey.id(),
                surveyAverage,
                questionResults
        );
    }

    private Map<Long, List<Answer>> groupAnswersByQuestion(List<Answer> answers) {
        return answers.stream()
                .collect(Collectors.groupingBy(
                        Answer::questionId
                ));
    }

    private Map<Long, Question> getQuestions(Set<Long> questionIds) {
        Map<Long, Question> questionMap = questionRepository
                .findQuestionByIds(questionIds.stream().toList())
                .stream()
                .collect(Collectors.toMap(
                        Question::id,
                        Function.identity()
                ));
        questionIds.forEach(questionId -> {
            if (!questionMap.containsKey(questionId)) {
                throw new RuntimeException(
                        "Question not found for id " + questionId
                );
            }
        });
        return questionMap;
    }

    private List<Answer> getAnswers(Long surveyId, Set<Long> surveyQuestionIds) {

        return userSurveyRepository.findBySurveyId(surveyId)
                .stream()
                .flatMap(r -> r.answers().stream())
                .filter(answer -> surveyQuestionIds.contains(answer.questionId()))
                .toList();
    }

    private Set<Long> getSurveyQuestionIds(Survey survey) {
        return survey.surveyQuestions()
                .stream()
                .map(SurveyQuestion::questionId)
                .collect(Collectors.toCollection(HashSet::new));
    }

    private Survey checkSurvey(Long surveyId) {

        return surveyRepository.findById(surveyId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Survey not found"
                        ));
    }
}
