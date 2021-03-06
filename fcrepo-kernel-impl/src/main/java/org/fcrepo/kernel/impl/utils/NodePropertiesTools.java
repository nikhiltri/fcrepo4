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
package org.fcrepo.kernel.impl.utils;

import static com.google.common.collect.Iterables.toArray;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;

import com.hp.hpl.jena.rdf.model.Resource;
import org.fcrepo.kernel.FedoraResource;
import org.fcrepo.kernel.exception.IdentifierConversionException;
import org.fcrepo.kernel.exception.NoSuchPropertyDefinitionException;
import org.fcrepo.kernel.identifiers.IdentifierConverter;
import org.fcrepo.kernel.services.functions.JcrPropertyFunctions;
import org.modeshape.jcr.IsExternal;
import org.slf4j.Logger;

import static javax.jcr.PropertyType.UNDEFINED;

/**
 * Tools for replacing, appending and deleting JCR node properties
 * @author Chris Beer
 * @since May 10, 2013
 */
public class NodePropertiesTools {

    private static final Logger LOGGER = getLogger(NodePropertiesTools.class);
    public static final String REFERENCE_PROPERTY_SUFFIX = "_ref";
    private static final IsExternal isExternal = new IsExternal();

    /**
     * Given a JCR node, property and value, either:
     *  - if the property is single-valued, replace the existing property with
     *    the new value
     *  - if the property is multivalued, append the new value to the property
     * @param node the JCR node
     * @param propertyName a name of a JCR property (either pre-existing or
     *   otherwise)
     * @param newValue the JCR value to insert
     * @throws RepositoryException
     */
    public Property appendOrReplaceNodeProperty(final Node node, final String propertyName, final Value newValue)
        throws RepositoryException {

        final Property property;

        // if it already exists, we can take some shortcuts
        if (node.hasProperty(propertyName)) {

            property = node.getProperty(propertyName);

            if (property.isMultiple()) {
                LOGGER.debug("Appending value {} to {} property {}", newValue,
                             PropertyType.nameFromValue(property.getType()),
                             propertyName);

                // if the property is multi-valued, go ahead and append to it.
                final List<Value> newValues = new ArrayList<>();
                Collections.addAll(newValues,
                                   node.getProperty(propertyName).getValues());

                if (!newValues.contains(newValue)) {
                    newValues.add(newValue);
                    property.setValue(toArray(newValues, Value.class));
                }
            } else {
                // or we'll just overwrite it
                LOGGER.debug("Overwriting {} property {} with new value {}", PropertyType.nameFromValue(property
                        .getType()), propertyName, newValue);
                property.setValue(newValue);
            }
        } else {
            boolean isMultiple = true;
            try {
                isMultiple = isMultivaluedProperty(node, propertyName);

            } catch (final NoSuchPropertyDefinitionException e) {
                // simply represents a new kind of property on this node
            }
            if (isMultiple) {
                LOGGER.debug("Creating new multivalued {} property {} with " +
                             "initial value [{}]",
                             PropertyType.nameFromValue(newValue.getType()),
                             propertyName, newValue);
                property = node.setProperty(propertyName, new Value[]{newValue}, newValue.getType());
            } else {
                LOGGER.debug("Creating new single-valued {} property {} with " +
                             "initial value {}",
                             PropertyType.nameFromValue(newValue.getType()),
                             propertyName, newValue);
                property = node.setProperty(propertyName, newValue, newValue.getType());
            }
        }

        return property;
    }

    /**
     * Add a reference placeholder from one node to another in-domain resource
     * @param idTranslator
     * @param node
     * @param property
     * @param resource
     * @throws RepositoryException
     */
    public void addReferencePlaceholders(final IdentifierConverter<Resource,FedoraResource> idTranslator,
                                          final Node node,
                                          final Property property,
                                          final Resource resource) throws RepositoryException {

        try {
            final Node refNode = idTranslator.convert(resource).getNode();

            if (isExternal.apply(refNode)) {
                // we can't apply REFERENCE properties to external resources
                return;
            }

            final String referencePropertyName = getReferencePropertyName(property);

            if (!property.isMultiple() && node.hasProperty(referencePropertyName)) {
                node.setProperty(referencePropertyName, (Value[])null);
            }

            final Value v = node.getSession().getValueFactory().createValue(refNode, true);
            appendOrReplaceNodeProperty(node, referencePropertyName, v);

        } catch (final IdentifierConversionException e) {
            // no-op
        }
    }

    /**
     * Remove a reference placeholder that links one node to another in-domain resource
     * @param idTranslator
     * @param node
     * @param property
     * @param resource
     * @throws RepositoryException
     */
    public void removeReferencePlaceholders(final IdentifierConverter<Resource,FedoraResource> idTranslator,
                                             final Node node,
                                             final Property property,
                                             final Resource resource) throws RepositoryException {

        if (property == null) {
            return;
        }

        final String referencePropertyName = getReferencePropertyName(property);

        if (!property.isMultiple() && node.hasProperty(referencePropertyName)) {
            node.setProperty(referencePropertyName, (Value[])null);
        } else {
            final Node refNode = idTranslator.convert(resource).getNode();
            final Value v = node.getSession().getValueFactory().createValue(refNode, true);
            removeNodeProperty(node, referencePropertyName, v);
        }
    }
    /**
     * Given a JCR node, property and value, remove the value (if it exists)
     * from the property, and remove the
     * property if no values remove
     *
     * @param node the JCR node
     * @param propertyName a name of a JCR property (either pre-existing or
     *   otherwise)
     * @param valueToRemove the JCR value to remove
     * @throws RepositoryException
     */
    public Property removeNodeProperty(final Node node, final String propertyName, final Value valueToRemove)
        throws RepositoryException {
        final Property property;

        // if the property doesn't exist, we don't need to worry about it.
        if (node.hasProperty(propertyName)) {

            property = node.getProperty(propertyName);

            if (JcrPropertyFunctions.isMultipleValuedProperty.apply(property)) {

                final List<Value> newValues = new ArrayList<>();

                boolean remove = false;

                for (final Value v : node.getProperty(propertyName).getValues()) {
                    if (v.equals(valueToRemove)) {
                        remove = true;
                    } else {
                        newValues.add(v);
                    }
                }

                // we only need to update the property if we did anything.
                if (remove) {
                    if (newValues.isEmpty()) {
                        LOGGER.debug("Removing property {}", propertyName);
                        property.setValue((Value[])null);
                    } else {
                        LOGGER.debug("Removing value {} from property {}",
                                     valueToRemove, propertyName);
                        property
                            .setValue(toArray(newValues, Value.class));
                    }
                }

            } else {
                if (property.getValue().equals(valueToRemove)) {
                    LOGGER.debug("Removing value from property {}", propertyName);
                    property.setValue((Value)null);
                }
            }
        } else {
            property = null;
        }

        return property;
    }

    /**
     * When we add certain URI properties, we also want to leave a reference node
     * @param propertyName
     * @return property name as a reference
     */
    public static String getReferencePropertyName(final String propertyName) {
        return propertyName + REFERENCE_PROPERTY_SUFFIX;
    }

    /**
     * Given an internal reference node property, get the original name
     * @param refPropertyName
     * @return original property name of the reference property
     */
    public static String getReferencePropertyOriginalName(final String refPropertyName) {
        final int i = refPropertyName.lastIndexOf(REFERENCE_PROPERTY_SUFFIX);

        if (i < 0) {
            return refPropertyName;
        }
        return refPropertyName.substring(0, i);
    }

    private static String getReferencePropertyName(final Property property) throws RepositoryException {
        return getReferencePropertyName(property.getName());
    }
    /**
     * Get the JCR property type ID for a given property name. If unsure, mark
     * it as UNDEFINED.
     *
     * @param node the JCR node to add the property on
     * @param propertyName the property name
     * @return a PropertyType value
     * @throws RepositoryException
     */
    public int getPropertyType(final Node node, final String propertyName)
        throws RepositoryException {
        LOGGER.debug("Getting type of property: {} from node: {}",
                propertyName, node);
        final PropertyDefinition def =
            getDefinitionForPropertyName(node, propertyName);

        if (def == null) {
            return UNDEFINED;
        }

        return def.getRequiredType();
    }

    /**
     * Determine if a given JCR property name is single- or multi- valued.
     * If unsure, choose the least restrictive
     * option (multivalued)
     *
     * @param node the JCR node to check
     * @param propertyName the property name
     *   (which may or may not already exist)
     * @return true if the property is (or could be) multivalued
     * @throws RepositoryException
     */
    private static boolean isMultivaluedProperty(final Node node,
                                                final String propertyName)
        throws RepositoryException {
        final PropertyDefinition def =
            getDefinitionForPropertyName(node, propertyName);

        if (def == null) {
            throw new NoSuchPropertyDefinitionException();
        }

        return def.isMultiple();
    }

    /**
     * Get the property definition information (containing type and multi-value
     * information)
     *
     * @param node the node to use for inferring the property definition
     * @param propertyName the property name to retrieve a definition for
     * @return a JCR PropertyDefinition, if available, or null
     * @throws javax.jcr.RepositoryException
     */
    private static PropertyDefinition getDefinitionForPropertyName(final Node node,
                                                                  final String propertyName)
            throws RepositoryException {

        final NodeType primaryNodeType = node.getPrimaryNodeType();
        final PropertyDefinition[] propertyDefinitions = primaryNodeType.getPropertyDefinitions();
        LOGGER.debug("Looking for property name: {}", propertyName);
        for (final PropertyDefinition p : propertyDefinitions) {
            LOGGER.debug("Checking property: {}", p.getName());
            if (p.getName().equals(propertyName)) {
                return p;
            }
        }

        for (final NodeType nodeType : node.getMixinNodeTypes()) {
            for (final PropertyDefinition p : nodeType.getPropertyDefinitions()) {
                if (p.getName().equals(propertyName)) {
                    return p;
                }
            }
        }
        return null;
    }


}
