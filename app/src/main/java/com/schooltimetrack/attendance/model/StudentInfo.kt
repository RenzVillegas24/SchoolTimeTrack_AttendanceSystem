package com.schooltimetrack.attendance.model

data class StudentInfo(
    val userType: String = "student",
    val email: String,
    val password: String,
    val name: Array<String>,
    val age: Int,
    val address: Array<String>,
    val addressId: String,
    val section: String,
    val grade: String,
    val contactNumber: Array<String>,
    val embedding: FloatArray
)