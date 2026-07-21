
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "MeshStream"

include(
    ":app",
    ":core",
    ":feature:recorder",
    ":feature:storage",
    ":feature:crypto",
    ":feature:mesh",
    ":feature:relay"
)
