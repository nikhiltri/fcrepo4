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
package org.fcrepo.transform.transformations;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.rdf.model.Model;

import org.apache.commons.io.IOUtils;
import org.fcrepo.kernel.utils.iterators.RdfStream;
import org.fcrepo.transform.Transformation;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * SPARQL Query-based transforms
 *
 * @author cbeer
 */
public class SparqlQueryTransform implements Transformation<QueryExecution> {

    private final InputStream query;

    /**
     * Construct a new SparqlQueryTransform from the data from
     * the InputStream
     * @param query
     */
    public SparqlQueryTransform(final InputStream query) {
        this.query = query;
    }

    @Override
    public QueryExecution apply(final RdfStream rdfStream) {

        try {
            final Model model = rdfStream.asModel();
            final Query sparqlQuery =
                QueryFactory.create(IOUtils.toString(query));

            return QueryExecutionFactory.create(sparqlQuery, model);
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public InputStream getQuery() {
        return query;
    }

    @Override
    public boolean equals(final Object other) {
        return other instanceof SparqlQueryTransform &&
                   query.equals(((SparqlQueryTransform)other).getQuery());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getQuery());
    }

    @Override
    public SparqlQueryTransform newTransform(final InputStream query) {
        return new SparqlQueryTransform(query);
    }

}
