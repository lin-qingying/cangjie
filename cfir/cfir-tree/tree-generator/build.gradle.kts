plugins {
    kotlin("jvm")
    application
}

dependencies {
    implementation(project(":generators"))
    implementation(project(":util"))
}

sourceSets {
    "main" {
        projectDefault()
    }
    "test" {}
}

application {
    mainClass.set("org.cangjie.cfir.tree.generator.MainKt")
}
