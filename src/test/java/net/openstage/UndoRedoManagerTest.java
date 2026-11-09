package net.openstage;

import net.openstage.core.UndoRedoManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UndoRedoManager Adversarial QA and History Tests")
class UndoRedoManagerTest {
    private UndoRedoManager manager;

    @BeforeEach
    void setUp() {
        manager = new UndoRedoManager();
    }

    private static class TrackingAction implements UndoRedoManager.UndoableAction {
        private final int id;
        private final List<Integer> executionLog;
        private final List<Integer> undoLog;

        public TrackingAction(int id, List<Integer> executionLog, List<Integer> undoLog) {
            this.id = id;
            this.executionLog = executionLog;
            this.undoLog = undoLog;
        }

        @Override
        public void execute() {
            executionLog.add(id);
        }

        @Override
        public void undo() {
            undoLog.add(id);
        }

        @Override
        public String description() {
            return "Action " + id;
        }
    }

    @Test
    @DisplayName("undo and redo on empty stacks do not throw exceptions")
    void testUndoRedoOnEmptyStacks() {
        assertFalse(manager.canUndo());
        assertFalse(manager.canRedo());
        assertEquals(0, manager.undoDepth());
        assertEquals(0, manager.redoDepth());

        assertDoesNotThrow(() -> manager.undo());
        assertDoesNotThrow(() -> manager.redo());

        assertEquals(0, manager.undoDepth());
        assertEquals(0, manager.redoDepth());
    }

    @Test
    @DisplayName("Max history capacity: pushing 150 actions drops oldest 50 and retains newest 100")
    void testMaxHistoryCapacity() {
        List<Integer> execLog = new ArrayList<>();
        List<Integer> undoLog = new ArrayList<>();

        for (int i = 0; i < 150; i++) {
            manager.perform(new TrackingAction(i, execLog, undoLog));
        }

        assertEquals(150, execLog.size());
        assertEquals(UndoRedoManager.MAX_HISTORY, manager.undoDepth());
        assertEquals(100, manager.undoDepth());

        for (int i = 0; i < 100; i++) {
            assertTrue(manager.canUndo());
            manager.undo();
        }

        assertEquals(0, manager.undoDepth());
        assertFalse(manager.canUndo());
        assertEquals(100, undoLog.size());

        for (int i = 0; i < 100; i++) {
            int expectedId = 149 - i;
            assertEquals(expectedId, undoLog.get(i));
        }

        assertFalse(undoLog.contains(0));
        assertFalse(undoLog.contains(49));
        assertTrue(undoLog.contains(50));
        assertTrue(undoLog.contains(149));

        assertDoesNotThrow(() -> manager.undo());
    }

    @Test
    @DisplayName("perform after undo clears redo stack")
    void testPerformAfterUndoClearsRedoStack() {
        List<Integer> execLog = new ArrayList<>();
        List<Integer> undoLog = new ArrayList<>();

        manager.perform(new TrackingAction(1, execLog, undoLog));
        manager.perform(new TrackingAction(2, execLog, undoLog));
        manager.perform(new TrackingAction(3, execLog, undoLog));

        assertEquals(3, manager.undoDepth());
        assertEquals(0, manager.redoDepth());

        manager.undo();
        manager.undo();

        assertEquals(1, manager.undoDepth());
        assertEquals(2, manager.redoDepth());
        assertTrue(manager.canRedo());

        manager.perform(new TrackingAction(4, execLog, undoLog));

        assertEquals(2, manager.undoDepth());
        assertEquals(0, manager.redoDepth());
        assertFalse(manager.canRedo());

        assertDoesNotThrow(() -> manager.redo());
        assertEquals(0, manager.redoDepth());
    }

    @Test
    @DisplayName("Execution and reversal order (LIFO undo, FIFO redo order)")
    void testExecutionAndReversalOrder() {
        List<String> state = new ArrayList<>();

        class AppendAction implements UndoRedoManager.UndoableAction {
            private final String value;
            AppendAction(String value) { this.value = value; }
            @Override public void execute() { state.add(value); }
            @Override public void undo() { state.remove(state.size() - 1); }
            @Override public String description() { return "Append " + value; }
        }

        manager.perform(new AppendAction("A"));
        manager.perform(new AppendAction("B"));
        manager.perform(new AppendAction("C"));

        assertEquals(List.of("A", "B", "C"), state);

        manager.undo();
        assertEquals(List.of("A", "B"), state);

        manager.undo();
        assertEquals(List.of("A"), state);

        manager.redo();
        assertEquals(List.of("A", "B"), state);

        manager.redo();
        assertEquals(List.of("A", "B", "C"), state);

        manager.undo();
        manager.undo();
        manager.undo();
        assertTrue(state.isEmpty());
    }

    @Test
    @DisplayName("Null action throws NullPointerException")
    void testNullActionThrows() {
        assertThrows(NullPointerException.class, () -> manager.perform(null));
    }

    @Test
    @DisplayName("clear empties both undo and redo stacks")
    void testClear() {
        List<Integer> execLog = new ArrayList<>();
        List<Integer> undoLog = new ArrayList<>();

        manager.perform(new TrackingAction(1, execLog, undoLog));
        manager.perform(new TrackingAction(2, execLog, undoLog));
        manager.undo();

        assertEquals(1, manager.undoDepth());
        assertEquals(1, manager.redoDepth());

        manager.clear();

        assertEquals(0, manager.undoDepth());
        assertEquals(0, manager.redoDepth());
        assertFalse(manager.canUndo());
        assertFalse(manager.canRedo());
    }
}

