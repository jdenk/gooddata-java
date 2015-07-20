/*
 * Copyright (C) 2007-2015, GoodData(R) Corporation. All rights reserved.
 */
package com.gooddata.featureflag;

import com.gooddata.project.Project;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonTypeInfo;
import org.codehaus.jackson.annotate.JsonTypeName;
import org.springframework.web.util.UriTemplate;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import static com.gooddata.util.Validate.notEmpty;
import static org.springframework.util.CollectionUtils.isEmpty;

@JsonTypeInfo(include = JsonTypeInfo.As.WRAPPER_OBJECT, use = JsonTypeInfo.Id.NAME)
@JsonTypeName("featureFlags")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProjectFeatureFlags implements Iterable<ProjectFeatureFlag> {

    public static final String PROJECT_FEATURE_FLAGS_URI = Project.URI + "/projectFeatureFlags";
    public static final UriTemplate PROJECT_FEATURE_FLAGS_TEMPLATE = new UriTemplate(PROJECT_FEATURE_FLAGS_URI);

    private final List<ProjectFeatureFlag> items = new LinkedList<>();

    @JsonCreator
    ProjectFeatureFlags(@JsonProperty("items") List<ProjectFeatureFlag> items) {
        if (!isEmpty(items)) {
            this.items.addAll(items);
        }
    }

    /**
     *{@inheritDoc Iterable#iterator}
     */
    @Override
    public Iterator<ProjectFeatureFlag> iterator() {
        return items.iterator();
    }

    /**
     * Returns true if the project feature flag with given name exists and is enabled, false otherwise.
     *
     * @param flagName the name of project feature flag
     * @return true if the project feature flag with given name exists and is enabled, false otherwise
     */
    public boolean isEnabled(final String flagName) {
        notEmpty(flagName, "flagName");
        for (final ProjectFeatureFlag flag : items) {
            if (flagName.equalsIgnoreCase(flag.getName())) {
                return flag.getEnabled();
            }
        }
        return false;
    }

    /**
     * @deprecated use {@link #isEnabled(String)} method or {@link Iterable} feature of this class
     */
    @Deprecated
    public List<ProjectFeatureFlag> getItems() {
        return items;
    }
}
