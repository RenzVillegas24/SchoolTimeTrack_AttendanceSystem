package com.schooltimetrack.attendance.layout

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textview.MaterialTextView
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.transition.MaterialSharedAxis
import com.ml.shubham0204.facenet_android.domain.embeddings.FaceNet
import com.ml.shubham0204.facenet_android.domain.face_detection.FaceSpoofDetector
import com.schooltimetrack.attendance.R
import com.schooltimetrack.attendance.address.*
import com.schooltimetrack.attendance.ai.FaceRecognition
import com.schooltimetrack.attendance.model.StudentInfo
import com.schooltimetrack.attendance.model.TeacherInfo
import com.schooltimetrack.attendance.qr.EncryptedGenerator
import com.schooltimetrack.attendance.ui.FaceVerificationBottomSheet
import com.schooltimetrack.attendance.ui.GeneratedQRBottomSheet
import com.schooltimetrack.attendance.ui.ImageCropBottomSheet
import com.schooltimetrack.attendance.ui.SegmentedControl
import io.appwrite.Client
import io.appwrite.services.Account
import io.appwrite.services.Storage
import io.appwrite.ID
import io.appwrite.Query
import io.appwrite.models.InputFile
import io.appwrite.services.Databases
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.Calendar
import kotlin.math.floor
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.log

class SignUp : Fragment() {

    private lateinit var client: Client
    private lateinit var account: Account
    private lateinit var storage: Storage
    private lateinit var databases: Databases
    private lateinit var ivProfileImage: ImageView
    private lateinit var fArrProfileImage: FloatArray
    private lateinit var addressManager: AddressManager
    private var selectedRegion: Region? = null
    private var selectedProvince: Province? = null
    private var selectedCityMun: CityMun? = null
    private var selectedBrgy: Barangay? = null
    private var selectedImageUri: Uri? = null
    private val PICK_IMAGE_REQUEST = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize Appwrite
        client = Client(requireContext())
            .setEndpoint("https://cloud.appwrite.io/v1")
            .setProject("6773c26a001612edc5fb")
        account = Account(client)
        storage = Storage(client)
        databases = Databases(client)
        addressManager = AddressManager(requireContext())
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_sign_up, container, false)

        val segAccountType = view.findViewById<SegmentedControl>(R.id.segAccountType)
        val tvAccountType = view.findViewById<MaterialTextView>(R.id.tvAccountType)
        val etFirstName = view.findViewById<EditText>(R.id.etFirstName)
        val etMiddleName = view.findViewById<EditText>(R.id.etMiddleName)
        val etLastName = view.findViewById<EditText>(R.id.etLastName)
        val etSuffixName = view.findViewById<EditText>(R.id.etSuffixName)
        val tilGrade = view.findViewById<TextInputLayout>(R.id.tilGrade)
        val etGrade = view.findViewById<EditText>(R.id.etGrade)
        val tilSubject = view.findViewById<TextInputLayout>(R.id.tilSubject)
        val etSubject = view.findViewById<EditText>(R.id.etSubject)
        val etSection = view.findViewById<EditText>(R.id.etSection)
        val etAge = view.findViewById<EditText>(R.id.etAge)
        val etRegion = view.findViewById<AutoCompleteTextView>(R.id.etRegion)
        val etProvince = view.findViewById<AutoCompleteTextView>(R.id.etProvince)
        val etCityMun = view.findViewById<AutoCompleteTextView>(R.id.etCityMun)
        val etBrgy = view.findViewById<AutoCompleteTextView>(R.id.etBrgy)
        val etStreet = view.findViewById<EditText>(R.id.etStreet)
        val etCountryCode = view.findViewById<AutoCompleteTextView>(R.id.etCountryCode)
        val etContactNumber = view.findViewById<EditText>(R.id.etContactNumber)
        val etEmail = view.findViewById<EditText>(R.id.etEmail)
        val etPassword = view.findViewById<EditText>(R.id.etPassword)
        val etConfPassword = view.findViewById<EditText>(R.id.etConfPassword)
        val etBirthday = view.findViewById<EditText>(R.id.etBirthday)
        val etGender = view.findViewById<AutoCompleteTextView>(R.id.etGender)
        val cbAgreeTerms = view.findViewById<CheckBox>(R.id.cbAgreeTerms)
        val btnSignUp = view.findViewById<Button>(R.id.btnSignUp)
        val btnLogin = view.findViewById<Button>(R.id.btnLogin)
        val btnLoginQR = view.findViewById<Button>(R.id.btnLoginQR)
        ivProfileImage = view.findViewById(R.id.ivProfileImage)
        val btnUploadImage = view.findViewById<Button>(R.id.btnUploadImage)
        val btnRemoveImage = view.findViewById<Button>(R.id.btnRemoveImage)

        val ablToolbar = view.findViewById<AppBarLayout>(R.id.ablToolbar)
        val sBottom = view.findViewById<Space>(R.id.sBottom)

        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val statusBar = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navBar = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            ablToolbar.setPadding(0, statusBar.top, 0, 0)
            sBottom.layoutParams.height = navBar.bottom
            insets
        }


        val genderOptions = listOf("Male", "Female", "Other")
        val genderAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, genderOptions)

        val countryCodes = listOf("+63")
        val countryCodeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, countryCodes)
        etCountryCode.setAdapter(countryCodeAdapter)
        etCountryCode.setText(countryCodes[0], false)

        exitTransition = MaterialSharedAxis(MaterialSharedAxis.X, /* forward= */ true)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, /* forward= */ true)
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.X, /* forward= */ false)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, /* forward= */ false)

        val regions = addressManager.getAllRegions()
        val regionAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            regions.map { it.regDesc }
        )
        etRegion.setAdapter(regionAdapter)

        etRegion.setOnItemClickListener { _, _, position, _ ->
            selectedRegion = regions[position]
            etProvince.text.clear()
            etCityMun.text.clear()
            etBrgy.text.clear()

            val provinces = addressManager.getProvincesByRegion(selectedRegion?.regCode ?: "")
            val provinceAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                provinces.map { it.provDesc }
            )
            etProvince.setAdapter(provinceAdapter)
        }

        etProvince.setOnItemClickListener { _, _, position, _ ->
            selectedProvince = addressManager.getProvincesByRegion(selectedRegion?.regCode ?: "")[position]
            etCityMun.text.clear()
            etBrgy.text.clear()

            val cityMuns = addressManager.getCityMunsByProvince(selectedProvince?.provCode ?: "")
            val cityMunAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                cityMuns.map { it.citymunDesc }
            )
            etCityMun.setAdapter(cityMunAdapter)
        }

        etCityMun.setOnItemClickListener { _, _, position, _ ->
            selectedCityMun = addressManager.getCityMunsByProvince(selectedProvince?.provCode ?: "")[position]
            etBrgy.text.clear()

            val brgys = addressManager.getBrgysByCityMun(selectedCityMun?.citymunCode ?: "")
            val brgyAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                brgys.map { it.brgyDesc }
            )
            etBrgy.setAdapter(brgyAdapter)
        }

        etBrgy.setOnItemClickListener { _, _, position, _ ->
            selectedBrgy = addressManager.getBrgysByCityMun(selectedCityMun?.citymunCode ?: "")[position]
        }

        etGender.setAdapter(genderAdapter)
        etGender.setOnClickListener {
            etGender.showDropDown()
        }
        btnUploadImage.background.alpha = 170
        btnRemoveImage.background.alpha = 170

        segAccountType.setOnSegmentSelectedListener(object : SegmentedControl.OnSegmentSelectedListener {
            override fun onSegmentSelected(index: Int) {
                tilSubject.visibility = if (index == 1) View.VISIBLE else View.GONE
            }
        })

        btnUploadImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
            }
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        ivProfileImage.setOnClickListener {
            selectedImageUri?.let { uri ->
                openCropImageBottomSheet(uri)
            }
        }

        btnRemoveImage.setOnClickListener {
            ivProfileImage.setImageResource(R.drawable.ic_person_16)
            selectedImageUri = null
        }

        etBirthday.setOnClickListener {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.YEAR, -16)
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setSelection(calendar.timeInMillis)
                .build()
            datePicker.addOnPositiveButtonClickListener { timestamp ->
                val selectedDate = LocalDate.ofEpochDay(floor(timestamp.toDouble() / 86400000).toLong())
                etBirthday.setText(selectedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))

                // Calculate age
                val today = LocalDate.now()
                val age = Period.between(selectedDate, today).years
                etAge.setText(age.toString())
            }
            datePicker.show(parentFragmentManager, "DATE_PICKER")
        }

        btnSignUp.setOnClickListener {
            val userType = if (segAccountType.getSelectedIndex() == 0) "student" else "teacher"
            val firstName = etFirstName.text.toString().trim()
            val middleName = etMiddleName.text.toString().trim()
            val lastName = etLastName.text.toString().trim()
            val suffixName = etSuffixName.text.toString().trim()
            val grade = etGrade.text.toString().trim()
            val subject = etSubject.text.toString().trim()
            val section = etSection.text.toString().trim()
            val street = etStreet.text.toString().trim()
            val age = etAge.text.toString().trim()
            val countryCode = etCountryCode.text.toString().trim()
            val contactNumber = etContactNumber.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confPassword = etConfPassword.text.toString().trim()
            val birthday = etBirthday.text.toString().trim()
            val gender = etGender.text.toString().trim()

            viewLifecycleOwner.lifecycleScope.launch {
                val userDocDeferred = async {
                    databases.listDocuments(
                        databaseId = "6774d5c500013f347412",
                        collectionId = "6785debf002943b87bb1",
                        queries = listOf(
                            Query.equal("grade", grade),
                            Query.equal("section", section),
                            Query.equal("subject", subject)
                        )
                    ).documents.firstOrNull()
                }
                val userDoc = userDocDeferred.await()

                if (userDoc != null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Combination of grade, section, and subject already exists", Toast.LENGTH_SHORT).show()
                    }
                }

                if (firstName.isNotEmpty() && lastName.isNotEmpty() && grade.isNotEmpty() &&
                    section.isNotEmpty() && age.isNotEmpty() &&
                    selectedRegion != null && selectedProvince != null && selectedCityMun != null && selectedBrgy != null && street.isNotEmpty() &&
                    email.isNotEmpty() && password.isNotEmpty() && confPassword.isNotEmpty() &&
                    birthday.isNotEmpty() && gender.isNotEmpty() &&
                    password.matches(".*\\d.*".toRegex()) &&
                    password.matches(".*[a-z].*".toRegex()) &&
                    password.matches(".*[A-Z].*".toRegex()) &&
                    password.matches(".*[!@#\$%^&*()-+].*".toRegex()) &&
                    password.length >= 8 && password == confPassword &&
                    cbAgreeTerms.isChecked && selectedImageUri != null
                ) {

                    (ivProfileImage.drawable as? BitmapDrawable)?.bitmap?.let { profileBitmap ->
                        FaceVerificationBottomSheet(profileBitmap)  { bottomSheet,_ ->
                            Toast.makeText(context, "Face verification successful", Toast.LENGTH_SHORT).show()
                            bottomSheet.dismiss()

                            // On verification success
                            viewLifecycleOwner.lifecycleScope.launch {
                                try {
                                    // Create user account
                                    val user = account.create(
                                        userId = ID.unique(),
                                        email = email,
                                        password = password,
                                        name = "$firstName $lastName"
                                    )

                                    // create user new schedule if teacher
                                    databases.createDocument(
                                        databaseId = "6774d5c500013f347412",
                                        collectionId = "6785debf002943b87bb1",
                                        documentId = user.id,
                                        data = mapOf(
                                            "grade" to grade,
                                            "section" to section,
                                            "type" to if (userType == "student") "attendance" else "schedule",
                                        ).plus(
                                            if (userType == "teacher") {
                                                mapOf("subject" to subject)
                                            } else {
                                                mapOf()
                                            }
                                        )
                                    )

                                    val resizedBitmap = Bitmap.createScaledBitmap(profileBitmap, 750, 750, true)
                                    val byteArrayOutputStream = ByteArrayOutputStream()
                                    resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 95, byteArrayOutputStream)
                                    val byteArray = byteArrayOutputStream.toByteArray()

                                    // Upload profile image if selected
                                    val profileImageId = withContext(Dispatchers.IO) {
                                        val file = InputFile.fromBytes(
                                            byteArray,
                                            filename = "profile_${user.id}.jpg",
                                            mimeType = "image/jpeg",
                                        )
                                        storage.createFile(
                                            bucketId = "6774d59e001b225502c9",
                                            fileId = ID.unique(),
                                            file = file
                                        ).id
                                    }

                                    // Save user data
                                    var data = mapOf(
                                        "userId" to user.id,
                                        "userType" to userType,
                                        "name" to arrayOf(firstName, middleName, lastName, suffixName),
                                        "grade" to grade,
                                        "subject" to subject,
                                        "section" to section,
                                        "age" to Integer.parseInt(age),
                                        "address" to arrayOf(
                                            (selectedRegion?.regDesc ?: "").uppercase(),
                                            (selectedProvince?.provDesc ?: "").uppercase(),
                                            (selectedCityMun?.citymunDesc ?: "").uppercase(),
                                            (selectedBrgy?.brgyDesc ?: "").uppercase(),
                                            street.uppercase()),
                                        "addressId" to selectedBrgy?.brgyCode,
                                        "birthday" to birthday + "T00:00:02+00:00",
                                        "gender" to gender,
                                        "profileImageId" to profileImageId,
                                        "embedding" to fArrProfileImage,
                                        "email" to email,
                                        "contactNumber" to arrayOf(countryCode, contactNumber),
                                        "password" to password
                                    )

                                    databases.createDocument(
                                        databaseId = "6774d5c500013f347412",
                                        collectionId = "677f45d0003a18299bdc",
                                        documentId = user.id,
                                        data = data
                                    )

                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Sign Up Successful", Toast.LENGTH_SHORT).show()
                                        findNavController().navigate(R.id.action_signUp_to_login)
                                    }

                                    bottomSheet.dismiss()

                                    GeneratedQRBottomSheet(data).show(parentFragmentManager, "GeneratedQRBottomSheet")

                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Sign Up Failed: ${e.message}",
                                            Toast.LENGTH_SHORT).show()
                                    }
                                    e.printStackTrace()
                                }
                            }
                        }.show(parentFragmentManager, "FaceVerificationBottomSheet")
                    }
                } else if (password != confPassword) {
                    Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                } else if (!cbAgreeTerms.isChecked) {
                    Toast.makeText(context, "Please agree to the terms and conditions", Toast.LENGTH_SHORT).show()
                } else if (selectedImageUri == null) {
                    Toast.makeText(context, "Please upload a profile image", Toast.LENGTH_SHORT).show()
                } else if (password.length < 8) {
                    Toast.makeText(context, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                } else if (!password.matches(".*\\d.*".toRegex())) {
                    Toast.makeText(context, "Password must contain at least one digit", Toast.LENGTH_SHORT).show()
                } else if (!password.matches(".*[a-z].*".toRegex())) {
                    Toast.makeText(context, "Password must contain at least one lowercase letter", Toast.LENGTH_SHORT).show()
                } else if (!password.matches(".*[A-Z].*".toRegex())) {
                    Toast.makeText(context, "Password must contain at least one uppercase letter", Toast.LENGTH_SHORT).show()
                } else if (!password.matches(".*[!@#\$%^&*()-+].*".toRegex())) {
                    Toast.makeText(context, "Password must contain at least one special character", Toast.LENGTH_SHORT).show()
                } else if (!contactNumber.matches("^\\d{10}\$".toRegex())) {
                    Toast.makeText(context, "Invalid contact number format", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                }

            }



        }

        btnLogin.setOnClickListener {
            findNavController().navigate(R.id.action_signUp_to_login)
        }

        btnLoginQR.setOnClickListener {
            findNavController().navigate(R.id.action_signUp_to_loginQR)
        }

        return view
    }


    fun openCropImageBottomSheet(uri: Uri) {
        val inputStream = context?.contentResolver?.openInputStream(uri)
        inputStream?.let {
            Log.i("ImageCropBottomSheet", "Image URI: $uri")
            ImageCropBottomSheet(it,
                { croppedBitmap ->
                    Log.i("ImageCropBottomSheet", "Cropped Bitmap: $croppedBitmap")
                    viewLifecycleOwner.lifecycleScope.launch {
                        val faceRecognition = FaceRecognition(requireContext(),
                            FaceNet(requireContext()),
                            FaceSpoofDetector(requireContext())
                        )
                        faceRecognition.detectFace(croppedBitmap).getOrNull()?.let { (_, floatArray) ->
                            fArrProfileImage = floatArray
                            ivProfileImage.setImageBitmap(croppedBitmap)
                        } ?: run {
                            Toast.makeText(context, "Face not detected", Toast.LENGTH_SHORT).show()
                            fArrProfileImage = FloatArray(512)
                            ivProfileImage.setImageResource(R.drawable.ic_person_16)
                        }
                    }
                },
                {}, requirePerson = true, centerCropY = -200f)
        }?.show(parentFragmentManager, "ImageCropBottomSheet")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                selectedImageUri = uri
                openCropImageBottomSheet(uri)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        addressManager.close()
    }
}