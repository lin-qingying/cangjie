package org.cangnova.cangjie.analysis.low.level.api.cfir.providers

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.IndexNotReadyException
import com.intellij.psi.util.PsiModificationTracker
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolUnavailableException
import org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.materializeTopLevelExtendFiles
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirResolvableModuleSession
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.resolve.providers.CfirExtendProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSessionExtendProvider
import org.cangnova.cangjie.cfir.resolve.services.CfirExtendIndexStore
import org.cangnova.cangjie.cfir.session.services.CfirExtendTargetKey
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.session.typeResolver
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import java.util.concurrent.ConcurrentHashMap

/**
 * Low-level source session 的 extend provider。
 *
 * 主编译器在 `CfirExtensionsResolveProcessor.beforePhase` 中用 `CfirProviderImpl.getAllFiles()`
 * 刷新 extend 索引；LL session 没有全量文件 provider，只能以当前 session 已构建的
 * `ModuleFileCache` 作为 lazy resolve 可见文件集合。这里在 provider 查询入口刷新同一份
 * [CfirExtendIndexStore]，然后复用主干 [CfirSessionExtendProvider] 的语义查询。
 *
 * 与 Kotlin 的偏离（显式记录）：Kotlin 镜像里**没有** extend provider
 * （`LLFirSessionExtendProvider` / `LLFirExtendableSymbolProvider` 均不存在），它靠
 * `LLFirSessionInvalidationService` + PSI/ProjectRoot modification tracker 让整个 session 失效。
 * 仓颉的 extend 是按「被扩展目标类型」建索引的派生数据，必须有一个按 session 重建的入口；
 * 这里以「已建索引的文件集」作为版本号，配合 [CfirExtendIndexStore.modificationCount]，
 * 等价于 Kotlin 的「session 快照内派生数据只建一次」。
 */
internal class LLCfirSessionExtendProvider(
    /**
     * 当前源码可解析模块 session。
     */
    private val session: LLCfirResolvableModuleSession,

    /**
     * 保存 extend 索引的 session 级存储。
     */
    private val indexStore: CfirExtendIndexStore,
) : CfirExtendProvider {
    /**
     * 复用主干 extend provider 的查询语义。
     */
    private val delegate = CfirSessionExtendProvider(session, indexStore)

    @Volatile
    /**
     * 上一次提交到 [indexStore] 的 CFIR 文件集合，充当 extend 索引版本号。
     *
     * 语义是「这批文件已经建过索引」，**与单个文件头部是否解析成功无关**：
     * 失败文件同样提交。理由有二：
     *
     * - 官方模型是「声明保留、只让该条 extend 的目标键缺席」
     *   （`external/cangjie_compiler/src/Sema/TypeCheckExtend.cpp:358-407` 的 `UpdateExtendMap`
     *   对头部类型不正确的 extend 只 `continue`），extend 表每轮从 AST 重建，没有跨轮 tombstone；
     * - 若把失败文件排除在外，文件集比较永远不相等，于是**每次查询都全量重建索引**，
     *   且每次查询都会给失败文件累加一次失败次数，重试预算在最初几次属性访问内就被烧穿。
     */
    private var indexedFiles: Set<CfirFile> = emptySet()

    /** 当前线程是否正为这批文件建立 extend 索引，防止类型解析重入启动相同工作。 */
    @Volatile
    private var indexBuildInProgress: Boolean = false

    /**
     * 上一次建索引时的 PSI 修改计数，用作「外部新鲜度」信号。
     *
     * 只有它变化时，带降级文件的那一批才会被重建，从而给暂时失败的 extend 一次重试机会，
     * 而不是永久冻结在首个版本上。
     */
    @Volatile
    private var lastBuildModificationCount: Long = 0L

    /**
     * 最近一次建索引时头部解析失败的文件。
     *
     * 它参与重建判定：该集合非空且 PSI 修改计数变化时，允许对这批文件重试一次
     * （文件集不变、也没有任何外部改动时，重复解析同一处失败没有收益）。
     * 官方在同一轮里也不会第二次尝试失败的 extend。
     */
    private val degradedFiles: MutableSet<CfirFile> = ConcurrentHashMap.newKeySet()

    /** 全量扩展枚举与定向查询使用相同的懒解析索引版本。 */
    override fun getAllExtends(): List<CfirExtend> {
        ensureIndexIsFresh()
        return delegate.getAllExtends()
    }

    /**
     * 查询目标 key 对应的扩展声明。
     */
    override fun getExtendsForTarget(targetKey: CfirExtendTargetKey): List<CfirExtend> {
        ensureIndexIsFresh()
        return delegate.getExtendsForTarget(targetKey)
    }

    /**
     * 查询作用于指定 class id 的扩展声明。
     */
    override fun getExtendsForClass(classId: ClassId): List<CfirExtend> {
        ensureIndexIsFresh()
        return delegate.getExtendsForClass(classId)
    }

    /**
     * 查询指定包中的扩展声明。
     */
    override fun getExtendsInPackage(packageFqName: FqName): List<CfirExtend> {
        ensureIndexIsFresh()
        return delegate.getExtendsInPackage(packageFqName)
    }

    /**
     * 查询作用于内建基本类型的扩展声明。
     */
    override fun getExtendsForBuiltinType(kind: PrimitiveTypeKind): List<CfirExtend> {
        ensureIndexIsFresh()
        return delegate.getExtendsForBuiltinType(kind)
    }

    /**
     * 查询 [symbol] 所属的扩展声明。
     */
    override fun getContainingExtend(symbol: CfirCallableSymbol<*>): CfirExtend? {
        ensureIndexIsFresh()
        return delegate.getContainingExtend(symbol)
    }

    /**
     * 查询 [extend] 所在包名。
     */
    override fun getPackageFqName(extend: CfirExtend): FqName? {
        ensureIndexIsFresh()
        return delegate.getPackageFqName(extend)
    }

    /** 查询 [extend] 的结构性源码文件归属。 */
    override fun getContainingFile(extend: CfirExtend): CfirFile? {
        ensureIndexIsFresh()
        return delegate.getContainingFile(extend)
    }

    /**
     * 确保 [indexStore] 与当前 session 已缓存的顶层 extend 文件一致。
     *
     * 重建判据是**文件集版本**：集合不变就直接返回，集合变化才在同步块内重建一次。
     * 头部解析失败不会让索引看起来「不新鲜」——否则每次查询都会全量重建，而重建本身
     * 又会重新尝试同一批失败文件。唯一例外是「本批有降级文件、且 PSI 修改计数变了」：
     * 那是外部新鲜度信号（用户改动了代码），此时给失败文件一次重试机会，
     * 避免一次索引未就绪被冻结到整个 session。
     *
     * 版本提交语义：一批文件（成功与否）只提交一次；随后在目标索引里体现为
     * 「解析成功的 extend 有目标键、失败的没有」，与官方「声明保留、关系条目缺席」一致。
     */
    private fun ensureIndexIsFresh() {
        session.symbolProvider.materializeTopLevelExtendFiles()
        val files = session.moduleComponents.cache.getAllCachedCfirFilesForResolution().toList()
        if (files.isEmpty()) return

        val fileSet = files.toSet()
        val modificationCount = PsiModificationTracker.getInstance(session.project).modificationCount
        val needsRebuild = fileSet != indexedFiles ||
            (degradedFiles.isNotEmpty() && modificationCount != lastBuildModificationCount)
        if (!needsRebuild) return

        synchronized(this) {
            if (indexBuildInProgress) return

            val latestFiles = session.moduleComponents.cache.getAllCachedCfirFilesForResolution().toList()
            if (latestFiles.isEmpty()) return

            val latestFileSet = latestFiles.toSet()
            if (latestFileSet == indexedFiles &&
                (degradedFiles.isEmpty() || modificationCount == lastBuildModificationCount)
            ) {
                return
            }

            indexBuildInProgress = true
            try {
                // 包与文件 owner 在 TYPES 完成前已由文件结构确定，先发布该部分索引，供类型解析中的
                // extend 可见性查询读取；完整目标/接口模型仍在头部 resolve 完成后一次性重建。
                indexStore.recordDeclarationSources(latestFiles)

                // LL 索引消费与主编译器 EXTENSIONS 阶段保持一致：extend 头部必须先完成 TYPES。
                // 单条 extend 失败不阻塞其它声明，也不阻止本批文件进入索引。
                for (file in latestFiles) {
                    resolveTopLevelExtendsToTypes(file)
                }

                // 全部文件都进索引：头部未定型的 extend 由 toSemanticModel 给出的
                // targetKey == null 自然缺席目标索引，但声明本身仍可按文件/声明查询到，
                // 也可以定位它自己的结构诊断。
                indexStore.rebuild(latestFiles, session.typeResolver)

                // 逐条建模失败已在存储层隔离，这里只补一次可观测记录：
                // 失败条目的声明保留、目标键缺席，与官方 `UpdateExtendMap` 的逐条 `continue` 一致。
                if (indexStore.lastRebuildFailureCount > 0) {
                    LOG.warn(
                        "extend 语义建模失败 ${indexStore.lastRebuildFailureCount} 条，已按「目标索引缺席」降级",
                        indexStore.lastRebuildFailure,
                    )
                }

                indexedFiles = latestFileSet
                lastBuildModificationCount = modificationCount
            } catch (cancelled: ProcessCanceledException) {
                throw cancelled
            } catch (indexNotReady: IndexNotReadyException) {
                throw indexNotReady
            } catch (failure: Exception) {
                // 真缺陷要暴露，但同一批文件不能每次查询都重跑同一处失败：
                // 先提交版本再上抛，异常只出现一次。
                indexedFiles = latestFileSet
                lastBuildModificationCount = modificationCount
                throw failure
            } finally {
                indexBuildInProgress = false
            }
        }
    }

    /**
     * 将 [file] 中的顶层 extend 声明推进到 [CfirResolvePhase.TYPES]。
     *
     * 失败粒度是**单条声明**：官方 `PreCheckExtend` 对每条 extend 独立处理，一条坏的只跳过它自己
     * （`external/cangjie_compiler/src/Sema/TypeCheckExtend.cpp:358-407`），同文件后续 extend
     * 不应因此停滞。因此 try/catch 落在循环体内。
     *
     * 重试发生在索引版本变化时（文件集变化，或本批存在降级文件且 PSI 修改计数变化），
     * 而不是每次查询——官方在单轮里不会第二次尝试失败的 extend
     * （`external/cangjie_compiler/src/Sema/PreCheck.cpp:2270-2302` 是单次顺序流水线，
     * 整树没有多轮/重试框架）；PSI 变化只是 IDE 特有的「下一轮」信号。
     *
     * 取消与索引未就绪必须原样上抛——把它们当成「这个文件坏了」会让整个索引永久降级。
     */
    private fun resolveTopLevelExtendsToTypes(file: CfirFile) {
        var degraded = false
        for (declaration in file.declarations) {
            if (declaration !is CfirExtend) continue
            try {
                declaration.lazyResolveToPhase(CfirResolvePhase.TYPES)
            } catch (cancelled: ProcessCanceledException) {
                throw cancelled
            } catch (indexNotReady: IndexNotReadyException) {
                throw indexNotReady
            } catch (failure: CaSymbolUnavailableException) {
                // 只吞可分类的「符号不可恢复」；NPE/ISE 这类真缺陷必须暴露（设计决策 6）。
                LOG.warn("extend 头部解析失败，该条 extend 本轮缺席目标索引：$declaration", failure)
                degraded = true
            }
        }

        if (degraded) degradedFiles.add(file) else degradedFiles.remove(file)
    }
}

/**
 * extend 索引重建的日志对象。
 */
private val LOG = Logger.getInstance("org.cangnova.cangjie.analysis.low.level.api.cfir.providers.LLCfirSessionExtendProvider")
