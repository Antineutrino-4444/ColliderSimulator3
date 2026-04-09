package com.lhcsim.app;

import com.badlogic.gdx.Screen;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Manages a stack of {@link Screen}s and a registry of factory suppliers
 * keyed by {@link Screens}.  Supports push/pop navigation.
 */
public final class ScreenManager {

    private final Map<Screens, Supplier<Screen>> factories = new LinkedHashMap<>();
    private final Deque<Screen> stack = new ArrayDeque<>();
    private final LhcSimGame game;

    public ScreenManager(LhcSimGame game) {
        this.game = game;
    }

    /** Register a factory that creates a fresh Screen instance on demand. */
    public void register(Screens id, Supplier<Screen> factory) {
        factories.put(id, factory);
    }

    /** Push a new screen onto the stack and set it as the active screen. */
    public void push(Screens id) {
        Supplier<Screen> factory = factories.get(id);
        if (factory == null) {
            throw new IllegalArgumentException("No factory registered for screen: " + id);
        }
        push(factory.get());
    }

    /** Push an already-created screen onto the stack. */
    public void push(Screen screen) {
        stack.push(screen);
        game.setScreen(screen);
    }

    /**
     * Pop the current screen and return to the previous one.
     *
     * @return the popped screen, or {@code null} if the stack had only one entry
     */
    public Screen pop() {
        if (stack.size() <= 1) {
            return null;
        }
        Screen popped = stack.pop();
        popped.dispose();
        game.setScreen(stack.peek());
        return popped;
    }

    /** Returns the currently active screen (top of stack). */
    public Screen current() {
        return stack.peek();
    }

    /** Returns the depth of the screen stack. */
    public int depth() {
        return stack.size();
    }
}
