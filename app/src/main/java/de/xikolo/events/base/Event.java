package de.xikolo.events.base;

public abstract class Event {

    protected final String message;

    public Event(String message) {
        this.message  = message;
    }

    public Event() {
        this.message = "An Event occurred.";
    }

    public String getMessage() {
        return message;
    }

}
