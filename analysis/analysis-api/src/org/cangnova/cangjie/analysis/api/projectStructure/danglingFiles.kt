package org.cangnova.cangjie.analysis.api.projectStructure

import com.intellij.openapi.util.Key
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.testFramework.LightVirtualFile
import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.psi.CjCodeFragment
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.UserDataProperty
import org.cangnova.cangjie.psi.elementContext

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
     * 仅影响分析请求内部的非局部引用解析；显式请求副本中的声明符号时仍分析该声明，
     * 局部声明始终来自副本，不会映射为原件中位置相同的局部声明。
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
 * 显式绑定与作用域化解析模式互斥；调用方须先清除此绑定再使用 `analyzeCopy`。
 */
@CaExperimentalApi
var CjFile.explicitModule: CaModule?
    get() = getUserData(EXPLICIT_MODULE_KEY)
    set(value) {
        @OptIn(CaPlatformInterface::class)
        require(value is CaDanglingFileModule?) { "Only dangling file modules can be set as explicit modules" }
        require(value == null || danglingFileResolutionMode == null) {
            "An explicit module cannot be set while a resolution mode override is active"
        }

        val virtualFile = virtualFile
        if (virtualFile != null) {
            require(virtualFile is LightVirtualFile) { "'explicitModule' is only available for in-memory files" }
        }
        putUserData(EXPLICIT_MODULE_KEY, value)
    }

/**
 * 文件副本的原件。兼容 PSI 的 [PsiFile.originalFile] 与 [PsiFileFactory.ORIGINAL_FILE] 两种来源。
 */
@CaExperimentalApi
val PsiFile.copyOrigin: PsiFile?
    get() = originalFile.takeUnless { it == this }
        ?: getUserData(PsiFileFactory.ORIGINAL_FILE)?.takeUnless { it == this }

/**
 * 存储内存文件显式上下文模块的 user-data key。
 */
private val CONTEXT_MODULE_KEY = Key.create<CaModule>("CONTEXT_MODULE")

/**
 * 显式指定分析当前内存文件时所处的**上下文模块**。
 *
 * 对齐 Kotlin `KtFile.contextModule`。上下文模块通常由上下文元素或原件自动推导；
 * 本属性用于在自动推导不成立时显式指定——典型场景是 `psiFactory.createFile` 造出的
 * 副本：它既没有 [copyOrigin] 也没有 [elementContext]，自动推导会落到
 * 「不在任何内容根下」的模块上，下游的模块归属、可见性与解析全部走偏。
 *
 * **不能**给代码片段设置：代码片段按定义就在另一个元素的上下文中分析，这个行为不可覆盖。
 */
@CaExperimentalApi
var CjFile.contextModule: CaModule?
    get() = getUserData(CONTEXT_MODULE_KEY)
    set(value) {
        require(this !is CjCodeFragment) { "'contextModule' cannot be set for code fragments" }

        val virtualFile = virtualFile
        if (virtualFile != null) {
            require(virtualFile is LightVirtualFile) { "'contextModule' is only available for in-memory files" }
        }
        putUserData(CONTEXT_MODULE_KEY, value)
    }

/**
 * 文件是否需要以游离模块分析。普通内存文件、副本与具有分析上下文的文件均属此类。
 */
@OptIn(CaExperimentalApi::class)
val CjFile.isDangling: Boolean
    get() = isCodeFragment || this is CjCodeFragment || !isPhysical || copyOrigin != null || elementContext != null

/**
 * 当前线程为文件显式覆盖的解析模式；没有覆盖时为 `null`，由平台决定默认模式。
 *
 * 不同模式对应不同的游离模块与 LL 会话。覆盖不随 PSI 副本复制，也不传递到其他线程。
 */
val CjFile.danglingFileResolutionMode: CaDanglingFileResolutionMode?
    get() = danglingFileResolutionModeState?.get()

/**
 * 仅在 [action] 内为 [file] 覆盖解析模式。客户端应使用 [org.cangnova.cangjie.analysis.api.analyzeCopy]。
 *
 * 输入必须为具有 [copyOrigin] 的游离文件。已绑定 [explicitModule] 的文件不支持作用域覆盖：
 * 显式模块是固定的完整配置，不能静默忽略本次模式，也不能隐式拆分多文件模块。
 *
 * 模式存储按文件、线程隔离；同文件与不同文件均可嵌套，正常返回、异常和取消均恢复外层状态。
 * [action] 必须同步执行；跨线程分析须在目标线程重新建立覆盖。此函数不放宽分析读锁及生命周期限制。
 */
@CaImplementationDetail
@OptIn(CaExperimentalApi::class)
fun <R> withDanglingFileResolutionMode(file: CjFile, mode: CaDanglingFileResolutionMode, action: () -> R): R {
    require(file.isDangling) { "'withDanglingFileResolutionMode()' is only available to dangling files" }
    require(file.copyOrigin != null) { "'withDanglingFileResolutionMode()' is only available to file copies" }
    require(file.explicitModule == null) { "Resolution mode cannot be overridden for a file with an explicit module" }

    val modeState = getOrCreateDanglingFileResolutionModeState(file)
    val oldValue = modeState.get()
    try {
        modeState.set(mode)
        return action()
    } finally {
        if (oldValue == null) modeState.remove() else modeState.set(oldValue)
    }
}

/** 同一文件并发首次分析时只创建一个 ThreadLocal 容器。 */
private fun getOrCreateDanglingFileResolutionModeState(file: CjFile): ThreadLocal<CaDanglingFileResolutionMode?> =
    synchronized(file) {
        file.danglingFileResolutionModeState ?: ThreadLocal<CaDanglingFileResolutionMode?>().also {
            file.danglingFileResolutionModeState = it
        }
    }

private var CjFile.danglingFileResolutionModeState: ThreadLocal<CaDanglingFileResolutionMode?>?
    by UserDataProperty(Key.create("DANGLING_FILE_RESOLUTION_MODE"))
