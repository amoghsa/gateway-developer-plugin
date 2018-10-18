package com.ca.apim.gateway.cagatewayexport.tasks.explode.filter.entityfilters;

import com.ca.apim.gateway.cagatewayexport.tasks.explode.bundle.Bundle;
import com.ca.apim.gateway.cagatewayexport.tasks.explode.bundle.entity.PrivateKeyEntity;
import com.ca.apim.gateway.cagatewayexport.tasks.explode.filter.EntityFilter;
import com.ca.apim.gateway.cagatewayexport.tasks.explode.filter.FilterConfiguration;
import com.ca.apim.gateway.cagatewayexport.util.gateway.DependencyUtils;
import org.jetbrains.annotations.NotNull;

import javax.inject.Singleton;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.ca.apim.gateway.cagatewayexport.tasks.explode.bundle.entity.PrivateKeyEntity.SSL_DEFAULT_PRIVATE_KEY;

@Singleton
public class PrivateKeyFilter implements EntityFilter<PrivateKeyEntity> {

    private static final Set<Class<? extends EntityFilter>> FILTER_DEPENDENCIES = Stream.of(
            PolicyFilter.class,
            ServiceFilter.class).collect(Collectors.toSet());
    private final String ENTITY_NAME = "privateKeys";

    @Override
    public @NotNull Collection<Class<? extends EntityFilter>> getDependencyEntityFilters() {
        return FILTER_DEPENDENCIES;
    }

    @Override
    @SuppressWarnings("squid:S1157")
    public List<PrivateKeyEntity> filter(String folderPath, FilterConfiguration filterConfiguration, Bundle bundle, Bundle filteredBundle) {
        Stream<PrivateKeyEntity> stream = DependencyUtils.filterDependencies(PrivateKeyEntity.class, bundle, filteredBundle, e -> filterConfiguration.getRequiredEntityNames(ENTITY_NAME).contains(e.getName())).stream();
        if (!filterConfiguration.getRequiredEntityNames(ENTITY_NAME).contains(SSL_DEFAULT_PRIVATE_KEY.toUpperCase())) {
            stream = stream
                    // filter out the default ssl key
                    .filter(p -> !p.getName().equals(SSL_DEFAULT_PRIVATE_KEY.toUpperCase()));
        }
        if (!filterConfiguration.getRequiredEntityNames(ENTITY_NAME).contains(SSL_DEFAULT_PRIVATE_KEY.toLowerCase())) {
            stream = stream
                    // filter out the default ssl key
                    .filter(p -> !p.getName().equals(SSL_DEFAULT_PRIVATE_KEY.toLowerCase()));
        }
        List<PrivateKeyEntity> privateKeys = stream.collect(Collectors.toList());
        DependencyUtils.validateEntitiesInList(privateKeys, filterConfiguration.getRequiredEntityNames(ENTITY_NAME), "Private Key(s)");
        return privateKeys;
    }

    @Override
    public String getFilterableEntityName() {
        return ENTITY_NAME;
    }
}
