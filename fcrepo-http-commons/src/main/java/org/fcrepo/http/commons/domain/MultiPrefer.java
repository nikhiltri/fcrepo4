
package org.fcrepo.http.commons.domain;

import java.text.ParseException;
import java.util.Iterator;
import java.util.Set;

import javax.ws.rs.HeaderParam;

/**
 * Aggregate information from multiple Prefer HTTP headers.
 *
 * @author ajs6f
 */
public class MultiPrefer {

    Prefer kernel;

    public MultiPrefer(@HeaderParam("Prefer") Set<Prefer> prefers) throws ParseException {
        Iterator<Prefer> iprefers = prefers.iterator();
        kernel = new Prefer(iprefers.next());
        while (iprefers.hasNext()) {
            kernel = new Prefer(kernel, iprefers.next());
        }
    }

}
