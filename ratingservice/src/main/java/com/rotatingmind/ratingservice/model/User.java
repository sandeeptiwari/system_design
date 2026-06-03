package com.rotatingmind.ratingservice.model;

import java.util.List;

public record User(Long id, String firstName, String lastName, String email, List<Role> roles) {
}
