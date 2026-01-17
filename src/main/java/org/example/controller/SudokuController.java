package org.example.controller;

import org.example.model.Session;
import org.example.service.SudokuService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/sudoku")
@RequiredArgsConstructor
@CrossOrigin("*")
public class SudokuController {

    private final SudokuService service;

    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createGame(
            @RequestParam String difficulty,
            @RequestParam String userId,
            @RequestParam String name) {   // ← убрали initData

        Session session = service.createGame(difficulty, userId, name);

        Map<String, Object> resp = new HashMap<>();
        resp.put("sessionId", session.getSessionId());
        resp.put("board", session.getCurrentBoard());
        resp.put("players", session.getPlayers());
        resp.put("currentTurnIndex", session.getCurrentTurnIndex());
        resp.put("finished", session.isFinished());

        return ResponseEntity.ok(resp);
    }

    @PostMapping("/join/{sessionId}")
    public ResponseEntity<Map<String, Object>> joinGame(
            @PathVariable String sessionId,
            @RequestParam String userId,
            @RequestParam String name) {   // ← убрали initData

        Optional<Session> opt = service.joinGame(sessionId, userId, name);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Session session = opt.get();

        Map<String, Object> resp = new HashMap<>();
        resp.put("board", session.getCurrentBoard());
        resp.put("players", session.getPlayers());
        resp.put("currentTurnIndex", session.getCurrentTurnIndex());
        resp.put("finished", session.isFinished());

        return ResponseEntity.ok(resp);
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<Map<String, Object>> getState(@PathVariable String sessionId) {

        Optional<Session> opt = service.getGameState(sessionId);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Session s = opt.get();

        Map<String, Object> resp = new HashMap<>();
        resp.put("board", s.getCurrentBoard());
        resp.put("players", s.getPlayers());
        resp.put("currentTurnIndex", s.getCurrentTurnIndex());
        resp.put("finished", s.isFinished());

        return ResponseEntity.ok(resp);
    }

    @PostMapping("/{sessionId}/move")
    public ResponseEntity<Map<String, Object>> makeMove(
            @PathVariable String sessionId,
            @RequestParam String userId,
            @RequestParam int row,
            @RequestParam int col,
            @RequestParam int value) {   // ← убрали initData

        boolean success = service.makeMove(sessionId, userId, row, col, value);

        Map<String, Object> resp = new HashMap<>();
        resp.put("success", success);

        Optional<Session> opt = service.getGameState(sessionId);
        opt.ifPresent(s -> {
            resp.put("board", s.getCurrentBoard());
            resp.put("currentTurnIndex", s.getCurrentTurnIndex());
            resp.put("finished", s.isFinished());
        });

        return ResponseEntity.ok(resp);
    }

    @PostMapping("/{sessionId}/pass")
    public ResponseEntity<Map<String, Object>> passTurn(
            @PathVariable String sessionId,
            @RequestParam String userId) {   // ← убрали initData

        boolean success = service.passTurn(sessionId, userId);

        Map<String, Object> resp = new HashMap<>();
        resp.put("success", success);

        Optional<Session> opt = service.getGameState(sessionId);
        opt.ifPresent(s -> {
            resp.put("currentTurnIndex", s.getCurrentTurnIndex());
        });

        return ResponseEntity.ok(resp);
    }
}