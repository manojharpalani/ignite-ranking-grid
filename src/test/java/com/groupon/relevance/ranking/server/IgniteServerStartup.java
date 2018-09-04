package com.groupon.relevance.ranking.server;

import org.apache.ignite.IgniteException;
import org.apache.ignite.Ignition;

public class IgniteServerStartup {
        /**
         * Start up an empty node with example compute configuration.
         *
         * @param args Command line arguments, none required.
         * @throws IgniteException If failed.
         */
        public static void main(String[] args) throws IgniteException {
            Ignition.start("examples/config/example-ignite.xml");
        }
}
