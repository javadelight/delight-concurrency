package delight.concurrency.jre.internal;

import delight.concurrency.Concurrency;
import delight.concurrency.configuration.ConcurrencyConfiguration;
import delight.concurrency.jre.JreConcurrency;
import delight.factories.Configuration;
import delight.factories.Dependencies;
import delight.factories.Factory;

public class JreConcurrencyFactory implements Factory<Concurrency, ConcurrencyConfiguration, Dependencies> {

    @Override
    public boolean canInstantiate(final Configuration conf) {
        return conf instanceof ConcurrencyConfiguration;
    }

    @Override
    public Concurrency create(final ConcurrencyConfiguration conf, final Dependencies dependencies) {
        return new JreConcurrency();
    }

}
