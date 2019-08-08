package com.cyberlink.facemedemo.data

import android.graphics.Bitmap
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import com.cyberlink.faceme.FaceAttribute
import com.cyberlink.faceme.FaceFeature

data class SavedFace(@Nullable var feature: FaceFeature?, @Nullable var attribute: FaceAttribute?, @Nullable var bitmap: Bitmap?, @NonNull var path:String, @NonNull var name:String)