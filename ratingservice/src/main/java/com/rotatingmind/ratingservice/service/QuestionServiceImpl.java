package com.rotatingmind.ratingservice.service;

import com.rotatingmind.ratingservice.model.Question;
import com.rotatingmind.ratingservice.repo.QuestionRepository;

import java.util.List;

public class QuestionServiceImpl implements QuestionService {
    private QuestionRepository  questionRepository = QuestionRepository.getInstance();

    @Override
    public Question createQuestion(Question q) {
        return questionRepository.save(q);
    }

    @Override
    public Question getQuestionById(Long id) {
        return questionRepository.findById(id).orElseThrow(() -> new RuntimeException("Question not found"));
    }

    @Override
    public List<Question> getQuestions() {
        return List.of();
    }
}
