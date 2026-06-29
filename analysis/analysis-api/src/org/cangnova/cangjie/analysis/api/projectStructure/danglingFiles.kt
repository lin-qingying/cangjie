package org.cangnova.cangjie.analysis.api.projectStructure

import com.intellij.openapi.util.Key
import com.intellij.testFramework.LightVirtualFile
import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.UserDataProperty

/**
 * 控制游离文件中**非局部声明**引用的解析策略。
 *
 * 对齐 Kotlin Analysis API 的 `KaDanglingFileResolutionMode`。
 */
enum class CaDanglingFileResolutionMode {
    /**
     * 优先解析到游离文件自身中的声明,必要时再回退到原始文件或上下文模块。
     */
    PREFER_SELF,

    /**
     * 默认忽略游离文件中的非局部声明,直接解析到原始文件或上下文模块。
     *
     * 该模式主要用于性能优化:当游离文件只是被复制出来做临时修改、
     * 而调用方并不关心新增的顶层声明时,可避免重新分析游离声明。
     */
    IGNORE_SELF,
}

/**
 * 存储内存文件显式绑定模块的 user-data key。
 */
private val EXPLICIT_MODULE_KEY = Key.create<CaModule>("EXPLICIT_MODULE")

/**
 * 显式指定当前内存文件使用的 Analysis API 模块。
 *
 * 对齐 Kotlin `KtFile.explicitModule`：当前只允许显式绑定 [CaDanglingFileModule]，
 * 用于测试或调用方手动构造由 PSI 文件承载的 dangling file module。
 */
@CaExperimentalApi
var CjFile.explicitModule: CaModule?
    get() = getUserData(EXPLICIT_MODULE_KEY)
    set(value) {
        @OptIn(CaPlatformInterface::class)
        require(value is CaDanglingFileModule?) { "Only dangling file modules can be set as explicit modules" }

        val virtualFile = virtualFile
        if (virtualFile != null) {
            require(virtualFile is LightVirtualFile) { "'explicitModule' is only available for in-memory files" }
        }
        putUserData(EXPLICIT_MODULE_KEY, value)
    }
