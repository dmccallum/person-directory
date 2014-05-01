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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.set.ListOrderedSet;
import org.apache.commons.lang.Validate;

/**
 * Same as {@link MultivaluedAttributeMerger} but will not set duplicate values for a given attribute.
 * Also optionally enforces a de-duplicated result set, period, which is the default behavior. If
 * you're sourcing attributes for which multiple values must always exist, even if they are duplicate,
 * then
 */
public class NonDuplicatingMultivaluedAttributeMerger extends BaseAdditiveAttributeMerger {

    public static enum DeduplicationMode {
        /**
         * De-duplicate all attributes in the target ("toModify") attribute map, even those which aren't present in the
         * current source ("toConsider") map. Implies {@link #COLLIDING_TARGET_ATTRIBUTES} and
         * {@link #COLLIDING_TARGET_ATTRIBUTES}, but adds the step of de-duplicating any target attribute which hadn't
         * been considered during the merge of source attributes.
         */
        ALL_TARGET_ATTRIBUTES,
        /**
         * De-duplicate attributes which are present in both the target ("toModify") and source ("toConsider") attribute
         * maps. Implies {@link #SOURCE_VALUES}, but adds the step of then de-duplicating the merged attribute
         * state after that rule/mode has been applied.
         */
        COLLIDING_TARGET_ATTRIBUTES {
            @Override
            public void postProcess(Map<String, List<Object>> toModify, Map<String, List<Object>> toConsider, Set<String> mergedAttrNames) {
                // no-op
            }
        },
        /**
         * De-duplicate attribute <em>values</em> in the source ("toConsider") map, but leave existing duplicates in
         * corresponding target ("toModify") attributes.
         */
        SOURCE_VALUES {
            @Override
            public void postProcessMergedAttribute(String attrName, List<Object> mergedValues)  {
                // no-op
            }
            @Override
            public void postProcess(Map<String, List<Object>> toModify, Map<String, List<Object>> toConsider, Set<String> mergedAttrNames) {
                // no-op
            }
        };

        // Default impls below represent the most aggressive de-dupe behavior. Less aggressive Modes can override
        // these to no-ops as necessary.

        /**
         * Merge {@code toConsiderValue} into {@code toModifyValues}.
         *
         * @param attrName the name of the attribute being merged
         * @param toModifyValues the "target" values
         * @param toConsiderValue the "source" value
         */
        public void processAttributeValue(String attrName, List<Object> toModifyValues, Object toConsiderValue) {
            if ( !(toModifyValues.contains(toConsiderValue)) ) {
                toModifyValues.add(toConsiderValue);
            }
        }

        /**
         * Called after {@link #processAttributeValue(String, java.util.List, Object)} has been called for all
         * "source" attributes for a given attribute name.
         *
         * @param attrName the name of the attribute being processed
         * @param mergedValues the collected result of {@link #processAttributeValue(String, java.util.List, Object)}
         */
        public void postProcessMergedAttribute(String attrName, List<Object> mergedValues) {
            deduplicate(mergedValues);
        }

        /**
         * Called after all "source" attributes have been merged into the "target". Provides an opportunity to process
         * "target" attributes which were not present in the "source", for example.
         *
         * @param toModify the "target" attribute map
         * @param toConsider the "source" attribute map
         * @param mergedAttrNames all attribute names for which {@link #processAttributeValue(String, java.util.List, Object)}
         *                        was previously called. Mainly present to give impls an optimization opportunity b/c
         *                        some attrs might be skipped if they've already been deduplicated
         */
        public void postProcess(Map<String, List<Object>> toModify, Map<String, List<Object>> toConsider, Set<String> mergedAttrNames) {
            for ( Map.Entry<String,List<Object>> toModifyEntries : toModify.entrySet() ) {
                if ( mergedAttrNames != null && mergedAttrNames.contains(toModifyEntries.getKey()) ) {
                    continue;
                }
                deduplicate(toModifyEntries.getValue());
            }
        }

        protected void deduplicate(List<Object> toDedupe) {
            // De-duplication doesn't actually work correctly if we just pass toModifyValues into the decoration...
            Set<Object> uniqueValues = ListOrderedSet.decorate(new ArrayList<Object>(toDedupe.size()));
            uniqueValues.addAll(toDedupe);
            toDedupe.clear();
            toDedupe.addAll(uniqueValues);
        }
    }

    public static final DeduplicationMode DEFAULT_DEDUPLICATION_MODE = DeduplicationMode.COLLIDING_TARGET_ATTRIBUTES;

    private DeduplicationMode deduplicationMode = DEFAULT_DEDUPLICATION_MODE;

    /* (non-Javadoc)
         * @see org.jasig.services.persondir.support.merger.BaseAdditiveAttributeMerger#mergePersonAttributes(java.util.Map, java.util.Map)
         */
    @Override
    protected Map<String, List<Object>> mergePersonAttributes(Map<String, List<Object>> toModify, Map<String, List<Object>> toConsider) {
        Validate.notNull(toModify, "toModify cannot be null");
        Validate.notNull(toConsider, "toConsider cannot be null");

        Set<String> mergedAttrNames = new HashSet<String>();
        for (final Map.Entry<String, List<Object>> sourceEntry : toConsider.entrySet()) {
            final String sourceKey = sourceEntry.getKey();

            List<Object> destList = toModify.get(sourceKey);
            if (destList == null) {
                destList = new LinkedList<Object>();
                toModify.put(sourceKey, destList);
            }

            final List<Object> sourceValue = sourceEntry.getValue();
            if ( sourceValue == null ) {
                continue;
            }
            for ( Object singleSourceValue : sourceValue ) {
                mergedAttrNames.add(sourceKey);
                deduplicationMode.processAttributeValue(sourceKey, destList, singleSourceValue);
            }
            deduplicationMode.postProcessMergedAttribute(sourceKey, destList);
        }

        deduplicationMode.postProcess(toModify, toConsider, mergedAttrNames);

        return toModify;
    }

    public DeduplicationMode getDeduplicationMode() {
        return deduplicationMode;
    }

    /**
     * See javadoc in {@link DeduplicationMode}. Default is {@link #DEFAULT_DEDUPLICATION_MODE}.
     *
     * <p>A null argument will reset state back to the default.</p>
     *
     * @param deduplicationMode
     */
    public void setDeduplicationMode(DeduplicationMode deduplicationMode) {
        if ( deduplicationMode == null ) {
            this.deduplicationMode = DEFAULT_DEDUPLICATION_MODE;
            return;
        }
        this.deduplicationMode = deduplicationMode;
    }

}
