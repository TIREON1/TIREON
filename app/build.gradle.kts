import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// release 서명 정보는 저장소 루트의 keystore.properties에서만 읽는다.
// 이 파일은 .gitignore에 등록되어 있으며, 없으면 release 빌드를 명확히 실패시킨다.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.isFile) {
        keystorePropertiesFile.inputStream().use(::load)
    }
}
val requiredSigningProperties = listOf("storeFile", "storePassword", "keyAlias", "keyPassword")
val missingSigningProperties = requiredSigningProperties.filter {
    keystoreProperties.getProperty(it).isNullOrBlank()
}
val releaseSigningConfigured = keystorePropertiesFile.isFile && missingSigningProperties.isEmpty()

android {
    namespace = "ai.tireon.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "ai.tireon.app"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
    }

    signingConfigs {
        if (releaseSigningConfigured) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (releaseSigningConfigured) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    buildFeatures {
        compose = true
    }

    sourceSets {
        getByName("main") {
            java.srcDirs("src/main/kotlin")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
    }
}

val verifyReleaseSigning = tasks.register("verifyReleaseSigning") {
    group = "verification"
    description = "release 서명 설정이 준비되었는지 확인합니다."

    doLast {
        if (!keystorePropertiesFile.isFile) {
            throw GradleException(
                "release 서명 설정이 없습니다. keystore.properties.example을 " +
                    "keystore.properties로 복사한 뒤 값을 채우세요: $keystorePropertiesFile",
            )
        }
        if (missingSigningProperties.isNotEmpty()) {
            throw GradleException(
                "keystore.properties에 필요한 항목이 없습니다: " +
                    missingSigningProperties.joinToString(),
            )
        }
        val storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
        if (!storeFile.isFile) {
            throw GradleException("keystore 파일을 찾을 수 없습니다: $storeFile")
        }
    }
}

// release APK를 실제로 패키징할 때만 서명 설정을 검증한다.
// debug 빌드, 단위 테스트, Lint는 서명 키 없이도 동작해야 한다.
tasks.matching { it.name == "packageRelease" }.configureEach {
    dependsOn(verifyReleaseSigning)
}

dependencies {
    implementation(project(":core"))
    implementation(project(":pleos"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.compose.ui.tooling.preview)

    testImplementation(libs.junit)
    testImplementation(kotlin("test"))
}
