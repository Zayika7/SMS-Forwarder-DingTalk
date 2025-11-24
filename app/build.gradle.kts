plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    // 【新增】引用刚才在 toml 里定义的插件
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.example.myapplication"
    compileSdk = 34 // 建议改为 34 或 35，36 (Android 16) 目前还是预览版，可能会不稳定

    defaultConfig {
        applicationId = "com.example.myapplication"
        minSdk = 24
        targetSdk = 34 // 与 compileSdk 保持一致
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 允许使用矢量图
        vectorDrawables {
            useSupportLibrary = true
        }
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

    // ==========================================
    // 【关键修复】这部分是你之前缺少的
    // ==========================================
    buildFeatures {
        compose = true // 启用 Compose 功能
    }

    composeOptions {
        // 这是一个通用的稳定版本，适配大部分 Kotlin 版本
        kotlinCompilerExtensionVersion = "1.5.1"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Compose 相关依赖
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.activity:activity-compose:1.8.1")
    implementation(platform("androidx.compose:compose-bom:2023.08.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    // 网络请求 (OkHttp)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    // JSON解析 (Gson)
    implementation("com.google.code.gson:gson:2.10.1")
    // 扩展图标库 (Settings, Play, Stop 图标)
    implementation("androidx.compose.material:material-icons-extended:1.5.4")
}