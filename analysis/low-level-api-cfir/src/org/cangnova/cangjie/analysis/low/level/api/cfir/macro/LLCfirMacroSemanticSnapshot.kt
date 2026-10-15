package org.cangnova.cangjie.analysis.low.level.api.cfir.macro

import com.intellij.openapi.util.ModificationTracker
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.psi
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroExpansionRegistry
import org.cangnova.cangjie.cfir.resolve.providers.macro.RecordableRawCfirFiles
import org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjClassLikeDeclaration
import org.cangnova.cangjie.psi.CjFile
import java.util.Collections
import java.util.IdentityHashMap
import java.util.concurrent.atomic.AtomicReference

/**
 * 已完成宏构造的源码模块 CFIR 视图。
 *
 * 所有索引都从同一批最终文件构建。PSI 文件身份、CFIR 声明容器、名称过滤与符号查询因此共享一个原子发布边界。
 */
class LLCfirMacroSemanticSnapshot internal constructor(
    sourceFiles: List<CjFile>,
    /** 本次发布的完整 expanded CFIR 文件集合。 */
    val files: List<CfirFile>,
    /** 当前快照的宏来源和生成源码位置映射。 */
    val registry: MacroExpansionRegistry,
) {
    /** 原始物理 PSI 文件到最终 CFIR 文件的身份映射。 */
    private val filesBySourceIdentity: Map<CjFile, CfirFile>

    /** 每个最终文件所属的包。 */
    val filesByPackage: Map<FqName, List<CfirFile>>

    /** 最终文件中的 class-like 声明，按稳定 ClassId 聚合。 */
    private val classifiersByClassId: Map<ClassId, List<CfirClassLikeDeclaration>>

    /** 最终文件中的源码 classifier，按 PSI 对象身份索引。 */
    private val classifiersBySourceIdentity: Map<CjClassLikeDeclaration, CfirClassLikeDeclaration>

    /** 最终文件中的顶层 callable 声明，按包名与名称聚合。 */
    private val callablesById: Map<CallableId, List<CfirCallableDeclaration>>

    /** 最终源码模块中的顶层 classifier 与 callable 名称。 */
    private val classifierNamesByPackage: Map<FqName, Set<Name>>
    private val callableNamesByPackage: Map<FqName, Set<Name>>
    /** 最终快照中由宏来源映射确认的顶层 class-like 名称。 */
    private val macroGeneratedClassifierNamesByPackage: Map<FqName, Set<Name>>
    /** 最终快照中由宏来源映射确认的顶层 callable 名称。 */
    private val macroGeneratedCallableNamesByPackage: Map<FqName, Set<Name>>

    /** 展开后每个 CFIR 声明对应的最终容器文件，使用对象身份避免结构相等误匹配。 */
    private val containerByDeclaration: Map<CfirDeclaration, CfirFile>

    init {
        require(sourceFiles.size == files.size) {
            "sourceFiles size (${sourceFiles.size}) must match expanded files size (${files.size})"
        }

        val sourceIdentityMap = IdentityHashMap<CjFile, CfirFile>(sourceFiles.size)
        sourceFiles.forEachIndexed { index, sourceFile ->
            check(sourceIdentityMap.put(sourceFile, files[index]) == null) {
                "A source PSI file occurs more than once in a macro semantic snapshot."
            }
        }
        filesBySourceIdentity = Collections.unmodifiableMap(sourceIdentityMap)

        val packageFiles = linkedMapOf<FqName, MutableList<CfirFile>>()
        val classifiers = linkedMapOf<ClassId, MutableList<CfirClassLikeDeclaration>>()
        val classifiersBySource = IdentityHashMap<CjClassLikeDeclaration, CfirClassLikeDeclaration>()
        val callables = linkedMapOf<CallableId, MutableList<CfirCallableDeclaration>>()
        val classifierNames = linkedMapOf<FqName, MutableSet<Name>>()
        val callableNames = linkedMapOf<FqName, MutableSet<Name>>()
        val macroGeneratedClassifierNames = linkedMapOf<FqName, MutableSet<Name>>()
        val macroGeneratedCallableNames = linkedMapOf<FqName, MutableSet<Name>>()
        val declarationContainers = IdentityHashMap<CfirDeclaration, CfirFile>()

        for (file in files) {
            val packageFqName = file.packageDirective.packageFqName
            packageFiles.getOrPut(packageFqName, ::mutableListOf).add(file)

            for (declaration in file.declarations) {
                val macroGenerated = declaration.source
                    ?.let { registry.originSourceForGeneratedSource(it) != null }
                    ?: false
                if (declaration is CfirClassLikeDeclaration) {
                    classifiers.getOrPut(declaration.symbol.classId, ::mutableListOf).add(declaration)
                    val sourcePsi = declaration.psi as? CjClassLikeDeclaration
                    if (sourcePsi != null) classifiersBySource[sourcePsi] = declaration
                    classifierNames.getOrPut(packageFqName, ::linkedSetOf).add(declaration.symbol.name)
                    if (macroGenerated) {
                        macroGeneratedClassifierNames.getOrPut(packageFqName, ::linkedSetOf).add(declaration.symbol.name)
                    }
                }
                if (declaration is CfirCallableDeclaration) {
                    val callableName = declaration.symbol.name
                    callables.getOrPut(CallableId(packageFqName, callableName), ::mutableListOf).add(declaration)
                    callableNames.getOrPut(packageFqName, ::linkedSetOf).add(callableName)
                    if (macroGenerated) {
                        macroGeneratedCallableNames.getOrPut(packageFqName, ::linkedSetOf).add(callableName)
                    }
                }
            }

            file.accept(object : CfirVisitorVoid() {
                override fun visitElement(element: org.cangnova.cangjie.cfir.CfirElement) {
                    if (element is CfirDeclaration) declarationContainers[element] = file
                    element.acceptChildren(this)
                }
            })
        }

        filesByPackage = freezeLists(packageFiles)
        classifiersByClassId = freezeLists(classifiers)
        classifiersBySourceIdentity = Collections.unmodifiableMap(classifiersBySource)
        callablesById = freezeLists(callables)
        classifierNamesByPackage = freezeSets(classifierNames)
        callableNamesByPackage = freezeSets(callableNames)
        macroGeneratedClassifierNamesByPackage = freezeSets(macroGeneratedClassifierNames)
        macroGeneratedCallableNamesByPackage = freezeSets(macroGeneratedCallableNames)
        containerByDeclaration = Collections.unmodifiableMap(declarationContainers)
    }

    /** 返回包内最终语义文件；快照发布后结果可精确为空。 */
    fun filesInPackage(packageFqName: FqName): List<CfirFile> = filesByPackage[packageFqName].orEmpty()

    /** 返回 class id 对应的最终 class-like 声明。 */
    fun classifiers(classId: ClassId): List<CfirClassLikeDeclaration> = classifiersByClassId[classId].orEmpty()

    /** 通过物理 PSI 对象身份返回最终 classifier，供 IDE 声明视图精确关联源声明。 */
    fun classifierForSource(source: CjClassLikeDeclaration): CfirClassLikeDeclaration? =
        classifiersBySourceIdentity[source]

    /** 返回顶层 callable id 对应的最终声明。 */
    fun callables(callableId: CallableId): List<CfirCallableDeclaration> = callablesById[callableId].orEmpty()

    /** 返回包内顶层 classifier 名称。 */
    fun classifierNames(packageFqName: FqName): Set<Name> = classifierNamesByPackage[packageFqName].orEmpty()

    /** 返回包内顶层 callable 名称。 */
    fun callableNames(packageFqName: FqName): Set<Name> = callableNamesByPackage[packageFqName].orEmpty()

    /** 返回包内经宏来源映射确认生成的顶层 class-like 名称。 */
    fun macroGeneratedClassifierNames(packageFqName: FqName): Set<Name> =
        macroGeneratedClassifierNamesByPackage[packageFqName].orEmpty()

    /** 返回包内经宏来源映射确认生成的顶层 callable 名称。 */
    fun macroGeneratedCallableNames(packageFqName: FqName): Set<Name> =
        macroGeneratedCallableNamesByPackage[packageFqName].orEmpty()

    /** 返回某物理 source file 对应的最终语义 CFIR。 */
    fun fileForSource(sourceFile: CjFile): CfirFile? = filesBySourceIdentity[sourceFile]

    /** 返回声明所在的最终容器文件。 */
    fun containerFile(declaration: CfirDeclaration): CfirFile? = containerByDeclaration[declaration]

    private fun <K, V> freezeLists(source: Map<K, List<V>>): Map<K, List<V>> =
        Collections.unmodifiableMap(source.mapValues { (_, values) -> values.toList() })

    private fun <K, V> freezeSets(source: Map<K, MutableSet<V>>): Map<K, Set<V>> =
        Collections.unmodifiableMap(source.mapValues { (_, values) -> Collections.unmodifiableSet(values.toSet()) })
}

/**
 * 源码模块宏语义快照的原子发布点，同时作为 scope-session 缓存的修改跟踪器。
 */
class LLCfirMacroSemanticSnapshotStore : ModificationTracker {
    private data class State(val modificationCount: Long, val snapshot: LLCfirMacroSemanticSnapshot?)

    private val state = AtomicReference(State(0L, null))

    /** 当前完整语义快照；在发布成功前为 null。 */
    val snapshot: LLCfirMacroSemanticSnapshot?
        get() = state.get().snapshot

    override fun getModificationCount(): Long = state.get().modificationCount

    /** 原子发布同一宏构造结果对应的源码文件、expanded CFIR 与映射索引。 */
    @Synchronized
    fun publish(
        sourceFiles: List<CjFile>,
        recordableFiles: RecordableRawCfirFiles,
        registry: MacroExpansionRegistry,
    ): Boolean {
        val current = state.get()
        if (current.snapshot != null) return false
        if (sourceFiles.size != recordableFiles.files.size) return false

        val nextSnapshot = LLCfirMacroSemanticSnapshot(sourceFiles, recordableFiles.files, registry)
        state.set(State(current.modificationCount + 1, nextSnapshot))
        return true
    }
}
