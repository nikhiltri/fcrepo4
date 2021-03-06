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
package org.fcrepo.kernel.impl.identifiers;

import com.google.common.base.Converter;
import com.hp.hpl.jena.rdf.model.Resource;
import org.fcrepo.kernel.FedoraResource;
import org.fcrepo.kernel.impl.DatastreamImpl;
import org.fcrepo.kernel.impl.FedoraBinaryImpl;
import org.fcrepo.kernel.impl.FedoraObjectImpl;
import org.fcrepo.kernel.impl.TombstoneImpl;

import javax.jcr.Node;

/**
 * @author cabeer
 * @since 10/15/14
 */
public class NodeResourceConverter extends Converter<Node, FedoraResource> {
    public static final NodeResourceConverter nodeConverter = new NodeResourceConverter();

    /**
     * Get a converter that can transform a Node to a Resource
     * @param c
     * @return
     */
    public static Converter<Node, Resource> nodeToResource(final Converter<Resource, FedoraResource> c) {
        return nodeConverter.andThen(c.reverse());
    }

    @Override
    protected FedoraResource doForward(final Node node) {

        final FedoraResource fedoraResource;

        if (DatastreamImpl.hasMixin(node)) {
            fedoraResource = new DatastreamImpl(node);
        } else if (FedoraBinaryImpl.hasMixin(node)) {
            fedoraResource = new FedoraBinaryImpl(node);
        } else if (TombstoneImpl.hasMixin(node)) {
            fedoraResource = new TombstoneImpl(node);
        } else {
            fedoraResource = new FedoraObjectImpl(node);
        }

        return fedoraResource;
    }

    @Override
    protected Node doBackward(final FedoraResource resource) {
        return resource.getNode();
    }
}
