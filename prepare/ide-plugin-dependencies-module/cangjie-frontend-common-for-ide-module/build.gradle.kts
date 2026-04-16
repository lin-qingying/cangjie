plugins {
    kotlin("jvm")
}

description = "IDE 插件依赖：打包 :common、:util、:compiler:arguments、:resolution.common。"

dependencies {
    api(project(":common"))
    api(project(":util"))
    api(project(":analysis:cj-references"))
    api(project(":compiler:arguments"))
    api(project(":resolution.common"))
}