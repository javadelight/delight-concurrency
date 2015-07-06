package delight.concurrency.jre;

import delight.concurrency.Concurrency;
import delight.concurrency.jre.internal.JreConcurrencyFactory;
import delight.factories.Factory;

public class ConcurrencyJre {

    public static Concurrency create() {
        return new JreConcurrency();
    }

    public static Factory<?, ?, ?> createFactory() {
        return new JreConcurrencyFactory();
    }

}
