package com.lhcsim.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe publish/subscribe event bus for decoupled communication
 * between game subsystems.
 */
public final class EventBus {

    @FunctionalInterface
    public interface EventListener<T> {
        void onEvent(T event);
    }

    private final Map<Class<?>, CopyOnWriteArrayList<EventListener<?>>> listeners =
            new ConcurrentHashMap<>();

    /**
     * Subscribe a listener for events of a specific type.
     */
    @SuppressWarnings("unchecked")
    public <T> void subscribe(Class<T> eventType, EventListener<T> listener) {
        listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
                 .add(listener);
    }

    /**
     * Unsubscribe a previously registered listener.
     */
    public <T> void unsubscribe(Class<T> eventType, EventListener<T> listener) {
        CopyOnWriteArrayList<EventListener<?>> list = listeners.get(eventType);
        if (list != null) {
            list.remove(listener);
        }
    }

    /**
     * Post an event to all listeners registered for its type.
     */
    @SuppressWarnings("unchecked")
    public <T> void post(T event) {
        CopyOnWriteArrayList<EventListener<?>> list = listeners.get(event.getClass());
        if (list != null) {
            for (EventListener<?> raw : list) {
                ((EventListener<T>) raw).onEvent(event);
            }
        }
    }

    /**
     * Remove all registered listeners.
     */
    public void clear() {
        listeners.clear();
    }
}
