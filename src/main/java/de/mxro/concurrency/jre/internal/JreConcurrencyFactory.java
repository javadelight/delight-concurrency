package de.mxro.concurrency.jre.internal;

import delight.factories.Configuration;
import delight.factories.Dependencies;
import delight.factories.Factory;

import de.mxro.concurrency.Concurrency;
import de.mxro.concurrency.configuration.ConcurrencyConfiguration;
import de.mxro.concurrency.jre.JreConcurrency;

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
