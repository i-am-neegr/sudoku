package org.example.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
public class Session {
    private String sessionId = UUID.randomUUID().toString();
    private int[][] puzzle;
    private int[][] solution;
    private List<String> players = new ArrayList<>();
    private int currentTurn = 0;
    private int[][] board;
}