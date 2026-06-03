package com.rotatingmind.ratingservice.service;

import com.rotatingmind.ratingservice.model.User;

public interface UserService {

    User createUser(User user);

    User getUserById(Long id);
}
