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
package org.fcrepo.kernel.impl.rdf.impl;

import static com.hp.hpl.jena.graph.Triple.create;
import static com.hp.hpl.jena.vocabulary.RDF.type;
import static org.fcrepo.jcr.FedoraJcrTypes.LDP_HAS_MEMBER_RELATION;
import static org.fcrepo.jcr.FedoraJcrTypes.LDP_MEMBER_RESOURCE;
import static org.fcrepo.kernel.RdfLexicon.CONTAINER;
import static org.fcrepo.kernel.RdfLexicon.DIRECT_CONTAINER;
import static org.fcrepo.kernel.RdfLexicon.HAS_MEMBER_RELATION;
import static org.fcrepo.kernel.RdfLexicon.LDP_MEMBER;
import static org.fcrepo.kernel.RdfLexicon.MEMBERSHIP_RESOURCE;
import static org.fcrepo.kernel.RdfLexicon.RDF_SOURCE;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.fcrepo.kernel.FedoraObject;
import org.fcrepo.kernel.FedoraResource;
import org.fcrepo.kernel.identifiers.IdentifierConverter;
import org.slf4j.Logger;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * @author cabeer
 * @since 9/16/14
 */
public class LdpRdfContext extends NodeRdfContext {

    private static final Logger LOGGER = getLogger(LdpRdfContext.class);

    /**
     * Default constructor.
     *
     * @param resource
     * @param idTranslator
     */
    public LdpRdfContext(final FedoraResource resource,
                         final IdentifierConverter<Resource, FedoraResource> idTranslator) {
        super(resource, idTranslator);

        concat(typeContext());

        if (isContainer()) {
            concat(membershipRelationContext());
        }

    }

    private boolean isContainer() {
        return resource() instanceof FedoraObject;
    }

    private Collection<Triple> membershipRelationContext() {

        final Set<Triple> triples1 = new HashSet<>();

        if (!resource().hasProperty(LDP_HAS_MEMBER_RELATION)) {
            triples1.add(create(subject(), HAS_MEMBER_RELATION.asNode(), LDP_MEMBER.asNode()));
        }

        if (!resource().hasProperty(LDP_MEMBER_RESOURCE)) {
            triples1.add(create(subject(), MEMBERSHIP_RESOURCE.asNode(), subject()));
        }

        return triples1;
    }

    private Triple[] typeContext() {
        final Triple rdfSource = create(subject(), type.asNode(), RDF_SOURCE.asNode());

        if (isContainer()) {
            return new Triple[]{
                    create(subject(), type.asNode(), CONTAINER.asNode()),
                    create(subject(), type.asNode(), DIRECT_CONTAINER.asNode()),
                    rdfSource
            };
        }
        return new Triple[] { rdfSource };
    }

}
