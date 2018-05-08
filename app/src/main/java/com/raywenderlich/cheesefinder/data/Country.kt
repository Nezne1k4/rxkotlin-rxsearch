package com.raywenderlich.cheesefinder.data

import com.google.gson.annotations.SerializedName

data class Country(

	@field:SerializedName("label")
	val label: String? = null,

	@field:SerializedName("value")
	val value: String? = null
)