package com.rotatingmind.ratingservice.repo;

import com.rotatingmind.ratingservice.model.Question;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class QuestionRepository {
    private final ConcurrentHashMap<Long, Question> questions =
            new ConcurrentHashMap<>();

    private QuestionRepository() {
    }

    // Holder class for lazy thread-safe singleton initialization
    private static class Holder {
        private static final QuestionRepository INSTANCE = new QuestionRepository();
    }

    // Public accessor for the singleton
    public static QuestionRepository getInstance() {
        return QuestionRepository.Holder.INSTANCE;
    }

    public Question save(Question question) {
        questions.put(question.id(), question);
        return question;
    }

    public Optional<Question> findById(Long id) {
        return Optional.ofNullable(questions.get(id));
    }

    public List<Question> findAll() {
        return questions.values().stream().toList();
    }

    public Optional<Question> deleteById(Long id) {

        return Optional.ofNullable(
                questions.remove(id)
        );
    }

    public List<Question> findQuestionByIds(List<Long> questionIds) {
        return questions.values().stream()
                .filter(q -> questionIds.contains(q.id()))
                .toList();
    }

    public void clear() {
        questions.clear();
    }
}
