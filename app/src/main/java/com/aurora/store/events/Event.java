package com.aurora.store.events;

public class Event {

    private Events event;

    public Event(Events event) {
        this.event = event;
    }

    public Events getEvent() {
        return event;
    }

    public void setEvent(Events event) {
        this.event = event;
    }
}
