package org.example.controller;

import org.example.model.Player;
import org.example.model.Session;
import org.example.service.SudokuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.HashMap;
import java.util.Map;

@Controller
public class SudokuWebSocketController {

    private final SudokuService service;
    private final SimpMessagingTemplate template;

    @Autowired
    public SudokuWebSocketController(SudokuService service, SimpMessagingTemplate template) {
        this.service = service;
        this.template = template;
    }

    @MessageMapping("/create")
    public void create(@Payload Map<String, String> payload) {
        String difficulty = payload.get("difficulty");
        String userId = payload.get("userId");
        String name = payload.getOrDefault("name", "Anonymous");

        Session session = service.createGame(difficulty, userId, name);

        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", session.getSessionId());
        response.put("board", session.getCurrentBoard());
        response.put("players", session.getPlayers());
        response.put("currentTurnIndex", session.getCurrentTurnIndex());

        // Отправляем создателю приватно
        template.convertAndSendToUser(userId, "/topic/session", response);
    }

    @MessageMapping("/join")
    public void join(@Payload Map<String, String> payload) {
        String sessionId = payload.get("sessionId");
        String userId = payload.get("userId");
        String name = payload.getOrDefault("name", "Anonymous");

        var optSession = service.joinGame(sessionId, userId, name);
        if (optSession.isEmpty()) {
            // Можно отправить ошибку, но для простоты просто игнорируем
            return;
        }

        Session session = optSession.get();

        // Уведомляем всех в комнате об обновлённом списке игроков
        template.convertAndSend("/topic/" + sessionId + "/players", Map.of(
                "players", session.getPlayers()
        ));

        // Отправляем новому игроку текущее состояние
        Map<String, Object> state = new HashMap<>();
        state.put("board", session.getCurrentBoard());
        state.put("currentTurnIndex", session.getCurrentTurnIndex());
        state.put("players", session.getPlayers());
        state.put("finished", session.isFinished());

        template.convertAndSendToUser(userId, "/topic/session", state);
    }

    @MessageMapping("/move")
    public void makeMove(@Payload Map<String, Object> payload) {
        String sessionId = (String) payload.get("sessionId");
        String userId = (String) payload.get("userId");
        Integer row = getInt(payload, "row");
        Integer col = getInt(payload, "col");
        Integer value = getInt(payload, "value");

        if (row == null || col == null || value == null) return;

        boolean success = service.makeMove(sessionId, userId, row, col, value);
        if (!success) return;

        var optSession = service.getGameState(sessionId);
        if (optSession.isEmpty()) return;

        Session session = optSession.get();

        Map<String, Object> update = new HashMap<>();
        update.put("board", session.getCurrentBoard());
        update.put("currentTurnIndex", session.getCurrentTurnIndex());

        // Рассылаем обновление всем в сессии
        template.convertAndSend("/topic/" + sessionId + "/board", update);

        if (session.isFinished()) {
            template.convertAndSend("/topic/" + sessionId + "/finish", Map.of(
                    "finished", true,
                    "message", "Судоку решено!"
            ));
        }
    }

    @MessageMapping("/pass")
    public void pass(@Payload Map<String, String> payload) {
        String sessionId = payload.get("sessionId");
        String userId = payload.get("userId");

        boolean success = service.passTurn(sessionId, userId);
        if (!success) return;

        var optSession = service.getGameState(sessionId);
        if (optSession.isEmpty()) return;

        Session session = optSession.get();

        template.convertAndSend("/topic/" + sessionId + "/turn", Map.of(
                "currentTurnIndex", session.getCurrentTurnIndex()
        ));
    }

    // Вспомогательный метод для безопасного получения int из payload
    private Integer getInt(Map<String, Object> payload, String key) {
        Object val = payload.get(key);
        if (val instanceof Number) {
            return ((Number) val).intValue();
        }
        return null;
    }
}