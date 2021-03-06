/*
 * Copyright (c) 2018 CA. All rights reserved.
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package com.ca.apim.gateway.cagatewayconfig.beans;

import com.ca.apim.gateway.cagatewayconfig.bundle.builder.AnnotableEntity;
import com.ca.apim.gateway.cagatewayconfig.bundle.builder.AnnotatedEntity;
import com.ca.apim.gateway.cagatewayconfig.bundle.builder.Metadata;
import com.ca.apim.gateway.cagatewayconfig.config.loader.ConfigLoadException;
import com.ca.apim.gateway.cagatewayconfig.util.IdGenerator;
import com.ca.apim.gateway.cagatewayconfig.util.entity.EntityTypes;
import com.ca.apim.gateway.cagatewayconfig.util.paths.PathUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

import javax.inject.Named;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.ObjectUtils.firstNonNull;

@JsonInclude(NON_EMPTY)
@Named("POLICY")
public class Policy extends Folderable implements AnnotableEntity {

    @JsonIgnore
    private String policyXML;
    @JsonIgnore
    private String guid;
    @JsonIgnore
    private Element policyDocument;
    @JsonIgnore
    private Set<Annotation> annotations = new HashSet<>();
    @JsonIgnore
    private final Set<Policy> dependencies = new HashSet<>();
    private String tag;
    @JsonIgnore
    private String subtag;
    @JsonIgnore
    private PolicyType policyType;
    @JsonIgnore
    private Set<Dependency> usedEntities = new LinkedHashSet<>();
    @JsonIgnore
    private AnnotatedEntity<? extends GatewayEntity> annotatedEntity;

    private boolean hasRouting;
    /* Set to true if any Parent (Policy or Encass) in hierarchy is annotated with @shared */
    @JsonIgnore
    private boolean parentEntityShared;

    public Policy() {
    }

    public Policy(final Builder builder) {
        setName(builder.name);
        setId(builder.id);
        this.guid = builder.guid;
        this.policyXML = builder.policy;
        this.tag = builder.tag;
        this.subtag = builder.subtag;
        setParentFolder(builder.parentFolderId != null ? new Folder(builder.parentFolderId, null) : null);
    }

    // Copy constructor
    public Policy(Policy otherPolicy) {
        merge(otherPolicy);
    }

    public Set<Dependency> getUsedEntities() {
        return usedEntities;
    }

    public void setUsedEntities(Set<Dependency> usedEntities) {
        this.usedEntities = usedEntities;
    }

    public String getPolicyXML() {
        return policyXML;
    }

    public void setPolicyXML(String policyXML) {
        this.policyXML = policyXML;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public String getGuid() {
        return guid;
    }

    public void setPolicyDocument(Element policyDocument) {
        this.policyDocument = policyDocument;
    }

    public Element getPolicyDocument() {
        return policyDocument;
    }

    public Set<Policy> getDependencies() {
        return dependencies;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getSubtag() {
        return subtag;
    }

    public void setSubtag(String subtag) {
        this.subtag = subtag;
    }

    public PolicyType getPolicyType() {
        return policyType;
    }

    @Override
    public Set<Annotation> getAnnotations() {
        return annotations;
    }

    public void setAnnotations(Set<Annotation> annotations) {
        this.annotations = annotations;
    }

    public void setPolicyType(PolicyType policyType) {
        this.policyType = policyType;
    }

    public boolean isHasRouting() {
        return hasRouting;
    }

    public void setHasRouting(boolean hasRouting) {
        this.hasRouting = hasRouting;
    }

    public boolean isParentEntityShared() {
        return parentEntityShared;
    }

    public void setParentEntityShared(boolean parentEntityShared) {
        this.parentEntityShared = parentEntityShared;
    }

    Policy merge(Policy otherPolicy) {
        this.policyXML = firstNonNull(otherPolicy.policyXML, this.policyXML);
        this.setPath(firstNonNull(otherPolicy.getPath(), this.getPath()));
        this.setName(firstNonNull(otherPolicy.getName(), this.getName()));
        this.setParentFolder(firstNonNull(otherPolicy.getParentFolder(), this.getParentFolder()));
        this.guid = firstNonNull(otherPolicy.guid, this.guid);
        this.policyDocument = firstNonNull(otherPolicy.policyDocument, this.policyDocument);
        this.dependencies.addAll(firstNonNull(otherPolicy.dependencies, Collections.emptySet()));
        this.setId(firstNonNull(otherPolicy.getId(), this.getId()));
        this.tag = firstNonNull(otherPolicy.tag, this.tag);
        this.subtag = firstNonNull(otherPolicy.subtag, this.subtag);
        this.policyType = firstNonNull(otherPolicy.policyType, this.policyType);
        this.hasRouting = otherPolicy.hasRouting || this.hasRouting;
        this.usedEntities.addAll(firstNonNull(otherPolicy.usedEntities, Collections.emptySet()));
        this.annotations.addAll(firstNonNull(otherPolicy.annotations, Collections.emptySet()));
        return this;
    }

    public static class Builder {

        private String name;
        private String id;
        private String guid;
        private String policy;
        private String tag;
        private String subtag;
        private String parentFolderId;

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setId(String id) {
            this.id = id;
            return this;
        }

        public Builder setGuid(String guid) {
            this.guid = guid;
            return this;
        }

        public Builder setPolicy(String policy) {
            this.policy = policy;
            return this;
        }

        public Builder setTag(String tag) {
            this.tag = tag;
            return this;
        }

        public Builder setSubtag(String subtag) {
            this.subtag = subtag;
            return this;
        }

        public Builder setParentFolderId(String parentFolderId) {
            this.parentFolderId = parentFolderId;
            return this;
        }

        public Policy build() {
            return new Policy(this);
        }
    }

    static void checkRepeatedTags(Bundle bundle, PolicyType policyType) {
        Set<String> errors = new HashSet<>();
        new HashMap<>(bundle.getPolicies()).values()
                .stream()
                .filter(p -> p.getTag() != null)
                .collect(groupingBy(Policy::getTag, Collectors.mapping(identity(), toList())))
                .forEach((key, value) -> {
                    if (value.size() > 1) {
                        errors.add(
                                String.format(
                                        "Found more then one %s policy with tag '%s': [%s]",
                                        policyType.getType(),
                                        key,
                                        String.join(", ", value.stream().map(Policy::getPath).collect(toList())
                                        )
                                ));
                    }
                });
        if (!errors.isEmpty()) {
            throw new ConfigLoadException(String.join("\n", errors));
        }
    }

    @Override
    public void postLoad(String entityKey, Bundle bundle, @Nullable File rootFolder, IdGenerator idGenerator) {
        setPath(PathUtils.unixPath(getPath()));
    }

    @Override
    public AnnotatedEntity getAnnotatedEntity() {
        if (annotatedEntity == null && annotations != null) {
            annotatedEntity = createAnnotatedEntity();
        }
        return annotatedEntity;
    }

    @VisibleForTesting
    public void setAnnotatedEntity(AnnotatedEntity annotatedEntity){
        this.annotatedEntity = annotatedEntity;
    }

    @Override
    public String getEntityType(){
        return EntityTypes.POLICY_TYPE;
    }

    @JsonIgnore
    @Override
    public Metadata getMetadata() {
        return new Metadata() {
            @Override
            public String getType() {
                return EntityTypes.POLICY_TYPE;
            }

            @Override
            public String getName() {
                return Policy.this.getName();
            }

            @Override
            public String getId() {
                return Policy.this.getId();
            }

            @Override
            public String getGuid() {
                return Policy.this.getGuid();
            }

        };
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Policy)) {
            return false;
        }
        Policy policy = (Policy) o;
        return Objects.equals(getPath(), policy.getPath());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getPath());
    }
}
