/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * CyberLink FaceMe (R) SDK
 * Copyright (C) 2018 CyberLink Corp. All rights reserved.
 * https://www.cyberlink.com
 */
package com.mirobotic.facematcher.sdk.manageface;

interface IManageCollectionListener {
    void onEditCollection(CollectionHolder collectionHolder);
    void onDeleteCollection(CollectionHolder collectionHolder);
    void onClickCollection(CollectionHolder collectionHolder);
}
