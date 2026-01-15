package org.example.service;

import org.example.model.Session;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SudokuService {

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public Session createSession(String difficulty, String userId) {
        int cellsToRemove = switch (difficulty.toLowerCase()) {
            case "easy"   -> 40;   // ~41–50 подсказок
            case "medium" -> 50;   // ~31–40 подсказок
            case "hard"   -> 60;   // ~21–30 подсказок
            default       -> 50;
        };

        // 1. Генерируем полностью заполненную доску (81 клетка)
        int[][] fullBoard = generateFullBoard();

        // 2. Копируем для хранения правильного решения
        int[][] solution = copyBoard(fullBoard);

        // 3. Создаём головоломку — удаляем клетки
        int[][] puzzle = copyBoard(fullBoard);
        removeCells(puzzle, cellsToRemove);

        // 4. Создаём сессию
        Session session = new Session();
        session.setPuzzle(puzzle);
        session.setSolution(solution);
        session.setBoard(copyBoard(puzzle));  // начальное состояние игры
        session.getPlayers().add(userId);
        session.setCurrentTurn(0);
        // session.setFinished(false);  // если у тебя есть такое поле — раскомментируй

        sessions.put(session.getSessionId(), session);

        // Отладочный вывод (можно удалить позже)
        System.out.printf("Сессия создана | sessionId: %s%n", session.getSessionId());
        System.out.printf("До удаления: %d клеток заполнено%n", countFilled(fullBoard));
        System.out.printf("После удаления: %d клеток заполнено (удалено %d)%n",
                countFilled(puzzle), cellsToRemove);

        return session;
    }

    public Session joinSession(String sessionId, String userId) {
        Session session = sessions.get(sessionId);
        if (session == null) {
            return null;
        }
        if (!session.getPlayers().contains(userId)) {
            session.getPlayers().add(userId);
        }
        return session;
    }

    public Session getSession(String sessionId) {
        return sessions.get(sessionId);
    }

    // ────────────────────────────────────────────────
    //               Генерация судоку
    // ────────────────────────────────────────────────

    private int[][] generateFullBoard() {
        int[][] board = new int[9][9];

        // Пытаемся заполнить (в 99.9% случаев получается с первого раза)
        if (!fillBoard(board)) {
            // Очень редкий случай — повторяем
            board = new int[9][9];
            fillBoard(board);
        }

        return board;
    }

    private boolean fillBoard(int[][] board) {
        // Ищем первую пустую клетку
        int row = -1;
        int col = -1;
        boolean foundEmpty = false;

        outer:
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                if (board[i][j] == 0) {
                    row = i;
                    col = j;
                    foundEmpty = true;
                    break outer;
                }
            }
        }

        // Если пустых клеток нет → доска заполнена
        if (!foundEmpty) {
            return true;
        }

        // Пробуем числа 1–9 в случайном порядке
        List<Integer> numbers = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9));
        Collections.shuffle(numbers, random);

        for (int num : numbers) {
            if (isSafe(board, row, col, num)) {
                board[row][col] = num;

                if (fillBoard(board)) {
                    return true;
                }

                // откат
                board[row][col] = 0;
            }
        }

        return false;
    }

    private boolean isSafe(int[][] board, int row, int col, int num) {
        // строка
        for (int x = 0; x < 9; x++) {
            if (board[row][x] == num) return false;
        }

        // столбец
        for (int x = 0; x < 9; x++) {
            if (board[x][col] == num) return false;
        }

        // 3×3 блок
        int startRow = row - row % 3;
        int startCol = col - col % 3;

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (board[startRow + i][startCol + j] == num) {
                    return false;
                }
            }
        }

        return true;
    }

    private void removeCells(int[][] board, int count) {
        int removed = 0;
        while (removed < count) {
            int r = random.nextInt(9);
            int c = random.nextInt(9);
            if (board[r][c] != 0) {
                board[r][c] = 0;
                removed++;
            }
        }
    }

    // ────────────────────────────────────────────────
    //               Вспомогательные методы
    // ────────────────────────────────────────────────

    private int[][] copyBoard(int[][] src) {
        int[][] dest = new int[9][9];
        for (int i = 0; i < 9; i++) {
            System.arraycopy(src[i], 0, dest[i], 0, 9);
        }
        return dest;
    }

    private int countFilled(int[][] board) {
        int count = 0;
        for (int[] row : board) {
            for (int cell : row) {
                if (cell != 0) count++;
            }
        }
        return count;
    }

    // ────────────────────────────────────────────────
    //               Методы для ходов (пример)
    // ────────────────────────────────────────────────

    public boolean makeMove(String sessionId, String userId, int row, int col, int value) {
        Session session = sessions.get(sessionId);
        if (session == null) return false;

        // Здесь твоя логика проверки очереди, пустой клетки и правильности значения
        // ...

        // Пример минимальной реализации
        if (session.getBoard()[row][col] == 0 &&
                session.getSolution()[row][col] == value) {
            session.getBoard()[row][col] = value;

            // Переход хода
            session.setCurrentTurn(
                    (session.getCurrentTurn() + 1) % session.getPlayers().size()
            );

            // Проверка завершения (если нужно)
            if (countFilled(session.getBoard()) == 81) {
                // session.setFinished(true);
                // можно отправить сообщение всем
            }

            return true;
        }
        return false;
    }

    public boolean passTurn(String sessionId, String userId) {
        Session session = sessions.get(sessionId);
        if (session == null) return false;

        // Проверка, что это ход этого игрока
        // ...

        session.setCurrentTurn(
                (session.getCurrentTurn() + 1) % session.getPlayers().size()
        );
        return true;
    }

    public boolean isBoardFull(int[][] board) {
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                if (board[r][c] == 0) {
                    return false;
                }
            }
        }
        return true;
    }
}