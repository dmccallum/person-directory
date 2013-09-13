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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jasig.services.persondir.util.Util;

/**
 * Testcase for the MultivaluedAttributeMerger.
 * @author andrew.petro@yale.edu
 * @version $Revision$ $Date$
 */
public class MultivaluedAttributeMergerTest extends BaseAdditiveAttributeMergerTest {

    private MultivaluedAttributeMerger adder = new MultivaluedAttributeMerger();

    /**
     * Test identity of adding an empty map.
     */
    public void testAddEmpty() {
        Map<String, List<Object>> someAttributes = new HashMap<String, List<Object>>();
        someAttributes.put("attName", Util.list("attValue"));
        someAttributes.put("attName2", Util.list("attValue2"));
        
        Map<String, List<Object>> expected = new HashMap<String, List<Object>>();
        expected.putAll(someAttributes);
        
        Map<String, List<Object>> result = this.adder.mergeAttributes(someAttributes, new HashMap<String, List<Object>>());
        
        assertEquals(expected, result);
    }

    /**
     * Test a simple case of adding one map of attributes to another, with
     * no collisions.
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
        
        Map<String, List<Object>> result = this.adder.mergeAttributes(someAttributes, otherAttributes);
        assertEquals(expected, result);
    }
    
    /**
     * Test that colliding attributes are not added.
     */
    public void testColliding() {
        Map<String, List<Object>> someAttributes = new HashMap<String, List<Object>>();
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
        
        
        Map<String, List<Object>> otherAttributes = new HashMap<String, List<Object>>();
        otherAttributes.put("attName3", Util.list((Object)null));
        otherAttributes.put("attName4", Util.list("attValue4"));
        
        otherAttributes.put("attName5", Util.list((Object)null));
        otherAttributes.put("attName6", Util.list("attValue6"));
        otherAttributes.put("attName7", Util.list((Object)null));
        otherAttributes.put("attName8", Util.list("attValue8.2"));
        
        otherAttributes.put("attName9", Util.list("attValue9.1", "attValue9.2"));
        otherAttributes.put("attName10", Util.list("attValue10.1", "attValue10.2"));
        otherAttributes.put("attName11", Util.list((Object)null));
        otherAttributes.put("attName12", Util.list("attValue12"));
        otherAttributes.put("attName13", Util.list("attValue13.2.1", "attValue13.2.2"));

        
        Map<String, List<Object>> expected = new HashMap<String, List<Object>>();
        expected.put("attName1", Util.list((Object)null));
        expected.put("attName2", Util.list("attValue2"));
        expected.put("attName3", Util.list((Object)null));
        expected.put("attName4", Util.list("attValue4"));
        expected.put("attName5", Util.list(null, null));
        expected.put("attName6", Util.list(null, "attValue6"));
        expected.put("attName7", Util.list("attValue7", null));
        expected.put("attName8", Util.list("attValue8.1", "attValue8.2"));

        expected.put("attName9", Util.list(null, "attValue9.1", "attValue9.2"));
        expected.put("attName10", Util.list("attValue10", "attValue10.1", "attValue10.2"));
        
        expected.put("attName11", Util.list("attValue11.1", "attValue11.2", null));
        expected.put("attName12", Util.list("attValue12.1", "attValue12.2", "attValue12"));
        expected.put("attName13", Util.list("attValue13.1.1", "attValue13.1.2", "attValue13.2.1", "attValue13.2.2"));
        
        Map<String, List<Object>> result = this.adder.mergeAttributes(someAttributes, otherAttributes);
        assertEquals(expected, result);
    }

    /* (non-Javadoc)
     * @see org.jasig.services.persondir.support.merger.AbstractAttributeMergerTest#getAttributeMerger()
     */
    @Override
    protected IAttributeMerger getAttributeMerger() {
        return adder;
    }
}
