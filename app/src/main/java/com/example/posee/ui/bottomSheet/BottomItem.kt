package com.example.poseeui

import androidx.annotation.DrawableRes

data class BottomItem(
    @DrawableRes val imageRes: Int,
    val time : String,
    val explanation : String
)
