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
package org.fcrepo.transform.sparql;

import com.google.common.base.Function;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.engine.binding.Binding;
import org.fcrepo.kernel.FedoraResource;
import org.fcrepo.kernel.identifiers.IdentifierConverter;
import org.fcrepo.kernel.impl.rdf.converters.ValueConverter;
import org.slf4j.Logger;

import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import java.util.Iterator;
import java.util.List;

import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Iterators.transform;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Translate a JCR QueryResult to a SPARQL ResultSet
 *
 * @author cabeer
 */
public class JQLResultSet implements ResultSet {

    private static final Logger LOGGER = getLogger(JQLResultSet.class);

    private final RowIterator iterator;

    private final Session session;

    private final IdentifierConverter<Resource, FedoraResource> idTranslator;

    private final QueryResult queryResult;

    private int rowNumber = 0;

    /**
     * Translate a JCR QueryResult to a SPARQL ResultSet, respecting any
     * IdentifierTranslator translation for JCR Paths
     * @param idTranslator
     * @param queryResult
     * @throws RepositoryException
     */
    public JQLResultSet(final Session session, final IdentifierConverter<Resource,FedoraResource> idTranslator,
        final QueryResult queryResult) throws RepositoryException {
        this.session = session;
        this.idTranslator = idTranslator;

        this.queryResult = queryResult;
        this.iterator = queryResult.getRows();
    }

    /**
     * Get the raw JCR query result
     * @return raw JCR query result
     */
    public QueryResult getQueryResult() {
        return this.queryResult;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }

    @Override
    public QuerySolution next() {
        rowNumber++;
        final JQLQuerySolution jqlQuerySolution = new JQLQuerySolution(idTranslator,
                iterator.nextRow(),
                getResultVars());
        LOGGER.trace("Getting QuerySolution (#{}): {}", rowNumber, jqlQuerySolution);

        return jqlQuerySolution;
    }

    @Override
    public QuerySolution nextSolution() {
        return next();
    }

    @Override
    public Binding nextBinding() {
        return (Binding)next();
    }

    @Override
    public int getRowNumber() {
        return rowNumber;
    }

    @Override
    public List<String> getResultVars() {
        try {
            return copyOf(queryResult.getColumnNames());
        } catch (final RepositoryException e) {
            throw propagate(e);
        }
    }

    @Override
    public Model getResourceModel() {
        return null;
    }

    /**
     * Maps a JCR Query's Row to a QuerySolution
     */
    private class JQLQuerySolution implements QuerySolution, Binding {
        private final Row row;
        private final List<String> columns;
        private final ValueConverter valueConverter;


        /**
         * Create a new query solution to translate a JCR Row to a SPARQL Binding
         * @param idTranslator
         * @param row
         * @param columns
         */
        public JQLQuerySolution(final IdentifierConverter<Resource, FedoraResource> idTranslator,
                                final Row row,
                                final List<String> columns) {
            this.row = row;
            this.columns = columns;
            valueConverter = new ValueConverter(session, idTranslator);
        }

        @Override
        public RDFNode get(final String varName) {
            try {
                return valueConverter.convert(row.getValue(varName));
            } catch (final RepositoryException e) {
                throw propagate(e);
            }
        }

        @Override
        public Resource getResource(final String varName) {
            return get(varName).asResource();
        }

        @Override
        public Literal getLiteral(final String varName) {
            return get(varName).asLiteral();
        }

        @Override
        public boolean contains(final String varName) {
            try {
                final Value value = row.getValue(varName);
                return value != null;
            } catch (final ItemNotFoundException e) {
                LOGGER.trace("Unable to find var {} in result set", varName, e);
                return false;
            } catch (final RepositoryException e) {
                throw propagate(e);
            }
        }

        @Override
        public Iterator<String> varNames() {
            return columns.iterator();
        }

        @Override
        public Iterator<Var> vars() {
            return transform(columns.iterator(), new Function<String, Var>() {

                @Override
                public Var apply(final String s) {
                    return Var.alloc(s);
                }
            });
        }

        @Override
        public boolean contains(final Var var) {
            return contains(var.getName());
        }

        @Override
        public Node get(final Var var) {
            return get(var.getName()).asNode();
        }

        @Override
        public int size() {
            return columns.size();
        }

        @Override
        public boolean isEmpty() {
            return columns.isEmpty();
        }
    }
}
