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

import com.google.common.base.Converter;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import org.fcrepo.kernel.Datastream;
import org.fcrepo.kernel.FedoraBinary;
import org.fcrepo.kernel.FedoraResource;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import static com.hp.hpl.jena.rdf.model.ResourceFactory.createProperty;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static javax.jcr.PropertyType.REFERENCE;
import static javax.jcr.PropertyType.URI;
import static org.fcrepo.jcr.FedoraJcrTypes.FEDORA_BINARY;
import static org.fcrepo.jcr.FedoraJcrTypes.LDP_IS_MEMBER_OF_RELATION;
import static org.fcrepo.jcr.FedoraJcrTypes.LDP_MEMBER_RESOURCE;
import static org.fcrepo.kernel.impl.identifiers.NodeResourceConverter.nodeToResource;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * @author cabeer
 */
public class LdpIsMemberOfRdfContextTest {

    private LdpIsMemberOfRdfContext testObj;

    @Mock
    private FedoraBinary mockBinary;

    @Mock
    private Datastream mockBinaryDescription;

    @Mock
    private Node mockBinaryNode;

    @Mock
    private FedoraResource mockResource;

    @Mock
    private Node mockResourceNode;

    @Mock
    private FedoraResource mockContainer;

    @Mock
    private Node mockContainerNode;

    @Mock
    private Node mockNode;

    @Mock
    private Property mockRelationProperty;

    @Mock
    private Property mockMembershipProperty;

    @Mock
    private Session mockSession;

    private DefaultIdentifierTranslator subjects;

    @Before
    public void setUp() throws RepositoryException {
        initMocks(this);
        when(mockResource.getPath()).thenReturn("/a");
        when(mockResource.getContainer()).thenReturn(mockContainer);
        when(mockResource.getNode()).thenReturn(mockResourceNode);
        when(mockResourceNode.getDepth()).thenReturn(1);

        when(mockContainer.getPath()).thenReturn("/");
        when(mockContainer.getNode()).thenReturn(mockContainerNode);
        when(mockContainerNode.getDepth()).thenReturn(0);

        when(mockNode.getPath()).thenReturn("/some/path");

        when(mockBinary.hasType(FEDORA_BINARY)).thenReturn(true);
        when(mockBinary.getPath()).thenReturn("/a/jcr:content");
        when(mockBinary.getNode()).thenReturn(mockBinaryNode);
        when(mockBinary.getContainer()).thenReturn(mockContainer);

        subjects = new DefaultIdentifierTranslator(mockSession);
    }

    @Test
    public void testIsMemberOfRelationWithRootResource() throws RepositoryException {
        testObj = new LdpIsMemberOfRdfContext(mockContainer, subjects);

        final Model model = testObj.asModel();

        assertTrue("Expected stream to be empty", model.isEmpty());
    }

    @Test
    public void testIsMemberOfRelationWithoutIsMemberOfResource() throws RepositoryException {
        testObj = new LdpIsMemberOfRdfContext(mockResource, subjects);

        final Model model = testObj.asModel();

        assertTrue("Expected stream to be empty", model.isEmpty());
    }

    @Test
    public void testIsMemberOfRelation() throws RepositoryException {
        when(mockContainer.hasProperty(LDP_IS_MEMBER_OF_RELATION)).thenReturn(true);
        when(mockContainer.getProperty(LDP_IS_MEMBER_OF_RELATION)).thenReturn(mockRelationProperty);
        when(mockContainer.hasProperty(LDP_MEMBER_RESOURCE)).thenReturn(true);
        when(mockContainer.getProperty(LDP_MEMBER_RESOURCE)).thenReturn(mockMembershipProperty);
        when(mockMembershipProperty.getType()).thenReturn(REFERENCE);
        when(mockMembershipProperty.getNode()).thenReturn(mockNode);

        final String property = "some:uri";
        when(mockRelationProperty.getString()).thenReturn(property);
        testObj = new LdpIsMemberOfRdfContext(mockResource, subjects);

        final Model model = testObj.asModel();

        assertTrue("Expected stream to contain triple",
                model.contains(subjects.reverse().convert(mockResource),
                        createProperty(property),
                        nodeToResource(subjects).convert(mockNode)));
    }


    @Test
    public void testIsMemberOfRelationToExternalResource() throws RepositoryException {
        when(mockContainer.hasProperty(LDP_IS_MEMBER_OF_RELATION)).thenReturn(true);
        when(mockContainer.getProperty(LDP_IS_MEMBER_OF_RELATION)).thenReturn(mockRelationProperty);
        when(mockContainer.hasProperty(LDP_MEMBER_RESOURCE)).thenReturn(true);
        when(mockContainer.getProperty(LDP_MEMBER_RESOURCE)).thenReturn(mockMembershipProperty);
        when(mockMembershipProperty.getType()).thenReturn(URI);
        when(mockMembershipProperty.getString()).thenReturn("some:resource");

        final String property = "some:uri";
        when(mockRelationProperty.getString()).thenReturn(property);
        testObj = new LdpIsMemberOfRdfContext(mockResource, subjects);

        final Model model = testObj.asModel();

        final Converter<FedoraResource, Resource> nodeSubjects = subjects.reverse();

        assertTrue("Expected stream to contain triple",
                model.contains(nodeSubjects.convert(mockResource),
                        createProperty(property),
                        createResource("some:resource")));
    }

    @Test
    public void testIsMemberOfRelationForBinary() throws RepositoryException {
        when(mockContainer.hasProperty(LDP_IS_MEMBER_OF_RELATION)).thenReturn(true);
        when(mockContainer.getProperty(LDP_IS_MEMBER_OF_RELATION)).thenReturn(mockRelationProperty);
        when(mockContainer.hasProperty(LDP_MEMBER_RESOURCE)).thenReturn(true);
        when(mockContainer.getProperty(LDP_MEMBER_RESOURCE)).thenReturn(mockMembershipProperty);
        when(mockMembershipProperty.getType()).thenReturn(REFERENCE);
        when(mockMembershipProperty.getNode()).thenReturn(mockNode);

        final String property = "some:uri";

        when(mockRelationProperty.getString()).thenReturn(property);
        testObj = new LdpIsMemberOfRdfContext(mockBinary, subjects);

        final Model model = testObj.asModel();

        final Converter<FedoraResource, Resource> nodeSubjects = subjects.reverse();

        assertTrue("Expected stream to contain triple",
                model.contains(nodeSubjects.convert(mockBinary),
                        createProperty(property),
                        nodeToResource(subjects).convert(mockNode)));
    }
}