package com.lhcsim.core;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe publish/subscribe event bus for decoupled communication
 * between game subsystems.
 * <p>
 * {@link #post(Object)} dispatches synchronously on the calling thread.
 * {@link #publishAsync(Object)} enqueues the event; the queue is drained
 * at the start of the next frame via {@link #drainAsync()}.
 */
public final class EventBus {

    @FunctionalInterface
    public interface EventListener<T> {
        void onEvent(T event);
    }

    private final Map<Class<?>, CopyOnWriteArrayList<EventListener<?>>> listeners =
            new ConcurrentHashMap<>();

    private final Queue<Object> asyncQueue = new ConcurrentLinkedQueue<>();

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
     * Dispatches synchronously on the calling thread.
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
     * Enqueue an event for deferred delivery.  The event will be
     * dispatched during the next call to {@link #drainAsync()}.
     */
    public void publishAsync(Object event) {
        asyncQueue.add(event);
    }

    /**
     * Drain the async queue, posting each enqueued event synchronously.
     * Should be called once per frame at frame start.
     */
    public void drainAsync() {
        Object event;
        while ((event = asyncQueue.poll()) != null) {
            post(event);
        }
    }

    /**
     * Remove all registered listeners and pending async events.
     */
    public void clear() {
        listeners.clear();
        asyncQueue.clear();
    }
}
