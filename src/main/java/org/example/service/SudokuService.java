package org.example.service;

import org.example.model.Player;
import org.example.model.Session;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SudokuService {

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    public Session createGame(String difficulty, String userId, String name) {
        int cellsToRemove = switch (difficulty.toLowerCase()) {
            case "easy" -> 40;
            case "medium" -> 50;
            case "hard" -> 60;
            default -> 50;
        };

        int[][] board = generateFullBoard();
        int[][] solution = copyBoard(board);
        removeCells(board, cellsToRemove);

        Session session = new Session();
        session.setPuzzle(copyBoard(board));
        session.setSolution(solution);
        session.setCurrentBoard(copyBoard(board));

        Player player = new Player();
        player.setId(userId);
        player.setName(name);
        session.getPlayers().add(player);

        sessions.put(session.getSessionId(), session);
        return session;
    }

    public Optional<Session> joinGame(String sessionId, String userId, String name) {
        Session session = sessions.get(sessionId);
        if (session == null || session.isFinished()) {
            return Optional.empty();
        }
        if (session.getPlayers().stream().noneMatch(p -> p.getId().equals(userId))) {
            Player player = new Player();
            player.setId(userId);
            player.setName(name);
            session.getPlayers().add(player);
        }
        return Optional.of(session);
    }

    public Optional<Session> getGameState(String sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
    }

    public boolean makeMove(String sessionId, String userId, int row, int col, int value) {
        Session session = sessions.get(sessionId);
        if (session == null || session.isFinished()) return false;

        int playerIndex = session.getPlayers().stream().filter(p -> p.getId().equals(userId)).mapToInt(session.getPlayers()::indexOf).findFirst().orElse(-1);
        if (playerIndex < 0) return false;

        if (session.getPlayers().size() > 1 && playerIndex != session.getCurrentTurnIndex()) {
            return false; // Не очередь в мультиплеере
        }

        if (row < 0 || row > 8 || col < 0 || col > 8 || value < 1 || value > 9) {
            return false;
        }

        if (session.getCurrentBoard()[row][col] != 0) {
            return false;
        }

        if (session.getSolution()[row][col] != value) {
            return false;
        }

        session.getCurrentBoard()[row][col] = value;
        if (session.getPlayers().size() > 1) {
            session.setCurrentTurnIndex((session.getCurrentTurnIndex() + 1) % session.getPlayers().size());
        }

        if (isSolved(session.getCurrentBoard())) {
            session.setFinished(true);
        }

        return true;
    }

    public boolean passTurn(String sessionId, String userId) {
        Session session = sessions.get(sessionId);
        if (session == null || session.isFinished() || session.getPlayers().size() <= 1) return false;

        int playerIndex = session.getPlayers().stream().filter(p -> p.getId().equals(userId)).mapToInt(session.getPlayers()::indexOf).findFirst().orElse(-1);
        if (playerIndex < 0 || playerIndex != session.getCurrentTurnIndex()) {
            return false;
        }

        session.setCurrentTurnIndex((session.getCurrentTurnIndex() + 1) % session.getPlayers().size());
        return true;
    }

    // Генерация судоку (без изменений)
    private int[][] generateFullBoard() {
        int[][] board = new int[9][9];
        fillBoard(board);
        return board;
    }

    private boolean fillBoard(int[][] board) {
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                if (board[r][c] == 0) {
                    List<Integer> nums = new ArrayList<>(List.of(1,2,3,4,5,6,7,8,9));
                    Collections.shuffle(nums);
                    for (int num : nums) {
                        if (isSafe(board, r, c, num)) {
                            board[r][c] = num;
                            if (fillBoard(board)) return true;
                            board[r][c] = 0;
                        }
                    }
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isSafe(int[][] board, int row, int col, int num) {
        for (int i = 0; i < 9; i++) {
            if (board[row][i] == num || board[i][col] == num) return false;
        }
        int startRow = row - row % 3;
        int startCol = col - col % 3;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (board[startRow + i][startCol + j] == num) return false;
            }
        }
        return true;
    }

    private void removeCells(int[][] board, int count) {
        Random rand = new Random();
        while (count > 0) {
            int r = rand.nextInt(9);
            int c = rand.nextInt(9);
            if (board[r][c] != 0) {
                board[r][c] = 0;
                count--;
            }
        }
    }

    private boolean isSolved(int[][] board) {
        for (int[] row : board) {
            for (int cell : row) {
                if (cell == 0) return false;
            }
        }
        return true;
    }

    private int[][] copyBoard(int[][] src) {
        int[][] dest = new int[9][9];
        for (int i = 0; i < 9; i++) {
            System.arraycopy(src[i], 0, dest[i], 0, 9);
        }
        return dest;
    }
}