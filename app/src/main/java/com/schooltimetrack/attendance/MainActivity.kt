package com.schooltimetrack.attendance

import UserDocument
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.ActivityNavigator
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import io.appwrite.Client
import io.appwrite.services.Account
import io.appwrite.services.Databases
import io.appwrite.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class MainActivity : AppCompatActivity() {
    lateinit var navController: NavController
    lateinit var client: Client
    lateinit var account: Account
    lateinit var databases: Databases
    lateinit var userDocument: UserDocument

    private val requiredPermissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen().apply {
            setKeepOnScreenCondition {
                findViewById<View>(R.id.splashScreen).visibility == View.VISIBLE
            }
        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Initialize Appwrite
        client = Client(this)
            .setEndpoint("https://cloud.appwrite.io/v1")
            .setProject("6773c26a001612edc5fb")
        account = Account(client)
        databases = Databases(client)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.mainFragment) as NavHostFragment
        navController = navHostFragment.navController

        // Show Splash Screen
        val splashScreen = findViewById<View>(R.id.splashScreen)
        splashScreen.visibility = View.VISIBLE
        findViewById<View>(R.id.mainFragment).visibility = View.GONE

        // Check login status when app starts
        lifecycleScope.launch {
            checkLoginStatus(splashScreen)
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(systemBars.left, 0, systemBars.right, imeInsets.bottom )
            insets
        }

        checkAndRequestPermissions()

        val window = window
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    }

    private suspend fun checkLoginStatus(splashScreen: View) {
        try {
            coroutineScope {
                val userDeferred = async { account.get() }
                val user = userDeferred.await()

                val userDocDeferred = async {
                    databases.listDocuments(
                        databaseId = "6774d5c500013f347412",
                        collectionId = "677f45d0003a18299bdc",
                        queries = listOf(
                            Query.equal("email", user.email)
                        )
                    ).documents.firstOrNull()
                }
                val userDoc = userDocDeferred.await()


                withContext(Dispatchers.Main) {
                    // Set hardware acceleration
                    splashScreen.setLayerType(View.LAYER_TYPE_HARDWARE, null)
                    
                    // Set initial values
                    splashScreen.scaleX = 1.0f
                    splashScreen.scaleY = 1.0f
                    splashScreen.alpha = 1.0f

                    Log.d("User", userDoc.toString())

                    userDoc?.let {
                        val userDocument = UserDocument(
                            userId = it.id,
                            userType = it.data["userType"].toString(),
                            name = (it.data["name"] as ArrayList<String>),
                            grade = it.data["grade"].toString(),
                            subject = it.data["subject"].toString(),
                            section = it.data["section"].toString(),
                            age = it.data["age"].toString().toInt(),
                            address = it.data["address"] as ArrayList<String>,
                            addressId = it.data["addressId"].toString(),
                            birthday = it.data["birthday"].toString(),
                            gender = it.data["gender"].toString(),
                            profileImageId = it.data["profileImageId"].toString(),
                            email = it.data["email"].toString(),
                            contactNumber = it.data["contactNumber"] as ArrayList<String>
                        )
                        this@MainActivity.userDocument = userDocument

                        navController.popBackStack(R.id.welcome, true)

                        when (userDoc.data["userType"].toString()) {
                            "student" -> navController.navigate(
                                R.id.studentMenu,
                                Bundle().apply {
                                    putParcelable("UserDocument", userDocument)
                                })
                            "teacher" -> navController.navigate(
                                R.id.teacherMenu,
                                Bundle().apply {
                                    putParcelable("UserDocument", userDocument)
                                })
                            else -> navController.navigate(R.id.welcome)
                        }
                    }

                    // Animate splash screen with property animator
                    splashScreen.animate()
                        .scaleX(1.2f)
                        .scaleY(1.2f)
                        .alpha(0f)
                        .setDuration(400)
                        .withEndAction {
                            splashScreen.visibility = View.GONE
                            findViewById<View>(R.id.mainFragment).visibility = View.VISIBLE
                            splashScreen.setLayerType(View.LAYER_TYPE_NONE, null)
                        }
                        .start()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()

            withContext(Dispatchers.Main) {
                splashScreen.setLayerType(View.LAYER_TYPE_HARDWARE, null)
                
                splashScreen.scaleX = 1.0f
                splashScreen.scaleY = 1.0f
                splashScreen.alpha = 1.0f
                
                navController.popBackStack(R.id.welcome, true)
                navController.navigate(R.id.welcome)
                
                splashScreen.animate()
                    .scaleX(1.2f)
                    .scaleY(1.2f)
                    .alpha(0f)
                    .setDuration(400)
                    .withEndAction {
                        splashScreen.visibility = View.GONE
                        findViewById<View>(R.id.mainFragment).visibility = View.VISIBLE
                        splashScreen.setLayerType(View.LAYER_TYPE_NONE, null)
                    }
                    .start()
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                PERMISSIONS_REQUEST_CODE
            )
        }
    }

    companion object {
        private const val PERMISSIONS_REQUEST_CODE = 123
    }

    override fun finish() {
        super.finish()
        ActivityNavigator.applyPopAnimationsToPendingTransition(this)
    }
}