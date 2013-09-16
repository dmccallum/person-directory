package org.jasig.services.persondir.support.merger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import org.jasig.services.persondir.IPersonAttributes;
import org.jasig.services.persondir.support.CaseInsensitiveNamedPersonImpl;

public abstract class BaseAdditiveAttributeMergerTest extends AbstractAttributeMergerTest {

    protected BaseAdditiveAttributeMerger getBaseAdditiveAttributeMerger() {
        return (BaseAdditiveAttributeMerger)getAttributeMerger();
    }

    public void testCaseInsensitiveUsername() {

        Set<IPersonAttributes> toModify = new HashSet<IPersonAttributes>() {{
            add(new CaseInsensitiveNamedPersonImpl("username",
                    new LinkedHashMap<String,List<Object>>() {{
                        put("attr-name-1", new ArrayList<Object>() {{ add("attr-value-1"); }});
                    }}));
        }};
        Set<IPersonAttributes> toConsider = new HashSet<IPersonAttributes>() {{
            add(new CaseInsensitiveNamedPersonImpl("USERNAME",
                    new LinkedHashMap<String,List<Object>>() {{
                        put("attr-name-1", new ArrayList<Object>() {{ add("attr-value-1"); }});
                    }}));
        }};

        getBaseAdditiveAttributeMerger().setCaseSensitiveUsernames(false);

        getBaseAdditiveAttributeMerger().mergeResults(toModify, toConsider);
        assertEquals("Treated \"username\" and \"USERNAME\" as distinct while in case-insensitive mode.", 1, toModify.size());
        assertEquals("Should have normalized username in the \"toModify\" collection to lower case",
                "username", toModify.iterator().next().getName());
    }

    public void testCaseInsensitiveUsernameReverseResultOrdering() {

        // Same as testCaseInsensitiveUsername but with casings reversed between
        // the toModify and toConsider maps. During system level testing of the
        // first attempt at a patch we saw that the toModify casing would
        // always "win" in such a way that we'd end up with two users in
        // search results.

        Set<IPersonAttributes> toModify = new HashSet<IPersonAttributes>() {{
            add(new CaseInsensitiveNamedPersonImpl("USERNAME",
                    new LinkedHashMap<String,List<Object>>() {{
                        put("attr-name-1", new ArrayList<Object>() {{ add("attr-value-1"); }});
                    }}));
        }};
        Set<IPersonAttributes> toConsider = new HashSet<IPersonAttributes>() {{
            add(new CaseInsensitiveNamedPersonImpl("username",
                    new LinkedHashMap<String,List<Object>>() {{
                        put("attr-name-1", new ArrayList<Object>() {{ add("attr-value-1"); }});
                    }}));
        }};

        getBaseAdditiveAttributeMerger().setCaseSensitiveUsernames(false);

        getBaseAdditiveAttributeMerger().mergeResults(toModify, toConsider);
        assertEquals("Treated \"username\" and \"USERNAME\" as distinct while in case-insensitive mode.", 1, toModify.size());
        assertEquals("Should have normalized username in the \"toModify\" collection to lower case",
                "username", toModify.iterator().next().getName());
    }

    public void testCaseSensitiveUsername() {

        Set<IPersonAttributes> toModify = new HashSet<IPersonAttributes>() {{
            add(new CaseInsensitiveNamedPersonImpl("username",
                    new LinkedHashMap<String,List<Object>>() {{
                        put("attr-name-1", new ArrayList<Object>() {{ add("attr-value-1"); }});
                    }}));
        }};
        Set<IPersonAttributes> toConsider = new HashSet<IPersonAttributes>() {{
            add(new CaseInsensitiveNamedPersonImpl("USERNAME",
                    new LinkedHashMap<String,List<Object>>() {{
                        put("attr-name-1", new ArrayList<Object>() {{ add("attr-value-1"); }});
                    }}));
        }};;

        getBaseAdditiveAttributeMerger().setCaseSensitiveUsernames(true);

        getBaseAdditiveAttributeMerger().mergeResults(toModify, toConsider);
        assertEquals("Treated \"username\" and \"USERNAME\" as identical while in case-sensitive mode.", 2, toModify.size());
    }

}
