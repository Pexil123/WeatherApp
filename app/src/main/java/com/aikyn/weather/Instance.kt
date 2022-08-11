package com.aikyn.weather

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Instance (
    var info: String,
    var miniInfo: String,
    var jsonInfo: String,
    var imageUrl: String,
    var mapUrl: String
) : Parcelable