/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk.handlers;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.extensions.DiscoveryExtensionNode;
import org.opensearch.extensions.ExtensionDependencyResponse;
import org.opensearch.extensions.ExtensionsManager;
import org.opensearch.sdk.ExtensionsRunner;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.transport.TransportException;
import org.opensearch.transport.TransportResponseHandler;

/**
 * This class handles the response from OpenSearch to a {@link ExtensionsRunner#sendExtensionDependencyRequest} call.
 */
public class ExtensionDependencyResponseHandler implements TransportResponseHandler<ExtensionDependencyResponse> {
    private static final Logger logger = LogManager.getLogger(ExtensionDependencyResponseHandler.class);
    private final CompletableFuture<ExtensionDependencyResponse> inProgressFuture;
    private List<DiscoveryExtensionNode> extensions;

    /**
     * Instantiates a new ExtensionDependencyHandler with ConpletableFuture
     */
    public ExtensionDependencyResponseHandler() {
        this.inProgressFuture = new CompletableFuture<>();
        this.extensions = extensions;
    }

    @Override
    public void handleResponse(ExtensionDependencyResponse response) {

        // Set cluster state from response
        this.extensions = response.getExtensionDependency();
        inProgressFuture.complete(response);
    }

    @Override
    public void handleException(TransportException exp) {
        logger.info("ExtensionDependencyResponseHandler failed", exp);
        inProgressFuture.completeExceptionally(exp);
    }

    @Override
    public String executor() {
        return ThreadPool.Names.GENERIC;
    }

    @Override
    public ExtensionDependencyResponse read(StreamInput in) throws IOException {
        return new ExtensionDependencyResponse(in);
    }

    /**
     * Invokes await on the ExtensionDependencyResponseHandler count down latch
     * @throws Exception
     *     if the response times out,
     *     if the response has been cancelled
     *     if the response failed
     */
    public void awaitResponse() throws Exception {
        inProgressFuture.orTimeout(ExtensionsManager.EXTENSION_REQUEST_WAIT_TIMEOUT, TimeUnit.SECONDS).get();
    }

    /**
     * Get the dependency information form the Response
     * @return dependency information
     */
    public List<DiscoveryExtensionNode> getExtensionDependencies() {
        return this.extensions;
    }
}
