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
package org.jasig.services.persondir.support.merger;


import org.jasig.services.persondir.util.Util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NonDuplicatingMultivaluedAttributeMergerTest extends AbstractAttributeMergerTest {

    private NonDuplicatingMultivaluedAttributeMerger merger = new NonDuplicatingMultivaluedAttributeMerger();

    /**
     * Test identity of adding an empty map. (Copy of
     * {@link org.jasig.services.persondir.support.merger.MultivaluedAttributeMergerTest#testAddEmpty()})
     */
    public void testAddEmpty() {
        Map<String, List<Object>> someAttributes = new HashMap<String, List<Object>>();
        someAttributes.put("attName", Util.list("attValue"));
        someAttributes.put("attName2", Util.list("attValue2"));

        Map<String, List<Object>> expected = new HashMap<String, List<Object>>();
        expected.putAll(someAttributes);

        Map<String, List<Object>> result = getAttributeMerger().mergeAttributes(someAttributes, new HashMap<String, List<Object>>());

        assertEquals(expected, result);
    }

    /**
     * Test a simple case of adding one map of attributes to another, with
     * no collisions. (Copy of
     * {@link org.jasig.services.persondir.support.merger.MultivaluedAttributeMergerTest#testAddNoncolliding()})
     */
    public void testAddNoncolliding() {
        Map<String, List<Object>> someAttributes = new HashMap<String, List<Object>>();
        someAttributes.put("attName", Util.list("attValue"));
        someAttributes.put("attName2", Util.list("attValue2"));

        Map<String, List<Object>> otherAttributes = new HashMap<String, List<Object>>();
        otherAttributes.put("attName3", Util.list("attValue3"));
        otherAttributes.put("attName4", Util.list("attValue4"));

        Map<String, List<Object>> expected = new HashMap<String, List<Object>>();
        expected.putAll(someAttributes);
        expected.putAll(otherAttributes);

        Map<String, List<Object>> result = getAttributeMerger().mergeAttributes(someAttributes, otherAttributes);
        assertEquals(expected, result);
    }

    /**
     * Test that colliding attributes are not added. (Mostly a copy of
     * {@link org.jasig.services.persondir.support.merger.MultivaluedAttributeMergerTest#testColliding()} with a
     * more granular assertion design and additional data scenarios.)
     */
    public void testColliding_FullyDeduplicating() {

        ((NonDuplicatingMultivaluedAttributeMerger)getAttributeMerger())
                .setDeduplicationMode(NonDuplicatingMultivaluedAttributeMerger.DeduplicationMode.ALL_TARGET_ATTRIBUTES);

        final Map<String, List<Object>> someAttributes = standardCollisionTestTargetAttributes();
        final Map<String, List<Object>> otherAttributes = standardCollisionTestSourceAttributes();

        // More granular assertions than in MultivaluedAttributeMergerTest to try to make it easier to parse
        // out the offending entries
        Map<String, List<Object>> result = getAttributeMerger().mergeAttributes(someAttributes, otherAttributes);
        assertEquals(Util.list((Object)null), result.get("attName1"));
        assertEquals(Util.list("attValue2"), result.get("attName2"));
        assertEquals(Util.list((Object)null), result.get("attName3"));
        assertEquals(Util.list("attValue4"), result.get("attName4"));
        assertEquals(Util.list((Object)null), result.get("attName5"));
        assertEquals(Util.list(null, "attValue6"), result.get("attName6"));
        assertEquals(Util.list("attValue7", null), result.get("attName7"));
        assertEquals(Util.list("attValue8.1", "attValue8.2"), result.get("attName8"));
        assertEquals(Util.list(null, "attValue9.1", "attValue9.2"), result.get("attName9"));
        assertEquals(Util.list("attValue10", "attValue10.1", "attValue10.2"), result.get("attName10"));
        assertEquals(Util.list("attValue11.1", "attValue11.2", null), result.get("attName11"));
        assertEquals(Util.list("attValue12.1", "attValue12.2", "attValue12"), result.get("attName12"));
        assertEquals(Util.list("attValue13.1.1", "attValue13.1.2", "attValue13.2.1", "attValue13.2.2"), result.get("attName13"));

        // now the departures from MultivaluedAttributeMergerTest
        assertEquals(Util.list(null), result.get("attName14")); // colliding, single-valued, duplicate null
        assertEquals(Util.list(null), result.get("attName15")); // colliding, multi-valued, duplicate nulls
        // 15.1 added as after thought. Easier to just break the naming scheme that rename everything that follows
        assertEquals(Util.list("attValue15.1"), result.get("attName15.1")); // colliding, multi-valued, duplicate nulls
        assertEquals(Util.list("attValue16"), result.get("attName16")); // colliding, duplicate non-null
        assertEquals(Util.list("attValue17.1","attValue17.2"), result.get("attName17")); // colliding, duplicate non-nulls, same order
        assertEquals(Util.list("attValue18.1","attValue18.2"), result.get("attName18")); // colliding, duplicate non-nulls, different order
        assertEquals(Util.list("attValue19.1",null), result.get("attName19")); // colliding, duplicate null and non-null, same order
        assertEquals(Util.list("attValue20.1",null), result.get("attName20")); // colliding, duplicate null and non-null, different order
        assertEquals(Util.list(null), result.get("attName21")); // non-colliding, only in target, multi-valued, duplicate nulls
        assertEquals(Util.list("attValue22"), result.get("attName22")); // non-colliding, only in target, multi-valued, duplicate non-nulls
        assertEquals(Util.list(null), result.get("attName23")); // non-colliding, only in source, multi-valued, duplicate nulls
        assertEquals(Util.list("attValue24"), result.get("attName24")); // non-colliding, only in source, multi-valued, duplicate non-nulls

    }

    /**
     * Same as {@link #testColliding_FullyDeduplicating()} but with the merger configured to only deduplicate
     * values in the <em>source</em> attributes, not in the <em>target</em>.
     *
     * @see org.jasig.services.persondir.support.merger.NonDuplicatingMultivaluedAttributeMerger.DeduplicationMode#COLLIDING_TARGET_ATTRIBUTES
     * @see NonDuplicatingMultivaluedAttributeMerger#setDeduplicationMode(org.jasig.services.persondir.support.merger.NonDuplicatingMultivaluedAttributeMerger.DeduplicationMode)
     */
    public void testColliding_CollidingTargetsDeduplicating() {

        ((NonDuplicatingMultivaluedAttributeMerger)getAttributeMerger())
                .setDeduplicationMode(NonDuplicatingMultivaluedAttributeMerger.DeduplicationMode.COLLIDING_TARGET_ATTRIBUTES);

        final Map<String, List<Object>> someAttributes = standardCollisionTestTargetAttributes();
        final Map<String, List<Object>> otherAttributes = standardCollisionTestSourceAttributes();

        // More granular assertions than in MultivaluedAttributeMergerTest to try to make it easier to parse
        // out the offending entries
        Map<String, List<Object>> result = getAttributeMerger().mergeAttributes(someAttributes, otherAttributes);
        assertEquals(Util.list((Object)null), result.get("attName1"));
        assertEquals(Util.list("attValue2"), result.get("attName2"));
        assertEquals(Util.list((Object)null), result.get("attName3"));
        assertEquals(Util.list("attValue4"), result.get("attName4"));
        assertEquals(Util.list((Object)null), result.get("attName5"));
        assertEquals(Util.list(null, "attValue6"), result.get("attName6"));
        assertEquals(Util.list("attValue7", null), result.get("attName7"));
        assertEquals(Util.list("attValue8.1", "attValue8.2"), result.get("attName8"));
        assertEquals(Util.list(null, "attValue9.1", "attValue9.2"), result.get("attName9"));
        assertEquals(Util.list("attValue10", "attValue10.1", "attValue10.2"), result.get("attName10"));
        assertEquals(Util.list("attValue11.1", "attValue11.2", null), result.get("attName11"));
        assertEquals(Util.list("attValue12.1", "attValue12.2", "attValue12"), result.get("attName12"));
        assertEquals(Util.list("attValue13.1.1", "attValue13.1.2", "attValue13.2.1", "attValue13.2.2"), result.get("attName13"));

        // now the departures from MultivaluedAttributeMergerTest
        assertEquals(Util.list(null), result.get("attName14")); // colliding, single-valued, duplicate null
        assertEquals(Util.list(null), result.get("attName15")); // colliding, multi-valued, duplicate nulls
        // 15.1 added as after thought. Easier to just break the naming scheme that rename everything that follows
        assertEquals(Util.list("attValue15.1"), result.get("attName15.1")); // colliding, multi-valued, duplicate nulls
        assertEquals(Util.list("attValue16"), result.get("attName16")); // colliding, duplicate non-null
        assertEquals(Util.list("attValue17.1","attValue17.2"), result.get("attName17")); // colliding, duplicate non-nulls, same order
        assertEquals(Util.list("attValue18.1","attValue18.2"), result.get("attName18")); // colliding, duplicate non-nulls, different order
        assertEquals(Util.list("attValue19.1",null), result.get("attName19")); // colliding, duplicate null and non-null, same order
        assertEquals(Util.list("attValue20.1",null), result.get("attName20")); // colliding, duplicate null and non-null, different order

        // these two are the key difference from testColliding_FullyDeduplicating()
        assertEquals(Util.list(null,null), result.get("attName21")); // non-colliding, only in target, multi-valued, duplicate nulls
        assertEquals(Util.list("attValue22","attValue22"), result.get("attName22")); // non-colliding, only in target, multi-valued, duplicate non-nulls

        assertEquals(Util.list(null), result.get("attName23")); // non-colliding, only in source, multi-valued, duplicate nulls
        assertEquals(Util.list("attValue24"), result.get("attName24")); // non-colliding, only in source, multi-valued, duplicate non-nulls

    }

    /**
     * Same as {@link #testColliding_FullyDeduplicating()} but with the merger configured to only deduplicate
     * values in the <em>source</em> attributes, not in the <em>target</em>.
     *
     * @see org.jasig.services.persondir.support.merger.NonDuplicatingMultivaluedAttributeMerger.DeduplicationMode#COLLIDING_TARGET_ATTRIBUTES
     * @see NonDuplicatingMultivaluedAttributeMerger#setDeduplicationMode(org.jasig.services.persondir.support.merger.NonDuplicatingMultivaluedAttributeMerger.DeduplicationMode)
     */
    public void testColliding_SourceValuesDeduplicating() {
        ((NonDuplicatingMultivaluedAttributeMerger)getAttributeMerger())
                .setDeduplicationMode(NonDuplicatingMultivaluedAttributeMerger.DeduplicationMode.SOURCE_VALUES);

        final Map<String, List<Object>> someAttributes = standardCollisionTestTargetAttributes();
        final Map<String, List<Object>> otherAttributes = standardCollisionTestSourceAttributes();

        // More granular assertions than in MultivaluedAttributeMergerTest to try to make it easier to parse
        // out the offending entries
        Map<String, List<Object>> result = getAttributeMerger().mergeAttributes(someAttributes, otherAttributes);
        assertEquals(Util.list((Object)null), result.get("attName1"));
        assertEquals(Util.list("attValue2"), result.get("attName2"));
        assertEquals(Util.list((Object)null), result.get("attName3"));
        assertEquals(Util.list("attValue4"), result.get("attName4"));
        assertEquals(Util.list((Object)null), result.get("attName5"));
        assertEquals(Util.list(null, "attValue6"), result.get("attName6"));
        assertEquals(Util.list("attValue7", null), result.get("attName7"));
        assertEquals(Util.list("attValue8.1", "attValue8.2"), result.get("attName8"));
        assertEquals(Util.list(null, "attValue9.1", "attValue9.2"), result.get("attName9"));
        assertEquals(Util.list("attValue10", "attValue10.1", "attValue10.2"), result.get("attName10"));
        assertEquals(Util.list("attValue11.1", "attValue11.2", null), result.get("attName11"));
        assertEquals(Util.list("attValue12.1", "attValue12.2", "attValue12"), result.get("attName12"));
        assertEquals(Util.list("attValue13.1.1", "attValue13.1.2", "attValue13.2.1", "attValue13.2.2"), result.get("attName13"));

        // now the departures from MultivaluedAttributeMergerTest
        assertEquals(Util.list(null), result.get("attName14")); // colliding, single-valued, duplicate null

        // these two are a key difference from testColliding_CollidingTargetsDeduplicating() and
        // testColliding_FullyDeduplicating
        assertEquals(Util.list(null, null), result.get("attName15")); // colliding, multi-valued, duplicate nulls
        // 15.1 added as after thought. Easier to just break the naming scheme that rename everything that follows
        assertEquals(Util.list("attValue15.1","attValue15.1"), result.get("attName15.1")); // colliding, multi-valued, duplicate nulls

        assertEquals(Util.list("attValue16"), result.get("attName16")); // colliding, duplicate non-null
        assertEquals(Util.list("attValue17.1","attValue17.2"), result.get("attName17")); // colliding, duplicate non-nulls, same order
        assertEquals(Util.list("attValue18.1","attValue18.2"), result.get("attName18")); // colliding, duplicate non-nulls, different order
        assertEquals(Util.list("attValue19.1",null), result.get("attName19")); // colliding, duplicate null and non-null, same order
        assertEquals(Util.list("attValue20.1",null), result.get("attName20")); // colliding, duplicate null and non-null, different order

        // these two are the key difference from testColliding_FullyDeduplicating(), but behavior is the
        // same as testColliding_CollidingTargetsDeduplicating()
        assertEquals(Util.list(null,null), result.get("attName21")); // non-colliding, only in target, multi-valued, duplicate nulls
        assertEquals(Util.list("attValue22","attValue22"), result.get("attName22")); // non-colliding, only in target, multi-valued, duplicate non-nulls

        assertEquals(Util.list(null), result.get("attName23")); // non-colliding, only in source, multi-valued, duplicate nulls
        assertEquals(Util.list("attValue24"), result.get("attName24")); // non-colliding, only in source, multi-valued, duplicate non-nulls

    }

    // make sure this stays sync'd up with standardCollisionTestSourceAttributes()
    private Map<String, List<Object>> standardCollisionTestTargetAttributes() {
        // first the std MultivaluedAttributeMergerTest setup
        final Map<String, List<Object>> someAttributes = new HashMap<String, List<Object>>();
        someAttributes.put("attName1", Util.list((Object)null));
        someAttributes.put("attName2", Util.list("attValue2"));

        someAttributes.put("attName5", Util.list((Object)null));
        someAttributes.put("attName6", Util.list((Object)null));
        someAttributes.put("attName7", Util.list("attValue7"));
        someAttributes.put("attName8", Util.list("attValue8.1"));

        someAttributes.put("attName9", Util.list((Object)null));
        someAttributes.put("attName10", Util.list("attValue10"));
        someAttributes.put("attName11", Util.list("attValue11.1", "attValue11.2"));
        someAttributes.put("attName12", Util.list("attValue12.1", "attValue12.2"));
        someAttributes.put("attName13", Util.list("attValue13.1.1", "attValue13.1.2"));

        // now the departures from MultivaluedAttributeMergerTest
        someAttributes.put("attName14", Util.list(null)); // colliding, single-valued, duplicate null
        someAttributes.put("attName15", Util.list(null, null)); // colliding, multi-valued, duplicate nulls
        // 15.1 added as after thought. Easier to just break the naming scheme that rename everything that follows
        someAttributes.put("attName15.1", Util.list("attValue15.1", "attValue15.1")); // colliding, multi-valued, duplicate non-nulls
        someAttributes.put("attName16", Util.list("attValue16")); // colliding, duplicate non-null
        someAttributes.put("attName17", Util.list("attValue17.1","attValue17.2")); // colliding, duplicate non-nulls, same order
        someAttributes.put("attName18", Util.list("attValue18.1","attValue18.2")); // colliding, duplicate non-nulls, different order
        someAttributes.put("attName19", Util.list("attValue19.1",null)); // colliding, duplicate null and non-null, same order
        someAttributes.put("attName20", Util.list("attValue20.1",null)); // colliding, duplicate null and non-null, different order

        someAttributes.put("attName21", Util.list(null, null)); // non-colliding, only in target, multi-valued, duplicate nulls
        someAttributes.put("attName22", Util.list("attValue22", "attValue22")); // non-colliding, only in target, multi-valued, duplicate non-nulls

        return someAttributes;

    }

    // make sure this stays sync'd up with standardCollisionTestTargetAttributes()
    private Map<String, List<Object>> standardCollisionTestSourceAttributes() {
        // back to std MultivaluedAttributeMergerTest setup
        Map<String, List<Object>> otherAttributes = new HashMap<String, List<Object>>();
        otherAttributes.put("attName3", Util.list((Object)null));
        otherAttributes.put("attName4", Util.list("attValue4"));

        otherAttributes.put("attName5", Util.list((Object)null));
        otherAttributes.put("attName6", Util.list("attValue6"));
        otherAttributes.put("attName7", Util.list((Object)null));
        otherAttributes.put("attName8", Util.list("attValue8.2")); // colliding, non-duplicate

        otherAttributes.put("attName9", Util.list("attValue9.1", "attValue9.2"));
        otherAttributes.put("attName10", Util.list("attValue10.1", "attValue10.2")); // colliding, non-duplicate
        otherAttributes.put("attName11", Util.list((Object)null));
        otherAttributes.put("attName12", Util.list("attValue12")); // colliding, non-duplicate
        otherAttributes.put("attName13", Util.list("attValue13.2.1", "attValue13.2.2")); // colliding, non-duplicate

        // now more departures from MultivaluedAttributeMergerTest
        otherAttributes.put("attName14", Util.list(null)); // colliding, single-valued, duplicate null
        otherAttributes.put("attName15", Util.list(null, null)); // colliding, multi-valued, duplicate nulls
        // 15.1 added as after thought. Easier to just break the naming scheme that rename everything that follows
        otherAttributes.put("attName15.1", Util.list("attValue15.1", "attValue15.1")); // colliding, multi-valued, duplicate non-nulls
        otherAttributes.put("attName16", Util.list("attValue16")); // colliding, duplicate non-null
        otherAttributes.put("attName17", Util.list("attValue17.1","attValue17.2")); // colliding, duplicate non-nulls, same order
        otherAttributes.put("attName18", Util.list("attValue18.2","attValue18.1")); // colliding, duplicate non-nulls, different order
        otherAttributes.put("attName19", Util.list("attValue19.1",null)); // colliding, duplicate null and non-null, same order
        otherAttributes.put("attName20", Util.list(null, "attValue20.1")); // colliding, duplicate null and non-null, different order

        // intentionally leave attrName21 and attrName22 out... those should be non-colliding on the target side. Then
        // we need some non-collisions with duplicates on the source side (we do have attrName3 and 4 above, which
        // are non-colliding on the source side, but those attrs don't contain duplicate values)
        otherAttributes.put("attName23", Util.list(null, null)); // non-colliding, only in source, multi-valued, duplicate nulls
        otherAttributes.put("attName24", Util.list("attValue24", "attValue24")); // non-colliding, only in source, multi-valued, duplicate non-nulls

        return otherAttributes;
    }

    @Override
    protected IAttributeMerger getAttributeMerger() {
        return merger;
    }
}
