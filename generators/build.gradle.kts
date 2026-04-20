plugins {
    kotlin("jvm")
}

dependencies {
    compileOnly(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    implementation(project(":util"))
    implementation(project(":common"))
//    implementation(intellijCore())
    api(intellijPlatformUtil()) {
        exclude(module = "annotations")
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
