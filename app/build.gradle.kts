plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("com.google.gms.google-services")
    id("com.google.devtools.ksp")
    id("kotlin-parcelize")
}

android {
    namespace = "com.schooltimetrack.attendance"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.schooltimetrack.attendance"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
    }
}



dependencies {
    ksp(libs.androidx.room.compiler)

    implementation(libs.material)

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.annotation)

    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    implementation(libs.androidx.legacy.support.v4)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.preference)
    implementation(libs.androidx.recyclerview)

    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.camera2)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.common)
    implementation(libs.androidx.room.ktx)

    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.navigation.runtime.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)

    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.core)

    implementation(libs.play.services.mlkit.barcode.scanning)
    implementation(libs.play.services.mlkit.face.detection)


    implementation (libs.tensorflow.lite)
    implementation (libs.tensorflow.lite.gpu)
    implementation (libs.tensorflow.lite.gpu.api)
    implementation (libs.tensorflow.lite.support)





//    implementation(libs.litert.api)
//    implementation(libs.litert)
//    implementation(libs.litert.gpu)
//    implementation(libs.litert.support.api)


    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)




    implementation(libs.material.calendar)

    implementation("io.appwrite:sdk-for-android:6.0.0")


    implementation(libs.face.detection)

    implementation(libs.photoview)
    implementation(libs.qrgenerator)
    implementation(libs.json)
}
