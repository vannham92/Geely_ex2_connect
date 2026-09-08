plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

import java.util.Properties

android {
    namespace = "com.geely.ex2.tools"
    compileSdk = 35

    val versionPropsFile = file("version.properties")
    val versionProps = Properties().also { props ->
        versionPropsFile.inputStream().use { props.load(it) }
    }
    val appVersionName = versionProps.getProperty("VERSION_NAME", "0.0.0")
    val appVersionCode = versionProps.getProperty("VERSION_CODE", "1").toInt()

    defaultConfig {
        applicationId = "com.geely.ex2.tools"
        minSdk = 26
        targetSdk = 35
        versionCode = appVersionCode
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    /**
     * `user` — debug/sideload thông thường (như hiện tại).
     * `system` — sharedUserId=android.uid.system + platform-signature (giống CentralEXAuto);
     *           nếu không setAVASMode / CAR_CONTROL_AUDIO_VOLUME sẽ không hoạt động.
     */
    flavorDimensions += "install"
    productFlavors {
        create("user") {
            dimension = "install"
            isDefault = true
        }
        create("system") {
            dimension = "install"
            versionNameSuffix = "-system"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/INDEX.LIST"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.profileinstaller)
    implementation(libs.tencent.mmkv)
    debugImplementation(libs.androidx.ui.tooling)
    
    // JSON cho CarTcpServer (NDJSON qua raw TCP :47800)
    implementation(libs.kotlinx.serialization.json)
}
