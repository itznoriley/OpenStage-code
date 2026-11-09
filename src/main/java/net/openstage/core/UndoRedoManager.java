package net.openstage.core;

import java.util.ArrayDeque;
import java.util.Deque;

public final class UndoRedoManager {
    public static final int MAX_HISTORY = 100;

    public interface UndoableAction {
        void execute();

        void undo();

        String description();
    }

    private final Deque<UndoableAction> undoStack = new ArrayDeque<>();
    private final Deque<UndoableAction> redoStack = new ArrayDeque<>();
    private static final UndoRedoManager INSTANCE = new UndoRedoManager();

    public static UndoRedoManager getInstance() {
        return INSTANCE;
    }

    public UndoRedoManager() {}

    public void perform(UndoableAction action) {
        if (action == null) {
            throw new NullPointerException("UndoableAction must not be null");
        }
        action.execute();
        undoStack.push(action);
        redoStack.clear();

        while (undoStack.size() > MAX_HISTORY) {
            ((ArrayDeque<UndoableAction>) undoStack).removeLast();
        }
    }

    public void undo() {
        if (undoStack.isEmpty()) {
            return;
        }
        UndoableAction action = undoStack.pop();
        action.undo();
        redoStack.push(action);
    }

    public void redo() {
        if (redoStack.isEmpty()) {
            return;
        }
        UndoableAction action = redoStack.pop();
        action.execute();
        undoStack.push(action);
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    public int undoDepth() {
        return undoStack.size();
    }

    public int redoDepth() {
        return redoStack.size();
    }

    public void clear() {
        undoStack.clear();
        redoStack.clear();
    }
}

