/*
 * Copyright (c) 2018 CA. All rights reserved.
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package com.ca.apim.gateway.cagatewayexport.tasks.explode.writer;

import com.ca.apim.gateway.cagatewayexport.tasks.explode.bundle.Bundle;
import com.ca.apim.gateway.cagatewayexport.tasks.explode.bundle.entity.EncassEntity;
import com.ca.apim.gateway.cagatewayexport.tasks.explode.writer.beans.Encass;
import com.ca.apim.gateway.cagatewayexport.tasks.explode.writer.beans.EncassParam;
import com.ca.apim.gateway.cagatewayexport.util.file.DocumentFileUtils;
import com.ca.apim.gateway.cagatewayexport.util.json.JsonTools;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.util.Map;
import java.util.stream.Collectors;

import static com.ca.apim.gateway.cagatewayexport.tasks.explode.writer.WriterHelper.writeFile;

@Singleton
public class EncassWriter implements EntityWriter {
    private static final String ENCASS_FILE = "encass";
    private final DocumentFileUtils documentFileUtils;
    private final JsonTools jsonTools;

    @Inject
    EncassWriter(DocumentFileUtils documentFileUtils, JsonTools jsonTools) {
        this.documentFileUtils = documentFileUtils;
        this.jsonTools = jsonTools;
    }

    @Override
    public void write(Bundle bundle, File rootFolder) {
        Map<String, Encass> encassBeans = bundle.getEntities(EncassEntity.class)
                .values()
                .stream()
                .collect(Collectors.toMap(EncassEntity::getPath, this::getEncassBean));

        writeFile(rootFolder, documentFileUtils, jsonTools, encassBeans, ENCASS_FILE, Encass.class);
    }

    @NotNull
    private Encass getEncassBean(EncassEntity encassEntity) {
        Encass encassBean = new Encass();
        encassBean.setArguments(encassEntity.getArguments().stream().map(encassParam -> new EncassParam(encassParam.getName(), encassParam.getType())).collect(Collectors.toSet()));
        encassBean.setResults(encassEntity.getResults().stream().map(encassParam -> new EncassParam(encassParam.getName(), encassParam.getType())).collect(Collectors.toSet()));
        return encassBean;
    }
}