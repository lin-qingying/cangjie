package org.cangnova.cangjie.cfir.serialization.cjo

import org.cangnova.cangjie.cfir.declarations.cjoDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.resolve.providers.symbolProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirCompositeSymbolProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.session.CfirCjmpFamilySnapshot
import org.cangnova.cangjie.cfir.source.CfirSourceGraph
import org.cangnova.cangjie.cfir.source.CfirSourceGraphProvenance
import org.cangnova.cangjie.cfir.source.requireCanonicalOwningDeclarations
import java.util.Collections
import java.util.IdentityHashMap
import java.nio.file.Path

/**
 * 通过 CJO provider 能力解析 CJMP 导出所需的原始包快照。
 *
 * 快照选择属于二进制来源边界：它只读取 provider 当前代已经装载的事实，不按路径重新打开文件。
 */
object CjoExportSourceSnapshots {
    /** 定义来源在分析 freeze 前完成，普通 borrowed target 不通过此能力取得全包正文。 */
    fun sourceExportSnapshots(declarations: Collection<CfirDeclaration>, includeContent: Boolean): List<CjoSourceExportSnapshot> {
        declarations.filter { it.cjoDeclarationOrigin != null }.forEach { declaration ->
            selectSnapshot(declaration) { it.sourceExportSnapshotOf(declaration, includeContent) }
        }
        // 完成遍不能保留首个返回值；同 binder 后续请求也可能发布新 header/来源别名。
        return completedSourceExportSnapshots(declarations, includeContent)
    }

    fun completedSourceExportSnapshots(declarations: Collection<CfirDeclaration>, includeContent: Boolean): List<CjoSourceExportSnapshot> {
        val selected = IdentityHashMap<Any, CjoSourceExportSnapshot>()
        val ordered = ArrayList<CjoSourceExportSnapshot>()
        declarations.filter { it.cjoDeclarationOrigin != null }.forEach { declaration ->
            val snapshot = selectSnapshot(declaration) { it.completedSourceExportSnapshotOf(declaration, includeContent) }
            val previous = selected.putIfAbsent(snapshot.binderIdentity, snapshot)
            check(previous == null || previous.publicationIdentity === snapshot.publicationIdentity) {
                "Completed SOURCE snapshots of one binder must belong to one publication"
            }
            if (previous == null) ordered += snapshot
        }
        uniqueLoadedPackages(ordered.map { it.source })
        return Collections.unmodifiableList(ordered.toList())
    }

    private fun selectSnapshot(declaration: CfirDeclaration,
        query: (CjoSourceExportSnapshotProvider) -> CjoSourceExportSnapshot?): CjoSourceExportSnapshot {
        val matches = snapshotProviders(declaration.moduleData.session.symbolProvider)
            .filterIsInstance<CjoSourceExportSnapshotProvider>().mapNotNull(query)
        check(matches.isNotEmpty()) { "CJO export definition has no formal source snapshot capability: ${declaration.symbol}" }
        val snapshot = matches.first()
        check(matches.all { it.binderIdentity === snapshot.binderIdentity && it.source === snapshot.source && it.domain === snapshot.domain }) {
            "A CJO definition resolves to multiple actual contexts"
        }
        return snapshot
    }

    /** 完成全部明确 scope，再统一被动捕获；匹配失败不让其它 provider 覆盖。 */
    internal fun definitionExportSnapshots(requests: Collection<CjoDefinitionSnapshotRequest>): List<CjoDefinitionSnapshotResult.Completed> {
        requests.forEach { request -> selectDefinitionSnapshot(request) { it.definitionExportSnapshotOf(request) } }
        return completedDefinitionExportSnapshots(requests)
    }

    internal fun completedDefinitionExportSnapshots(requests: Collection<CjoDefinitionSnapshotRequest>): List<CjoDefinitionSnapshotResult.Completed> =
        Collections.unmodifiableList(requests.map { request ->
            selectDefinitionSnapshot(request) { it.completedDefinitionExportSnapshotOf(request) }
        })

    private fun selectDefinitionSnapshot(request: CjoDefinitionSnapshotRequest,
        query: (CjoSourceExportSnapshotProvider) -> CjoDefinitionSnapshotResult): CjoDefinitionSnapshotResult.Completed {
        val providers = Collections.newSetFromMap(IdentityHashMap<CjoSourceExportSnapshotProvider, Boolean>())
        request.seeds.forEach { seed -> snapshotProviders(seed.moduleData.session.symbolProvider)
            .filterIsInstance<CjoSourceExportSnapshotProvider>().forEach(providers::add) }
        var completed: CjoDefinitionSnapshotResult.Completed? = null
        providers.forEach { provider ->
            when (val result = query(provider)) {
                CjoDefinitionSnapshotResult.NotOwner -> Unit
                is CjoDefinitionSnapshotResult.Failure -> error(result.reason)
                is CjoDefinitionSnapshotResult.Completed -> {
                    check(completed == null) { "A scoped definition request has multiple actual provider owners" }
                    completed = result
                }
            }
        }
        return checkNotNull(completed) { "A scoped definition request has no actual provider owner" }
    }

    /** 只读观察全部正式来源；历史关系只解析本次 caller 根，不扩大 SOURCE 选择。 */
    internal fun completedInputs(declarations: Collection<CfirDeclaration>, includeContent: Boolean,
        originals: Collection<CfirSourceGraph>, preparedDeclarations: Collection<CfirDeclaration>,
        definitionResults: Collection<CjoDefinitionSnapshotResult.Completed> = emptyList()): CjoSourcePublicationInputs {
        val snapshots = completedSourceExportSnapshots(declarations, includeContent)
        val sources = ArrayList<CjoSourceExportSnapshot>()
        val binders = IdentityHashMap<Any, CjoSourceExportSnapshot>()
        val publications = IdentityHashMap<CjoSourceGraphPublicationOwner, Any>()
        val graphs = ArrayList<CfirSourceGraph>()
        fun observe(snapshot: CjoSourceExportSnapshot) {
            val graph = checkNotNull(snapshot.graph)
            val owner = (graph.provenance as? CfirSourceGraphProvenance.Published)?.owner as? CjoSourceGraphPublicationOwner
                ?: error("CJO SOURCE snapshot requires its actual publication capability")
            check(owner.binderIdentity === snapshot.binderIdentity && snapshot.graphPublications[graph] === graph)
            check(publications.put(owner, snapshot.publicationIdentity).let { it == null || it === snapshot.publicationIdentity })
            val previous = binders.putIfAbsent(snapshot.binderIdentity, snapshot)
            check(previous == null || previous.source === snapshot.source && previous.domain === snapshot.domain &&
                previous.publicationIdentity === snapshot.publicationIdentity) { "SOURCE input mixes publications of one binder" }
            if (previous == null) sources += snapshot
        }
        snapshots.forEach(::observe)
        definitionResults.forEach { result ->
            check(result.snapshot.source === result.selection.request.source &&
                result.snapshot.binderIdentity === result.selection.request.binderIdentity &&
                result.snapshot.domain === result.selection.request.domain) { "Definition selection belongs to a different source publication" }
            observe(result.snapshot)
            graphs += checkNotNull(result.snapshot.graph)
        }
        originals.forEach { original ->
            original.requireCanonicalOwningDeclarations()
            when (val provenance = original.provenance) {
                CfirSourceGraphProvenance.Owned -> graphs += original
                is CfirSourceGraphProvenance.Published -> {
                    val owner = provenance.owner as? CjoSourceGraphPublicationOwner
                        ?: error("Published SOURCE has no actual completed publication capability")
                    val snapshot = owner.completedSnapshotOf(original)
                    observe(snapshot)
                    graphs += snapshot.finalPublishedGraph(original)
                }
            }
        }
        snapshots.mapNotNullTo(graphs) { it.graph }
        // borrowed header owner 也保存代次，含没有 SOURCE owning 槽的实际 context。
        preparedDeclarations.mapNotNull { it.contentSource as? CjoSourceGraphPublicationOwner }.forEach { owner ->
            val publication = owner.currentPublicationIdentity()
            check(publications.put(owner, publication).let { it == null || it === publication }) { "SOURCE preparation observed a concurrent publication" }
        }
        uniqueLoadedPackages(sources.map { it.source })
        return CjoSourcePublicationInputs(Collections.unmodifiableList(sources), Collections.unmodifiableList(graphs.distinct()),
            Collections.unmodifiableMap(publications), Collections.unmodifiableList(definitionResults.toList()))
    }
    /** 重复引用同一个包只保留一次；同一路径的不同装载事务是冲突，不能先按路径吞掉证据。 */
    fun uniqueLoadedPackages(packages: Collection<CjoLoadedPackage>): List<CjoLoadedPackage> {
        val selected = linkedMapOf<Path, CjoLoadedPackage>()
        packages.forEach { snapshot ->
            val path = snapshot.sourcePath.toAbsolutePath().normalize()
            val previous = selected.putIfAbsent(path, snapshot)
            check(previous == null || previous === snapshot) {
                "CJO source $path resolves to multiple loaded package snapshots"
            }
        }
        return java.util.Collections.unmodifiableList(selected.values.toList())
    }
    /**
     * 按完整的 CJO 来源路径读取当前 provider 代已经加载的快照。
     * 普通库导出遇到外部引用时用此能力投影官方 imported FullId；该查询不会重新搜索或打开文件。
     */
    fun loadedPackage(provider: CfirSymbolProvider, sourcePath: Path): CjoLoadedPackage? {
        val normalizedPath = sourcePath.toAbsolutePath().normalize()
        val matches = uniqueLoadedPackages(snapshotProviders(provider)
            .mapNotNull { it.loadedPackageSnapshot(normalizedPath) })
        check(matches.all { it.sourcePath.toAbsolutePath().normalize() == normalizedPath })
        check(matches.size <= 1) {
            "CJO source $normalizedPath resolves to multiple loaded package snapshots"
        }
        return matches.singleOrNull()
    }

    fun loadedPackages(
        provider: CfirSymbolProvider,
        family: CfirCjmpFamilySnapshot,
    ): List<CjoLoadedPackage> {
        val paths = family.packageDeclarationsForExport()
            .plus(family.familyCommonDeclarations())
            .mapNotNull { it.cjoDeclarationOrigin }
            .map { Path.of(it.packagePath).toAbsolutePath().normalize() }
            .distinct()
        val snapshotProviders = snapshotProviders(provider)
        return paths.map { sourcePath ->
            val matches = uniqueLoadedPackages(snapshotProviders
                .mapNotNull { it.loadedPackageSnapshot(sourcePath) })
            check(matches.all { it.sourcePath.toAbsolutePath().normalize() == sourcePath })
            check(matches.isNotEmpty()) { "CJMP export cannot find the loaded CJO snapshot for $sourcePath" }
            check(matches.size == 1) { "CJMP export origin $sourcePath resolves to multiple loaded package snapshots" }
            matches.single()
        }
    }

    private fun snapshotProviders(provider: CfirSymbolProvider): List<CjoLoadedPackageSnapshotProvider> = when (provider) {
        is CjoLoadedPackageSnapshotProvider -> listOf(provider)
        is CfirCompositeSymbolProvider -> provider.providers.flatMap(::snapshotProviders)
        else -> emptyList()
    }
}

/** 准备协调和冻结后校验持有原被动路由；snapshot 自身没有可刷新 reader。 */
internal class CjoSourcePublicationInputs(
    val snapshots: List<CjoSourceExportSnapshot>,
    val graphs: List<CfirSourceGraph>,
    private val publications: Map<CjoSourceGraphPublicationOwner, Any>,
    val definitionResults: List<CjoDefinitionSnapshotResult.Completed>,
) {
    fun sameInputs(other: CjoSourcePublicationInputs): Boolean = graphs.size == other.graphs.size &&
        graphs.indices.all { graphs[it] === other.graphs[it] } && publications.size == other.publications.size &&
        publications.all { (owner, token) -> other.publications[owner] === token } &&
        definitionResults.size == other.definitionResults.size && definitionResults.indices.all { index ->
            val left = definitionResults[index]; val right = other.definitionResults[index]
            left.selection.request === right.selection.request && left.selection.identity === right.selection.identity &&
                left.snapshot.publicationIdentity === right.snapshot.publicationIdentity && left.snapshot.graph === right.snapshot.graph
        } && snapshots.size == other.snapshots.size && snapshots.indices.all { index ->
            val left = snapshots[index]; val right = other.snapshots[index]
            left.binderIdentity === right.binderIdentity && left.source === right.source && left.domain === right.domain &&
                left.publicationIdentity === right.publicationIdentity
        }

    fun validateUnchanged() = publications.forEach { (owner, token) -> owner.requirePublicationUnchanged(token) }

    fun <T> withReadOnlyInput(action: () -> T): T {
        val owners = publications.keys.toList()
        fun read(index: Int): T = if (index == owners.size) action() else owners[index].withReadOnlyContent { read(index + 1) }
        return read(0)
    }
}
