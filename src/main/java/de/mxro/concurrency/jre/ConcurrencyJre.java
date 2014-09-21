package de.mxro.concurrency.jre;

import de.mxro.concurrency.Concurrency;
import de.mxro.concurrency.jre.internal.JreConcurrencyFactory;
import de.mxro.factories.Factory;

public class ConcurrencyJre {

    public static Concurrency create() {
        return new JreConcurrency();
    }

    public static Factory<?, ?, ?> createFactory() {
        return new JreConcurrencyFactory();
    }

}
