plugins {
    kotlin("jvm")
    application
}

dependencies {
    compileOnly(project(":generators"))
    implementation(project(":compiler:arguments"))

}

application {
    mainClass.set("org.cangnova.cangjie.frontend.arguments.generator.MainKt")
}
