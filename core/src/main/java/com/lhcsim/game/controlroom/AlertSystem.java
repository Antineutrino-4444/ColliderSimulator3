package com.lhcsim.game.controlroom;

import com.lhcsim.core.EventBus;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages time-limited control-room alerts that the player must acknowledge
 * before they expire.
 */
public class AlertSystem {

    // ── Severity ────────────────────────────────────────────────────

    public enum AlertSeverity {
        INFO,
        WARNING,
        CRITICAL
    }

    // ── Alert record ────────────────────────────────────────────────

    public record Alert(
            long id,
            String subsystem,
            String message,
            AlertSeverity severity,
            double responseWindowSeconds,
            double timeRemainingSeconds,
            boolean acknowledged
    ) {
        public Alert withTimeRemaining(double remaining) {
            return new Alert(id, subsystem, message, severity,
                    responseWindowSeconds, remaining, acknowledged);
        }

        public Alert acknowledge() {
            return new Alert(id, subsystem, message, severity,
                    responseWindowSeconds, timeRemainingSeconds, true);
        }
    }

    // ── EventBus event records ──────────────────────────────────────

    public record AlertCreated(Alert alert) {}
    public record AlertExpired(Alert alert) {}
    public record AlertAcknowledged(Alert alert) {}

    // ── State ───────────────────────────────────────────────────────

    private final EventBus eventBus;
    private final CopyOnWriteArrayList<Alert> activeAlerts = new CopyOnWriteArrayList<>();
    private long nextId = 1;

    public AlertSystem(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    /**
     * Creates and posts a new alert.
     *
     * @return the created alert
     */
    public Alert createAlert(String subsystem, String message,
                             AlertSeverity severity, double responseWindowSeconds) {
        Alert alert = new Alert(nextId++, subsystem, message, severity,
                responseWindowSeconds, responseWindowSeconds, false);
        activeAlerts.add(alert);
        eventBus.post(new AlertCreated(alert));
        return alert;
    }

    /**
     * Acknowledges the alert with the given id.
     *
     * @return {@code true} if the alert was found and acknowledged
     */
    public boolean acknowledgeAlert(long alertId) {
        for (int i = 0; i < activeAlerts.size(); i++) {
            Alert a = activeAlerts.get(i);
            if (a.id() == alertId && !a.acknowledged()) {
                Alert acked = a.acknowledge();
                activeAlerts.set(i, acked);
                eventBus.post(new AlertAcknowledged(acked));
                return true;
            }
        }
        return false;
    }

    /**
     * Advances all active alert timers by {@code deltaSeconds} and
     * expires any that have run out of time without being acknowledged.
     */
    public void update(double deltaSeconds) {
        for (int i = activeAlerts.size() - 1; i >= 0; i--) {
            Alert a = activeAlerts.get(i);
            if (a.acknowledged()) {
                activeAlerts.remove(i);
                continue;
            }
            double remaining = a.timeRemainingSeconds() - deltaSeconds;
            if (remaining <= 0) {
                activeAlerts.remove(i);
                eventBus.post(new AlertExpired(a.withTimeRemaining(0)));
            } else {
                activeAlerts.set(i, a.withTimeRemaining(remaining));
            }
        }
    }

    /** Returns an unmodifiable view of currently active alerts. */
    public List<Alert> getActiveAlerts() {
        return Collections.unmodifiableList(activeAlerts);
    }
}
