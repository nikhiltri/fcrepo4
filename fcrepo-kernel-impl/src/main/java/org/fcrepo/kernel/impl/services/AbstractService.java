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

import org.fcrepo.kernel.exception.TombstoneException;
import org.fcrepo.kernel.impl.TombstoneImpl;
import org.fcrepo.kernel.services.Service;
import org.modeshape.jcr.api.JcrTools;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import static org.fcrepo.jcr.FedoraJcrTypes.FEDORA_PAIRTREE;
import static org.fcrepo.kernel.impl.utils.FedoraTypesUtils.getClosestExistingAncestor;
import static org.modeshape.jcr.api.JcrConstants.NT_FOLDER;


/**
 * @author bbpennel
 * @since Feb 20, 2014
 */
public abstract class AbstractService implements Service {
    protected final static JcrTools jcrTools = new JcrTools();

    protected Node findOrCreateNode(final Session session,
                                    final String path,
                                    final String finalNodeType) throws RepositoryException {

        final Node preexistingNode = getClosestExistingAncestor(session, path);

        if (TombstoneImpl.hasMixin(preexistingNode)) {
            throw new TombstoneException(new TombstoneImpl(preexistingNode));
        }

        final Node node = jcrTools.findOrCreateNode(session, path, NT_FOLDER, finalNodeType);

        if (node.isNew()) {
            tagHierarchyWithPairtreeMixin(preexistingNode, node);
        }

        return node;
    }

    private void tagHierarchyWithPairtreeMixin(final Node baseNode,
                                               final Node createdNode) throws RepositoryException {
        Node parent = createdNode.getParent();

        while (parent.isNew() && !parent.equals(baseNode)) {
            parent.addMixin(FEDORA_PAIRTREE);
            parent = parent.getParent();
        }
    }

}
