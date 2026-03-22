plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":common"))


    api(project(":util"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
