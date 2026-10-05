package org.banksolution.exception;

public class OnboardingRequiredException extends RuntimeException {

    public OnboardingRequiredException() {
        super("Complete onboarding before using banking features");
    }
}
