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

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import static org.fcrepo.jcr.FedoraJcrTypes.FEDORA_TOMBSTONE;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * @author cabeer
 */
public class TombstoneImplTest {
    @Mock
    private Node mockNode;

    @Mock
    private Node mockContainer;

    private TombstoneImpl testObj;

    @Before
    public void setUp() {
        initMocks(this);
        testObj = new TombstoneImpl(mockNode);
    }

    @Test
    public void testHasMixin() throws RepositoryException {
        when(mockNode.isNodeType(FEDORA_TOMBSTONE)).thenReturn(true);
        assertTrue(TombstoneImpl.hasMixin(mockNode));
    }

    @Test
    public void testHasMixinForOtherTypes() {
        assertFalse(TombstoneImpl.hasMixin(mockContainer));
    }

    @Test
    public void testDelete() throws RepositoryException {
        testObj.delete();
        verify(mockNode).remove();
    }

}