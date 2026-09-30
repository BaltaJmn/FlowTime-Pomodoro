package com.baltajmn.flowtime.core.persistence.model

import kotlinx.serialization.Serializable

@Serializable
data class RangeModel(var totalRange: Int, var endRange: Int, var rest: Int)
