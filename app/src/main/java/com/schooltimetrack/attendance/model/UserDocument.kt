import android.os.Parcelable

@kotlinx.parcelize.Parcelize
data class UserDocument(
    val userType: String,
    val name: ArrayList<String>,
    val gradeOrSubject: String,
    val section: String,
    val age: Int,
    val address: ArrayList<String>,
    val addressId: String,
    val birthday: String,
    val gender: String,
    val profileImageId: String,
    val email: String,
    val contactNumber: ArrayList<String>
) : Parcelable