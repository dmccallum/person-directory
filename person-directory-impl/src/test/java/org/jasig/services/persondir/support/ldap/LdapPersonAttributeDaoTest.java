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

package org.jasig.services.persondir.support.ldap;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jasig.services.persondir.IPersonAttributes;
import org.jasig.services.persondir.util.Util;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.ldap.test.AbstractDirContextTest;


/**
 * Testcase for LdapPersonAttributeDao.
 * @author andrew.petro@yale.edu
 * @author Eric Dalquist
 * @version $Revision$ $Date$
 */
public class LdapPersonAttributeDaoTest extends AbstractDirContextTest {
    /* (non-Javadoc)
     * @see org.springframework.ldap.test.AbstractDirContextTest#getPartitionName()
     */
    @Override
    protected String getPartitionName() {
        return "personDirectory";
    }
    
    /* (non-Javadoc)
     * @see org.springframework.ldap.test.AbstractDirContextTest#getBaseDn()
     */
    @Override
    protected String getBaseDn() {
        return "ou=people,o=personDirectory";
    }


    /* (non-Javadoc)
     * @see org.springframework.ldap.test.AbstractDirContextTest#initializationData()
     */
    @Override
    protected Resource[] initializationData() {
        final ClassPathResource ldapPersonInfo = new ClassPathResource("/ldapPersonInfo.ldif");
        return new Resource[] { ldapPersonInfo };
    }
    
    public void testNotFoundQuery() throws Exception {
        LdapPersonAttributeDao impl = new LdapPersonAttributeDao();
        
        Map<String, Object> ldapAttribsToPortalAttribs = new HashMap<String, Object>();
        ldapAttribsToPortalAttribs.put("mail", "email");
        
        impl.setResultAttributeMapping(ldapAttribsToPortalAttribs);

        impl.setContextSource(this.getContextSource());
        
        impl.setQueryAttributeMapping(Collections.singletonMap("uid", null));
        
        impl.afterPropertiesSet();
        
        Map<String, List<Object>> queryMap = new HashMap<String, List<Object>>();
        queryMap.put("uid", Util.list("unknown"));
        
        try {
            Map<String, List<Object>> attribs = impl.getMultivaluedUserAttributes(queryMap);
            assertNull(attribs);
        }
        catch (DataAccessResourceFailureException darfe) {
            //OK, No net connection
        }
    }

    /**
     * Test for a query with a single attribute. 
     */
    public void testSingleAttrQuery() throws Exception {
        LdapPersonAttributeDao impl = new LdapPersonAttributeDao();
        
        Map<String, Object> ldapAttribsToPortalAttribs = new HashMap<String, Object>();
        ldapAttribsToPortalAttribs.put("mail", "email");
        
        impl.setResultAttributeMapping(ldapAttribsToPortalAttribs);
        
        impl.setContextSource(this.getContextSource());
        
        impl.setQueryAttributeMapping(Collections.singletonMap("uid", "uid"));

        impl.afterPropertiesSet();
        
        Map<String, List<Object>> queryMap = new HashMap<String, List<Object>>();
        queryMap.put("uid", Util.list("edalquist"));

        try {
            Map<String, List<Object>> attribs = impl.getMultivaluedUserAttributes(queryMap);
            assertEquals(Util.list("eric.dalquist@example.com"), attribs.get("email"));
        }
        catch (DataAccessResourceFailureException darfe) {
            //OK, No net connection
        }
    }

    /**
     * Test for a query with a single attribute. 
     * 
     * This testcase will cease to work on that fateful day when edalquist
     * no longer appears in Yale University LDAP.
     */
    public void testMultipleMappings() throws Exception {
        LdapPersonAttributeDao impl = new LdapPersonAttributeDao();
        
        Map<String, Object> ldapAttribsToPortalAttribs = new HashMap<String, Object>();
        Set<String> portalAttributes = new HashSet<String>();
        portalAttributes.add("email");
        portalAttributes.add("work.email");
        ldapAttribsToPortalAttribs.put("mail", portalAttributes);
        
        impl.setResultAttributeMapping(ldapAttribsToPortalAttribs);
        
        impl.setContextSource(this.getContextSource());
        
        impl.setQueryAttributeMapping(Collections.singletonMap("uid", "uid"));

        impl.afterPropertiesSet();
        
        Map<String, List<Object>> queryMap = new HashMap<String, List<Object>>();
        queryMap.put("uid", Util.list("edalquist"));

        try {
            Map<String, List<Object>> attribs = impl.getMultivaluedUserAttributes(queryMap);
            assertEquals(Util.list("eric.dalquist@example.com"), attribs.get("email"));
            assertEquals(Util.list("eric.dalquist@example.com"), attribs.get("work.email"));
        }
        catch (DataAccessResourceFailureException darfe) {
            //OK, No net connection
        }
    }

    public void testInvalidAttrMap() throws Exception {
        LdapPersonAttributeDao impl = new LdapPersonAttributeDao();
        
        Map<String, Object> ldapAttribsToPortalAttribs = new HashMap<String, Object>();
        ldapAttribsToPortalAttribs.put("email", "email");
        
        impl.setResultAttributeMapping(ldapAttribsToPortalAttribs);
        
        impl.setContextSource(this.getContextSource());
        
        impl.setQueryAttributeMapping(Collections.singletonMap("uid", "uid"));

        impl.afterPropertiesSet();
        
        Map<String, List<Object>> queryMap = new HashMap<String, List<Object>>();
        queryMap.put("uid", Util.list("edalquist"));
        
        try {
            Map<String, List<Object>> attribs = impl.getMultivaluedUserAttributes(queryMap);
            assertNull(attribs.get("email"));
        }
        catch (DataAccessResourceFailureException darfe) {
            //OK, No net connection
        }
    }

    public void testDefaultAttrMap() throws Exception {
        LdapPersonAttributeDao impl = new LdapPersonAttributeDao();
        
        Map<String, Object> ldapAttribsToPortalAttribs = new HashMap<String, Object>();
        ldapAttribsToPortalAttribs.put("mail", null);
        
        impl.setResultAttributeMapping(ldapAttribsToPortalAttribs);
        
        impl.setContextSource(this.getContextSource());
        
        impl.setQueryAttributeMapping(Collections.singletonMap("uid", "uid"));

        impl.afterPropertiesSet();
        
        Map<String, List<Object>> queryMap = new HashMap<String, List<Object>>();
        queryMap.put("uid", Util.list("edalquist"));
        
        try {
            Map<String, List<Object>> attribs = impl.getMultivaluedUserAttributes(queryMap);
            assertEquals(Util.list("eric.dalquist@example.com"), attribs.get("mail"));
        }
        catch (DataAccessResourceFailureException darfe) {
            //OK, No net connection
        }
    }
    
    /**
     * Test case for a query that needs multiple attributes to complete and
     * more attributes than are needed to complete are passed to it.
     */
    public void testMultiAttrQuery() throws Exception {
        LdapPersonAttributeDao impl = new LdapPersonAttributeDao();
        
        Map<String, Object> ldapAttribsToPortalAttribs = new HashMap<String, Object>();
        ldapAttribsToPortalAttribs.put("mail", "email");
        
        impl.setResultAttributeMapping(ldapAttribsToPortalAttribs);
        
        impl.setContextSource(this.getContextSource());
        
        Map<String, String> queryAttrs = new HashMap<String, String>();
        queryAttrs.put("uid", "uid");
        queryAttrs.put("alias", "alias");
        impl.setQueryAttributeMapping(queryAttrs);

        impl.afterPropertiesSet();
        
        Map<String, List<Object>> queryMap = new HashMap<String, List<Object>>();
        queryMap.put("uid", Util.list("edalquist"));
        queryMap.put("givenname", Util.list("Eric"));
        queryMap.put("email", Util.list("edalquist@unicon.net"));
        
        try {
            Map<String, List<Object>> attribs = impl.getMultivaluedUserAttributes(queryMap);
            assertEquals(Util.list("eric.dalquist@example.com"), attribs.get("email"));
        }
        catch (DataAccessResourceFailureException darfe) {
            //OK, No net connection
        }
    }
    
    /**
     * A query that needs mulitple attributes to complete but the needed
     * attributes aren't passed to it.
     */
    public void testInsufficientAttrQuery() throws Exception {
        LdapPersonAttributeDao impl = new LdapPersonAttributeDao();
        
        Map<String, Object> ldapAttribsToPortalAttribs = new HashMap<String, Object>();
        ldapAttribsToPortalAttribs.put("mail", "email");
        
        impl.setResultAttributeMapping(ldapAttribsToPortalAttribs);
        impl.setContextSource(this.getContextSource());
        
        Map<String, String> queryAttrs = new HashMap<String, String>();
        queryAttrs.put("uid", null);
        queryAttrs.put("alias", null);
        impl.setQueryAttributeMapping(queryAttrs);
        impl.setRequireAllQueryAttributes(true);
        
        Map<String, List<Object>> queryMap = new HashMap<String, List<Object>>();
        queryMap.put("uid", Util.list("edalquist"));
        queryMap.put("email", Util.list("edalquist@example.net"));
        
        Map<String, List<Object>> attribs = impl.getMultivaluedUserAttributes(queryMap);
        assertNull(attribs);
    }
    
    /**
     * Test proper reporting of declared attribute names.
     */
    public void testAttributeNames() throws Exception {
        LdapPersonAttributeDao impl = new LdapPersonAttributeDao();
        
        Map<String, Object> ldapAttribsToPortalAttribs = new HashMap<String, Object>();
        ldapAttribsToPortalAttribs.put("mail", "email");
        ldapAttribsToPortalAttribs.put("shirtColor", "dressShirtColor");
        
        Set<String> surNameAttributeNames = new HashSet<String>();
        surNameAttributeNames.add("surName");
        surNameAttributeNames.add("lastName");
        surNameAttributeNames.add("familyName");
        surNameAttributeNames.add("thirdName");
        ldapAttribsToPortalAttribs.put("lastName", surNameAttributeNames);
        
        impl.setResultAttributeMapping(ldapAttribsToPortalAttribs);
        
        Set<String> expectedAttributeNames = new HashSet<String>();
        expectedAttributeNames.addAll(surNameAttributeNames);
        expectedAttributeNames.add("email");
        expectedAttributeNames.add("dressShirtColor");
        
        assertEquals(expectedAttributeNames, impl.getPossibleUserAttributeNames());
    }
    
    public void testProperties() throws Exception {
        LdapPersonAttributeDao impl = new LdapPersonAttributeDao();
        
        assertEquals("", impl.getBaseDN());
        impl.setBaseDN("BaseDN");
        assertEquals("BaseDN", impl.getBaseDN());
        impl.setBaseDN(null);
        assertEquals("", impl.getBaseDN());
        
        
        assertNull(impl.getResultAttributeMapping());
        Map<String, Object> attrMap = new HashMap<String, Object>();
        attrMap.put("mail", "email");
        impl.setResultAttributeMapping(attrMap);
        
        Map<String, Set<String>> expectedAttrMap = new HashMap<String, Set<String>>();
        expectedAttrMap.put("mail", Collections.singleton("email"));
        assertEquals(expectedAttrMap, impl.getResultAttributeMapping());
        
        
        assertNull(impl.getContextSource());
        impl.setContextSource(this.getContextSource());
        assertEquals(this.getContextSource(), impl.getContextSource());
        
        
        impl.setResultAttributeMapping(null);
        assertEquals(Collections.EMPTY_SET, impl.getPossibleUserAttributeNames());
        impl.setResultAttributeMapping(attrMap);
        assertEquals(Collections.singleton("email"), impl.getPossibleUserAttributeNames());
    }
    
    /**
     * Test proper reporting of declared attribute names.
     */
    public void testNullContext() throws Exception {
        LdapPersonAttributeDao impl = new LdapPersonAttributeDao();
        
        try {
            impl.afterPropertiesSet();
            fail("BeanCreationException should have been thrown with no context configured");
        }
        catch (BeanCreationException ise) {
            //expected
        }
    }

    // All of the following wildcard-related tests were copy/pasted from
    // AbstractJdbcPersonAttributeDaoTest. Given that this LDAP test class
    // extends AbstractDirContextTest, it was going to be too complicated to
    // find a way to mix-in these tests to both hierarchies. Though it would
    // be nice to do so (and for both the wildcarding and case-sensitivity
    // tests). Also note that both sets of tests have very different underlying
    // datasets in their persistent fixtures.
    public void testConfiguredDoubleEndedWildcards_NonUsernameQuery() throws Exception {
        LdapPersonAttributeDao impl = newDao();
        impl.setUseAllQueryAttributes(false);
        final Map<String, Object> columnsToAttributes = new LinkedHashMap<String, Object>();
        columnsToAttributes.put("uid", "username");
        columnsToAttributes.put("sn", "lastName");
        impl.setResultAttributeMapping(columnsToAttributes);
        final Map<String, Object> attributesToColumns = new LinkedHashMap<String, Object>();
        attributesToColumns.put("username", "uid");
        attributesToColumns.put("lastName", "sn");
        impl.setQueryAttributeMapping(attributesToColumns);
        impl.setWildcardDataAttributes(true); // key config difference
        impl.setUsernameDataAttribute("uid"); // key config point (without this, doesn't know when it's
                                              // looking at a username search term (specifically, will revert to
                                              // using the default app-layer username attribute, which
                                              // is almost always pointless when processing data-layer attributes
                                              // (which is when these wildcards are applied))
        impl.afterPropertiesSet();

        Map<String,Object> partialMatchQuery = new LinkedHashMap<String, Object>();
        partialMatchQuery.put("lastName", "alquis"); // missing letters on both ends
        final Set<IPersonAttributes> result = impl.getPeople(partialMatchQuery);
        assertEquals(1, result.size());
        Iterator<IPersonAttributes> resultIterator = result.iterator();
        IPersonAttributes currentResult = resultIterator.next();
        assertEquals("edalquist", currentResult.getName());
        assertEquals("Dalquist", currentResult.getAttributeValue("lastName"));
    }

    public void testConfiguredDoubleEndedWildcards_NonUsernameQuery_ExcludedAttribute() throws Exception {
        LdapPersonAttributeDao impl = newDao();
        impl.setUseAllQueryAttributes(false);
        final Map<String, Object> columnsToAttributes = new LinkedHashMap<String, Object>();
        columnsToAttributes.put("uid", "username");
        columnsToAttributes.put("sn", "lastName");
        impl.setResultAttributeMapping(columnsToAttributes);
        final Map<String, Object> attributesToColumns = new LinkedHashMap<String, Object>();
        attributesToColumns.put("username", "uid");
        attributesToColumns.put("lastName", "sn");
        impl.setQueryAttributeMapping(attributesToColumns);
        impl.setWildcardDataAttributes(true); // key config difference
        impl.setUsernameDataAttribute("uid"); // key config point (without this, doesn't know when it's
                                              // looking at a username search term (specifically, will revert to
                                              // using the default app-layer username attribute, which
                                              // is almost always pointless when processing data-layer attributes
                                              // (which is when these wildcards are applied))
        impl.setWildcardedDataAttributeExclusions(Collections.singleton("sn")); // key config point
        impl.afterPropertiesSet();

        // This test really only makes sense when paired with
        // testConfiguredDoubleEndedWildcards_NonUsernameQuery(), which verifies
        // that you *do* get results when you haven't excluded firstName from
        // wildcarding
        Map<String,Object> partialMatchQuery = new LinkedHashMap<String, Object>();
        partialMatchQuery.put("lastName", "alquis"); // missing letters on both ends
        final Set<IPersonAttributes> result = impl.getPeople(partialMatchQuery);
        assertEquals(0, result.size());
    }

    public void testSingleUserSpecifiedWildcard_DoubleEndedWildcardsEnabled() throws Exception {
        LdapPersonAttributeDao impl = newDao();
        impl.setUseAllQueryAttributes(false);
        final Map<String, Object> columnsToAttributes = new LinkedHashMap<String, Object>();
        columnsToAttributes.put("uid", "username");
        columnsToAttributes.put("sn", "lastName");
        impl.setResultAttributeMapping(columnsToAttributes);
        final Map<String, Object> attributesToColumns = new LinkedHashMap<String, Object>();
        attributesToColumns.put("username", "uid");
        attributesToColumns.put("lastName", "sn");
        impl.setQueryAttributeMapping(attributesToColumns);
        impl.setWildcardDataAttributes(true); // key config point
        impl.setUsernameDataAttribute("uid"); // key config point (without this, doesn't know when it's
                                              // looking at a username search term (specifically, will revert to
                                              // using the default app-layer username attribute, which
                                              // is almost always pointless when processing data-layer attributes
                                              // (which is when these wildcards are applied))
        impl.afterPropertiesSet();


        Map<String,Object> internalWildcardQuery = new LinkedHashMap<String, Object>();
        internalWildcardQuery.put("lastName", "Dal*uist");
        Set<IPersonAttributes> result = impl.getPeople(internalWildcardQuery);
        assertEquals(1, result.size());
        Iterator<IPersonAttributes> resultIterator = result.iterator();
        IPersonAttributes currentResult = resultIterator.next();
        assertEquals("edalquist", currentResult.getName());
        assertEquals("Dalquist", currentResult.getAttributeValue("lastName"));

        // now make sure the app isn't sneaking wildcards onto the ends, even tho
        // that feature is enabled (the user-specified wildcards should take
        // precedence)
        Map<String,Object> missingCharsQuery = new LinkedHashMap<String, Object>();
        missingCharsQuery.put("lastName", "al*uis");
        result = impl.getPeople(missingCharsQuery);
        assertEquals(0, result.size());
    }


    public void testSingleUserSpecifiedWildcard_DoubleEndedWildcardsDisabled() throws Exception {
        LdapPersonAttributeDao impl = newDao();
        impl.setUseAllQueryAttributes(false);
        final Map<String, Object> columnsToAttributes = new LinkedHashMap<String, Object>();
        columnsToAttributes.put("uid", "username");
        columnsToAttributes.put("sn", "lastName");
        impl.setResultAttributeMapping(columnsToAttributes);
        final Map<String, Object> attributesToColumns = new LinkedHashMap<String, Object>();
        attributesToColumns.put("username", "uid");
        attributesToColumns.put("lastName", "sn");
        impl.setQueryAttributeMapping(attributesToColumns);
        impl.setWildcardDataAttributes(false); // key config difference
        impl.setUsernameDataAttribute("uid"); // key config point (without this, doesn't know when it's
                                              // looking at a username search term (specifically, will revert to
                                              // using the default app-layer username attribute, which
                                              // is almost always pointless when processing data-layer attributes
                                              // (which is when these wildcards are applied))
        impl.afterPropertiesSet();


        Map<String,Object> internalWildcardQuery = new LinkedHashMap<String, Object>();
        internalWildcardQuery.put("lastName", "Dal*uist");
        Set<IPersonAttributes> result = impl.getPeople(internalWildcardQuery);
        assertEquals(1, result.size());
        Iterator<IPersonAttributes> resultIterator = result.iterator();
        IPersonAttributes currentResult = resultIterator.next();
        assertEquals("edalquist", currentResult.getName());
        assertEquals("Dalquist", currentResult.getAttributeValue("lastName"));

        // now make sure the app isn't sneaking wildcards onto the ends, even tho
        // that feature is enabled (the user-specified wildcards should take
        // precedence)
        Map<String,Object> missingCharsQuery = new LinkedHashMap<String, Object>();
        missingCharsQuery.put("lastName", "al*uis");
        result = impl.getPeople(missingCharsQuery);
        assertEquals(0, result.size());
    }

    public void testMulitpleUserSpecifiedWildcards_DoubleEndedWildcardsEnabled() throws Exception {
        LdapPersonAttributeDao impl = newDao();
        impl.setUseAllQueryAttributes(false);
        final Map<String, Object> columnsToAttributes = new LinkedHashMap<String, Object>();
        columnsToAttributes.put("uid", "username");
        columnsToAttributes.put("sn", "lastName");
        impl.setResultAttributeMapping(columnsToAttributes);
        final Map<String, Object> attributesToColumns = new LinkedHashMap<String, Object>();
        attributesToColumns.put("username", "uid");
        attributesToColumns.put("lastName", "sn");
        impl.setQueryAttributeMapping(attributesToColumns);
        impl.setWildcardDataAttributes(true); // key config point
        impl.setUsernameDataAttribute("uid"); // key config point (without this, doesn't know when it's
                                              // looking at a username search term (specifically, will revert to
                                              // using the default app-layer username attribute, which
                                              // is almost always pointless when processing data-layer attributes
                                              // (which is when these wildcards are applied))
        impl.afterPropertiesSet();


        Map<String,Object> internalWildcardQuery = new LinkedHashMap<String, Object>();
        internalWildcardQuery.put("lastName", "Da*q*ist");
        Set<IPersonAttributes> result = impl.getPeople(internalWildcardQuery);
        assertEquals(1, result.size());
        Iterator<IPersonAttributes> resultIterator = result.iterator();
        IPersonAttributes currentResult = resultIterator.next();
        assertEquals("edalquist", currentResult.getName());
        assertEquals("Dalquist", currentResult.getAttributeValue("lastName"));

    }

    public void testMulitpleUserSpecifiedWildcards_DoubleEndedWildcardsDisabled() throws Exception {
        LdapPersonAttributeDao impl = newDao();
        impl.setUseAllQueryAttributes(false);
        final Map<String, Object> columnsToAttributes = new LinkedHashMap<String, Object>();
        columnsToAttributes.put("uid", "username");
        columnsToAttributes.put("sn", "lastName");
        impl.setResultAttributeMapping(columnsToAttributes);
        final Map<String, Object> attributesToColumns = new LinkedHashMap<String, Object>();
        attributesToColumns.put("username", "netid");
        attributesToColumns.put("lastName", "sn");
        impl.setQueryAttributeMapping(attributesToColumns);
        impl.setWildcardDataAttributes(false); // key config point (but just reasserts the default)
        impl.setUsernameDataAttribute("uid"); // key config point (without this, doesn't know when it's
                                                // looking at a username search term (specifically, will revert to
                                                // using the default app-layer username attribute, which
                                                // is almost always pointless when processing data-layer attributes
                                                // (which is when these wildcards are applied))
        impl.afterPropertiesSet();

        Map<String,Object> internalWildcardQuery = new LinkedHashMap<String, Object>();
        internalWildcardQuery.put("lastName", "Da*q*ist");
        Set<IPersonAttributes> result = impl.getPeople(internalWildcardQuery);
        assertEquals(1, result.size());
        Iterator<IPersonAttributes> resultIterator = result.iterator();
        IPersonAttributes currentResult = resultIterator.next();
        assertEquals("edalquist", currentResult.getName());
        assertEquals("Dalquist", currentResult.getAttributeValue("lastName"));

    }

    public void testUserSpecifiedUsernameWildcards_UsernameWildcardsEnabled_DoubleEndedWildcardsEnabled() throws Exception {
        LdapPersonAttributeDao impl = newDao();
        impl.setUseAllQueryAttributes(false);
        final Map<String, Object> columnsToAttributes = new LinkedHashMap<String, Object>();
        columnsToAttributes.put("uid", "username");
        impl.setResultAttributeMapping(columnsToAttributes);
        final Map<String, Object> attributesToColumns = new LinkedHashMap<String, Object>();
        attributesToColumns.put("username", "uid");
        impl.setQueryAttributeMapping(attributesToColumns);
        impl.setWildcardDataAttributes(true); // key config point
        impl.setAllowUsernameWildcards(true); // key config point (but just reasserts the default)
        impl.setUsernameDataAttribute("uid"); // key config point (without this, doesn't know when it's
                                                // looking at a username search term (specifically, will revert to
                                                // using the default app-layer username attribute, which
                                                // is almost always pointless when processing data-layer attributes
                                                // (which is when these wildcards are applied))
        impl.afterPropertiesSet();

        IPersonAttributes result = impl.getPerson("edal*uist");
        assertNotNull(result);
        assertEquals("edalquist", result.getName());

        // make sure it's not just wrapping wildcards around the whole thing
        result = impl.getPerson("l*");
        assertNull(result);
    }

    public void testUserSpecifiedUsernameWildcards_UsernameWildcardsEnabled_DoubleEndedWildcardsDisabled() throws Exception {
        LdapPersonAttributeDao impl = newDao();
        impl.setUseAllQueryAttributes(false);
        final Map<String, Object> columnsToAttributes = new LinkedHashMap<String, Object>();
        columnsToAttributes.put("uid", "username");
        impl.setResultAttributeMapping(columnsToAttributes);
        final Map<String, Object> attributesToColumns = new LinkedHashMap<String, Object>();
        attributesToColumns.put("username", "uid");
        impl.setQueryAttributeMapping(attributesToColumns);
        impl.setWildcardDataAttributes(false); // key config point (but just reasserts the default)
        impl.setAllowUsernameWildcards(true); // key config point (but just reasserts the default)
        impl.setUsernameDataAttribute("uid"); // key config point (without this, doesn't know when it's
                                                // looking at a username search term (specifically, will revert to
                                                // using the default app-layer username attribute, which
                                                // is almost always pointless when processing data-layer attributes
                                                // (which is when these wildcards are applied))
        impl.afterPropertiesSet();

        IPersonAttributes result = impl.getPerson("edal*uist");
        assertNotNull(result);
        assertEquals("edalquist", result.getName());

        // make sure it's not just wrapping wildcards around the whole thing
        result = impl.getPerson("l*");
        assertNull(result);
    }

    public void testUserSpecifiedUsernameWildcards_UsernameWildcardsDisabled_DoubleEndedWildcardsEnabled() throws Exception {
        LdapPersonAttributeDao impl = newDao();
        impl.setUseAllQueryAttributes(false);
        final Map<String, Object> columnsToAttributes = new LinkedHashMap<String, Object>();
        columnsToAttributes.put("uid", "username");
        impl.setResultAttributeMapping(columnsToAttributes);
        final Map<String, Object> attributesToColumns = new LinkedHashMap<String, Object>();
        attributesToColumns.put("username", "uid");
        impl.setQueryAttributeMapping(attributesToColumns);
        impl.setWildcardDataAttributes(true); // key config point
        impl.setAllowUsernameWildcards(false); // key config point
        impl.setUsernameDataAttribute("uid"); // key config point (without this, doesn't know when it's
                                              // looking at a username search term (specifically, will revert to
                                              // using the default app-layer username attribute, which
                                              // is almost always pointless when processing data-layer attributes
                                              // (which is when these wildcards are applied))
        impl.afterPropertiesSet();

        IPersonAttributes result = impl.getPerson("edal*uist");
        assertNull(result);
    }

    public void testUserSpecifiedUsernameWildcards_UsernameWildcardsDisabled_DoubleEndedWildcardsDisabled() throws Exception {
        LdapPersonAttributeDao impl = newDao();
        impl.setUseAllQueryAttributes(false);
        final Map<String, Object> columnsToAttributes = new LinkedHashMap<String, Object>();
        columnsToAttributes.put("uid", "username");
        impl.setResultAttributeMapping(columnsToAttributes);
        final Map<String, Object> attributesToColumns = new LinkedHashMap<String, Object>();
        attributesToColumns.put("username", "uid");
        impl.setQueryAttributeMapping(attributesToColumns);
        impl.setWildcardDataAttributes(false); // key config point (but just reasserts the default)
        impl.setAllowUsernameWildcards(false); // key config point
        impl.setUsernameDataAttribute("uid"); // key config point (without this, doesn't know when it's
                                              // looking at a username search term (specifically, will revert to
                                              // using the default app-layer username attribute, which
                                              // is almost always pointless when processing data-layer attributes
                                              // (which is when these wildcards are applied))
        impl.afterPropertiesSet();

        IPersonAttributes result = impl.getPerson("edal*uist");
        assertNull(result);
    }

    public void testPartialInternalUsernameMatch_DoubleEndedWildcardsEnabled() throws Exception {
        LdapPersonAttributeDao impl = newDao();
        impl.setUseAllQueryAttributes(false);
        final Map<String, Object> columnsToAttributes = new LinkedHashMap<String, Object>();
        columnsToAttributes.put("uid", "username");
        impl.setResultAttributeMapping(columnsToAttributes);
        final Map<String, Object> attributesToColumns = new LinkedHashMap<String, Object>();
        attributesToColumns.put("username", "uid");
        impl.setQueryAttributeMapping(attributesToColumns);
        impl.setWildcardDataAttributes(true); // key config point
        impl.setUsernameDataAttribute("uid"); // key config point (without this, doesn't know when it's
                                              // looking at a username search term (specifically, will revert to
                                              // using the default app-layer username attribute, which
                                              // is almost always pointless when processing data-layer attributes
                                              // (which is when these wildcards are applied))
        impl.afterPropertiesSet();

        IPersonAttributes result = impl.getPerson("dalquis");
        assertNull(result); // double-ended wildcarding config *never* applies to usernames,
                            // even if no user-specified wildcards present in search term
    }

    /**
     * Note that {@link org.jasig.services.persondir.support.ldap.LdapPersonAttributeDao#afterPropertiesSet()}
     * has <em>not</em> been called on the returned instance. Assumes you
     * probably have more config to set to prepare for your test.
     *
     * @return
     */
    protected LdapPersonAttributeDao newDao() {
        LdapPersonAttributeDao impl = new LdapPersonAttributeDao();
        impl.setContextSource(this.getContextSource());
        return impl;
    }


}