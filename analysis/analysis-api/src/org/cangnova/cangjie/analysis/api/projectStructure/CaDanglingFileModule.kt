package org.cangnova.cangjie.analysis.api.projectStructure

interface CaDanglingFileModule : CaSourceModule {
    val contextModule: CaModule?

    /**
     * 控制游离文件在解析非局部声明时优先走自身还是上下文模块。
     */
    val resolutionMode: CaDanglingFileResolutionMode

    override val moduleDescription: String
        get() = "Dangling file module $name"
}
