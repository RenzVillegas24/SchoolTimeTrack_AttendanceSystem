package com.schooltimetrack.attendance.model

data class TeacherInfo(
    val userType: String = "teacher",
    val email: String,
    val password: String,
    val name: Array<String>,
    val age: Int,
    val birthday: String,
    val address: Array<String>,
    val addressId: String,
    val grade: String,
    val section: String,
    val subject: String,
    val gender: String,
    val contactNumber: Array<String>,
    val embedding: FloatArray
)