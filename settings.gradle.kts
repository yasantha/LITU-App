pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
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

rootProject.name = "litu-app"

include(":app")
include(":audio_pack")
include(":benchmark")

include(":core:model")
include(":core:content")
include(":core:progress")
include(":core:domain")
include(":core:sync")
include(":core:billing")
include(":core:config")
include(":core:audio")
include(":core:designsystem")
include(":core:analytics")

include(":feature:onboarding")
include(":feature:home")
include(":feature:practice")
include(":feature:review")
include(":feature:mock")
include(":feature:notes")
include(":feature:progress")
include(":feature:timer")
include(":feature:paywall")
include(":feature:settings")
