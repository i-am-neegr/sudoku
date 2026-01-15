package org.example.controller;

import org.example.model.Session;
import org.example.service.SudokuService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/sudoku")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
public class SudokuController {

    private final SudokuService sudokuService;

    public SudokuController(SudokuService sudokuService) {
        this.sudokuService = sudokuService;
    }

    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createSession(
            @RequestParam String difficulty,
            @RequestParam String userId) {

        Session session = sudokuService.createSession(difficulty, userId);

        return ResponseEntity.ok(Map.of(
                "sessionId", session.getSessionId(),
                "board", session.getBoard(),
                "players", session.getPlayers(),
                "currentTurn", session.getCurrentTurn()
        ));
    }

    // Опционально — GET для получения состояния (если polling нужен)
    @GetMapping("/{sessionId}")
    public ResponseEntity<Map<String, Object>> getSession(@PathVariable String sessionId) {
        Session session = sudokuService.getSession(sessionId);
        if (session == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of(
                "board", session.getBoard(),
                "players", session.getPlayers(),
                "currentTurn", session.getCurrentTurn()
        ));
    }
}