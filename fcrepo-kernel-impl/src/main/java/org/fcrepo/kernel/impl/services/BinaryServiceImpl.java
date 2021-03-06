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
package org.fcrepo.kernel.impl.services;

import org.fcrepo.kernel.FedoraBinary;
import org.fcrepo.kernel.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.exception.ResourceTypeException;
import org.fcrepo.kernel.impl.FedoraBinaryImpl;
import org.fcrepo.kernel.services.BinaryService;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import static org.fcrepo.jcr.FedoraJcrTypes.FEDORA_BINARY;
import static org.fcrepo.jcr.FedoraJcrTypes.FEDORA_DATASTREAM;
import static org.fcrepo.jcr.FedoraJcrTypes.FEDORA_RESOURCE;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.modeshape.jcr.api.JcrConstants.NT_FILE;
import static org.modeshape.jcr.api.JcrConstants.NT_RESOURCE;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author cabeer
 * @since 10/10/14
 */
@Component
public class BinaryServiceImpl extends AbstractService implements BinaryService {

    private static final Logger LOGGER = getLogger(BinaryServiceImpl.class);

    /**
     * Retrieve a Datastream instance by pid and dsid
     *
     * @param path jcr path to the datastream
     * @return datastream
     * @throws javax.jcr.RepositoryException
     */
    @Override
    public FedoraBinary findOrCreateBinary(final Session session, final String path) {
        try {
            final Node dsNode = findOrCreateNode(session, path, NT_FILE);

            if (dsNode.isNew()) {
                initializeNewDatastreamProperties(dsNode);
            }

            return asBinary(dsNode.getNode(JCR_CONTENT));
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    private void initializeNewDatastreamProperties(final Node node) {
        try {

            if (node.canAddMixin(FEDORA_RESOURCE)) {
                node.addMixin(FEDORA_RESOURCE);
            }

            if (node.canAddMixin(FEDORA_DATASTREAM)) {
                node.addMixin(FEDORA_DATASTREAM);
            }

            final Node contentNode = jcrTools.findOrCreateChild(node, JCR_CONTENT, NT_RESOURCE);

            if (contentNode.canAddMixin(FEDORA_BINARY)) {
                contentNode.addMixin(FEDORA_BINARY);
            }
        } catch (final RepositoryException e) {
            LOGGER.warn("Could not decorate {} with datastream properties: {}", node, e);
        }

    }
    /**
     * Retrieve a Datastream instance by pid and dsid
     *
     * @param node datastream node
     * @return node as datastream
     */
    @Override
    public FedoraBinary asBinary(final Node node) {
        assertIsType(node);
        return new FedoraBinaryImpl(node);
    }

    private void assertIsType(final Node node) {
        if (!FedoraBinaryImpl.hasMixin(node)) {
            throw new ResourceTypeException(node + " can not be used as a binary");
        }
    }


}
