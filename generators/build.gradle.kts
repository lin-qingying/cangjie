plugins {
    kotlin("jvm")
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    implementation(project(":util"))
    implementation(project(":common"))
    implementation(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
