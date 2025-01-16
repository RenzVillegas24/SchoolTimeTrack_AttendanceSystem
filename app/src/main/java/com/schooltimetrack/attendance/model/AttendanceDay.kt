data class AttendanceDay(
    val date: LocalDate,
    val type: ViewType,
    val timeIn: LocalTime? = null,
    val timeOut: LocalTime? = null,
    val targetTimeIn: LocalTime = LocalTime.of(8, 0),
    val targetTimeOut: LocalTime = LocalTime.of(17, 0),
    val isAttended: Boolean = false,
    val isLate: Boolean = false
)

enum class ViewType {
    PREVIOUS, CURRENT, FUTURE, NONE
}