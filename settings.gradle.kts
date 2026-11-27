pluginManagement {
  repositories {
    google {
      content {
        includeGroupByRegex("com\\.android.*")
        includeGroupByRegex("com\\.google.*")
        includeGroupByRegex("androidx.*")
      }
    }
    // Google-hosted Maven Central mirror reduces CI failures from Central HTTP 429 limits.
    maven { url = uri("https://maven-central-eu.storage-download.googleapis.com/maven2/") }
    mavenCentral()
    gradlePluginPortal()
  }
}

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    // Prefer Google's Maven Central mirror; retain Central as a fallback.
    maven { url = uri("https://maven-central-eu.storage-download.googleapis.com/maven2/") }
    mavenCentral()
  }
}

rootProject.name = "LumaNAV"

include(":app")
