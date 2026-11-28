plugins {
    kotlin("jvm")
}

description = "Cangjie frontend argument model and generated argument descriptions."

dependencies {
    api(project(":common"))
    compileOnly(project(":generators"))
}
