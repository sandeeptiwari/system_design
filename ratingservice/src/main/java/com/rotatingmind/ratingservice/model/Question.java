package com.rotatingmind.ratingservice.model;

import java.util.List;

public record Question(Long id, String questionTxt, List<Option> options) {
}
