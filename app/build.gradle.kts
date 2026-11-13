plugins {
    id("com.android.application")
}

android {
    namespace = "com.camvivr.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.camvivr.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 7
        versionName = "5.1"
    }

    signingConfigs {
        create("camvivr") {
            storeFile = file("camvivr.p12")
            storePassword = "camvivr123"
            keyAlias = "camvivr"
            keyPassword = "camvivr123"
            storeType = "PKCS12"
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("camvivr")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("camvivr")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
