/**
 * Licensed to Jasig under one or more contributor license
 * agreements. See the NOTICE file distributed with this work
 * for additional information regarding copyright ownership.
 * Jasig licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.jasig.services.persondir.support.jdbc;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sql.DataSource;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.jasig.services.persondir.IPersonAttributeDao;
import org.jasig.services.persondir.IPersonAttributes;
import org.jasig.services.persondir.support.AbstractQueryPersonAttributeDao;
import org.jasig.services.persondir.support.QueryType;
import org.jasig.services.persondir.util.CaseCanonicalizationMode;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

/**
 * Provides common logic for executing a JDBC based query including building the WHERE clause SQL string.
 * <br>
 * <br>
 * Configuration:
 * <table border="1">
 *     <tr>
 *         <th align="left">Property</th>
 *         <th align="left">Description</th>
 *         <th align="left">Required</th>
 *         <th align="left">Default</th>
 *     </tr>
 *     <tr>
 *         <td align="right" valign="top">queryType</td>
 *         <td>
 *             How multiple attributes in a query should be concatenated together. The other option is OR.
 *         </td>
 *         <td valign="top">No</td>
 *         <td valign="top">AND</td>
 *     </tr>
 * </table>
 * 
 * @author Eric Dalquist 
 * @version $Revision$
 */
public abstract class AbstractJdbcPersonAttributeDao<R> extends AbstractQueryPersonAttributeDao<PartialWhereClause> {
    private static final Pattern WHERE_PLACEHOLDER = Pattern.compile("\\{0\\}");
    private static final Map<CaseCanonicalizationMode,MessageFormat> DEFAULT_DATA_ATTRIBUTE_CASE_CANONICALIZATION_FUNCTIONS =
            Collections.unmodifiableMap(new LinkedHashMap<CaseCanonicalizationMode, MessageFormat>() {{
                put(CaseCanonicalizationMode.LOWER, new MessageFormat("lower({0})"));
                put(CaseCanonicalizationMode.UPPER, new MessageFormat("upper({0})"));
                put(CaseCanonicalizationMode.NONE, new MessageFormat("{0}"));
            }});
    
    private final SimpleJdbcTemplate simpleJdbcTemplate;
    private final String queryTemplate;
    private QueryType queryType = QueryType.AND;
    private Map<String,CaseCanonicalizationMode> caseInsensitiveDataAttributes;
    private Map<CaseCanonicalizationMode,MessageFormat> dataAttributeCaseCanonicalizationFunctions = DEFAULT_DATA_ATTRIBUTE_CASE_CANONICALIZATION_FUNCTIONS;
    private boolean wildcardDataAttributes = false; // false preserves historical behavior, true makes it work more like uPortal
    private Set<String> wildcardedDataAttributeExclusions; // null/empty set preserves historical behavior, true makes it work more like uPortal
    private boolean allowUsernameWildcards = true; // true preserves historical behavior, true makes it work more like uPortal
    private String usernameDataAttribute;

    /**
     * @param ds The DataSource to use for queries
     * @param queryTemplate Template to use for SQL query generation. Use {0} as the placeholder for where the generated portion of the WHERE clause should be inserted. 
     */
    public AbstractJdbcPersonAttributeDao(DataSource ds, String queryTemplate) {
        Validate.notNull(ds, "DataSource can not be null");
        Validate.notNull(queryTemplate, "queryTemplate can not be null");
        
        this.simpleJdbcTemplate = new SimpleJdbcTemplate(ds);
        this.queryTemplate = queryTemplate;
    }
    
    /**
     * @return the queryTemplate
     */
    public String getQueryTemplate() {
        return queryTemplate;
    }

    /**
     * @return the queryType
     */
    public QueryType getQueryType() {
        return queryType;
    }
    /**
     * Type of logical operator to use when joining WHERE clause components
     * 
     * @param queryType the queryType to set
     */
    public void setQueryType(QueryType queryType) {
        this.queryType = queryType;
    }


    /**
     * Takes the {@link List} from the query and parses it into the {@link List} of {@link IPersonAttributes} attributes to be returned.
     * 
     * @param queryResults Results from the query.
     * @param queryUserName The username passed in the query map, if no username attribute existed in the query Map null is provided.
     * @return The results of the query 
     */
    protected abstract List<IPersonAttributes> parseAttributeMapFromResults(final List<R> queryResults, String queryUserName);
    
    /**
     * @return The ParameterizedRowMapper to handle the results of the SQL query.
     */
    protected abstract ParameterizedRowMapper<R> getRowMapper();
    
    /* (non-Javadoc)
     * @see org.jasig.services.persondir.support.AbstractQueryPersonAttributeDao#appendAttributeToQuery(java.lang.Object, java.lang.String, java.util.List)
     */
    @Override
    protected PartialWhereClause appendAttributeToQuery(PartialWhereClause queryBuilder, String dataAttribute, List<Object> queryValues) {
        for (final Object queryValue : queryValues) {
            final String queryString = queryValue != null ? queryValue.toString() : null;
            if (StringUtils.isNotBlank(queryString)) {
                if (queryBuilder == null) {
                    queryBuilder = new PartialWhereClause();
                }
                else if (queryBuilder.sql.length() > 0) {
                    queryBuilder.sql.append(" ").append(this.queryType.toString()).append(" ");
                }

                //Convert to SQL wildcard
                String formattedQueryValue = queryString;
                final String usernameDataAttribute = this.getConfiguredUsernameDataAttribute();
                boolean isUsernameDataAttribute = dataAttribute != null && dataAttribute.equalsIgnoreCase(usernameDataAttribute);
                final Matcher queryValueMatcher = IPersonAttributeDao.WILDCARD_PATTERN.matcher(queryString);
                // Weird nested ifs here to try to match the structure of LdapPersonAttributeDao.
                // Original version had fewer, more complex boolean expressions, which proved
                // difficult to port to LdapPersonAttributeDao. The latter ended up with a much
                // less clever, but I think more readable impl, which I decided to port back here.
                if ( isUsernameDataAttribute ) {
                    if ( queryValueMatcher.find() ) {
                        if ( allowUsernameWildcards ) {
                            formattedQueryValue = queryValueMatcher.replaceAll("%"); // this includes a Matcher.reset()
                        }
                    } // we honor user-specific wildcards if that feature is enabled, but we
                      // never automatically wrap username attrib in wildcards on our own
                } else {
                    if ( queryValueMatcher.find() ) {
                        formattedQueryValue = queryValueMatcher.replaceAll("%"); // this includes a Matcher.reset()
                    } else if ( wildcardDataAttributes && ((wildcardedDataAttributeExclusions == null) || !(wildcardedDataAttributeExclusions.contains(dataAttribute))) ) {
                        formattedQueryValue = "%" + queryString + "%";
                    }
                }
                
                queryBuilder.arguments.add(formattedQueryValue);
                if (dataAttribute != null) {
                    dataAttribute = canonicalizeDataAttributeForSql(dataAttribute);
                    queryBuilder.sql.append(dataAttribute);
                    if (formattedQueryValue.equals(queryString)) {
                        queryBuilder.sql.append(" = ");
                    }
                    else {
                        queryBuilder.sql.append(" LIKE ");
                    }
                }
                queryBuilder.sql.append("?");
            }
        }
        
        return queryBuilder;
    }

    /**
     * Canonicalize the data-layer attribute column with the given name via
     * SQL function. This is as opposed to canonicalizing query attributes
     * or application attributes passed into or mapped out of the data layer.
     * Canonicalization of a data-layer column should only be necessary if
     * the data layer if you require case-insensitive searching on a mixed-case
     * column in a case-sensitive data layer. Careful, though, as this can
     * result in table scanning if the data layer does not support
     * function-based indices.
     *
     * @param dataAttribute
     * @return
     */
    protected String canonicalizeDataAttributeForSql(String dataAttribute) {
        if (this.caseInsensitiveDataAttributes == null || this.caseInsensitiveDataAttributes.isEmpty() || !(this.caseInsensitiveDataAttributes.containsKey(dataAttribute))) {
            return dataAttribute;
        }
        if ( this.dataAttributeCaseCanonicalizationFunctions == null || this.dataAttributeCaseCanonicalizationFunctions.isEmpty() ) {
            return dataAttribute;
        }
        CaseCanonicalizationMode canonicalizationMode = this.caseInsensitiveDataAttributes.get(dataAttribute);
        if ( canonicalizationMode == null ) {
            canonicalizationMode = getDefaultCaseCanonicalizationMode();
        }
        MessageFormat mf = this.dataAttributeCaseCanonicalizationFunctions.get(canonicalizationMode);
        if ( mf == null ) {
            return dataAttribute;
        }
        return mf.format(new String[] { dataAttribute });
    }

    
    /* (non-Javadoc)
     * @see org.jasig.services.persondir.support.AbstractQueryPersonAttributeDao#getPeopleForQuery(java.lang.Object, java.lang.String)
     */
    @Override
    protected List<IPersonAttributes> getPeopleForQuery(PartialWhereClause queryBuilder, String queryUserName) {
        //Execute the query
        final ParameterizedRowMapper<R> rowMapper = this.getRowMapper();
        
        final List<R> results;
        if (queryBuilder != null) {
            //Merge the generated SQL with the base query template
            final StringBuilder partialSqlWhere = queryBuilder.sql;
            final Matcher queryMatcher = WHERE_PLACEHOLDER.matcher(this.queryTemplate);
            final String querySQL = queryMatcher.replaceAll(partialSqlWhere.toString());
            
            results = this.simpleJdbcTemplate.query(querySQL, rowMapper, queryBuilder.arguments.toArray());
            
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("Executed '" + this.queryTemplate + "' with arguments " + queryBuilder.arguments + " and got results " + results);
            }
        }
        else {
            results = this.simpleJdbcTemplate.query(this.queryTemplate, rowMapper);
            
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("Executed '" + this.queryTemplate + "' and got results " + results);
            }
        }

        return this.parseAttributeMapFromResults(results, queryUserName);
    }

    public Map<String, CaseCanonicalizationMode> getCaseInsensitiveDataAttributes() {
        return caseInsensitiveDataAttributes;
    }

    public void setCaseInsensitiveDataAttributes(Map<String, CaseCanonicalizationMode> caseInsensitiveDataAttributes) {
        this.caseInsensitiveDataAttributes = caseInsensitiveDataAttributes;
    }

    public void setCaseInsensitiveDataAttributesAsCollection(Collection<String> caseInsensitiveDataAttributes) {
        if (caseInsensitiveDataAttributes == null || caseInsensitiveDataAttributes.isEmpty()) {
            setCaseInsensitiveDataAttributes(null);
        } else {
            Map<String, CaseCanonicalizationMode> asMap = new HashMap<String, CaseCanonicalizationMode>();
            for ( String attrib : caseInsensitiveDataAttributes ) {
                asMap.put(attrib, null);
            }
            setCaseInsensitiveDataAttributes(asMap);
        }
    }

    /**
     * Assign {@link MessageFormat}s describing how to wrap a JDBC/SQL column
     * reference in a function corresponding to a given
     * {@link CaseCanonicalizationMode}. For example, a typical mapping for
     * {@link CaseCanonicalizationMode#LOWER} would be "lower({0})". The
     * defaults are just what you'd expect ("lower({0})" and "upper({0})").
     *
     * <p>Setting {@code null} or an empty map has the effect of never
     * wrapping any columns in canonicalizing functions, even if they are
     * referenced by {@link #setCaseInsensitiveDataAttributes(java.util.Map)}.</p>
     *
     * @param dataAttributeCaseCanonicalizationFunctions
     */
    public void setDataAttributeCaseCanonicalizationFunctions(Map<CaseCanonicalizationMode, MessageFormat> dataAttributeCaseCanonicalizationFunctions) {
        this.dataAttributeCaseCanonicalizationFunctions = dataAttributeCaseCanonicalizationFunctions;
    }

    public Map<CaseCanonicalizationMode, MessageFormat> getDataAttributeCaseCanonicalizationFunctions() {
        return dataAttributeCaseCanonicalizationFunctions;
    }

    public boolean isAllowUsernameWildcards() {
        return allowUsernameWildcards;
    }

    public void setAllowUsernameWildcards(boolean allowUsernameWildcards) {
        this.allowUsernameWildcards = allowUsernameWildcards;
    }


    public boolean isWildcardDataAttributes() {
        return wildcardDataAttributes;
    }

    /**
     * If {@code true}, searches on non-username attributes will be wrapped in
     * wildcards unless the predicate already contains at least one wildcard or
     * searched-on <em>data</em> attribute is listed in
     * {@code wildcardedDataAttributeExclusions}. Defaults
     * to {@link false}, which preserves historical behavior. Set to {@code true}
     * for more consistent behavior w/r/t uPortal's internal person DAO. Setting
     * to true does, of course, typically eliminate the value of any indices you
     * may have in the underlying data store.
     *
     * @param wildcardDataAttributes
     */
    public void setWildcardDataAttributes(boolean wildcardDataAttributes) {
        this.wildcardDataAttributes = wildcardDataAttributes;
    }

    public Set<String> getWildcardedDataAttributeExclusions() {
        return wildcardedDataAttributeExclusions;
    }

    /**
     * If {@code wildcardDataAttributes} is {@code true}, all non-username
     * search predicate values will be wrapped in wildcards except for the
     * data-layer attributes listed here. This is especially useful for
     * multi-valued LDAP attributes which typically do not support wildcarded
     * searches.
     *
     * @param wildcardedDataAttributeExclusions
     */
    public void setWildcardedDataAttributeExclusions(Set<String> wildcardedDataAttributeExclusions) {
        this.wildcardedDataAttributeExclusions = wildcardedDataAttributeExclusions;
    }

    /**
     * Was a username <em>data</em> attribute explicitly configured. I.e.
     * if {@code true}, you know {@link #getConfiguredUsernameDataAttribute()}
     * is not going to fall back to {@link #getConfiguredUserNameAttribute()}.
     *
     * @return
     */
    public boolean isUsernameDataAttributeConfigured() {
        return getUsernameDataAttribute() != null;
    }

    /**
     * Get the currently configured username <em>data</em> attribute, if any,
     * falling back to the currently configured username app attribute
     * ({@link #getConfiguredUserNameAttribute()}).
     *
     * <p>This property is duplicated in the LDAP DAO because, while it could
     * theoretically be factored into their shared parent class
     * ({@link AbstractQueryPersonAttributeDao}), that class doesn't actually
     * do anything with the property, so exposing it as config at that level
     * is technically incorrect. The same duplicating treatment is given
     * to other wildcarding-related properties because their handling is
     * specific to data-layer attribute processing which means it is
     * specific to concrete DAOs (currently, anyway).</p>
     *
     * @return
     */
    public String getConfiguredUsernameDataAttribute() {
        if ( !(isUsernameDataAttributeConfigured()) ) {
            // Almost certainly not what you want, but in the rare case where
            // the app- and data-layer usernames have the same name, this
            // can save you a small amount of config.
            return getConfiguredUserNameAttribute();
        }
        return getUsernameDataAttribute();
    }

    public String getUsernameDataAttribute() {
        return usernameDataAttribute;
    }

    public void setUsernameDataAttribute(String usernameDataAttribute) {
        this.usernameDataAttribute = usernameDataAttribute;
    }

}
