plugins {
    kotlin("jvm")
}

description = "IDE 插件依赖：打包 :analysis:analysis-api-standalone、:analysis:analysis-internal-utils。"
dependencies {
    api(project(":analysis:analysis-api-standalone"))
    api(project(":analysis:analysis-internal-utils"))
}