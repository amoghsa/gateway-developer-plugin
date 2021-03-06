/*
 * Copyright (c) 2018 CA. All rights reserved.
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package com.ca.apim.gateway.cagatewayconfig;

import com.ca.apim.gateway.cagatewayconfig.environment.FullBundleCreator;
import com.ca.apim.gateway.cagatewayconfig.environment.MissingEnvironmentException;
import com.ca.apim.gateway.cagatewayconfig.util.environment.EnvironmentConfigurationUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;

import javax.inject.Inject;
import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.ca.apim.gateway.cagatewayconfig.ProjectDependencyUtils.filterBundleFiles;
import static com.ca.apim.gateway.cagatewayconfig.util.file.DocumentFileUtils.*;
import static com.ca.apim.gateway.cagatewayconfig.util.file.FileUtils.collectFiles;
import static com.ca.apim.gateway.cagatewayconfig.util.gateway.BuilderUtils.removeAllSpecialChars;
import static com.ca.apim.gateway.cagatewayconfig.util.injection.InjectionRegistry.getInstance;
import static com.ca.apim.gateway.cagatewayconfig.util.file.JsonFileUtils.METADATA_FILE_NAME_SUFFIX;
import static org.apache.commons.lang3.StringUtils.EMPTY;

/**
 * The BuildFullBundleTask task will grab provided environment properties and build a single bundle merged with the deployment bundles.
 */
public class BuildFullBundleTask extends DefaultTask {

    private final EnvironmentConfigurationUtils environmentConfigurationUtils;
    private final Property<Map> environmentConfig;
    private final ConfigurableFileCollection dependencyBundles;
    private final DirectoryProperty into;
    private final Property<Boolean> detemplatizeDeploymentBundles;
    private final DirectoryProperty configFolder;
    private final Property<String> configName;
    private final Property<Map> envConfig;

    @Inject
    public BuildFullBundleTask() {
        environmentConfigurationUtils = getInstance(EnvironmentConfigurationUtils.class);
        environmentConfig = getProject().getObjects().property(Map.class);
        envConfig = getProject().getObjects().property(Map.class);
        dependencyBundles = getProject().files();
        into = newOutputDirectory();
        detemplatizeDeploymentBundles = getProject().getObjects().property(Boolean.class);
        configFolder = newInputDirectory();
        configName = getProject().getObjects().property(String.class);
    }

    @InputFiles
    ConfigurableFileCollection getDependencyBundles() {
        return dependencyBundles;
    }

    @OutputDirectory
    DirectoryProperty getInto() {
        return into;
    }

    @Input
    @Optional
    Property<Map> getEnvironmentConfig() {
        return environmentConfig;
    }

    @Input
    @Optional
    Property<Map> getEnvConfig() {
        return envConfig;
    }

    @Input
    Property<Boolean> getDetemplatizeDeploymentBundles() {
        return detemplatizeDeploymentBundles;
    }

    @InputDirectory
    @Optional
    DirectoryProperty getConfigFolder() {
        return configFolder;
    }

    @Input
    @Optional
    Property<String> getConfigName() {
        return configName;
    }

    @TaskAction
    public void perform() {
        final FullBundleCreator fullBundleCreator = getInstance(FullBundleCreator.class);
        final String bundleDirectory = into.getAsFile().get().getPath();
        final String configurationName = configName != null ? removeAllSpecialChars(configName.get()) : EMPTY;
        final ProjectInfo projectInfo = new ProjectInfo(getProject().getName(), getProject().getGroup().toString(),
                getProject().getVersion().toString(), configurationName);
        final List<File> metaDataFiles = collectFiles(bundleDirectory, METADATA_FILE_NAME_SUFFIX);
        if (metaDataFiles.isEmpty()) {
            throw new MissingEnvironmentException("Metadata file does not exist.");
        }
        File configuredFolder = configFolder.getAsFile().getOrNull();
        Map environmentEntities = java.util.Optional.ofNullable(envConfig.getOrNull()).orElse(environmentConfig.getOrNull());

        metaDataFiles.stream().forEach(metaDataFile-> {

            final Pair<String, Map<String, String>> bundleEnvironmentValues = environmentConfigurationUtils.parseBundleMetadata(metaDataFile, configuredFolder);
            if (null != bundleEnvironmentValues) {
                String fullInstallBundleFilename = bundleEnvironmentValues.getLeft();
                if (StringUtils.isNotBlank(projectInfo.getVersion())) {
                    fullInstallBundleFilename = fullInstallBundleFilename + PREFIX_FULL;
                }
                fullInstallBundleFilename = fullInstallBundleFilename + INSTALL_BUNDLE_EXTENSION;
                //read environment properties from environmentConfig and merge it with config folder entities
                if(environmentEntities != null){
                    bundleEnvironmentValues.getRight().putAll(environmentConfigurationUtils.parseEnvironmentValues(environmentEntities));
                }
                fullBundleCreator.createFullBundle(
                        bundleEnvironmentValues,
                        filterBundleFiles(dependencyBundles.getAsFileTree().getFiles()),
                        bundleDirectory,
                        projectInfo,
                        fullInstallBundleFilename,
                        configuredFolder != null ? configuredFolder.getPath() : EMPTY,
                        detemplatizeDeploymentBundles.get()
                );
            }
        });
    }
}
