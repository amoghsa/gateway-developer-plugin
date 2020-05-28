/*
 * Copyright (c) 2018 CA. All rights reserved.
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package com.ca.apim.gateway.cagatewayconfig.bundle.builder;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"type", "name", "id", "guid"})
public interface Metadata {

    String getType();
    String getName();
    String getId();
    String getGuid();
}
