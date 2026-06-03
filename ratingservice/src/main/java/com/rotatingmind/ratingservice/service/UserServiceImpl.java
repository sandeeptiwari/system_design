package com.rotatingmind.ratingservice.service;

import com.rotatingmind.ratingservice.model.User;
import com.rotatingmind.ratingservice.repo.UserRepository;

public class UserServiceImpl implements UserService {

    private UserRepository userRepository = UserRepository.getInstance();

    @Override
    public User createUser(User user) {
        return userRepository.save(user);
    }

    @Override
    public User getUserById(Long id) {
        return userRepository.findById(id).get();
    }
}
