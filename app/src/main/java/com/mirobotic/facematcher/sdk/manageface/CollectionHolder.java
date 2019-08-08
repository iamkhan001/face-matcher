/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * CyberLink FaceMe (R) SDK
 * Copyright (C) 2018 CyberLink Corp. All rights reserved.
 * https://www.cyberlink.com
 */
package com.mirobotic.facematcher.sdk.manageface;

import java.util.ArrayList;

final class CollectionHolder {

    public final long collectionId;
    public final String collectionName;
    public final ArrayList<Long> faceIds;

    public CollectionHolder(long collectionId, String collectionName, ArrayList<Long> faceIds) {
        this.collectionId = collectionId;
        this.collectionName = collectionName;
        this.faceIds = faceIds;
    }
}
