

package org.cangnova.cangjie.analysis.low.level.api.cfir.compile

import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.low.level.api.cfir.projectStructure.llCfirModuleData
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.containingCjFileIfAny
import org.cangnova.cangjie.cfir.*
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.visitors.CfirDefaultVisitorVoid
import org.cangnova.cangjie.psi.CjTypeStatement
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.utils.exceptions.errorWithAttachment

/**
 * Processes the declaration, collecting files that would need to be submitted to the backend (or handled specially)
 * in case if the declaration is compiled.
 *
 * Kotlin FIR 这里会递归收集 inline peer。
 * 仓颉主干当前没有对应的跨文件 inline 驱动语义，因此这里只保留源文件归属收集，
 * 不再伪造 inline peer / inlined local class 路径。
 *
 * Note that compiled declarations are not analyzed, as the backend can inline them natively.
 */

class CompilationPeerCollector private constructor() {
    companion object {
        fun process(files: Collection<CfirFile>): CompilationPeerData {
            val collector = CompilationPeerCollector()
            files.forEach { collector.process(it) }

            return CompilationPeerData(
                peers = collector.peers,
                inlinedClasses = collector.inlinedClasses
            )
        }
    }

    /**
     * 按模块归组的待提交源码文件列表。
     */
    private val peers = LinkedHashMap<CaModule, MutableList<CjFile>>()

    /**
     * inline 语义下需要携带的本地 class 集合；仓颉当前主线保持为空。
     */
    private val inlinedClasses = LinkedHashSet<CjTypeStatement>()

    /**
     * 已访问过的 CFIR 文件，防止递归收集时重复处理。
     */
    private val visited = HashSet<CfirFile>()

    /**
     * 当前递归收集路径上的模块栈，用于检测跨模块循环。
     */
    private val moduleStack = ArrayDeque<CaModule>()

    /**
     * 处理单个 CFIR 文件并递归收集它依赖的 peer 文件。
     */
    private fun process(file: CfirFile) {
        ProgressManager.checkCanceled()

        val cjFile = file.containingCjFileIfAny
        if (cjFile == null || cjFile.isCompiled) {
            return
        }

        val module = file.llCfirModuleData.caModule
        if (module in moduleStack && module != moduleStack.last()) {
            // We cannot compile two or more modules together
            errorWithAttachment("Cyclic dependency between modules") {
                withEntry("cycle") {
                    (moduleStack + module).forEach { appendLine(it.toString()) }
                }
            }

            // Skip non-inlined indirect recursion
            return
        }

        if (!visited.add(file)) {
            // Skip the declaration we visited before
            // Sic: this happens after the inline recursion check
            return
        }

        // Avoid deep stacks by gathering callee files first
        val visitor = CompilationPeerCollectingVisitor(cjFile.project)
        file.accept(visitor)

        inlinedClasses.addAll(visitor.inlinedClasses)

        withModule(module) {
            visitor.files.forEach(::process)
        }

        peers.getOrPut(module, ::ArrayList).add(cjFile)
    }

    /**
     * 在模块栈中进入指定模块并执行收集动作。
     */
    private inline fun withModule(module: CaModule, block: () -> Unit) {
        if (moduleStack.lastOrNull() == module) {
            block()
            return
        }

        moduleStack.addLast(module)
        try {
            block()
        } finally {
            moduleStack.removeLast()
        }
    }
}


/**
 * 编译 peer 收集结果，包含按模块归组的源码文件和需要附带的本地 class。
 */
class CompilationPeerData(
    /**
     * The original file and all files that contain inline functions/variables called from that file or any other files from the list.
     * The returned list is in post-order.
     * Files in the list are unique.
     *
     * For example,
     *  - A is a source file in module M(A) to be compiled; it calls an inline function from the file B of module M(B).
     *  - B calls another inline function defined in C in module M(C).
     *  - [peers] returned by [CompilationPeerCollector.process] then will be {C, B, A}.
     *
     * More formally, i-th element of [peers] will not have inline-dependency on any j-th element of
     * [peers], where j > i.
     *
     * This list does not contain duplicated files.
     */
    val peers: Map<CaModule, List<CjFile>>,

    /** Local classes inlined as a part of inline functions. */
    val inlinedClasses: Set<CjTypeStatement>,
)

/**
 * 遍历 CFIR 文件并收集需要一起提交的 peer 文件和 inline 本地 class。
 */
private class CompilationPeerCollectingVisitor(
    /**
     * 当前遍历文件所属 project。
     */
    val project: Project,
) : CfirDefaultVisitorVoid() {
    /**
     * 遍历过程中发现的 peer CFIR 文件集合。
     */
    val files: Set<CfirFile>
        field = LinkedHashSet<CfirFile>()

    /**
     * 遍历过程中发现的 inline 本地 class 集合。
     */
    val inlinedClasses: Set<CjTypeStatement>
        field = LinkedHashSet<CjTypeStatement>()

    /**
     * 默认继续访问子节点。
     */
    override fun visitElement(element: CfirElement) {
        element.acceptChildren(this)
    }

    /**
     * 访问构造器前先推进到 body resolve，确保调用关系可见。
     */
    override fun visitConstructor(constructor: CfirConstructor) {
        constructor.lazyResolveToPhase(CfirResolvePhase.BODY_RESOLVE)

        super.visitConstructor(constructor)
    }

    /**
     * 访问命名函数前先推进到 body resolve，确保函数体调用可见。
     */
    override fun visitNamedFunction(namedFunction: CfirNamedFunction) {
        namedFunction.lazyResolveToPhase(CfirResolvePhase.BODY_RESOLVE)
        super.visitFunction(namedFunction)
    }

    /**
     * 访问属性访问器；当前仓颉 peer 收集不在访问器层增加额外逻辑。
     */
    override fun visitPropertyAccessor(propertyAccessor: CfirPropertyAccessor) {
        super.visitPropertyAccessor(propertyAccessor)
    }

    /**
     * 访问属性前先推进到 body resolve，确保 initializer/accessor 调用可见。
     */
    override fun visitProperty(property: CfirProperty) {
        property.lazyResolveToPhase(CfirResolvePhase.BODY_RESOLVE)

        super.visitProperty(property)
    }

    /**
     * 访问 class；当前仓颉 peer 收集不在 class 层增加额外 inline class 逻辑。
     */
    override fun visitClass(klass: CfirClass) {
        super.visitClass(klass)
    }
}
