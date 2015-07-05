package de.mxro.concurrency.jre;

import delight.factories.Factory;

import de.mxro.concurrency.Concurrency;
import de.mxro.concurrency.jre.internal.JreConcurrencyFactory;

public class ConcurrencyJre {

    public static Concurrency create() {
        return new JreConcurrency();
    }

    public static Factory<?, ?, ?> createFactory() {
        return new JreConcurrencyFactory();
    }

}
