package com.universeexe.verity.voice;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Server-side queued voice request resolved to one or more {@link VerityVoiceVariant} steps.
 */
public final class VerityQueuedVoiceEvent {
    private final String requestId;
    private final VerityVoiceCategory category;
    private final VerityVoiceContext context;
    private final List<ResolvedStep> steps;
    @Nullable
    private final Runnable onEventComplete;
    private int stepIndex;
    private int pauseTicksRemaining;
    private int sessionId;
    private boolean started;

    private VerityQueuedVoiceEvent(
            String requestId,
            VerityVoiceCategory category,
            VerityVoiceContext context,
            List<ResolvedStep> steps,
            @Nullable Runnable onEventComplete
    ) {
        this.requestId = requestId;
        this.category = category;
        this.context = context;
        this.steps = List.copyOf(steps);
        this.onEventComplete = onEventComplete;
    }

    @Nullable
    public Runnable onEventComplete() {
        return onEventComplete;
    }

    public String requestId() {
        return requestId;
    }

    public VerityVoiceCategory category() {
        return category;
    }

    public VerityVoiceContext context() {
        return context;
    }

    public int sessionId() {
        return sessionId;
    }

    public void setSessionId(int sessionId) {
        this.sessionId = sessionId;
    }

    public boolean isStarted() {
        return started;
    }

    public void markStarted() {
        this.started = true;
    }

    public boolean hasSteps() {
        return !steps.isEmpty();
    }

    public boolean isComplete() {
        return stepIndex >= steps.size() && pauseTicksRemaining <= 0;
    }

    public int pauseTicksRemaining() {
        return pauseTicksRemaining;
    }

    public void tickPause() {
        if (pauseTicksRemaining > 0) {
            pauseTicksRemaining--;
        }
    }

    public Optional<ResolvedStep> currentStep() {
        if (stepIndex >= steps.size()) {
            return Optional.empty();
        }
        return Optional.of(steps.get(stepIndex));
    }

    public Optional<ResolvedStep> advanceAfterPlayback() {
        if (stepIndex >= steps.size()) {
            return Optional.empty();
        }
        ResolvedStep played = steps.get(stepIndex);
        stepIndex++;
        pauseTicksRemaining = played.pauseAfterTicks();
        if (pauseTicksRemaining <= 0 && stepIndex < steps.size()) {
            return Optional.of(steps.get(stepIndex));
        }
        return Optional.empty();
    }

    public Optional<ResolvedStep> nextStepAfterPause() {
        if (pauseTicksRemaining > 0 || stepIndex >= steps.size()) {
            return Optional.empty();
        }
        return Optional.of(steps.get(stepIndex));
    }

    public int totalDurationEstimate() {
        int total = 0;
        for (ResolvedStep step : steps) {
            total += step.variant().durationTicks() + step.pauseAfterTicks();
        }
        return total;
    }

    public record ResolvedStep(VerityVoiceVariant variant, int pauseAfterTicks) {
    }

    public static VerityQueuedVoiceEvent single(
            String requestId,
            VerityVoiceCategory category,
            VerityVoiceVariant variant,
            VerityVoiceContext context
    ) {
        return new VerityQueuedVoiceEvent(
                requestId,
                category,
                context,
                List.of(new ResolvedStep(variant, 0)),
                null
        );
    }

    public static VerityQueuedVoiceEvent fromConversation(
            String requestId,
            VerityConversation conversation,
            List<ResolvedStep> resolved,
            VerityVoiceContext context
    ) {
        return new VerityQueuedVoiceEvent(requestId, conversation.category(), context, resolved, null);
    }

    public static VerityQueuedVoiceEvent fromConversation(
            String requestId,
            VerityConversation conversation,
            List<ResolvedStep> resolved,
            VerityVoiceContext context,
            @Nullable Runnable onEventComplete
    ) {
        return new VerityQueuedVoiceEvent(requestId, conversation.category(), context, resolved, onEventComplete);
    }

    public static Builder builder(String requestId, VerityVoiceCategory category, VerityVoiceContext context) {
        return new Builder(requestId, category, context);
    }

    public static final class Builder {
        private final String requestId;
        private final VerityVoiceCategory category;
        private final VerityVoiceContext context;
        private final List<ResolvedStep> steps = new ArrayList<>();
        @Nullable
        private Runnable onEventComplete;

        private Builder(String requestId, VerityVoiceCategory category, VerityVoiceContext context) {
            this.requestId = requestId;
            this.category = category;
            this.context = context;
        }

        public Builder onComplete(@Nullable Runnable onEventComplete) {
            this.onEventComplete = onEventComplete;
            return this;
        }

        public Builder add(VerityVoiceVariant variant, int pauseAfterTicks) {
            steps.add(new ResolvedStep(variant, Math.max(0, pauseAfterTicks)));
            return this;
        }

        public VerityQueuedVoiceEvent build() {
            return new VerityQueuedVoiceEvent(requestId, category, context, steps, onEventComplete);
        }
    }
}
