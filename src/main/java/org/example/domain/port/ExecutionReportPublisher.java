package org.example.domain.port;

import org.example.domain.model.ExecutionReport;

/**
 * Observer interface for execution report consumers.
 *
 * Pattern: Observer — decouples the book (publisher) from downstream
 * systems (risk engine, OMS, FIX session layer) without the book
 * needing to know who listens.
 *
 * Thread-safety: implementations must document their own guarantees.
 */
@FunctionalInterface
public interface ExecutionReportPublisher {
    void publish(ExecutionReport report);
}