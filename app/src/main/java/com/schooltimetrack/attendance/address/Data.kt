package com.schooltimetrack.attendance.address

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "refbrgy")
data class Barangay(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "brgyCode") val barangayCode: String?,
    @ColumnInfo(name = "brgyDesc") val description: String?,
    @ColumnInfo(name = "regCode") val regionCode: String?,
    @ColumnInfo(name = "provCode") val provinceCode: String?,
    @ColumnInfo(name = "citymunCode") val cityMunicipalityCode: String?
)

@Entity(tableName = "refcitymun")
data class CityMunicipality(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val psgcCode: String?,
    @ColumnInfo(name = "citymunDesc") val description: String?,
    @ColumnInfo(name = "regDesc") val regionDescription: String?,
    @ColumnInfo(name = "provCode") val provinceCode: String?,
    @ColumnInfo(name = "citymunCode") val cityMunicipalityCode: String?
)

@Entity(tableName = "refprovince")
data class Province(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val psgcCode: String?,
    @ColumnInfo(name = "provDesc") val description: String?,
    @ColumnInfo(name = "regCode") val regionCode: String?,
    @ColumnInfo(name = "provCode") val provinceCode: String?
)

@Entity(tableName = "refregion")
data class Region(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val psgcCode: String?,
    @ColumnInfo(name = "regDesc") val description: String?,
    @ColumnInfo(name = "regCode") val regionCode: String?
)