package org.cangnova.cangjie.cfir.session

import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.common.moduleData
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirCjoDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.cjoDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.resolvePhase
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFileSymbol
import java.util.Collections

/** 原结构准备的声明域；按实际模块、session 与已绑定声明对象记录，不按路径或 ClassId 合并。 */
class CfirCjmpClassifierDomain private constructor(
    val actualSession: CfirSession,
    val actualModule: CfirModuleData,
    refinementModules: Collection<CfirModuleData>,
    declarations: Map<CfirClassLikeSymbol<*>, CfirClassLikeDeclaration>,
    roots: Map<CfirClassLikeSymbol<*>, RootWitness>,
) {
    private val refinementOrder: List<CfirModuleData> = Collections.unmodifiableList(ArrayList(refinementModules))
    val refinementModules: Set<CfirModuleData> = Collections.unmodifiableSet(LinkedHashSet(refinementOrder))
    private val declarations: Map<CfirClassLikeSymbol<*>, CfirClassLikeDeclaration> =
        Collections.unmodifiableMap(LinkedHashMap(declarations))
    private val modules: Map<CfirClassLikeSymbol<*>, CfirModuleData> =
        Collections.unmodifiableMap(declarations.mapValues { it.value.moduleData })
    private val roots: Map<CfirClassLikeSymbol<*>, RootWitness> = Collections.unmodifiableMap(LinkedHashMap(roots))

    /** 原物理 root 的证明来自文件槽或已加载的原始 header，发现组不能制造它。 */
    private sealed interface RootWitness {
        fun validate(declaration: CfirClassLikeDeclaration)

        class Source(val file: CfirFile, val index: Int) : RootWitness {
            override fun validate(declaration: CfirClassLikeDeclaration) {
                check(declaration.origin.fromSource && file.symbol.isBound && file.symbol.cfir === file &&
                    file.moduleData === declaration.moduleData && file.declarations.getOrNull(index) === declaration) {
                    "Original classifier file root changed after structural preparation"
                }
                check(file.packageDirective.packageFqName == declaration.symbol.classId.packageFqName) {
                    "Original classifier file package does not describe its type header"
                }
            }
        }

        class Loaded(val origin: CfirCjoDeclarationOrigin) : RootWitness {
            override fun validate(declaration: CfirClassLikeDeclaration) {
                check(!declaration.origin.fromSource && origin.isTopLevel && declaration.cjoDeclarationOrigin === origin) {
                    "Original loaded classifier root header changed after structural preparation"
                }
            }
        }
    }

    internal fun requirePhysicalRoot(symbol: CfirClassLikeSymbol<*>) {
        val declaration = requireDeclaration(symbol)
        checkNotNull(roots[symbol]) { "Classifier has no original physical top-level root proof" }.validate(declaration)
    }

    /** 查询发生在 resolve 的比较准备阶段；失效或尚未完成的 header 不能降为恒等投影。 */
    internal fun requireDeclaration(symbol: CfirClassLikeSymbol<*>): CfirClassLikeDeclaration {
        val declaration = checkNotNull(declarations[symbol]) { "Classifier was not part of this structural input" }
        check(symbol.isBound && symbol.cfir === declaration) { "Classifier binding changed after structural preparation" }
        check(declaration.moduleData === modules.getValue(symbol)) { "Classifier module changed after structural preparation" }
        check(declaration.resolvePhase >= CfirResolvePhase.TYPES) { "Classifier comparison requires its completed type header" }
        return declaration
    }

    internal fun moduleOf(symbol: CfirClassLikeSymbol<*>): CfirModuleData =
        checkNotNull(modules[symbol]) { "Classifier module was not captured in this structural input" }

    internal fun requireSession(session: CfirSession) {
        check(session === actualSession && session.moduleData === actualModule && session.kind == CfirSession.Kind.Source) {
            "A classifier relation must belong to the actual structural session"
        }
        val currentRefinements = actualModule.allRefinementDependencies
        check(currentRefinements.size == refinementOrder.size &&
            currentRefinements.indices.all { currentRefinements[it] === refinementOrder[it] }) {
            "Refinement dependencies changed after structural preparation"
        }
    }

    companion object {
        /** 在原 prepared-group 创建时捕获；没有 classifier 的合法字段组保留真实 session 和空声明域。 */
        fun capture(
            actualSession: CfirSession,
            declarations: Collection<CfirDeclaration>,
            sourceFiles: Collection<CfirFile>,
        ): CfirCjmpClassifierDomain {
            val classifiers = LinkedHashMap<CfirClassLikeSymbol<*>, CfirClassLikeDeclaration>()
            val roots = LinkedHashMap<CfirClassLikeSymbol<*>, RootWitness>()
            val files = LinkedHashMap<CfirFileSymbol, CfirFile>()
            for (file in sourceFiles) {
                check(file.symbol.isBound && file.symbol.cfir === file) { "Physical root input requires its original bound file" }
                val previous = files.put(file.symbol, file)
                check(previous == null || previous === file) { "A file symbol has conflicting original root products" }
            }
            for (declaration in declarations.filterIsInstance<CfirClassLikeDeclaration>()) {
                check(declaration.symbol.isBound && declaration.symbol.cfir === declaration) {
                    "Structural classifier inputs must retain their actual bound declaration"
                }
                val previous = classifiers.put(declaration.symbol, declaration)
                check(previous == null || previous === declaration) { "A classifier symbol has conflicting input declarations" }
                if (declaration.origin.fromSource) {
                    val occurrences = files.values.flatMap { file ->
                        file.declarations.mapIndexedNotNull { index, root ->
                            if (root === declaration) RootWitness.Source(file, index) else null
                        }
                    }
                    check(occurrences.size <= 1) { "Classifier has multiple original physical file roots" }
                    occurrences.singleOrNull()?.let { witness ->
                        witness.validate(declaration)
                        roots[declaration.symbol] = witness
                    }
                } else {
                    val origin = declaration.cjoDeclarationOrigin
                    if (origin != null && origin.isTopLevel) roots[declaration.symbol] = RootWitness.Loaded(origin)
                }
            }
            val module = actualSession.moduleData
            return CfirCjmpClassifierDomain(actualSession, module, module.allRefinementDependencies, classifiers, roots)
        }
    }
}

/** 真实名义结构 seed 的有向证明；字段和 extend 绑定不构造此产品。 */
class CfirCjmpClassifierRelation internal constructor(
    val common: CfirClassLikeSymbol<*>,
    val specific: CfirClassLikeSymbol<*>,
    val authority: CfirCjmpStructureSnapshot,
)
