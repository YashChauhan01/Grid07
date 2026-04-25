package com.grid07.assignment.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum InteractionType {
    BOT_REPLY(1),
    HUMAN_LIKE(20),
    HUMAN_COMMENT(50);

    private final int score;
}