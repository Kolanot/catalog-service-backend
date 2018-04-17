package org.apache.marmotta.worker.test;

import org.apache.marmotta.commons.sesame.filter.SesameFilter;
import org.apache.marmotta.worker.model.WorkerConfiguration;
import org.openrdf.model.Resource;

import java.util.Set;

/**
 * Mock worker configuration doing nothing
 *
 * @author Sebastian Schaffert (sschaffert@apache.org)
 */
public class MockWorkerConfiguration extends WorkerConfiguration {

    public MockWorkerConfiguration(String name) {
        super(name);
    }

    public MockWorkerConfiguration(String name, Set<SesameFilter<Resource>> filters) {
        super(name, filters);
    }

    @Override
    public String getType() {
        return "Mock";
    }
}
