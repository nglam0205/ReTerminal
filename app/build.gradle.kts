import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.rk.application"
    compileSdk = 37

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    signingConfigs {
        create("release") {
            val isGithubActions = System.getenv("GITHUB_ACTIONS") == "true"

            val propsFile = if (isGithubActions) {
                File("/tmp/signing.properties")
            } else {
                val localProps = File(System.getProperty("user.home"), "Android/xed-signing/signing.properties")
                if (localProps.exists()) localProps else File(rootDir, "signing.properties")
            }

            if (propsFile.exists() && propsFile.length() > 0L) {
                val props = Properties().apply {
                    propsFile.inputStream().use { load(it) }
                }

                val alias = props.getProperty("keyAlias")
                val keyPass = props.getProperty("keyPassword")
                val storePass = props.getProperty("storePassword")
                val storePath = props.getProperty("storeFile")

                if (!alias.isNullOrBlank() && !keyPass.isNullOrBlank() && !storePass.isNullOrBlank() && !storePath.isNullOrBlank()) {
                    keyAlias = alias
                    keyPassword = keyPass
                    storeFile = File(storePath)
                    storePassword = storePass
                    println("Using release signing config from ${propsFile.absolutePath}")
                } else {
                    throw GradleException("Invalid signing.properties: missing keyAlias/keyPassword/storePassword/storeFile")
                }
            } else {
                throw GradleException("Missing signing.properties at ${propsFile.absolutePath}. Build cannot continue without a valid signing config.")
            }
        }

        getByName("debug") {
            val debugKeystore = File(rootDir, "debug-keystore.jks")
            if (debugKeystore.exists()) {
                storeFile = debugKeystore
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            } else {
                storeFile = File(System.getProperty("java.io.tmpdir"), "debug-keystore.jks")
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isCrunchPngs = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
            resValue("string", "app_name", "ReTerminal")
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-DEBUG"
            resValue("string", "app_name", "ReTerminal-Debug")
        }
    }

    defaultConfig {
        applicationId = "com.rk.terminal"
        minSdk = 26
        targetSdk = 37
        versionCode = 10
        versionName = "1.3.0"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        viewBinding = true
        compose = true
        resValues = true
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {
    implementation(project(":core:main"))
}
