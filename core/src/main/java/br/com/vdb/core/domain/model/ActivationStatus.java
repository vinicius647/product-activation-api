package br.com.vdb.core.domain.model;

public enum ActivationStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED;

    /**
     * PENDING -> PROCESSING -> COMPLETED | FAILED, and PROCESSING -> PENDING for a new attempt.
     * COMPLETED and FAILED are final.
     */
    public boolean canTransitionTo(ActivationStatus target) {
        return switch (this) {
            case PENDING -> target == PROCESSING;
            case PROCESSING -> target == COMPLETED || target == FAILED || target == PENDING;
            case COMPLETED, FAILED -> false;
        };
    }

    public boolean isFinal() {
        return this == COMPLETED || this == FAILED;
    }
}
