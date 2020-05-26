/*
 * Copyright (c) 2018 CA. All rights reserved.
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package com.ca.apim.gateway.cagatewayconfig;

import com.ca.apim.gateway.cagatewayconfig.environment.EnvironmentBundleCreator;
import com.ca.apim.gateway.cagatewayconfig.environment.MissingEnvironmentException;
import com.ca.apim.gateway.cagatewayconfig.util.environment.EnvironmentConfigurationUtils;
import com.ca.apim.gateway.cagatewayconfig.util.file.JsonFileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import javax.inject.Inject;
import java.io.File;
import java.util.List;
import java.util.Map;

import static com.ca.apim.gateway.cagatewayconfig.environment.EnvironmentBundleCreationMode.PLUGIN;
import static com.ca.apim.gateway.cagatewayconfig.util.file.FileUtils.collectFiles;
import static com.ca.apim.gateway.cagatewayconfig.util.injection.InjectionRegistry.getInstance;
import static org.apache.commons.lang3.StringUtils.EMPTY;

/**
 * The BuildEnvironmentBundle task will grab provided environment properties and build a bundle.
 */
public class BuildEnvironmentBundleTask extends DefaultTask {

    private final DirectoryProperty into;
    private final Property<Map> environmentConfig;
    private final EnvironmentConfigurationUtils environmentConfigurationUtils;
    private final DirectoryProperty configFolder;
    private final Property<String> configName;

    @Inject
    public BuildEnvironmentBundleTask() {
        into = newOutputDirectory();
        environmentConfig = getProject().getObjects().property(Map.class);
        environmentConfigurationUtils = getInstance(EnvironmentConfigurationUtils.class);
        configFolder = newInputDirectory();
        configName = getProject().getObjects().property(String.class);
    }

    @OutputDirectory
    DirectoryProperty getInto() {
        return into;
    }

    @Input
    Property<Map> getEnvironmentConfig() {
        return environmentConfig;
    }

    @InputDirectory
    DirectoryProperty getConfigFolder() {
        return configFolder;
    }

    @Input
    Property<String> getConfigName() {
        return configName;
    }

    @TaskAction
    public void perform() {
        final EnvironmentBundleCreator environmentBundleCreator = getInstance(EnvironmentBundleCreator.class);
        final List<File> metaDataFiles = collectFiles(into.getAsFile().get().getPath(), JsonFileUtils.METADATA_FILE_NAME_SUFFIX);
        if (metaDataFiles.isEmpty()) {
            //read environment properties from environmentConfig
            if (environmentConfig.get().isEmpty()) {
                throw new MissingEnvironmentException("Metadata file does not exist and environment configuration is not specified in the gradle configuration file.");
            }
            final Map<String, String> environmentValuesFromConfig = environmentConfigurationUtils.parseEnvironmentValues(environmentConfig.get());
            final String bundleFileName = getProject().getName() + '-' + getProject().getVersion() + "-env.install.bundle";
            environmentBundleCreator.createEnvironmentBundle(
                    environmentValuesFromConfig,
                    into.getAsFile().get().getPath(),
                    into.getAsFile().get().getPath(),
                    EMPTY,
                    PLUGIN,
                    bundleFileName,
                    EMPTY
            );
        }
        metaDataFiles.stream().forEach(metaDataFile-> {
            final Pair<String, Map<String, String>> bundleEnvironmentValues = environmentConfigurationUtils.parseBundleMetadata(metaDataFile, configFolder.getAsFile().get());
            if (null != bundleEnvironmentValues) {
                final String bundleFileName = bundleEnvironmentValues.getLeft() + "-" + configName.get() + "env.install.bundle";
                //read environment properties from environmentConfig
                final Map<String, String> environmentValuesFromConfig = environmentConfigurationUtils.parseEnvironmentValues(environmentConfig.get());
                Map<String, String> environmentValuesFromMetadata = bundleEnvironmentValues.getRight();
                environmentValuesFromMetadata.putAll(environmentValuesFromConfig);
                environmentBundleCreator.createEnvironmentBundle(
                        environmentValuesFromMetadata,
                        into.getAsFile().get().getPath(),
                        into.getAsFile().get().getPath(),
                        EMPTY,
                        PLUGIN,
                        bundleFileName,
                        bundleEnvironmentValues.getLeft()
                );
            }
        });
    }
}
