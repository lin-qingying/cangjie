@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.analysis.low.level.api.cfir.sessions

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.CjSourceKind
import org.cangnova.cangjie.analysis.api.projectStructure.CaSourceModule
import org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.LLCangJieSymbolProvider
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurface
import org.cangnova.cangjie.cfir.resolve.providers.macro.PreMacroRawBuildResult
import org.cangnova.cangjie.cfir.resolve.providers.macro.buildPreMacroRawFiles
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.session.ensureRawMacroSurfaceUsageStore
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.CjAnnotation
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjMacroExpression

/**
 * 为源码 low-level session 收集完整的 module 级 pre-macro CFIR 输入。
 *
 * 该入口通过宿主 symbol name 索引枚举包，再由声明索引定位各包文件；每个输入文件都独立构建，
 * 不复用普通 LL `CfirProvider` 的文件缓存。宏构造会原地 stable-splice 输入树，因此它必须独占这些树，
 * 直到成功快照被发布或丢弃。
 * 只有包名集合可完整枚举时才返回输入，防止将部分文件误当作完整模块快照交给分类和构造阶段。
 *
 * 读访问按文件切分：整模块构建若独占一个读动作，EDT 上的写动作（例如 SDK 变更后的
 * `PsiManager.dropPsiCaches`）会一直等待直到全部文件构建完成，表现为 UI 冻结。
 * 逐文件读访问让写请求最多等待单个文件，并在文件之间响应取消。
 */
fun LLCfirSession.buildPreMacroRawBuildResultForModule(): PreMacroRawBuildResult? {
    if (caModule !is CaSourceModule) return null

    val resolvableSession = this as? LLCfirResolvableModuleSession ?: return null

    /*
     * 必须在构建前取得（必要时创建）surface store，并在构建后从**同一实例**读取。
     *
     * store 是懒创建的：raw builder 记录 surface 时会调用 ensureRawMacroSurfaceUsageStore()。
     * 如果这里只读 rawMacroSurfaceUsageStoreOrNull，新 session 的第一次构造就会拿到 null，
     * 录制到新建 store 里的 surface 全部被丢弃，表现为"有宏调用但 allSurfaces 为空"。
     */
    val surfaceStore = ensureRawMacroSurfaceUsageStore()

    val sourceFiles = collectModuleSourceFiles() ?: emptyList()
    if (sourceFiles.isEmpty()) {
        return buildPreMacroRawFiles(session = this, rawCfirFiles = emptyList())
    }

    val fileBuilder = resolvableSession.moduleComponents.cfirFileBuilder
    val buildCounter = ensurePreMacroRawBuildCounter()
    val cfirFiles = ArrayList<CfirFile>(sourceFiles.size)
    val fileSurfaces = ArrayList<List<MacroSurface>>(sourceFiles.size)
    for (file in sourceFiles) {
        ProgressManager.checkCanceled()
        val built = ReadAction.compute<Pair<CfirFile, List<MacroSurface>>, RuntimeException> {
            val cfirFile = fileBuilder.buildRawCfirFileUncached(file)
            cfirFile to surfaceStore.surfaces(cfirFile).orEmpty()
        }
        cfirFiles += built.first
        fileSurfaces += built.second
        buildCounter.increment()
    }

    return buildPreMacroRawFiles(
        session = this,
        rawCfirFiles = cfirFiles,
        fileSurfaces = fileSurfaces,
        sourceFileIdentities = sourceFiles,
    )
}

/**
 * 廉价预扫描：模块内是否至少存在一个可能成为宏 surface 的语法节点。
 *
 * 宏调用在语法层只有两种形态：注解（含 `@!` 强制语法的 `CjAnnotation`）与表达式宏调用
 * （`CjMacroExpression`）。两者都不存在的模块不可能产生宏 surface，因此可以在整模块
 * raw CFIR 构建之前短路，避免为无宏工程付出"每 revision 一次全模块构建再丢弃"的成本。
 *
 * 这是**保守下界**：扫描到注解就返回 true，该注解是否真的是宏由后续 construction 分类决定。
 * 源码文件枚举失败时返回 true（不短路），保持与完整构建一致的失败可见性。
 */
fun LLCfirSession.hasMacroCallCandidateForModule(): Boolean {
    if (caModule !is CaSourceModule) return false
    val sourceFiles = collectModuleSourceFiles() ?: return true
    for (file in sourceFiles) {
        ProgressManager.checkCanceled()
        val hasCandidate = ReadAction.compute<Boolean, RuntimeException> {
            PsiTreeUtil.findChildOfType(file, CjAnnotation::class.java) != null ||
                PsiTreeUtil.findChildOfType(file, CjMacroExpression::class.java) != null
        }
        if (hasCandidate) return true
    }
    return false
}

/**
 * 枚举模块内全部参与宏构造的源码文件；包名索引不可用时返回 `null`。
 *
 * 保留 `null` 与"空集合"的区别：前者表示无法完整枚举（不能据此下结论），
 * 后者表示模块确实没有源码文件。
 */
private fun LLCfirSession.collectModuleSourceFiles(): List<CjFile>? = ReadAction.compute<List<CjFile>?, RuntimeException> {
    val sourceSymbolProvider = cfirProvider.symbolProvider as? LLCangJieSymbolProvider
        ?: return@compute null
    val packageNames = sourceSymbolProvider.symbolNamesProvider.getPackageNames()
        ?: return@compute null
    packageNames
        .asSequence()
        .map(::FqName)
        .flatMap { packageFqName ->
            sourceSymbolProvider.declarationProvider.findFilesForFacadeByPackage(packageFqName).asSequence()
        }
        .filter { file ->
            file.sourceKind == CjSourceKind.SOURCE && CjSourceKind.isSourceFileName(file.name)
        }
        .distinct()
        .toList()
}

/**
 * 当前 session 已执行的 pre-macro raw 文件构建次数。
 *
 * 仅用于诊断/测试：宏构造的"无候选短路"必须发生在整模块 raw 构建之前，该计数为 0
 * 是这一不变量的直接证据（普通 LL 文件缓存构建不经过本计数器）。
 */
class PreMacroRawBuildCounter : org.cangnova.cangjie.cfir.session.CfirSessionComponent {
    private val count = java.util.concurrent.atomic.AtomicLong()

    internal fun increment() {
        count.incrementAndGet()
    }

    /** 已构建的 pre-macro 输入文件数。 */
    val value: Long
        get() = count.get()
}

private val CfirSession.preMacroRawBuildCounterOrNull: PreMacroRawBuildCounter? by
    CfirSession.nullableSessionComponentAccessor()

private fun CfirSession.ensurePreMacroRawBuildCounter(): PreMacroRawBuildCounter = synchronized(this) {
    preMacroRawBuildCounterOrNull ?: PreMacroRawBuildCounter().also {
        register(PreMacroRawBuildCounter::class, it)
    }
}

/**
 * 返回当前 session 已执行的 pre-macro raw 构建文件数。
 *
 * 宏构造因"无宏调用候选"短路时返回 0；发生整模块构建时按实际构建文件数递增。
 */
fun CfirSession.preMacroRawBuildCount(): Long = preMacroRawBuildCounterOrNull?.value ?: 0L
