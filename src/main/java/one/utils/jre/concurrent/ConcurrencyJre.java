package one.utils.jre.concurrent;

import one.utils.jre.concurrent.internal.JreConcurrencyFactory;
import de.mxro.concurrency.Concurrency;
import de.mxro.factories.Factory;

public class ConcurrencyJre {

    public static Concurrency create() {
        return new JreConcurrency();
    }

    public static Factory<?, ?, ?> createFactory() {
        return new JreConcurrencyFactory();
    }

}
