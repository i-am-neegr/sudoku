// src/main/java/org/example/controller/TestSudokuController.java

package org.example.controller;

import org.example.model.Session;
import org.example.service.SudokuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/test")
public class TestSudokuController {

    private final SudokuService sudokuService;

    @Autowired
    public TestSudokuController(SudokuService sudokuService) {
        this.sudokuService = sudokuService;
    }

    @PostMapping("/create-session")
    public ResponseEntity<Map<String, Object>> createTestSession(
            @RequestParam(defaultValue = "medium") String difficulty,
            @RequestParam(defaultValue = "test-user-123") String userId) {

        Session session = sudokuService.createGame(difficulty, userId, "bober666");

        return ResponseEntity.ok(Map.of(
                "sessionId", session.getSessionId(),
                "difficulty", difficulty,
                "players", session.getPlayers(),
                "currentTurnIndex", session.getCurrentTurnIndex(),
                "boardPreview", getBoardPreview(session.getCurrentBoard()),
                "puzzleSize", countFilledCells(session.getCurrentBoard()) + " / 81 filled"
        ));
    }

    // Вспомогательные методы для читаемого вывода в swagger
    private String getBoardPreview(int[][] board) {
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < board.length; r++) {  // первые 3 строки
            for (int c = 0; c < Math.min(9, board[r].length); c++) {
                sb.append(board[r][c] == 0 ? "." : board[r][c]).append(" ");
            }
            sb.append("\n");
        }
        sb.append("...");
        return sb.toString();
    }

    private int countFilledCells(int[][] board) {
        int count = 0;
        for (int[] row : board) {
            for (int cell : row) {
                if (cell != 0) count++;
            }
        }
        return count;
    }
}