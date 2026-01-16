package org.example.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
public class Session {
    private final String sessionId = UUID.randomUUID().toString();
    private int[][] puzzle;
    private int[][] solution;
    private List<Player> players = new ArrayList<>();
    private int currentTurnIndex = 0;
    private int[][] currentBoard;
    private boolean finished = false;
}