package org.example.domain.enums;

/**
 * FIX 4.4 Tag 150 (ExecType).
 * Describes the type of execution event in an ExecutionReport.
 */
public enum ExecType {
    NEW('0'),
    PARTIAL_FILL('1'),
    FILL('2'),
    CANCELED('4'),
    REPLACED('5'),
    REJECTED('8');

    private final char fixCode;

    ExecType(char fixCode) {
        this.fixCode = fixCode;
    }

    public char getFixCode() {
        return fixCode;
    }
}