/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.groupon.relevance.ranking.example;

import com.groupon.relevance.ranking.model.Deal;
import com.groupon.relevance.ranking.model.User;
import com.groupon.relevance.ranking.service.ScoringService;
import com.groupon.relevance.ranking.service.ScoringServiceImpl;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteServices;
import org.apache.ignite.Ignition;
import org.apache.ignite.lang.IgniteCallable;
import org.apache.ignite.resources.ServiceResource;

import java.util.Collection;
import java.util.UUID;

public class ServicesExample {
    /**
     * Executes example.
     *
     * @param args Command line arguments, none required.
     * @throws Exception If example execution failed.
     */
    public static void main(String[] args) throws Exception {
        // Mark this node as client node.
        Ignition.setClientMode(true);

        try (Ignite ignite = Ignition.start("examples/config/example-ignite.xml")) {

            // Deploy services only on server nodes.
            IgniteServices svcs = ignite.services(ignite.cluster().forServers());

            try {
                // Deploy cluster singleton.
                svcs.deployNodeSingleton("myClusterSingletonService", new ScoringServiceImpl());

                // Deploy node singleton.
                svcs.deployNodeSingleton("myNodeSingletonService", new ScoringServiceImpl());

                // Deploy 2 instances, regardless of number nodes.
                svcs.deployMultiple("myMultiService",
                    new ScoringServiceImpl(),
                    2 /*total number*/,
                    0 /*0 for unlimited*/);

                // Example for using a service proxy
                // to access a remotely deployed service.
                serviceProxyExample(ignite);

                // Example for auto-injecting service proxy
                // into remote closure execution.
                serviceInjectionExample(ignite);
            }
            finally {
                // Undeploy all services.
                ignite.services().cancelAll();
            }
        }
    }

    /**
     * Simple example to demonstrate service proxy invocation of a remotely deployed service.
     *
     * @param ignite Ignite instance.
     * @throws Exception If failed.
     */
    private static void serviceProxyExample(Ignite ignite) throws Exception {
        System.out.println(">>>");
        System.out.println(">>> Starting service proxy example.");
        System.out.println(">>>");

        // Get a sticky proxy for node-singleton map service.
        ScoringService service = ignite.services().serviceProxy("myNodeSingletonService",
                                                                ScoringService.class,
                                                                true);

        double score = service.score(new Deal(UUID.randomUUID(), 100), new User(9));

        System.out.println("Score " + score);
    }

    /**
     * Simple example to demonstrate how to inject service proxy into distributed closures.
     *
     * @param ignite Ignite instance.
     * @throws Exception If failed.
     */
    private static void serviceInjectionExample(Ignite ignite) throws Exception {
        System.out.println(">>>");
        System.out.println(">>> Starting service injection example.");
        System.out.println(">>>");

        // Get a sticky proxy for cluster-singleton map service.
        ScoringService service = ignite.services().serviceProxy("myClusterSingletonService",
            ScoringService.class,
            true);


        // Broadcast closure to every node.
        final Collection<Double> mapSizes = ignite.compute().broadcast(new SimpleClosure());

        System.out.println("Closure execution result: " + mapSizes);

    }

    /**
     * Simple closure to demonstrate auto-injection of the service proxy.
     */
    private static class SimpleClosure implements IgniteCallable<Double> {
        // Auto-inject service proxy.
        @ServiceResource(serviceName = "myClusterSingletonService", proxyInterface = ScoringService.class)
        private transient ScoringService mapSvc;

        /** {@inheritDoc} */
        @Override public Double call() throws Exception {
            double score = mapSvc.score(new Deal(UUID.randomUUID(), 100), new User(9));
            System.out.println("Score " + score);
            return score;
        }
    }
}