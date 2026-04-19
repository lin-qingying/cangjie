

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

    private val peers = LinkedHashMap<CaModule, MutableList<CjFile>>()
    private val inlinedClasses = LinkedHashSet<CjTypeStatement>()

    private val visited = HashSet<CfirFile>()
    private val moduleStack = ArrayDeque<CaModule>()

    private fun process(file: CfirFile) {
        ProgressManager.checkCanceled()

        val ktFile = file.containingCjFileIfAny
        if (ktFile == null || ktFile.isCompiled) {
            return
        }

        val module = file.llCfirModuleData.ktModule
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
        val visitor = CompilationPeerCollectingVisitor(ktFile.project)
        file.accept(visitor)

        inlinedClasses.addAll(visitor.inlinedClasses)

        withModule(module) {
            visitor.files.forEach(::process)
        }

        peers.getOrPut(module, ::ArrayList).add(ktFile)
    }

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


class CompilationPeerData(
    /**
     * The original file and all files that contain inline functions/properties called from that file or any other files from the list.
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

private class CompilationPeerCollectingVisitor(
    val project: Project,
) : CfirDefaultVisitorVoid() {
    val files: Set<CfirFile>
        field = LinkedHashSet<CfirFile>()

    val inlinedClasses: Set<CjTypeStatement>
        field = LinkedHashSet<CjTypeStatement>()

    override fun visitElement(element: CfirElement) {
        element.acceptChildren(this)
    }

    override fun visitConstructor(constructor: CfirConstructor) {
        constructor.lazyResolveToPhase(CfirResolvePhase.BODY_RESOLVE)

        super.visitConstructor(constructor)
    }

    override fun visitNamedFunction(namedFunction: CfirNamedFunction) {
        namedFunction.lazyResolveToPhase(CfirResolvePhase.BODY_RESOLVE)
        super.visitFunction(namedFunction)
    }

    override fun visitPropertyAccessor(propertyAccessor: CfirPropertyAccessor) {
        super.visitPropertyAccessor(propertyAccessor)
    }

    override fun visitProperty(property: CfirProperty) {
        property.lazyResolveToPhase(CfirResolvePhase.BODY_RESOLVE)

        super.visitProperty(property)
    }

    override fun visitClass(klass: CfirClass) {
        super.visitClass(klass)
    }
}
