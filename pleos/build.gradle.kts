import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "ai.tireon.pleos"
    compileSdk = 36

    defaultConfig {
        minSdk = 28
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    sourceSets {
        getByName("main") {
            java.srcDirs("src/main/kotlin")
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
    }
}

dependencies {
    // 도메인 타입(DrivingSample 등)은 :core에 있고, 공개 API로 노출되므로 api로 둔다.
    api(project(":core"))

    // Pleos Connect SDK 자체는 이 모듈 밖으로 노출하지 않는다.
    implementation(libs.pleos.vehicle)
    // Vehicle POM이 vehicle-data를 runtime scope로만 선언해 ErrorCode 등이
    // compile classpath에 노출되지 않는다. 오류 코드 분기를 위해 명시적으로 추가한다.
    implementation(libs.pleos.vehicle.data)

    // DrivingViewModel은 :app에서 직접 사용하므로 api로 노출한다.
    api(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(kotlin("test"))
}
