plugins {
    kotlin("jvm")
    application
}

dependencies {
    implementation(project(":generators"))
    implementation(project(":compiler:arguments"))

}

application {
    mainClass.set("org.cangnova.cangjie.frontend.arguments.generator.MainKt")
}
