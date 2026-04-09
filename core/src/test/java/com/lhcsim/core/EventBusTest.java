package com.lhcsim.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EventBusTest {

    private EventBus bus;

    @BeforeEach
    void setUp() {
        bus = new EventBus();
    }

    @Test
    void testSubscribeAndPost() {
        List<String> received = new ArrayList<>();
        bus.subscribe(String.class, received::add);

        bus.post("hello");

        assertThat(received).containsExactly("hello");
    }

    @Test
    void testTypeFiltering() {
        List<String> received = new ArrayList<>();
        bus.subscribe(String.class, received::add);

        bus.post(42);

        assertThat(received).isEmpty();
    }

    @Test
    void testUnsubscribe() {
        List<String> received = new ArrayList<>();
        EventBus.EventListener<String> listener = received::add;
        bus.subscribe(String.class, listener);
        bus.unsubscribe(String.class, listener);

        bus.post("hello");

        assertThat(received).isEmpty();
    }

    @Test
    void testClear() {
        List<String> received = new ArrayList<>();
        bus.subscribe(String.class, received::add);
        bus.clear();

        bus.post("hello");

        assertThat(received).isEmpty();
    }

    @Test
    void testPublishAsync_deferredUntilDrain() {
        List<String> received = new ArrayList<>();
        bus.subscribe(String.class, received::add);

        bus.publishAsync("deferred");

        // Not delivered yet
        assertThat(received).isEmpty();

        // Drain delivers it
        bus.drainAsync();
        assertThat(received).containsExactly("deferred");
    }

    @Test
    void testPublishAsync_multipleEvents() {
        List<String> received = new ArrayList<>();
        bus.subscribe(String.class, received::add);

        bus.publishAsync("a");
        bus.publishAsync("b");
        bus.drainAsync();

        assertThat(received).containsExactly("a", "b");
    }
}
