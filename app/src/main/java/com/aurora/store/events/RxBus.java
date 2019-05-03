package com.aurora.store.events;

import com.jakewharton.rxrelay2.PublishRelay;
import com.jakewharton.rxrelay2.Relay;

import io.reactivex.Observable;

public class RxBus {

    public static volatile RxBus instance;
    private final Relay<Object> bus = PublishRelay.create().toSerialized();

    public RxBus() {
        if (instance != null) {
            throw new RuntimeException("Use get() method to get the single instance of RxBus");
        }
    }

    public static RxBus get() {
        if (instance == null) {
            synchronized (RxBus.class) {
                if (instance == null) instance = new RxBus();
            }
        }
        return instance;
    }

    public static void publish(Object event) {
        RxBus auroraBus = RxBus.get();
        auroraBus.bus.accept(event);
    }

    public Observable<Object> toObservable() {
        return bus;
    }

    public boolean hasObservers() {
        return bus.hasObservers();
    }
}