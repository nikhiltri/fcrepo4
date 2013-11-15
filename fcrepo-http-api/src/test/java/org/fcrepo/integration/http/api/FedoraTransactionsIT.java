/**
 * Copyright 2013 DuraSpace, Inc.
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

package org.fcrepo.integration.http.api;

import static com.hp.hpl.jena.graph.Node.ANY;
import static com.hp.hpl.jena.graph.NodeFactory.createURI;
import static java.lang.Math.min;
import static java.lang.Thread.sleep;
import static java.util.UUID.randomUUID;
import static java.util.regex.Pattern.compile;
import static org.fcrepo.http.commons.test.util.TestHelpers.parseTriples;
import static org.fcrepo.kernel.Transaction.DEFAULT_TIMEOUT;
import static org.fcrepo.kernel.Transaction.TIMEOUT_SYSTEM_PROPERTY;
import static org.fcrepo.kernel.services.TransactionService.REAP_INTERVAL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.ws.rs.core.HttpHeaders;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.fcrepo.http.commons.domain.RDFMediaType;
import org.junit.Test;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.update.GraphStore;

public class FedoraTransactionsIT extends AbstractResourceIT {

    @Test
    public void testCreateTransaction() throws Exception {
        /* create a tx */
        final HttpPost createTx = new HttpPost(serverAddress + "fcr:tx");
        final HttpResponse response = execute(createTx);
        assertEquals(201, response.getStatusLine().getStatusCode());

        final String location = response.getFirstHeader("Location").getValue();

        logger.info("Got location {}", location);
        assertTrue(
                "Expected Location header to send us to root node path within the transaction",
                compile("tx:[0-9a-f-]+$").matcher(location).find());

    }

    @Test
    public void testRequestsInTransactionThatDoestExist() throws Exception {
        /* create a tx */
        final HttpPost createTx =
            new HttpPost(serverAddress + "tx:123/objects");
        final HttpResponse response = execute(createTx);
        assertEquals(410, response.getStatusLine().getStatusCode());

    }

    @Test
    public void testCreateAndTimeoutTransaction() throws Exception {

        /* create a short-lived tx */
        /** this is a pretty fragile test, as it is largely dependent on
         *  how long it takes to spin up the test resources. Too short,
         *  and the TX will expire before the first GET
         */
        System.setProperty(TIMEOUT_SYSTEM_PROPERTY, Long.toString(5 * REAP_INTERVAL));

        /* create a tx */
        final HttpPost createTx = new HttpPost(serverAddress + "fcr:tx");
        final HttpResponse response = execute(createTx);
        assertEquals(201, response.getStatusLine().getStatusCode());

        final String location = response.getFirstHeader("Location").getValue();

        final HttpGet getWithinTx = new HttpGet(location);
        HttpResponse resp = execute(getWithinTx);
        IOUtils.toString(resp.getEntity().getContent());
        assertEquals(location + " returned a status of " + resp.getStatusLine().getStatusCode(),
                200, resp.getStatusLine().getStatusCode());

        int statusCode = 0;

        sleep(REAP_INTERVAL * 6);
        final HttpGet getWithExpiredTx = new HttpGet(location);
        resp = execute(getWithExpiredTx);
        IOUtils.toString(resp.getEntity().getContent());
        statusCode = resp.getStatusLine().getStatusCode();

        try {
            assertEquals("Transaction did not expire", 410, statusCode);
        } finally {
            System.setProperty(TIMEOUT_SYSTEM_PROPERTY, Long
                    .toString(DEFAULT_TIMEOUT));
            System.clearProperty("fcrepo4.tx.timeout");
        }
    }

    @Test
    public void testCreateDoStuffAndRollbackTransaction() throws Exception {
        /* create a tx */
        final HttpPost createTx = new HttpPost(serverAddress + "fcr:tx");

        final HttpResponse response = execute(createTx);
        assertEquals(201, response.getStatusLine().getStatusCode());

        final String txLocation =
            response.getFirstHeader("Location").getValue();

        /* create a new object inside the tx */
        final HttpPost postNew =
            new HttpPost(txLocation + "/object-in-tx-rollback");
        HttpResponse resp = execute(postNew);
        assertEquals(201, resp.getStatusLine().getStatusCode());

        /* fetch the created tx from the endpoint */
        final HttpGet getTx =
            new HttpGet(txLocation + "/object-in-tx-rollback");
        getTx.setHeader(HttpHeaders.ACCEPT, RDFMediaType.N3_APPLICATION);
        resp = execute(getTx);
        assertEquals(
                "Expected to find our object within the scope of the transaction",
                200, resp.getStatusLine().getStatusCode());

        final GraphStore graphStore =
            parseTriples(resp.getEntity().getContent());
        logger.debug(graphStore.toString());

        assertTrue(graphStore.toDataset().asDatasetGraph().contains(Node.ANY,
                createURI(txLocation + "/object-in-tx-rollback"), ANY, ANY));

        /* fetch the created tx from the endpoint */
        final HttpGet getObj =
            new HttpGet(serverAddress + "/object-in-tx-rollback");
        resp = execute(getObj);
        assertEquals(
                "Expected to not find our object within the scope of the transaction",
                404, resp.getStatusLine().getStatusCode());

        /* and rollback */
        final HttpPost rollbackTx =
            new HttpPost(txLocation + "/fcr:tx/fcr:rollback");
        resp = execute(rollbackTx);

        assertEquals(204, resp.getStatusLine().getStatusCode());

    }

    @Test
    public void testCreateDoStuffAndCommitTransaction() throws Exception {
        /* create a tx */

        final String objectInTxCommit = randomUUID().toString();

        final HttpPost createTx = new HttpPost(serverAddress + "fcr:tx");

        final HttpResponse response = execute(createTx);
        assertEquals(201, response.getStatusLine().getStatusCode());

        final String txLocation =
            response.getFirstHeader("Location").getValue();

        /* create a new object inside the tx */
        final HttpPost postNew =
            new HttpPost(txLocation + "/" + objectInTxCommit);
        HttpResponse resp = execute(postNew);
        assertEquals(201, resp.getStatusLine().getStatusCode());

        /* fetch the created tx from the endpoint */
        final HttpGet getTx = new HttpGet(txLocation + "/" + objectInTxCommit);
        getTx.setHeader(HttpHeaders.ACCEPT, RDFMediaType.N3_APPLICATION);
        resp = execute(getTx);
        assertEquals(
                "Expected to find our object within the scope of the transaction",
                200, resp.getStatusLine().getStatusCode());

        final GraphStore graphStore =
            parseTriples(resp.getEntity().getContent());
        logger.debug(graphStore.toString());

        assertTrue(graphStore.toDataset().asDatasetGraph().contains(ANY,
                createURI(txLocation + "/" + objectInTxCommit), ANY, ANY));

        /* fetch the object-in-tx outside of the tx */
        final HttpGet getObj =
                new HttpGet(serverAddress + "/" + objectInTxCommit);
        resp = execute(getObj);
        assertEquals(
                "Expected to not find our object within the scope of the transaction",
                404, resp.getStatusLine().getStatusCode());

        /* and rollback */
        final HttpPost commitTx =
            new HttpPost(txLocation + "/fcr:tx/fcr:commit");
        resp = execute(commitTx);

        assertEquals(204, resp.getStatusLine().getStatusCode());

        /* fetch the object-in-tx outside of the tx after it has been committed */
        final HttpGet getObjCommitted =
            new HttpGet(serverAddress + "/" + objectInTxCommit);
        resp = execute(getObjCommitted);
        assertEquals(
                "Expected to  find our object after the transaction was committed",
                200, resp.getStatusLine().getStatusCode());

    }

}