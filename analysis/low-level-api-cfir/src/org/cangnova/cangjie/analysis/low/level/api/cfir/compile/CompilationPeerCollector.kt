

package org.cangnova.cangjie.analysis.low.level.api.cfir.compile

import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.low.level.api.cfir.projectStructure.llCfirModuleData
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.containingCjFileIfAny
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.getContainingFile
import org.cangnova.cangjie.codegen.state.GenerationState
import org.cangnova.cangjie.cfir.*
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.utils.hasBody
import org.cangnova.cangjie.cfir.declarations.utils.isInline
import org.cangnova.cangjie.cfir.expressions.CfirResolvable
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.symbols.SymbolInternals
import org.cangnova.cangjie.cfir.symbols.impl.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.visitors.CfirDefaultVisitorVoid
import org.cangnova.cangjie.psi
import org.cangnova.cangjie.psi.CjClassOrObject
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.utils.addIfNotNull
import org.cangnova.cangjie.utils.exceptions.errorWithAttachment

/**
 * Processes the declaration, collecting files that would need to be submitted to the backend (or handled specially)
 * in case if the declaration is compiled.
 *
 * Besides the file that owns the declaration, the visitor also recursively collects source files with called inline functions.
 * In addition, the visitor collects a list of inlined local classes. Such a list might be useful in [GenerationState.GenerateClassFilter]
 * to filter out class files unrelated to the current compilation.
 *
 * Note that compiled declarations are not analyzed, as the backend can inline them natively.
 */
@CaImplementationDetail
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
    private val inlinedClasses = LinkedHashSet<CjClassOrObject>()

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
                    (moduleStack + module).forEach { this@withEntry.println(it) }
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

@CaImplementationDetail
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
    val inlinedClasses: Set<CjClassOrObject>,
)

private class CompilationPeerCollectingVisitor(
    val project: Project,
) : CfirDefaultVisitorVoid() {
    private val collectedFunctions = HashSet<CfirFunction>()

    private var isInlineFunctionContext: Boolean = false

    val files: Set<CfirFile>
        field = LinkedHashSet<CfirFile>()

    val inlinedClasses: Set<CjClassOrObject>
        field = LinkedHashSet<CjClassOrObject>()

    override fun visitElement(element: CfirElement) {
        if (element is CfirResolvable) {
            processResolvable(element)
        }

        element.acceptChildren(this)
    }

    override fun visitConstructor(constructor: CfirConstructor) {
        constructor.lazyResolveToPhase(CfirResolvePhase.BODY_RESOLVE)

        super.visitConstructor(constructor)
    }

    override fun visitNamedFunction(namedFunction: CfirNamedFunction) {
        namedFunction.lazyResolveToPhase(CfirResolvePhase.BODY_RESOLVE)

        withInlineFunctionContext(namedFunction) {
            super.visitFunction(namedFunction)
        }
    }

    override fun visitPropertyAccessor(propertyAccessor: CfirPropertyAccessor) {
        withInlineFunctionContext(propertyAccessor) {
            super.visitPropertyAccessor(propertyAccessor)
        }
    }

    override fun visitProperty(property: CfirProperty) {
        property.lazyResolveToPhase(CfirResolvePhase.BODY_RESOLVE)

        super.visitProperty(property)
    }

    override fun visitClass(klass: CfirClass) {
        super.visitClass(klass)

        if (isInlineFunctionContext) {
            inlinedClasses.addIfNotNull(klass.psi as? CjClassOrObject)
        }
    }

    @OptIn(SymbolInternals::class)
    private fun processResolvable(element: CfirResolvable) {
        val reference = element.calleeReference
        if (reference !is CfirResolvedNamedReference) {
            return
        }

        val symbol = reference.resolvedSymbol
        if (symbol is CfirCallableSymbol<*>) {
            when (val fir = symbol.fir) {
                is CfirFunction -> {
                    register(fir)
                }
                is CfirProperty -> {
                    fir.getter?.let(::register)
                    fir.setter?.let(::register)
                }
                else -> {}
            }
        }
    }

    /**
     * Register a containing source file for an inline function.
     */
    private fun register(callee: CfirFunction) {
        val originalFunction = callee.unwrapSubstitutionOverrides()
        if (originalFunction.isInline) {
            if (originalFunction.hasBody) {
                if (collectedFunctions.add(originalFunction)) {
                    val calleeFile = callee.getContainingFile()
                    if (calleeFile != null && calleeFile.origin == CfirDeclarationOrigin.Source) {
                        files.add(calleeFile)
                    }
                }
            }
        }
    }

    private inline fun withInlineFunctionContext(function: CfirFunction, block: () -> Unit) {
        val needsInlineContext = !isInlineFunctionContext && function.isInline

        try {
            if (needsInlineContext) {
                isInlineFunctionContext = true
            }
            block()
        } finally {
            if (needsInlineContext) {
                isInlineFunctionContext = false
            }
        }
    }
}
