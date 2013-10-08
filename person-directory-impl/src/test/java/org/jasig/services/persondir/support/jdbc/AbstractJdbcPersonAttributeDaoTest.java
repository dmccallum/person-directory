package org.jasig.services.persondir.support.jdbc;

import java.sql.SQLException;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import javax.sql.DataSource;

import org.hsqldb.jdbcDriver;
import org.jasig.services.persondir.IPersonAttributes;
import org.jasig.services.persondir.support.AbstractDefaultQueryPersonAttributeDaoTest;
import org.jasig.services.persondir.util.CaseCanonicalizationMode;
import org.jasig.services.persondir.util.Util;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;

/**
 * Otherwise would be huge amounts of duplicated boilerplate for verifying
 * both {@link SingleRowJdbcPersonAttributeDao} and
 * {@link MultiRowJdbcPersonAttributeDao}.
 *
 * <p> Was no point, though, in extending
 * {@link org.jasig.services.persondir.support.AbstractDefaultQueryPersonAttributeDaoTest}
 * b/c those tests shouldn't necessarily behave the same way if we try to manipulate
 * the case-sensitivity behavior of the DAO under test.</p>
 */
public abstract class AbstractJdbcPersonAttributeDaoTest extends AbstractDefaultQueryPersonAttributeDaoTest {

    protected DataSource testDataSource;

    protected abstract void setUpSchema(DataSource dataSource) throws SQLException;
    protected abstract void tearDownSchema(DataSource dataSource) throws SQLException;
    protected abstract AbstractJdbcPersonAttributeDao<Map<String, Object>> newDao(DataSource dataSource);
    protected abstract void beforeNonUsernameQuery(AbstractJdbcPersonAttributeDao<Map<String, Object>> dao);

    /**
     * Some DAOs, e.g. {@link MultiRowJdbcPersonAttributeDao} cannot distinguish
     * between mulitple data attributes for case canonicalization purposes,
     * which invalidates some tests.
     *
     * @return
     */
    protected abstract boolean supportsPerDataAttributeCaseSensitivity();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        this.testDataSource = new SimpleDriverDataSource(new jdbcDriver(), "jdbc:hsqldb:mem:adhommemds", "sa", "");
        setUpSchema(testDataSource);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        tearDownSchema(this.testDataSource);
        this.testDataSource = null;
    }

    public void testCaseSensitiveUsernameQuery() {
        final AbstractJdbcPersonAttributeDao<Map<String, Object>> impl = newDao(testDataSource);
        impl.setUseAllQueryAttributes(false);
        final Map<String, Object> columnsToAttributes = new LinkedHashMap<String, Object>();
        columnsToAttributes.put("netid", "username");
        impl.setResultAttributeMapping(columnsToAttributes);
        final Map<String, Object> attributesToColumns = new LinkedHashMap<String, Object>();
        attributesToColumns.put("username", "netid");
        impl.setQueryAttributeMapping(attributesToColumns);

        final IPersonAttributes wrongCaseResult = impl.getPerson("AWP9");
        assertNull(wrongCaseResult);
        final IPersonAttributes correctCaseResult = impl.getPerson("awp9");
        assertNotNull(correctCaseResult);
        assertEquals("awp9", correctCaseResult.getName());
    }

    public void testCaseSensitiveUsernameQuery_CanonicalizedUsernameResult() {
        final AbstractJdbcPersonAttributeDao<Map<String, Object>> impl = newDao(testDataSource);
        impl.setUseAllQueryAttributes(false);
        final Map<String, Object> columnsToAttributes = new LinkedHashMap<String, Object>();
        columnsToAttributes.put("netid", "username");
        impl.setResultAttributeMapping(columnsToAttributes);
        final Map<String, Object> attributesToColumns = new LinkedHashMap<String, Object>();
        attributesToColumns.put("username", "netid");
        impl.setQueryAttributeMapping(attributesToColumns);
        // above was all boilerplate... here's the important stuff...
        impl.setUsernameCaseCanonicalizationMode(CaseCanonicalizationMode.UPPER);

        final IPersonAttributes wrongCaseResult = impl.getPerson("AWP9");
        assertNull(wrongCaseResult);
        final IPersonAttributes correctCaseResult = impl.getPerson("awp9");
        assertNotNull(correctCaseResult);
        assertEquals("AWP9", correctCaseResult.getName());
    }

    public void testCaseInsensitiveUsernameQuery() {
        final AbstractJdbcPersonAttributeDao<Map<String, Object>> impl = newDao(testDataSource);
        impl.setUseAllQueryAttributes(false);
        final Map<String, Object> columnsToAttributes = new LinkedHashMap<String, Object>();
        columnsToAttributes.put("netid", "username");
        impl.setResultAttributeMapping(columnsToAttributes);
        final Map<String, Object> attributesToColumns = new LinkedHashMap<String, Object>();
        attributesToColumns.put("username", "netid");
        impl.setQueryAttributeMapping(attributesToColumns);
        // above was all boilerplate... here's the important stuff...
        impl.setCaseInsensitiveQueryAttributesAsCollection(Util.genList("username"));

        final IPersonAttributes wrongCaseResult = impl.getPerson("AWP9");
        assertNotNull(wrongCaseResult);
        assertEquals("AWP9", wrongCaseResult.getName());
        // both casings should work
        final IPersonAttributes correctCaseResult = impl.getPerson("awp9");
        assertNotNull(correctCaseResult);
        assertEquals("awp9", correctCaseResult.getName());

    }

    public void testCaseInsensitiveUsernameQuery_CanonicalizedUsernameResult() {
        final AbstractJdbcPersonAttributeDao<Map<String, Object>> impl = newDao(testDataSource);
        impl.setUseAllQueryAttributes(false);
        final Map<String, Object> columnsToAttributes = new LinkedHashMap<String, Object>();
        columnsToAttributes.put("netid", "username");
        impl.setResultAttributeMapping(columnsToAttributes);
        final Map<String, Object> attributesToColumns = new LinkedHashMap<String, Object>();
        attributesToColumns.put("username", "netid");
        impl.setQueryAttributeMapping(attributesToColumns);
        // above was all boilerplate... here's the important stuff...
        impl.setCaseInsensitiveQueryAttributesAsCollection(Util.genList("username"));

        // username is a weird edge case... here you'd normally get the
        // casing from the value passed in to getPerson(); we're just proving
        // it can be coerced to an arbitrary casing in the mapped result.
        impl.setUsernameCaseCanonicalizationMode(CaseCanonicalizationMode.LOWER);
        final IPersonAttributes wrongCaseResult1 = impl.getPerson("AWP9");
        assertNotNull(wrongCaseResult1);
        assertEquals("awp9", wrongCaseResult1.getName());

        // and now show we can go the other way too
        impl.setUsernameCaseCanonicalizationMode(CaseCanonicalizationMode.UPPER);
        final IPersonAttributes wrongCaseResult2 = impl.getPerson("AwP9");
        assertNotNull(wrongCaseResult2);
        assertEquals("AWP9", wrongCaseResult2.getName());

    }

    public void testCaseSensitiveNonUsernameAttributeQuery() {
        final AbstractJdbcPersonAttributeDao<Map<String, Object>> impl = newDao(testDataSource);
        impl.setUseAllQueryAttributes(false);
        final Map<String, Object> columnsToAttributes = new LinkedHashMap<String, Object>();
        columnsToAttributes.put("netid", "username");
        columnsToAttributes.put("name", "firstName");
        impl.setResultAttributeMapping(columnsToAttributes);
        final Map<String, Object> attributesToColumns = new LinkedHashMap<String, Object>();
        attributesToColumns.put("username", "netid");
        attributesToColumns.put("firstName", "name");
        impl.setQueryAttributeMapping(attributesToColumns);
        beforeNonUsernameQuery(impl);

        Map<String,Object> wrongCase = new LinkedHashMap<String, Object>();
        wrongCase.put("firstName", "ANDREW");
        final Set<IPersonAttributes> wrongCaseResult = impl.getPeople(wrongCase);
        assertEquals(0, wrongCaseResult.size());

        Map<String,Object> correctCase = new LinkedHashMap<String, Object>();
        correctCase.put("firstName", "Andrew");
        final Set<IPersonAttributes> correctCaseResult = impl.getPeople(correctCase);
        assertEquals(2, correctCaseResult.size());
        Iterator<IPersonAttributes> correctCaseResultIterator = correctCaseResult.iterator();
        IPersonAttributes currentResult = correctCaseResultIterator.next();
        assertEquals("awp9", currentResult.getName());
        // make sure it preserved data-layer casing
        assertEquals("Andrew", currentResult.getAttributeValue("firstName"));
        currentResult = correctCaseResultIterator.next();
        assertEquals("atest", currentResult.getName());
        // make sure it preserved data-layer casing
        assertEquals("Andrew", currentResult.getAttributeValue("firstName"));
    }

    public void testCaseSensitiveNonUsernameAttributeQuery_CanonicalizedResult() {
        final AbstractJdbcPersonAttributeDao<Map<String, Object>> impl = newDao(testDataSource);
        impl.setUseAllQueryAttributes(false);
        final Map<String, Object> columnsToAttributes = new LinkedHashMap<String, Object>();
        columnsToAttributes.put("netid", "username");
        columnsToAttributes.put("name", "firstName");
        impl.setResultAttributeMapping(columnsToAttributes);
        final Map<String, Object> attributesToColumns = new LinkedHashMap<String, Object>();
        attributesToColumns.put("username", "netid");
        attributesToColumns.put("firstName", "name");
        impl.setQueryAttributeMapping(attributesToColumns);
        impl.setCaseInsensitiveResultAttributesAsCollection(Util.genList("firstName"));
        beforeNonUsernameQuery(impl);

        Map<String,Object> wrongCase = new LinkedHashMap<String, Object>();
        wrongCase.put("firstName", "ANDREW");
        final Set<IPersonAttributes> wrongCaseResult = impl.getPeople(wrongCase);
        assertEquals(0, wrongCaseResult.size());

        Map<String,Object> correctCase = new LinkedHashMap<String, Object>();
        correctCase.put("firstName", "Andrew");
        final Set<IPersonAttributes> correctCaseResult = impl.getPeople(correctCase);
        assertEquals(2, correctCaseResult.size());
        Iterator<IPersonAttributes> correctCaseResultIterator = correctCaseResult.iterator();
        IPersonAttributes currentResult = correctCaseResultIterator.next();
        assertEquals("awp9", currentResult.getName());
        // make sure it overrode data-layer casing
        assertEquals("andrew", currentResult.getAttributeValue("firstName"));
        currentResult = correctCaseResultIterator.next();
        assertEquals("atest", currentResult.getName());
        // make sure it overrode data-layer casing
        assertEquals("andrew", currentResult.getAttributeValue("firstName"));
    }

    public void testCaseInsensitiveNonUsernameAttributeQuery() {
        final AbstractJdbcPersonAttributeDao<Map<String, Object>> impl = newDao(testDataSource);
        impl.setUseAllQueryAttributes(false);
        final Map<String, Object> columnsToAttributes = new LinkedHashMap<String, Object>();
        columnsToAttributes.put("netid", "username");
        columnsToAttributes.put("name", "firstName");
        impl.setResultAttributeMapping(columnsToAttributes);
        final Map<String, Object> attributesToColumns = new LinkedHashMap<String, Object>();
        attributesToColumns.put("username", "netid");
        attributesToColumns.put("firstName", "name");
        impl.setQueryAttributeMapping(attributesToColumns);
        // above was all boilerplate... here's the important stuff...
        // intentionally not setting "name" in the
        // caseInsensitiveResultAttributes to verify that we have the option
        // of preserving data-layer casing when mapping values out, even if
        // the original query on that field was case-insensitive
        impl.setCaseInsensitiveQueryAttributesAsCollection(Util.genList("firstName"));
        impl.setCaseInsensitiveDataAttributesAsCollection(Util.genList("name"));
        beforeNonUsernameQuery(impl);

        Map<String,Object> wrongCase = new LinkedHashMap<String, Object>();
        wrongCase.put("firstName", "ANDREW");
        final Set<IPersonAttributes> wrongCaseResult = impl.getPeople(wrongCase);
        assertEquals(2, wrongCaseResult.size());
        Iterator<IPersonAttributes> resultIterator = wrongCaseResult.iterator();
        IPersonAttributes currentResult = resultIterator.next();
        assertEquals("awp9", currentResult.getName());
        // make sure it preserved data-layer casing
        assertEquals("Andrew", currentResult.getAttributeValue("firstName"));
        currentResult = resultIterator.next();
        assertEquals("atest", currentResult.getName());
        // make sure it preserved data-layer casing
        assertEquals("Andrew", currentResult.getAttributeValue("firstName"));

        Map<String,Object> correctCase = new LinkedHashMap<String, Object>();
        correctCase.put("firstName", "Andrew");
        final Set<IPersonAttributes> correctCaseResult = impl.getPeople(correctCase);
        assertEquals(2, correctCaseResult.size());
        resultIterator = correctCaseResult.iterator();
        currentResult = resultIterator.next();
        assertEquals("awp9", currentResult.getName());
        // make sure it preserved data-layer casing
        assertEquals("Andrew", currentResult.getAttributeValue("firstName"));
        currentResult = resultIterator.next();
        assertEquals("atest", currentResult.getName());
        // make sure it preserved data-layer casing
        assertEquals("Andrew", currentResult.getAttributeValue("firstName"));
    }

    public void testCaseInsensitiveNonUsernameAttributeQuery_CanonicalizedResult() {
        final AbstractJdbcPersonAttributeDao<Map<String, Object>> impl = newDao(testDataSource);
        impl.setUseAllQueryAttributes(false);
        final Map<String, Object> columnsToAttributes = new LinkedHashMap<String, Object>();
        columnsToAttributes.put("netid", "username");
        columnsToAttributes.put("name", "firstName");
        impl.setResultAttributeMapping(columnsToAttributes);
        final Map<String, Object> attributesToColumns = new LinkedHashMap<String, Object>();
        attributesToColumns.put("username", "netid");
        attributesToColumns.put("firstName", "name");
        impl.setQueryAttributeMapping(attributesToColumns);
        // above was all boilerplate... here's the important stuff...
        // (actually same as non-_CanonicalizedResult except we do configure
        // a case-insensitive result query)
        impl.setCaseInsensitiveQueryAttributesAsCollection(Util.genList("firstName"));
        impl.setCaseInsensitiveDataAttributesAsCollection(Util.genList("name"));
        impl.setCaseInsensitiveResultAttributesAsCollection(Util.genList("firstName"));
        beforeNonUsernameQuery(impl);

        Map<String,Object> wrongCase = new LinkedHashMap<String, Object>();
        wrongCase.put("firstName", "ANDREW");
        final Set<IPersonAttributes> wrongCaseResult = impl.getPeople(wrongCase);
        assertEquals(2, wrongCaseResult.size());
        Iterator<IPersonAttributes> resultIterator = wrongCaseResult.iterator();
        IPersonAttributes currentResult = resultIterator.next();
        assertEquals("awp9", currentResult.getName());
        // make sure it overrode data-layer casing
        assertEquals("andrew", currentResult.getAttributeValue("firstName"));
        currentResult = resultIterator.next();
        assertEquals("atest", currentResult.getName());
        // make sure it overrode data-layer casing
        assertEquals("andrew", currentResult.getAttributeValue("firstName"));

        Map<String,Object> correctCase = new LinkedHashMap<String, Object>();
        correctCase.put("firstName", "Andrew");
        final Set<IPersonAttributes> correctCaseResult = impl.getPeople(correctCase);
        assertEquals(2, correctCaseResult.size());
        resultIterator = correctCaseResult.iterator();
        currentResult = resultIterator.next();
        assertEquals("awp9", currentResult.getName());
        // make sure it overrode data-layer casing
        assertEquals("andrew", currentResult.getAttributeValue("firstName"));
        currentResult = resultIterator.next();
        assertEquals("atest", currentResult.getName());
        // make sure it overrode data-layer casing
        assertEquals("andrew", currentResult.getAttributeValue("firstName"));
    }

    // Guards against a bug discovered in the original SSP-1668/PERSONDIR-74
    // patch where setting any caseInsensitiveDataAttributes config would
    // cause all data attributes to be canonicalized
    public void testCaseSensitiveNonUsernameAttributeQuery_OtherCaseInsensitiveDataAttributes() {
        if ( !(supportsPerDataAttributeCaseSensitivity()) ) {
            // Some DAOs, e.g. MultiRowJdbcPersonAttributeDao cannot distinguish
            // between mulitple data attributes for case canonicalization purposes.
            return;
        }
        final AbstractJdbcPersonAttributeDao<Map<String, Object>> impl = newDao(testDataSource);
        impl.setUseAllQueryAttributes(false);
        final Map<String, Object> columnsToAttributes = new LinkedHashMap<String, Object>();
        columnsToAttributes.put("netid", "username");
        columnsToAttributes.put("name", "firstName");
        columnsToAttributes.put("email", "emailAddr");
        impl.setResultAttributeMapping(columnsToAttributes);
        final Map<String, Object> attributesToColumns = new LinkedHashMap<String, Object>();
        attributesToColumns.put("username", "netid");
        attributesToColumns.put("firstName", "name");
        attributesToColumns.put("emailAddr", "email");
        impl.setQueryAttributeMapping(attributesToColumns);
        impl.setCaseInsensitiveDataAttributesAsCollection(Util.genList("email"));
        beforeNonUsernameQuery(impl);

        Map<String,Object> wrongCase = new LinkedHashMap<String, Object>();
        wrongCase.put("firstName", "ANDREW");
        final Set<IPersonAttributes> wrongCaseResult = impl.getPeople(wrongCase);
        assertEquals(0, wrongCaseResult.size());

        Map<String,Object> correctCase = new LinkedHashMap<String, Object>();
        correctCase.put("firstName", "Andrew");
        final Set<IPersonAttributes> correctCaseResult = impl.getPeople(correctCase);
        assertEquals(2, correctCaseResult.size());
        Iterator<IPersonAttributes> correctCaseResultIterator = correctCaseResult.iterator();
        IPersonAttributes currentResult = correctCaseResultIterator.next();
        assertEquals("awp9", currentResult.getName());
        // make sure it preserved data-layer casing
        assertEquals("Andrew", currentResult.getAttributeValue("firstName"));
        currentResult = correctCaseResultIterator.next();
        assertEquals("atest", currentResult.getName());
        // make sure it preserved data-layer casing
        assertEquals("Andrew", currentResult.getAttributeValue("firstName"));
    }

    public void testConfiguredDoubleEndedWildcards_NonUsernameQuery() {
        final AbstractJdbcPersonAttributeDao<Map<String, Object>> impl = newDao(testDataSource);
        impl.setUseAllQueryAttributes(false);
        final Map<String, Object> columnsToAttributes = new LinkedHashMap<String, Object>();
        columnsToAttributes.put("netid", "username");
        columnsToAttributes.put("name", "firstName");
        impl.setResultAttributeMapping(columnsToAttributes);
        final Map<String, Object> attributesToColumns = new LinkedHashMap<String, Object>();
        attributesToColumns.put("username", "netid");
        attributesToColumns.put("firstName", "name");
        impl.setQueryAttributeMapping(attributesToColumns);
        impl.setWildcardDataAttributes(true); // key config difference
        impl.setUsernameDataAttribute("netid"); // key config point (without this, doesn't know when it's
                                                // looking at a username search term (specifically, will revert to
                                                // using the default app-layer username attribute, which
                                                // is almost always pointless when processing data-layer attributes
                                                // (which is when these wildcards are applied))
        beforeNonUsernameQuery(impl);


        Map<String,Object> partialMatchQuery = new LinkedHashMap<String, Object>();
        partialMatchQuery.put("firstName", "ndre"); // missing letters on both ends
        final Set<IPersonAttributes> result = impl.getPeople(partialMatchQuery);
        assertEquals(2, result.size());
        Iterator<IPersonAttributes> partialMatchResultIterator = result.iterator();
        IPersonAttributes currentResult = partialMatchResultIterator.next();
        assertEquals("awp9", currentResult.getName());
        assertEquals("Andrew", currentResult.getAttributeValue("firstName"));
        currentResult = partialMatchResultIterator.next();
        assertEquals("atest", currentResult.getName());
        assertEquals("Andrew", currentResult.getAttributeValue("firstName"));
    }

    public void testConfiguredDoubleEndedWildcards_NonUsernameQuery_ExcludedAttribute() {
        final AbstractJdbcPersonAttributeDao<Map<String, Object>> impl = newDao(testDataSource);
        impl.setUseAllQueryAttributes(false);
        final Map<String, Object> columnsToAttributes = new LinkedHashMap<String, Object>();
        columnsToAttributes.put("netid", "username");
        columnsToAttributes.put("name", "firstName");
        impl.setResultAttributeMapping(columnsToAttributes);
        final Map<String, Object> attributesToColumns = new LinkedHashMap<String, Object>();
        attributesToColumns.put("username", "netid");
        attributesToColumns.put("firstName", "name");
        impl.setQueryAttributeMapping(attributesToColumns);
        impl.setWildcardDataAttributes(true); // key config difference
        impl.setUsernameDataAttribute("netid"); // key config point (without this, doesn't know when it's
                                                // looking at a username search term (specifically, will revert to
                                                // using the default app-layer username attribute, which
                                                // is almost always pointless when processing data-layer attributes
                                                // (which is when these wildcards are applied))
        impl.setWildcardedDataAttributeExclusions(Collections.singleton("name"));
        beforeNonUsernameQuery(impl);


        // This test really only makes sense when paired with
        // testConfiguredDoubleEndedWildcards_NonUsernameQuery(), which verifies
        // that you *do* get results when you haven't excluded firstName from
        // wildcarding
        Map<String,Object> partialMatchQuery = new LinkedHashMap<String, Object>();
        partialMatchQuery.put("firstName", "ndre"); // missing letters on both ends
        final Set<IPersonAttributes> result = impl.getPeople(partialMatchQuery);
        assertEquals(0, result.size());
    }

    public void testSingleUserSpecifiedWildcard_DoubleEndedWildcardsEnabled() {
        final AbstractJdbcPersonAttributeDao<Map<String, Object>> impl = newDao(testDataSource);
        impl.setUseAllQueryAttributes(false);
        final Map<String, Object> columnsToAttributes = new LinkedHashMap<String, Object>();
        columnsToAttributes.put("netid", "username");
        columnsToAttributes.put("name", "firstName");
        impl.setResultAttributeMapping(columnsToAttributes);
        final Map<String, Object> attributesToColumns = new LinkedHashMap<String, Object>();
        attributesToColumns.put("username", "netid");
        attributesToColumns.put("firstName", "name");
        impl.setQueryAttributeMapping(attributesToColumns);
        impl.setWildcardDataAttributes(true); // key config point
        impl.setUsernameDataAttribute("netid"); // key config point (without this, doesn't know when it's
                                                // looking at a username search term (specifically, will revert to
                                                // using the default app-layer username attribute, which
                                                // is almost always pointless when processing data-layer attributes
                                                // (which is when these wildcards are applied))
        beforeNonUsernameQuery(impl);


        Map<String,Object> internalWildcardQuery = new LinkedHashMap<String, Object>();
        internalWildcardQuery.put("firstName", "An*rew");
        Set<IPersonAttributes> result = impl.getPeople(internalWildcardQuery);
        assertEquals(2, result.size());
        Iterator<IPersonAttributes> resultIterator = result.iterator();
        IPersonAttributes currentResult = resultIterator.next();
        assertEquals("awp9", currentResult.getName());
        assertEquals("Andrew", currentResult.getAttributeValue("firstName"));
        currentResult = resultIterator.next();
        assertEquals("atest", currentResult.getName());
        assertEquals("Andrew", currentResult.getAttributeValue("firstName"));

        // now make sure the app isn't sneaking wildcards onto the end, even tho
        // that feature is enabled (the user-specified wildcards should take
        // precedence)
        Map<String,Object> missingCharsQuery = new LinkedHashMap<String, Object>();
        missingCharsQuery.put("firstName", "n*re");
        result = impl.getPeople(missingCharsQuery);
        assertEquals(0, result.size());
    }

    public void testSingleUserSpecifiedWildcard_DoubleEndedWildcardsDisabled() {
        final AbstractJdbcPersonAttributeDao<Map<String, Object>> impl = newDao(testDataSource);
        impl.setUseAllQueryAttributes(false);
        final Map<String, Object> columnsToAttributes = new LinkedHashMap<String, Object>();
        columnsToAttributes.put("netid", "username");
        columnsToAttributes.put("name", "firstName");
        impl.setResultAttributeMapping(columnsToAttributes);
        final Map<String, Object> attributesToColumns = new LinkedHashMap<String, Object>();
        attributesToColumns.put("username", "netid");
        attributesToColumns.put("firstName", "name");
        impl.setQueryAttributeMapping(attributesToColumns);
        impl.setWildcardDataAttributes(false); // key config difference
        impl.setUsernameDataAttribute("netid"); // key config point (without this, doesn't know when it's
                                                // looking at a username search term (specifically, will revert to
                                                // using the default app-layer username attribute, which
                                                // is almost always pointless when processing data-layer attributes
                                                // (which is when these wildcards are applied))
        beforeNonUsernameQuery(impl);


        Map<String,Object> internalWildcardQuery = new LinkedHashMap<String, Object>();
        internalWildcardQuery.put("firstName", "An*rew");
        Set<IPersonAttributes> result = impl.getPeople(internalWildcardQuery);
        assertEquals(2, result.size());
        Iterator<IPersonAttributes> resultIterator = result.iterator();
        IPersonAttributes currentResult = resultIterator.next();
        assertEquals("awp9", currentResult.getName());
        assertEquals("Andrew", currentResult.getAttributeValue("firstName"));
        currentResult = resultIterator.next();
        assertEquals("atest", currentResult.getName());
        assertEquals("Andrew", currentResult.getAttributeValue("firstName"));

        // now make sure the app isn't sneaking wildcards onto the end
        Map<String,Object> missingCharsQuery = new LinkedHashMap<String, Object>();
        missingCharsQuery.put("firstName", "n*re");
        result = impl.getPeople(missingCharsQuery);
        assertEquals(0, result.size());
    }

    public void testMulitpleUserSpecifiedWildcards_DoubleEndedWildcardsEnabled() {
        final AbstractJdbcPersonAttributeDao<Map<String, Object>> impl = newDao(testDataSource);
        impl.setUseAllQueryAttributes(false);
        final Map<String, Object> columnsToAttributes = new LinkedHashMap<String, Object>();
        columnsToAttributes.put("netid", "username");
        columnsToAttributes.put("name", "firstName");
        impl.setResultAttributeMapping(columnsToAttributes);
        final Map<String, Object> attributesToColumns = new LinkedHashMap<String, Object>();
        attributesToColumns.put("username", "netid");
        attributesToColumns.put("firstName", "name");
        impl.setQueryAttributeMapping(attributesToColumns);
        impl.setWildcardDataAttributes(true); // key config point
        impl.setUsernameDataAttribute("netid"); // key config point (without this, doesn't know when it's
                                                // looking at a username search term (specifically, will revert to
                                                // using the default app-layer username attribute, which
                                                // is almost always pointless when processing data-layer attributes
                                                // (which is when these wildcards are applied))
        beforeNonUsernameQuery(impl);


        Map<String,Object> internalWildcardQuery = new LinkedHashMap<String, Object>();
        internalWildcardQuery.put("firstName", "An*r*w");
        final Set<IPersonAttributes> result = impl.getPeople(internalWildcardQuery);
        assertEquals(2, result.size());
        Iterator<IPersonAttributes> resultIterator = result.iterator();
        IPersonAttributes currentResult = resultIterator.next();
        assertEquals("awp9", currentResult.getName());
        assertEquals("Andrew", currentResult.getAttributeValue("firstName"));
        currentResult = resultIterator.next();
        assertEquals("atest", currentResult.getName());
        assertEquals("Andrew", currentResult.getAttributeValue("firstName"));

    }

    public void testMulitpleUserSpecifiedWildcards_DoubleEndedWildcardsDisabled() {
        final AbstractJdbcPersonAttributeDao<Map<String, Object>> impl = newDao(testDataSource);
        impl.setUseAllQueryAttributes(false);
        final Map<String, Object> columnsToAttributes = new LinkedHashMap<String, Object>();
        columnsToAttributes.put("netid", "username");
        columnsToAttributes.put("name", "firstName");
        impl.setResultAttributeMapping(columnsToAttributes);
        final Map<String, Object> attributesToColumns = new LinkedHashMap<String, Object>();
        attributesToColumns.put("username", "netid");
        attributesToColumns.put("firstName", "name");
        impl.setQueryAttributeMapping(attributesToColumns);
        impl.setWildcardDataAttributes(false); // key config point (but just reasserts the default)
        impl.setUsernameDataAttribute("netid"); // key config point (without this, doesn't know when it's
                                                // looking at a username search term (specifically, will revert to
                                                // using the default app-layer username attribute, which
                                                // is almost always pointless when processing data-layer attributes
                                                // (which is when these wildcards are applied))
        beforeNonUsernameQuery(impl);


        Map<String,Object> internalWildcardQuery = new LinkedHashMap<String, Object>();
        internalWildcardQuery.put("firstName", "An*r*w");
        final Set<IPersonAttributes> result = impl.getPeople(internalWildcardQuery);
        assertEquals(2, result.size());
        Iterator<IPersonAttributes> resultIterator = result.iterator();
        IPersonAttributes currentResult = resultIterator.next();
        assertEquals("awp9", currentResult.getName());
        assertEquals("Andrew", currentResult.getAttributeValue("firstName"));
        currentResult = resultIterator.next();
        assertEquals("atest", currentResult.getName());
        assertEquals("Andrew", currentResult.getAttributeValue("firstName"));

    }

    public void testUserSpecifiedUsernameWildcards_UsernameWildcardsEnabled_DoubleEndedWildcardsEnabled() {
        final AbstractJdbcPersonAttributeDao<Map<String, Object>> impl = newDao(testDataSource);
        impl.setUseAllQueryAttributes(false);
        final Map<String, Object> columnsToAttributes = new LinkedHashMap<String, Object>();
        columnsToAttributes.put("netid", "username");
        impl.setResultAttributeMapping(columnsToAttributes);
        final Map<String, Object> attributesToColumns = new LinkedHashMap<String, Object>();
        attributesToColumns.put("username", "netid");
        impl.setQueryAttributeMapping(attributesToColumns);
        impl.setWildcardDataAttributes(true); // key config point
        impl.setAllowUsernameWildcards(true); // key config point (but just reasserts the default)
        impl.setUsernameDataAttribute("netid"); // key config point (without this, doesn't know when it's
                                                // looking at a username search term (specifically, will revert to
                                                // using the default app-layer username attribute, which
                                                // is almost always pointless when processing data-layer attributes
                                                // (which is when these wildcards are applied))

        IPersonAttributes result = impl.getPerson("aw*9");
        assertNotNull(result);
        assertEquals("awp9", result.getName());

        // make sure it's not just wrapping wildcards around the whole thing
        result = impl.getPerson("w*");
        assertNull(result);
    }

    public void testUserSpecifiedUsernameWildcards_UsernameWildcardsEnabled_DoubleEndedWildcardsDisabled() {
        final AbstractJdbcPersonAttributeDao<Map<String, Object>> impl = newDao(testDataSource);
        impl.setUseAllQueryAttributes(false);
        final Map<String, Object> columnsToAttributes = new LinkedHashMap<String, Object>();
        columnsToAttributes.put("netid", "username");
        impl.setResultAttributeMapping(columnsToAttributes);
        final Map<String, Object> attributesToColumns = new LinkedHashMap<String, Object>();
        attributesToColumns.put("username", "netid");
        impl.setQueryAttributeMapping(attributesToColumns);
        impl.setWildcardDataAttributes(false); // key config point (but just reasserts the default)
        impl.setAllowUsernameWildcards(true); // key config point (but just reasserts the default)
        impl.setUsernameDataAttribute("netid"); // key config point (without this, doesn't know when it's
                                                // looking at a username search term (specifically, will revert to
                                                // using the default app-layer username attribute, which
                                                // is almost always pointless when processing data-layer attributes
                                                // (which is when these wildcards are applied))

        IPersonAttributes result = impl.getPerson("aw*9");
        assertNotNull(result);
        assertEquals("awp9", result.getName());

        // make sure it's not just wrapping wildcards around the whole thing
        result = impl.getPerson("w*");
        assertNull(result);
    }

    public void testUserSpecifiedUsernameWildcards_UsernameWildcardsDisabled_DoubleEndedWildcardsEnabled() {
        final AbstractJdbcPersonAttributeDao<Map<String, Object>> impl = newDao(testDataSource);
        impl.setUseAllQueryAttributes(false);
        final Map<String, Object> columnsToAttributes = new LinkedHashMap<String, Object>();
        columnsToAttributes.put("netid", "username");
        impl.setResultAttributeMapping(columnsToAttributes);
        final Map<String, Object> attributesToColumns = new LinkedHashMap<String, Object>();
        attributesToColumns.put("username", "netid");
        impl.setQueryAttributeMapping(attributesToColumns);
        impl.setWildcardDataAttributes(true); // key config point
        impl.setAllowUsernameWildcards(false); // key config point
        impl.setUsernameDataAttribute("netid"); // key config point (without this, doesn't know when it's
                                                // looking at a username search term (specifically, will revert to
                                                // using the default app-layer username attribute, which
                                                // is almost always pointless when processing data-layer attributes
                                                // (which is when these wildcards are applied))

        IPersonAttributes result = impl.getPerson("aw*9");
        assertNull(result);
    }

    public void testUserSpecifiedUsernameWildcards_UsernameWildcardsDisabled_DoubleEndedWildcardsDisabled() {
        final AbstractJdbcPersonAttributeDao<Map<String, Object>> impl = newDao(testDataSource);
        impl.setUseAllQueryAttributes(false);
        final Map<String, Object> columnsToAttributes = new LinkedHashMap<String, Object>();
        columnsToAttributes.put("netid", "username");
        impl.setResultAttributeMapping(columnsToAttributes);
        final Map<String, Object> attributesToColumns = new LinkedHashMap<String, Object>();
        attributesToColumns.put("username", "netid");
        impl.setQueryAttributeMapping(attributesToColumns);
        impl.setWildcardDataAttributes(false); // key config point (but just reasserts the default)
        impl.setAllowUsernameWildcards(false); // key config point
        impl.setUsernameDataAttribute("netid"); // key config point (without this, doesn't know when it's
                                                // looking at a username search term (specifically, will revert to
                                                // using the default app-layer username attribute, which
                                                // is almost always pointless when processing data-layer attributes
                                                // (which is when these wildcards are applied))

        IPersonAttributes result = impl.getPerson("aw*9");
        assertNull(result);
    }

    public void testPartialInternalUsernameMatch_DoubleEndedWildcardsEnabled() {
        final AbstractJdbcPersonAttributeDao<Map<String, Object>> impl = newDao(testDataSource);
        impl.setUseAllQueryAttributes(false);
        final Map<String, Object> columnsToAttributes = new LinkedHashMap<String, Object>();
        columnsToAttributes.put("netid", "username");
        impl.setResultAttributeMapping(columnsToAttributes);
        final Map<String, Object> attributesToColumns = new LinkedHashMap<String, Object>();
        attributesToColumns.put("username", "netid");
        impl.setQueryAttributeMapping(attributesToColumns);
        impl.setWildcardDataAttributes(true); // key config point
        impl.setUsernameDataAttribute("netid"); // key config point (without this, doesn't know when it's
                                                // looking at a username search term (specifically, will revert to
                                                // using the default app-layer username attribute, which
                                                // is almost always pointless when processing data-layer attributes
                                                // (which is when these wildcards are applied))

        final IPersonAttributes result = impl.getPerson("wp");
        assertNull(result); // double-ended wildcarding config *never* applies to usernames,
                            // even if no user-specified wildcards present in search term
    }




}
