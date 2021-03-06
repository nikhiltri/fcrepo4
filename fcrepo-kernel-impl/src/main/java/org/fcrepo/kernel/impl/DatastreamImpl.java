/**
 * Copyright 2014 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.kernel.impl;

import static org.fcrepo.kernel.impl.utils.FedoraTypesUtils.isFedoraDatastream;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.slf4j.LoggerFactory.getLogger;

import org.fcrepo.kernel.FedoraBinary;
import org.fcrepo.kernel.exception.RepositoryRuntimeException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.fcrepo.kernel.Datastream;
import org.slf4j.Logger;


/**
 * Abstraction for a Fedora datastream backed by a JCR node.
 *
 * @author ajs6f
 * @since Feb 21, 2013
 */
public class DatastreamImpl extends FedoraResourceImpl implements Datastream {

    private static final Logger LOGGER = getLogger(DatastreamImpl.class);

    /**
     * The JCR node for this datastream
     *
     * @param n an existing {@link Node}
     */
    public DatastreamImpl(final Node n) {
        super(n);
    }

    @Override
    public FedoraBinary getBinary() {
        return new FedoraBinaryImpl(getContentNode());
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.kernel.Datastream#getContent()
     */
    @Override
    public Node getContentNode() {
        LOGGER.trace("Retrieved datastream content node.");
        try {
            return node.getNode(JCR_CONTENT);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.kernel.Datastream#getContent()
     */
    @Override
    public boolean hasContent() {
        try {
            return node.hasNode(JCR_CONTENT);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    /**
     * Check if the node has a fedora:datastream mixin
     *
     * @param node node to check
     */
    public static boolean hasMixin(final Node node) {
        return isFedoraDatastream.apply(node);
    }

}
