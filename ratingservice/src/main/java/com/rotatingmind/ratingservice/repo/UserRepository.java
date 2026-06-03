package com.rotatingmind.ratingservice.repo;

import com.rotatingmind.ratingservice.model.User;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class UserRepository {
    private final ConcurrentHashMap<Long, User> users =
            new ConcurrentHashMap<>();

    private UserRepository() {
    }

    // Holder class for lazy thread-safe singleton initialization
    private static class Holder {
        private static final UserRepository INSTANCE = new UserRepository();
    }

    // Public accessor for the singleton
    public static UserRepository getInstance() {
        return UserRepository.Holder.INSTANCE;
    }

    public User save(User user) {
        users.put(user.id(), user);
        return user;
    }

    public Optional<User> findById(Long id) {
        return Optional.ofNullable(users.get(id));
    }

    public List<User> findAll() {
        return users.values().stream().toList();
    }

    public Optional<User> deleteById(Long id) {

        return Optional.ofNullable(
                users.remove(id)
        );
    }

    public void clear() {
        users.clear();
    }
}
