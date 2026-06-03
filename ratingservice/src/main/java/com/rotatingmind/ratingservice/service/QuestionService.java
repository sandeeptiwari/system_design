package com.rotatingmind.ratingservice.service;

import com.rotatingmind.ratingservice.model.Question;

import java.util.List;

public interface QuestionService {

    Question createQuestion(Question q);

    Question getQuestionById(Long id);

    List<Question> getQuestions();
}
