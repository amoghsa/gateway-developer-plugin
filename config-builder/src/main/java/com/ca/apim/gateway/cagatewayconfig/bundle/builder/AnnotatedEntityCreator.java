package com.ca.apim.gateway.cagatewayconfig.bundle.builder;

import com.ca.apim.gateway.cagatewayconfig.beans.Annotation;
import com.ca.apim.gateway.cagatewayconfig.beans.Encass;
import com.ca.apim.gateway.cagatewayconfig.util.entity.EntityTypes;
import org.apache.commons.lang3.StringUtils;

import java.util.Set;

import static com.ca.apim.gateway.cagatewayconfig.util.entity.AnnotationConstants.*;
import static com.ca.apim.gateway.cagatewayconfig.util.entity.AnnotationConstants.ANNOTATION_TYPE_EXCLUDE;

public class AnnotatedEntityCreator {

    public static final AnnotatedEntityCreator INSTANCE = new AnnotatedEntityCreator();

    /**
     * Creates AnnotatedEntity object by scanning all the annotations and gathering all the information required to
     * generate the bundle and its metadata.
     *
     * @param projectName Project name
     * @param projectVersion Project version
     * @param  encass Encass entity
     * @return AnnotatedEntity
     */
    public AnnotatedEntity createEntity(final String projectName,
                                        final String projectVersion, final Encass encass) {
        AnnotatedEntity annotatedEntity = new AnnotatedEntity();
        Set<Annotation> annotations =  encass.getAnnotations();
        annotations.forEach(annotation -> {
            switch (annotation.getType()) {
                case ANNOTATION_TYPE_BUNDLE:
                    String annotatedBundleName = annotation.getName();
                    if (StringUtils.isBlank(annotatedBundleName)) {
                        annotatedBundleName = projectName + "." + encass.getName();
                    }
                    String description = annotation.getDescription();
                    if (StringUtils.isBlank(description)) {
                        description = encass.getProperties().getOrDefault("description", "").toString();
                    }
                    annotatedEntity.setTags(annotation.getTags());
                    annotatedEntity.setBundleType(true);
                    annotatedEntity.setEntityName(encass.getName());
                    annotatedEntity.setDescription(description);
                    annotatedEntity.setEntityType(EntityTypes.ENCAPSULATED_ASSERTION_TYPE);
                    annotatedEntity.setBundleName(annotatedBundleName + "-" + projectVersion);
                    annotatedEntity.setPolicyName(encass.getPolicy());
                    break;
                case ANNOTATION_TYPE_REUSABLE:
                case ANNOTATION_TYPE_REUSABLE_BUNDLE:
                    annotatedEntity.setReusableType(true);
                    break;
                case ANNOTATION_TYPE_REUSABLE_ENTITY:
                    annotatedEntity.setReusableEntity(true);
                    break;
                case ANNOTATION_TYPE_REDEPLOYABLE:
                    annotatedEntity.setRedeployableType(true);
                    break;
                case ANNOTATION_TYPE_EXCLUDE:
                    annotatedEntity.setExcludeType(true);
                    break;
                default:
                    break;
            }
        });
        return annotatedEntity;
    }
}
