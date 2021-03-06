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
package org.fcrepo.http.api;

import static org.fcrepo.http.commons.test.util.TestHelpers.getUriInfoImpl;
import static org.fcrepo.http.commons.test.util.TestHelpers.mockSession;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.UriInfo;

import org.fcrepo.kernel.FedoraBinary;
import org.fcrepo.kernel.identifiers.IdentifierConverter;
import org.fcrepo.kernel.utils.iterators.RdfStream;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

/**
 * <p>FedoraFixityTest class.</p>
 *
 * @author awoods
 */
public class FedoraFixityTest {

    FedoraFixity testObj;

    private Session mockSession;

    private UriInfo uriInfo;

    @Mock
    private Request mockRequest;

    @Mock
    private Node mockNode;

    @Mock
    private FedoraBinary mockBinary;

    @Before
    public void setUp() {
        initMocks(this);
        testObj = spy(new FedoraFixity());
        this.uriInfo = getUriInfoImpl();
        setField(testObj, "uriInfo", uriInfo);
        mockSession = mockSession(testObj);
        setField(testObj, "session", mockSession);
    }

    @Test
    public void testGetDatastreamFixity() throws RepositoryException {
        final RdfStream expected = new RdfStream();
        final String externalPath = "objects/FedoraDatastreamsTest1/testDS";

        when(mockNode.getSession()).thenReturn(mockSession);

        doReturn(mockBinary).when(testObj).getResourceFromPath(externalPath);
        when(mockBinary.getFixity(any(IdentifierConverter.class))).thenReturn(expected);

        final RdfStream actual = testObj.getDatastreamFixity(externalPath, mockRequest, uriInfo);

        assertEquals(expected, actual);
    }
}
