/*
 * Copyright (c) 2018 CA. All rights reserved.
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package com.ca.apim.gateway.cagatewayconfig.bundle.builder;

import com.ca.apim.gateway.cagatewayconfig.ProjectInfo;
import com.ca.apim.gateway.cagatewayconfig.beans.*;
import com.ca.apim.gateway.cagatewayconfig.util.IdGenerator;
import com.ca.apim.gateway.cagatewayconfig.util.paths.PathUtils;
import com.ca.apim.gateway.cagatewayconfig.util.xml.DocumentParseException;
import com.ca.apim.gateway.cagatewayconfig.util.xml.DocumentTools;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.ca.apim.gateway.cagatewayconfig.util.entity.EntityTypes.SERVICE_TYPE;
import static com.ca.apim.gateway.cagatewayconfig.util.gateway.BundleElementNames.*;
import static com.ca.apim.gateway.cagatewayconfig.util.properties.PropertyConstants.*;
import static com.ca.apim.gateway.cagatewayconfig.util.xml.DocumentUtils.*;
import static org.junit.jupiter.api.Assertions.*;

class ServiceEntityBuilderTest {
    private ProjectInfo projectInfo = new ProjectInfo("TestName", "TestGroup", "1.0");

    @Test
    void buildNoServices() {
        ServiceEntityBuilder builder = new ServiceEntityBuilder(DocumentTools.INSTANCE, new IdGenerator());

        Bundle bundle = new Bundle(projectInfo);

        List<Entity> serviceEntities = builder.build(bundle, EntityBuilder.BundleType.DEPLOYMENT, DocumentTools.INSTANCE.getDocumentBuilder().newDocument());

        assertEquals(0, serviceEntities.size());
    }

    @Test
    void buildServicesWithoutPolicy() {
        ServiceEntityBuilder builder = new ServiceEntityBuilder(DocumentTools.INSTANCE, new IdGenerator());

        Bundle bundle = new Bundle(projectInfo);
        Folder parentFolder = new Folder();
        parentFolder.setId("asd");
        parentFolder.setName("my");
        parentFolder.setPath("my");
        parentFolder.setParentFolder(Folder.ROOT_FOLDER);

        Folder serviceParentFolder = new Folder();
        serviceParentFolder.setId("test");
        serviceParentFolder.setName("policy");
        serviceParentFolder.setPath("my/v1");
        serviceParentFolder.setParentFolder(parentFolder);

        bundle.putAllFolders(new HashMap<String, Folder>() {{
            put(parentFolder.getPath(), parentFolder);
            put(serviceParentFolder.getPath(), serviceParentFolder);
            put(Folder.ROOT_FOLDER.getPath(), Folder.ROOT_FOLDER);
        }});
        Service service = new Service();
        service.setHttpMethods(Stream.of("POST", "GET").collect(Collectors.toSet()));
        service.setUrl("/my/service/url");
        service.setProperties(new HashMap<String, Object>() {{
            put("key1", "value1");
            put("key2", "value2");
        }});
        service.setName("path");
        service.setParentFolder(serviceParentFolder);

        bundle.putAllServices(new HashMap<String, Service>() {{
            put("/my/policy/path", service);
        }});
        assertThrows(EntityBuilderException.class, () -> builder.build(bundle, EntityBuilder.BundleType.DEPLOYMENT, DocumentTools.INSTANCE.getDocumentBuilder().newDocument()));
    }

    @Test
    void buildOneServices() throws DocumentParseException {
        ServiceEntityBuilder builder = new ServiceEntityBuilder(DocumentTools.INSTANCE, new IdGenerator());

        Bundle bundle = new Bundle(projectInfo);

        Folder serviceParentFolder = setUpFolderAndPolicy(bundle, "/my/policy.xml", "policy");

        Service service = new Service();
        service.setHttpMethods(Stream.of("POST", "GET").collect(Collectors.toSet()));
        service.setUrl("/my/service/url");
        service.setPolicy("/my/policy.xml");
        service.setParentFolder(serviceParentFolder);
        service.setProperties(new HashMap<String, Object>() {{
            put("key1", "value1");
            put("ENV.key.environment", "something");
        }});
        service.setName("service1");

        bundle.putAllServices(new HashMap<String, Service>() {{
            put("/v1/service1", service);
        }});

        List<Entity> services = builder.build(bundle, EntityBuilder.BundleType.DEPLOYMENT, DocumentTools.INSTANCE.getDocumentBuilder().newDocument());

        assertEquals(1, services.size());

        Entity serviceEntity = services.get(0);

        verifyService(service, serviceEntity, 1, "service" + 1);
    }

    @Test
    void buildTwoServicesSamePolicy() throws DocumentParseException {
        ServiceEntityBuilder builder = new ServiceEntityBuilder(DocumentTools.INSTANCE, new IdGenerator());

        Bundle bundle = new Bundle(projectInfo);
        Folder serviceParentFolder = setUpFolderAndPolicy(bundle, "/my/policy.xml", "policy");

        Service service1 = getService1(serviceParentFolder);
        service1.setName("service1");
        Service service2 = new Service();
        service2.setHttpMethods(Stream.of("POST", "GET").collect(Collectors.toSet()));
        service2.setUrl("/my/url");
        service2.setPolicy("/my/policy.xml");
        service2.setParentFolder(bundle.getFolders().get("my"));
        service2.setProperties(new HashMap<String, Object>() {{
            put("key2", "value2");
            put("ENV.key.environment", "something");
        }});
        service2.setName("service2");

        bundle.putAllServices(new HashMap<String, Service>() {{
            put("my/v1/service1", service1);
            put("my/service2", service2);
        }});

        verifyMultipleServices(builder, bundle, service1, service2);
    }

    @Test
    void buildOneSoapServiceWithWSDLAndNoXSD() throws DocumentParseException {
        ServiceEntityBuilder builder = new ServiceEntityBuilder(DocumentTools.INSTANCE, new IdGenerator());

        Bundle bundle = new Bundle(projectInfo);

        Folder serviceParentFolder = setUpFolderAndPolicy(bundle, "/soap/policy.xml", "policy");

        Service service = new Service();
        service.setHttpMethods(Stream.of("POST", "GET").collect(Collectors.toSet()));
        service.setUrl("/soap/service/url");
        service.setPolicy("/soap/policy.xml");
        service.setParentFolder(serviceParentFolder);
        service.setName("soap-service1");
        service.setProperties(new HashMap<String, Object>() {{
            put("key1", "value1");
            put("ENV.key.environment", "something");
        }});
        service.setWssProcessingEnabled(true);
        service.setSoapVersion("1.1");
        SoapResource wsdlBean = new SoapResource();
        wsdlBean.setRootUrl("/test/rooturl/for/soap.wsdl");
        wsdlBean.setPath(PathUtils.unixPath(serviceParentFolder.getPath(), service.getName(), "soap.wsdl"));
        wsdlBean.setContent("wsdl xml content");
        wsdlBean.setType(SoapResourceType.WSDL.getType());
        service.setWsdlRootUrl(wsdlBean.getRootUrl());
        service.addSoapResource(wsdlBean);

        bundle.putAllServices(new HashMap<String, Service>() {{
            put(service.getName(), service);
        }});
        bundle.putAllSoapResources(ImmutableMap.of(wsdlBean.getPath(), wsdlBean));
        bundle.buildFolderTree();

        List<Entity> services = builder.build(bundle, EntityBuilder.BundleType.DEPLOYMENT, DocumentTools.INSTANCE.getDocumentBuilder().newDocument());

        assertEquals(1, services.size());

        Entity serviceEntity = services.get(0);

        verifyService(service, serviceEntity, 1,  "soap-service" + 1);
    }

    @Test
    void buildTwoServicesOneSoapWithWSDLAndXSDAndOneRest() throws DocumentParseException {
        ServiceEntityBuilder builder = new ServiceEntityBuilder(DocumentTools.INSTANCE, new IdGenerator());

        Bundle bundle = new Bundle(projectInfo);
        Folder service1ParentFolder = setUpFolderAndPolicy(bundle, "/my/policy.xml", "policy");
        Folder service2ParentFolder = setUpFolderAndPolicy(bundle, "/soap/policy.xml", "policy");

        Service service1 = getService1(service1ParentFolder);
        service1.setName("service1");

        Service service2 = new Service();
        service2.setName("service2");
        service2.setHttpMethods(Stream.of("POST", "GET").collect(Collectors.toSet()));
        service2.setUrl("/soap/service/url");
        service2.setPolicy("/soap/policy.xml");
        service2.setParentFolder(service2ParentFolder);
        service2.setProperties(new HashMap<String, Object>() {{
            put("key2", "value2");
            put("ENV.key.environment", "something");
        }});
        service2.setWssProcessingEnabled(true);
        service2.setSoapVersion("1.1");
        SoapResource wsdlBean = new SoapResource();
        wsdlBean.setRootUrl("/test/rooturl/for/soap.wsdl");
        wsdlBean.setContent("wsdl xml content");
        wsdlBean.setPath(PathUtils.unixPath(service2ParentFolder.getPath(), service2.getName(), "soap.wsdl"));
        wsdlBean.setType(SoapResourceType.WSDL.getType());

        SoapResource xsdBean = new SoapResource();
        xsdBean.setRootUrl("/test/rooturl/for/soap.xsd");
        xsdBean.setContent("xsd xml content");
        xsdBean.setPath(PathUtils.unixPath(service2ParentFolder.getPath(), service2.getName(), "soap.xsd"));
        xsdBean.setType(SoapResourceType.XMLSCHEMA.getType());

        service2.setWsdlRootUrl(wsdlBean.getRootUrl());
        service2.addSoapResource(wsdlBean);
        service2.addSoapResource(xsdBean);

        bundle.putAllServices(new HashMap<String, Service>() {{
            put(service1.getName(), service1);
            put(service2.getName(), service2);
        }});
        bundle.putAllSoapResources(ImmutableMap.of(wsdlBean.getPath(), wsdlBean, xsdBean.getPath(), xsdBean));
        bundle.buildFolderTree();

        verifyMultipleServices(builder, bundle, service1, service2);
    }

    private void verifyMultipleServices(ServiceEntityBuilder builder, Bundle bundle, Service service1, Service service2) throws DocumentParseException {
        List<Entity> services = builder.build(bundle, EntityBuilder.BundleType.DEPLOYMENT, DocumentTools.INSTANCE.getDocumentBuilder().newDocument());

        assertEquals(2, services.size());
        if (services.get(0).getName().equals("service1")) {
            verifyService(service1, services.get(0), 1,  "service" + 1 );
            verifyService(service2, services.get(1), 2, "service" + 2 );
        } else {
            verifyService(service2, services.get(0), 2, "service" + 2 );
            verifyService(service1, services.get(1), 1,  "service" + 1 );
        }
    }

    @NotNull
    private Service getService1(Folder serviceParentFolder) {
        Service service1 = new Service();
        service1.setHttpMethods(Stream.of("POST", "GET").collect(Collectors.toSet()));
        service1.setUrl("/my/service/url");
        service1.setPolicy("/my/policy.xml");
        service1.setParentFolder(serviceParentFolder);
        service1.setProperties(new HashMap<String, Object>() {{
            put("key1", "value1");
            put("ENV.key.environment", "something");
        }});
        return service1;
    }

    @NotNull
    private Folder setUpFolderAndPolicy(Bundle bundle, String policyPath, String policyName) throws DocumentParseException {
        Folder parentFolder = new Folder();
        parentFolder.setId("asd");
        parentFolder.setName("my");
        parentFolder.setPath("my");
        parentFolder.setParentFolder(Folder.ROOT_FOLDER);

        Folder serviceParentFolder = new Folder();
        serviceParentFolder.setId("test");
        serviceParentFolder.setName("v1");
        serviceParentFolder.setPath("my/v1");
        serviceParentFolder.setParentFolder(parentFolder);

        bundle.putAllFolders(new HashMap<String, Folder>() {{
            put(parentFolder.getPath(), parentFolder);
            put(serviceParentFolder.getPath(), serviceParentFolder);
            put(Folder.ROOT_FOLDER.getPath(), Folder.ROOT_FOLDER);
        }});

        Policy policy = new Policy();
        policy.setName(policyName);
        policy.setPath(policyPath);
        policy.setPolicyXML("<?xml version=\"1.0\" encoding=\"UTF-8\"?> <wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\"> <wsp:All wsp:Usage=\"Required\"/> </wsp:Policy>\n");
        policy.setPolicyDocument(DocumentTools.INSTANCE.parse(policy.getPolicyXML()).getDocumentElement());
        policy.setParentFolder(parentFolder);
        bundle.putAllPolicies(new HashMap<String, Policy>() {{
            put(policy.getPath(), policy);
        }});
        return serviceParentFolder;
    }

    private void verifyService(Service service, Entity serviceEntity, int serviceNumber, String expectedServiceName) throws DocumentParseException {
        assertEquals(SERVICE_TYPE, serviceEntity.getType());
        assertNotNull(serviceEntity.getId());
        Element serviceEntityXml = serviceEntity.getXml();
        assertEquals(SERVICE, serviceEntityXml.getTagName());
        Element serviceDetails = getSingleElement(serviceEntityXml, SERVICE_DETAIL);
        Element serviceName = getSingleElement(serviceDetails, NAME);
        assertEquals(expectedServiceName, serviceName.getTextContent());

        Element serviceMappings = getSingleElement(serviceDetails, SERVICE_MAPPINGS);
        Element serviceHttpMappings = getSingleElement(serviceMappings, HTTP_MAPPING);
        Element serviceUrlPattern = getSingleElement(serviceHttpMappings, URL_PATTERN);
        assertEquals(service.getUrl(), serviceUrlPattern.getTextContent());

        Element serviceHttpVerbs = getSingleElement(serviceHttpMappings, VERBS);
        NodeList verbList = serviceHttpVerbs.getElementsByTagName(VERB);
        assertEquals(2, verbList.getLength());
        assertEquals("POST", verbList.item(0).getTextContent());
        assertEquals("GET", verbList.item(1).getTextContent());

        Element serviceProperties = getSingleElement(serviceDetails, PROPERTIES);
        NodeList propertyList = serviceProperties.getElementsByTagName(PROPERTY);
        boolean isSoapService = CollectionUtils.isNotEmpty(service.getSoapResources());
        if (isSoapService) {
            assertEquals(5, propertyList.getLength());
        } else {
            assertEquals(3, propertyList.getLength());
        }

        final String propKey = "property.ENV.key.environment";
        boolean checkPropKey1 = false, checkWssProcessingEnabled = false, checkSoap = false, checkSoapVersion = false, checkPropKey2 = false;
        for (int counter = 0; counter < propertyList.getLength(); counter++) {
            Node property = propertyList.item(counter);
            if (property.getAttributes().getNamedItem(ATTRIBUTE_KEY).getTextContent().equals("property.key" + serviceNumber)) {
                assertEquals("value" + serviceNumber, getSingleElement((Element) property, STRING_VALUE).getTextContent());
                checkPropKey1 = true;
            } else {
                switch (property.getAttributes().getNamedItem(ATTRIBUTE_KEY).getTextContent()) {
                    case KEY_VALUE_WSS_PROCESSING_ENABLED:
                        if (isSoapService) {
                            assertTrue(Boolean.parseBoolean(getSingleElement((Element) property, BOOLEAN_VALUE).getTextContent()));
                        } else {
                            assertFalse(Boolean.parseBoolean(getSingleElement((Element) property, BOOLEAN_VALUE).getTextContent()));
                        }
                        checkWssProcessingEnabled = true;
                        break;
                    case KEY_VALUE_SOAP:
                        assertTrue(Boolean.parseBoolean(getSingleElement((Element) property, BOOLEAN_VALUE).getTextContent()));
                        checkSoap = true;
                        break;
                    case KEY_VALUE_SOAP_VERSION:
                        assertEquals("1.1", getSingleElement((Element) property, STRING_VALUE).getTextContent());
                        checkSoapVersion = true;
                        break;
                    case propKey:
                        assertEquals("SERVICE_PROPERTY_ENV." + expectedServiceName + ".key.environment", getSingleElement((Element) property, STRING_VALUE).getTextContent());
                        checkPropKey2 = true;
                        break;
                }
            }
        }
        if (isSoapService) {
            assertTrue(checkSoap);
            assertTrue(checkSoapVersion);
        }
        assertTrue(checkPropKey1);
        assertTrue(checkPropKey2);
        assertTrue(checkWssProcessingEnabled);


        Element serviceResources = getSingleElement(serviceEntityXml, RESOURCES);
        if(isSoapService) {
            List<Element> serviceResourceSets = getChildElements(serviceResources, RESOURCE_SET);
            Element serviceResource = null;
            Element wsdlResource = null;
            Element xsdResource = null;

            for(Element serviceResourceSet : serviceResourceSets) {
                String tagValue = serviceResourceSet.getAttribute(ATTRIBUTE_TAG);
                switch (tagValue) {
                    case TAG_VALUE_POLICY:
                        serviceResource = getSingleChildElement(serviceResourceSet, RESOURCE);
                        break;
                    case TAG_VALUE_WSDL:
                        List<Element> resources = getChildElements(serviceResourceSet, RESOURCE);
                        wsdlResource = resources.stream().filter(e -> SoapResourceType.WSDL.getType().equals(e.getAttribute(ATTRIBUTE_TYPE))).findFirst().orElse(null);
                        xsdResource = resources.stream().filter(e -> SoapResourceType.XMLSCHEMA.getType().equals(e.getAttribute(ATTRIBUTE_TYPE))).findFirst().orElse(null);
                        break;
                }
            }

            assertEquals(TAG_VALUE_POLICY, serviceResource.getAttributes().getNamedItem(ATTRIBUTE_TYPE).getTextContent());
            assertNotNull(serviceResource.getTextContent());
            assertEquals(SoapResourceType.WSDL.getType(), wsdlResource.getAttributes().getNamedItem(ATTRIBUTE_TYPE).getTextContent());
            assertNotNull(wsdlResource.getTextContent());
            if (xsdResource != null) {
                assertEquals(SoapResourceType.XMLSCHEMA.getType(), xsdResource.getAttributes().getNamedItem(ATTRIBUTE_TYPE).getTextContent());
                assertNotNull(xsdResource.getTextContent());
            }
        } else {
            Element serviceResourceSet = getSingleElement(serviceResources, RESOURCE_SET);
            Element serviceResource = getSingleElement(serviceResourceSet, RESOURCE);
            assertEquals(TAG_VALUE_POLICY, serviceResource.getAttributes().getNamedItem(ATTRIBUTE_TYPE).getTextContent());
            assertNotNull(serviceResource.getTextContent());
        }
    }
}