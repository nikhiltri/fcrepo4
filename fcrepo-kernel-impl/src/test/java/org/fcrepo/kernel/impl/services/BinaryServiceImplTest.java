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

import org.fcrepo.kernel.exception.ResourceTypeException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import static org.fcrepo.jcr.FedoraJcrTypes.FEDORA_BINARY;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;

/**
 * @author cabeer
 */
public class BinaryServiceImplTest {

    private BinaryServiceImpl testObj;

    @Mock
    private Session mockSession;

    @Mock
    private Node mockDsNode;

    @Mock
    private Node mockNode;

    @Mock
    private Node mockRoot;

    @Before
    public void setUp() throws RepositoryException {
        initMocks(this);
        testObj = new BinaryServiceImpl();
        when(mockSession.getRootNode()).thenReturn(mockRoot);
        when(mockDsNode.getNode(JCR_CONTENT)).thenReturn(mockNode);
        when(mockDsNode.getParent()).thenReturn(mockRoot);
        when(mockRoot.isNew()).thenReturn(false);
    }

    @Test
    public void testFindOrCreateBinary() throws Exception {
        final String testPath = "/foo/bar";
        when(mockRoot.getNode(testPath.substring(1))).thenReturn(mockDsNode);
        when(mockNode.isNodeType(FEDORA_BINARY)).thenReturn(true);
        testObj.findOrCreateBinary(mockSession, testPath);
        verify(mockRoot).getNode(testPath.substring(1));
    }

    @Test
    public void testAsBinary() throws Exception {
        when(mockNode.isNodeType(FEDORA_BINARY)).thenReturn(true);
        testObj.asBinary(mockNode);
    }

    @Test(expected = ResourceTypeException.class)
    public void testAsBinaryWithNonbinary() throws Exception {
        when(mockNode.isNodeType(FEDORA_BINARY)).thenReturn(false);
        testObj.asBinary(mockNode);
    }

}