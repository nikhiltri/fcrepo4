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
package org.fcrepo.integration.rdf;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.BasicHttpEntity;
import org.fcrepo.integration.http.api.AbstractResourceIT;

import javax.ws.rs.core.Response;
import java.io.IOException;

import static javax.ws.rs.core.Response.Status.CREATED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author cabeer
 */
public class AbstractIntegrationRdfIT extends AbstractResourceIT {

    protected HttpResponse createLDPRSAndCheckResponse(final String pid, final String body) {
        try {
            final HttpPut httpPut = new HttpPut(serverAddress + pid);
            httpPut.addHeader("Slug", pid);
            httpPut.addHeader("Content-Type", "text/turtle");
            final BasicHttpEntity e = new BasicHttpEntity();
            e.setContent(IOUtils.toInputStream(body));
            httpPut.setEntity(e);
            final HttpResponse response = client.execute(httpPut);
            checkResponse(response, CREATED);
            return response;
        } catch (final IOException e) {
            assertTrue("Got IOException " + e, false);
            return null;
        }
    }

    protected void checkResponse(final HttpResponse response, final Response.StatusType expected) {
        final int actual = response.getStatusLine().getStatusCode();
        assertEquals("Didn't get a CREATED response!", expected.getStatusCode(), actual);
    }


}
