
package org.fcrepo.services;

import static org.fcrepo.services.DatastreamService.getDatastreamContentInputStream;
import static org.jgroups.util.Util.assertEquals;
import static org.jgroups.util.Util.assertTrue;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.modeshape.jcr.api.JcrConstants.JCR_DATA;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.inject.Inject;
import javax.jcr.Repository;
import javax.jcr.Session;

import org.apache.tika.io.IOUtils;
import org.fcrepo.AbstractTest;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration({"/spring-test/repo.xml"})
public class DatastreamServiceTest extends AbstractTest {

    @Inject
    private Repository repository;

    @Inject
    private DatastreamService datastreamService;

    @Test
    public void testCreateDatastreamNode() throws Exception {
        Session session = repository.login();
        InputStream is = new ByteArrayInputStream("asdf".getBytes());

        datastreamService.createDatastreamNode(session, "testDatastreamNode",
                "application/octet-stream", is);
        session.save();
        session.logout();
        session = repository.login();

        assertTrue(session.getRootNode().hasNode("testDatastreamNode"));
        assertEquals("asdf", session.getNode("/testDatastreamNode").getNode(
                JCR_CONTENT).getProperty(JCR_DATA).getString());
        session.logout();
    }

    @Test
    public void testGetDatastreamContentInputStream() throws Exception {
        Session session = repository.login();
        InputStream is = new ByteArrayInputStream("asdf".getBytes());

        datastreamService.createDatastreamNode(session, "testDatastreamNode",
                "application/octet-stream", is);

        session.save();
        session.logout();
        session = repository.login();

        InputStream contentInputStream =
                getDatastreamContentInputStream(session, "/testDatastreamNode");

        assertEquals("asdf", IOUtils.toString(contentInputStream, "UTF-8"));
        session.logout();
    }
}
