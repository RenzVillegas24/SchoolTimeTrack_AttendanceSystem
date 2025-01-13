package com.schooltimetrack.attendance.address

data class Region(
    val id: Int,
    val psgcCode: String?,
    val regDesc: String?,
    val regCode: String?
)

data class Province(
    val id: Int,
    val psgcCode: String?,
    val provDesc: String?,
    val regCode: String?,
    val provCode: String?
)

data class CityMun(
    val id: Int,
    val psgcCode: String?,
    val citymunDesc: String?,
    val regDesc: String?,
    val provCode: String?,
    val citymunCode: String?
)

data class Barangay(
    val id: Int,
    val brgyCode: String?,
    val brgyDesc: String?,
    val regCode: String?,
    val provCode: String?,
    val citymunCode: String?
)