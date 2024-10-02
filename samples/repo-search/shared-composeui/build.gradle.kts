@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.jetbrains.compose)
  alias(libs.plugins.compose.compiler)
  id("com.android.library")
}

android {
  namespace = "app.cash.paging.samples.reposearch.shared.composeui"
  compileSdk = 34
  defaultConfig {
    minSdk = 21
    targetSdk = 34
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
}

kotlin {
  jvm()
  androidTarget()

  sourceSets {
    val commonMain by getting {
      dependencies {
        api(projects.samples.repoSearch.shared)
        implementation(projects.pagingComposeCommon)
        implementation(compose.ui)
        implementation(compose.material)
      }
    }
  }
}
