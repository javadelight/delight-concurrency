package de.mxro.concurrency.jre.internal;

import de.mxro.concurrency.Concurrency;
import de.mxro.concurrency.configuration.ConcurrencyConfiguration;
import de.mxro.concurrency.jre.JreConcurrency;
import de.mxro.factories.Configuration;
import de.mxro.factories.Dependencies;
import de.mxro.factories.Factory;

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
