plugins {
    kotlin("jvm")
}

description = "IDE 插件依赖：打包 :analysis:analysis-api-cfir、:analysis:low-level-api-cfir、:analysis:decompiled、:analysis:symbol-light-declarations。"

dependencies {
    api(project(":analysis:analysis-api-cfir"))
    api(project(":analysis:low-level-api-cfir"))
    api(project(":analysis:decompiled"))
    api(project(":analysis:stubs"))
    api(project(":analysis:symbol-light-declarations"))
}