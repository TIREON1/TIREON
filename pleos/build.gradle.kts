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

    // AAOS CarPropertyManager는 차량 프레임워크가 런타임에 제공한다.
    // 컴파일 시점에만 필요하므로 compileOnly로 둔다.
    compileOnly(files("${android.sdkDirectory}/platforms/android-36/optional/android.car.jar"))

    // Pleos Connect SDK 자체는 이 모듈 밖으로 노출하지 않는다.
    implementation(libs.pleos.vehicle)
    // Vehicle POM이 vehicle-data를 runtime scope로만 선언해 ErrorCode 등이
    // compile classpath에 노출되지 않는다. 오류 코드 분기를 위해 명시적으로 추가한다.
    implementation(libs.pleos.vehicle.data)
    // 코칭 음성 안내. ON_DEVICE 모드는 네트워크와 앱 등록이 필요 없다.
    implementation(libs.pleos.text.to.speech)

    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(kotlin("test"))
}
