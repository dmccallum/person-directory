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
        }};;

        getBaseAdditiveAttributeMerger().setCaseSensitiveUsernames(false);

        getBaseAdditiveAttributeMerger().mergeResults(toModify, toConsider);
        assertEquals("Treated \"username\" and \"USERNAME\" as distinct while in case-insensitive mode.", 1, toModify.size());
        assertEquals("Should have preserved the username in the \"toModify\" collection",
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
