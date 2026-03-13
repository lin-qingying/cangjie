plugins {
    kotlin("jvm")
    application
}

dependencies {
    implementation(project(":cfir:cfir-common"))

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
    mainClass.set("org.cangnova.cangjie.cfir.tree.generator.MainKt")
}
