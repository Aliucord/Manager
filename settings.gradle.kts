/*
 * Copyright (c) 2021 Juby210
 * Licensed under the Open Software License version 3.0
 */

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Aliucord Manager"
include(":app")