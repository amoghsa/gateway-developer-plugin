/*
 * Copyright (c) 2018 CA. All rights reserved.
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package com.ca.apim.gateway.cagatewayconfig;

import com.ca.apim.gateway.cagatewayconfig.beans.*;
import com.ca.apim.gateway.cagatewayconfig.beans.Bundle;
import com.ca.apim.gateway.cagatewayconfig.beans.GatewayEntity;
import com.ca.apim.gateway.cagatewayconfig.beans.Policy;
import com.ca.apim.gateway.cagatewayconfig.beans.Service;
import com.ca.apim.gateway.cagatewayconfig.bundle.builder.BundleArtifacts;
import com.ca.apim.gateway.cagatewayconfig.bundle.builder.BundleDefinedEntities;
import com.ca.apim.gateway.cagatewayconfig.bundle.builder.BundleEntityBuilder;
import com.ca.apim.gateway.cagatewayconfig.bundle.builder.EntityBuilder;
import com.ca.apim.gateway.cagatewayconfig.config.loader.EntityLoader;
import com.ca.apim.gateway.cagatewayconfig.config.loader.EntityLoaderRegistry;
import com.ca.apim.gateway.cagatewayconfig.config.loader.FolderLoaderUtils;
import com.ca.apim.gateway.cagatewayconfig.environment.BundleCache;
import com.ca.apim.gateway.cagatewayconfig.util.file.DocumentFileUtils;
import com.ca.apim.gateway.cagatewayconfig.util.file.JsonFileUtils;
import com.ca.apim.gateway.cagatewayconfig.util.xml.DocumentTools;
import org.w3c.dom.Document;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.xml.parsers.DocumentBuilder;
import java.io.File;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.ca.apim.gateway.cagatewayconfig.util.file.DocumentFileUtils.*;

@Singleton
public class BundleFileBuilder {

    private final DocumentFileUtils documentFileUtils;
    private final JsonFileUtils jsonFileUtils;
    private final EntityLoaderRegistry entityLoaderRegistry;
    private final BundleEntityBuilder bundleEntityBuilder;
    private final BundleCache cache;
    private final DocumentTools documentTools;

    private static final Logger LOGGER = Logger.getLogger(BundleFileBuilder.class.getName());

    @Inject
    public BundleFileBuilder(final DocumentTools documentTools,
                             final DocumentFileUtils documentFileUtils,
                             final JsonFileUtils jsonFileUtils,
                             final EntityLoaderRegistry entityLoaderRegistry,
                             final BundleEntityBuilder bundleEntityBuilder,
                             final BundleCache cache) {
        this.documentFileUtils = documentFileUtils;
        this.jsonFileUtils = jsonFileUtils;
        this.documentTools = documentTools;
        this.entityLoaderRegistry = entityLoaderRegistry;
        this.bundleEntityBuilder = bundleEntityBuilder;
        this.cache = cache;
    }

    public void buildBundle(File rootDir, File outputDir, List<DependentBundle> dependencies, ProjectInfo projectInfo) {
        final DocumentBuilder documentBuilder = documentTools.getDocumentBuilder();
        final Document document = documentBuilder.newDocument();

        final Bundle bundle = new Bundle(projectInfo);

        if (rootDir != null) {
            // Load the entities to build a deployment bundle
            final Collection<EntityLoader> entityLoaders = entityLoaderRegistry.getEntityLoaders();
            entityLoaders.parallelStream().forEach(e -> e.load(bundle, rootDir));

            // create the folder tree
            FolderLoaderUtils.createFolders(bundle, rootDir, bundle.getServices());

            //Load metadata Dependencies
            final Set<Bundle> dependencyBundles = new HashSet<>();
            for (DependentBundle  dependentBundle: dependencies) {
                List<File> metadataFiles = new ArrayList<>();
                final File dependencyFile = dependentBundle.getDependencyFile();
                //if the given file does not exists (in case of project dependency the dependency file is old artifact) read metadata file from parent folder
                if (!dependencyFile.exists()) {
                    File bundleDirectory = dependencyFile.getParentFile();
                    if (bundleDirectory != null && bundleDirectory.isDirectory()) {
                        File[] files = bundleDirectory.listFiles();
                        metadataFiles = Stream.of(files).filter(file -> file.getName().endsWith(JsonFileUtils.METADATA_FILE_NAME_SUFFIX)).collect(Collectors.toList());
                    }
                }

                if (!metadataFiles.isEmpty()) {
                    metadataFiles.forEach(file -> {
                        Bundle bundleDependency = cache.getBundleFromMetadataFile(file);
                        if (bundleDependency != null) {
                            dependencyBundles.add(bundleDependency);
                        }
                    });
                } else if (dependencyFile.getName().endsWith(JsonFileUtils.METADATA_FILE_NAME_SUFFIX)) {
                    Bundle bundleDependency = cache.getBundleFromMetadataFile(dependencyFile);
                    if (bundleDependency != null) {
                        //add dependent bundle only for bundle tag dependencies
                        bundleDependency.setDependentBundleFrom(dependentBundle);
                        dependencyBundles.add(bundleDependency);
                    }
                } else if (dependencyFile.getName().endsWith(BUNDLE_EXTENSION)) {
                    Bundle bundleDependency = cache.getBundleFromFile(dependencyFile);
                    //add dependent bundle only for bundle tag dependencies
                    bundleDependency.setDependentBundleFrom(dependentBundle);
                    dependencyBundles.add(bundleDependency);
                }
            }
            bundle.setDependencies(dependencyBundles);

            // Log overridden entities
            if (!dependencyBundles.isEmpty()) {
                logOverriddenEntities(bundle, dependencyBundles, Service.class);
                logOverriddenEntities(bundle, dependencyBundles, Policy.class);
            }
        }

        //Zip
        final Map<String, BundleArtifacts> bundleElementMap = bundleEntityBuilder.build(bundle,
                EntityBuilder.BundleType.DEPLOYMENT, document, projectInfo);
        bundleElementMap.forEach((k, v) -> writeBundleArtifacts(k, v, outputDir));
    }

    private void writeBundleArtifacts(final String bundleName, final BundleArtifacts bundleArtifacts, File outputDir) {
        documentFileUtils.createFile(bundleArtifacts.getBundle(), new File(outputDir,
                bundleArtifacts.getBundleFileName()).toPath());
        documentFileUtils.createFile(bundleArtifacts.getDeleteBundle(), new File(outputDir,
                bundleArtifacts.getDeleteBundleFileName()).toPath());
        jsonFileUtils.createBundleMetadataFile(bundleArtifacts.getBundleMetadata(), bundleName, outputDir);
    }

    protected <E extends GatewayEntity> void logOverriddenEntities(Bundle bundle, Set<Bundle> dependencyBundles, Class<E> entityClass) {
        bundle.getEntities(entityClass).keySet().forEach(entityName ->
                dependencyBundles.forEach(dependencyBundle -> {
                    if (dependencyBundle.getEntities(entityClass).containsKey(entityName)) {
                        LOGGER.log(Level.INFO, "{0} policy will be overwritten by local version", entityName);
                    }
                })
        );
    }
}
