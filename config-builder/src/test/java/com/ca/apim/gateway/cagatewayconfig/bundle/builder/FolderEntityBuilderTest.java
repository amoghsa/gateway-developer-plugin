/*
 * Copyright (c) 2018 CA. All rights reserved.
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package com.ca.apim.gateway.cagatewayconfig.bundle.builder;

import com.ca.apim.gateway.cagatewayconfig.ProjectInfo;
import com.ca.apim.gateway.cagatewayconfig.beans.Bundle;
import com.ca.apim.gateway.cagatewayconfig.beans.Folder;
import com.ca.apim.gateway.cagatewayconfig.bundle.builder.EntityBuilder.BundleType;
import com.ca.apim.gateway.cagatewayconfig.util.IdGenerator;
import com.ca.apim.gateway.cagatewayconfig.util.entity.EntityTypes;
import com.ca.apim.gateway.cagatewayconfig.util.string.CharacterBlacklistUtil;
import com.ca.apim.gateway.cagatewayconfig.util.xml.DocumentTools;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

import java.util.List;
import java.util.Map;

import static com.ca.apim.gateway.cagatewayconfig.bundle.builder.EntityBuilder.BundleType.DEPLOYMENT;
import static com.ca.apim.gateway.cagatewayconfig.util.TestUtils.createFolder;
import static com.ca.apim.gateway.cagatewayconfig.util.TestUtils.createRoot;
import static com.ca.apim.gateway.cagatewayconfig.util.gateway.BundleElementNames.*;
import static com.ca.apim.gateway.cagatewayconfig.util.xml.DocumentUtils.getSingleChildElement;
import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.trimToNull;
import static org.junit.jupiter.api.Assertions.*;

class FolderEntityBuilderTest {
    private ProjectInfo projectInfo = new ProjectInfo("TestName", "TestGroup", "1.0");
    private static final IdGenerator ID_GENERATOR = new IdGenerator();
    private static final String FOLDER_1 = "Folder1";
    private static final String FOLDER_2 = "Folder2";
    private static final String FOLDER_3 = "Folder3";

    @Test
    void buildFromEmptyBundle_noFolders() {
        FolderEntityBuilder builder = new FolderEntityBuilder(ID_GENERATOR);
        final List<Entity> entities = builder.build(new Bundle(projectInfo), DEPLOYMENT, DocumentTools.INSTANCE.getDocumentBuilder().newDocument());

        assertTrue(entities.isEmpty());
    }

    @Test
    void buildMissingRoot() {
        FolderEntityBuilder builder = new FolderEntityBuilder(ID_GENERATOR);
        Bundle bundle = new Bundle(projectInfo);
        bundle.putAllFolders(createTestFolders(null));

        assertThrows(EntityBuilderException.class, () -> builder.build(bundle, DEPLOYMENT, DocumentTools.INSTANCE.getDocumentBuilder().newDocument()));
    }

    @Test
    void buildEnvironment() {
        FolderEntityBuilder builder = new FolderEntityBuilder(ID_GENERATOR);
        Bundle bundle = new Bundle(projectInfo);
        Folder root = createRoot();
        bundle.getFolders().put(EMPTY, root);
        bundle.putAllFolders(createTestFolders(root));

        final List<Entity> entities = builder.build(bundle, BundleType.ENVIRONMENT, DocumentTools.INSTANCE.getDocumentBuilder().newDocument());

        assertNotNull(entities);
        assertTrue(entities.isEmpty());
    }

    @Test
    void buildDeployment() {
        FolderEntityBuilder builder = new FolderEntityBuilder(ID_GENERATOR);
        Bundle bundle = new Bundle(projectInfo);
        Folder root = createRoot();
        bundle.getFolders().put(EMPTY, root);
        bundle.putAllFolders(createTestFolders(root));

        final List<Entity> entities = builder.build(bundle, DEPLOYMENT, DocumentTools.INSTANCE.getDocumentBuilder().newDocument());

        assertNotNull(entities);
        assertFalse(entities.isEmpty());
        assertEquals(4, entities.size());

        final Map<String, Entity> entitiesMap = entities.stream().collect(toMap(Entity::getName, identity()));
        entitiesMap.forEach((k, entity) -> {
            k = Folder.ROOT_FOLDER_NAME.equals(k) ? EMPTY : k;

            Folder folder = bundle.getFolders().get(k);
            assertNotNull(folder);
            if (k.equals(EMPTY)) {
                assertEquals(Folder.ROOT_FOLDER_NAME, folder.getName());
            } else {
                assertEquals(folder.getName(), CharacterBlacklistUtil.filterAndReplace(k.substring(k.lastIndexOf('/') + 1)));
            }
            assertNotNull(entity.getId());
            assertNotNull(entity.getXml());
            assertEquals(EntityTypes.FOLDER_TYPE, entity.getType());

            Element xml = entity.getXml();
            assertEquals(FOLDER, xml.getTagName());
            assertNotNull(getSingleChildElement(xml, NAME));
            assertEquals(folder.getId(), trimToNull(xml.getAttribute(ATTRIBUTE_ID)));
            assertEquals(ofNullable(folder.getParentFolder()).orElse(new Folder()).getId(), trimToNull(xml.getAttribute(ATTRIBUTE_FOLDER_ID)));
        });

    }

    private static Map<String, Folder> createTestFolders(Folder root) {
        Folder folder1 = createFolder(FOLDER_1, FOLDER_1, root);
        Folder folder2 = createFolder(FOLDER_2, FOLDER_2, root);
        Folder folder3 = createFolder(FOLDER_3, FOLDER_3, folder2);

        return ImmutableMap.of(folder1.getPath(), folder1, folder2.getPath(), folder2, folder3.getPath(), folder3);
    }

}