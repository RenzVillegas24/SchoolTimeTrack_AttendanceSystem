

import java.time.LocalDate
import java.time.LocalTime

data class AttendanceRecord(
    val date: LocalDate,
    val timeIn: LocalTime?,
    val timeOut: LocalTime?
)