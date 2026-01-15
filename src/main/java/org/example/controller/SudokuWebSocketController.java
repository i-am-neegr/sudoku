package org.example.controller;

import org.example.model.Session;
import org.example.service.SudokuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

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

        Session session = service.createSession(difficulty, userId);

        Map<String, Object> response = Map.of(
                "sessionId", session.getSessionId(),
                "board", session.getBoard(),
                "players", session.getPlayers(),
                "currentTurnIndex", session.getCurrentTurn()
        );

        // Отправляем создателю приватно
        template.convertAndSendToUser(userId, "/topic/session", response);
    }

    @MessageMapping("/join")
    public void join(@Payload Map<String, String> payload) {
        String sessionId = payload.get("sessionId");
        String userId = payload.get("userId");

        Session session = service.joinSession(sessionId, userId);
        if (session == null) return;

        // Уведомляем всех в комнате
        template.convertAndSend("/topic/" + sessionId + "/players", Map.of(
                "players", session.getPlayers()
        ));

        // Отправляем новому игроку текущее состояние
        Map<String, Object> state = Map.of(
                "board", session.getBoard(),
                "currentTurnIndex", session.getCurrentTurn(),
                "players", session.getPlayers()
        );
        template.convertAndSendToUser(userId, "/topic/session", state);
    }

    @MessageMapping("/move")
    public void makeMove(@Payload Map<String, Object> payload) {
        String sessionId = (String) payload.get("sessionId");
        String userId = (String) payload.get("userId");
        int row = ((Number) payload.get("row")).intValue();
        int col = ((Number) payload.get("col")).intValue();
        int value = ((Number) payload.get("value")).intValue();

        boolean success = service.makeMove(sessionId, userId, row, col, value);
        if (!success) return;

        Session session = service.getSession(sessionId);
        if (session == null) return;

        Map<String, Object> update = Map.of(
                "board", session.getBoard(),
                "currentTurnIndex", session.getCurrentTurn()
        );

        // Рассылаем всем в сессии
        template.convertAndSend("/topic/" + sessionId + "/board", update);

        if (service.isBoardFull(session.getBoard())) return; {
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

        Session session = service.getSession(sessionId);
        if (session == null) return;

        template.convertAndSend("/topic/" + sessionId + "/turn", Map.of(
                "currentTurnIndex", session.getCurrentTurn()
        ));
    }
}