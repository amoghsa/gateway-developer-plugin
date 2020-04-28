/*
 * Copyright (c) 2018 CA. All rights reserved.
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package com.ca.apim.gateway.cagatewayconfig.bundle.builder;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

@JsonPropertyOrder({"id", "name", "groupName", "version", "type", "tags", "description", "reusable", "redeployable",
        "hasRouting", "environmentIncluded", "definedEntities", "environmentEntities", "dependencies"})
public class BundleMetadata implements Metadata {
    private String type;
    private String name;
    private String id;
    private String version;
    private String groupName;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String description;
    private Collection<Metadata> definedEntities;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Collection<String> tags;
    private boolean reusable;
    private boolean redeployable;
    private boolean hasRouting;
    private boolean environmentIncluded;
    private Collection<Metadata> environmentEntities;

    private BundleMetadata(String type, String id, String name, String groupName, String version) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.groupName = groupName;
        this.version = version;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    public String getVersion() {
        return version;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getDescription() {
        return description;
    }

    public Collection<Metadata> getDefinedEntities() {
        return definedEntities;
    }

    public Collection<String> getTags() {
        return tags;
    }

    public boolean isReusable() {
        return reusable;
    }

    public boolean isRedeployable() {
        return redeployable;
    }

    public boolean isHasRouting() {
        return hasRouting;
    }

    public boolean isEnvironmentIncluded() {
        return environmentIncluded;
    }

    public Collection<Metadata> getEnvironmentEntities() {
        return environmentEntities;
    }

    public static class Builder {
        private final String id;
        private final String name;
        private final String type;
        private String groupName;
        private final String version;
        private String description;
        private boolean reusable;
        private boolean redeployable;
        private Collection<String> tags;
        private Collection<Metadata> definedEntities = new LinkedList<>();
        private Collection<Metadata> environmentEntities = new LinkedList<>();
        private Collection<Metadata> dependencies = new LinkedList<>();

        public Builder(String type, String id, String name, String groupName, String version) {
            this.id = id;
            this.type = type;
            this.name = name;
            this.groupName = groupName;
            this.version = version;
        }

        public Builder definedEntities(final Collection<Metadata> entities) {
            definedEntities.clear();
            definedEntities.addAll(entities);
            return this;
        }

        public Builder environmentEntities(final Collection<Metadata> environmentEntities) {
            this.environmentEntities = environmentEntities;
            return this;
        }

        public Builder description(final String description) {
            this.description = description;
            return this;
        }

        public Builder reusableAndRedeployable(boolean reusable, boolean redeployable) {
            this.reusable = reusable;
            this.redeployable = redeployable;
            return this;
        }

        public Builder tags(final Collection<String> tags) {
            this.tags = tags;
            return this;
        }

        public BundleMetadata build() {
            BundleMetadata bundleMetadata = new BundleMetadata(type, id, name, groupName, version);
            bundleMetadata.description = description;
            bundleMetadata.definedEntities = definedEntities;
            bundleMetadata.reusable = reusable;
            bundleMetadata.redeployable = redeployable;
            bundleMetadata.tags = tags;
            bundleMetadata.environmentEntities = environmentEntities;
            return bundleMetadata;
        }
    }
}