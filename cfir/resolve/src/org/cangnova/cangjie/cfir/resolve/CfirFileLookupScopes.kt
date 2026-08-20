package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.ScopeSessionKey
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.resolve.services.CfirDefaultImportPriority
import org.cangnova.cangjie.cfir.resolve.services.CfirResolvedImportBinding
import org.cangnova.cangjie.cfir.scopes.CfirScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirExplicitSimpleImportingScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirExplicitStarImportingScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirFileDeclaredTopLevelScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirPackageMemberScope
import org.cangnova.cangjie.cfir.scopes.scopeSessionKey
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.importBindingStore
import org.cangnova.cangjie.cfir.session.symbolProvider

/**
 * 一个文件的结构性名称查找 scope 布局。
 *
 * [typeResolutionScopes] 按高优先级到低优先级排列，供类型候选 collector 直接迭代；
 * [towerInsertionScopes] 按低优先级到高优先级排列，供 tower context 依次压入。
 * 两种顺序共享同一组 scope 实例和 import binding，不再由各 resolve 阶段重复回放。
 */
data class CfirFileLookupScopes(
    /** tower context 依次压入的低到高优先级 scope。 */
    val towerInsertionScopes: List<CfirScope>,
) {
    /**
     * 类型解析直接消费的高到低优先级 scope。
     *
     * 该顺序只由同一份 [towerInsertionScopes] 反转得到，禁止维护第二套 scope 布局。
     */
    val typeResolutionScopes: List<CfirScope> = towerInsertionScopes.asReversed()
}

/**
 * 为 [file] 创建统一的文件级结构 scope。
 *
 * 源码显式 import 必须已经由 IMPORTS phase 写入 session binding store；缺失 binding
 * 表示解析阶段契约被破坏，不能退回 provider 现场重放。语言默认 import 也先转换为同一
 * binding 模型，并在 session 内只解析一次。
 */
fun CfirSession.createFileLookupScopes(
    file: CfirFile,
    scopeSession: ScopeSession,
): CfirFileLookupScopes = scopeSession.getOrBuild(
    CfirFileLookupScopesKey(this, file),
    FILE_LOOKUP_SCOPES,
) {
    computeFileLookupScopes(file)
}

/** 实际构造文件级结构 scope；调用结果由 [ScopeSession] 按文件和 session 复用。 */
private fun CfirSession.computeFileLookupScopes(file: CfirFile): CfirFileLookupScopes {
    val explicitBindings = importBindingStore.requireBindings(file).imports
    val highPriorityDefaultBindings =
        importBindingStore.requireDefaultImportBindings(CfirDefaultImportPriority.HIGH)
    val lowPriorityDefaultBindings =
        importBindingStore.requireDefaultImportBindings(CfirDefaultImportPriority.LOW)

    val fileDeclaredScope = CfirFileDeclaredTopLevelScope(file)
    val packageScope = CfirPackageMemberScope(file.packageDirective.packageFqName, this)
    val explicitSimpleScope = explicitBindings.simpleImportScopeOrNull()
    val explicitStarScope = explicitBindings.starImportScopeOrNull(symbolProvider)
    val highDefaultSimpleScope = highPriorityDefaultBindings.simpleImportScopeOrNull()
    val highDefaultStarScope = highPriorityDefaultBindings.starImportScopeOrNull(symbolProvider)
    val lowDefaultSimpleScope = lowPriorityDefaultBindings.simpleImportScopeOrNull()
    val lowDefaultStarScope = lowPriorityDefaultBindings.starImportScopeOrNull(symbolProvider)

    val towerInsertionScopes = buildList {
        lowDefaultStarScope?.let(::add)
        highDefaultStarScope?.let(::add)
        lowDefaultSimpleScope?.let(::add)
        highDefaultSimpleScope?.let(::add)
        explicitStarScope?.let(::add)
        add(packageScope)
        add(fileDeclaredScope)
        explicitSimpleScope?.let(::add)
    }
    return CfirFileLookupScopes(towerInsertionScopes)
}

/** 为 simple binding 创建结构性 import scope；空集合不创建无意义 scope。 */
private fun List<CfirResolvedImportBinding>.simpleImportScopeOrNull(): CfirScope? {
    val bindings = filterNot { it.importDirective.isAllUnder }
    return bindings.takeIf { it.isNotEmpty() }?.let(::CfirExplicitSimpleImportingScope)
}

/** 为 star binding 创建结构性 import scope；成员符号仍由 provider 按包查询。 */
private fun List<CfirResolvedImportBinding>.starImportScopeOrNull(
    symbolProvider: CfirSymbolProvider,
): CfirScope? {
    val bindings = filter { it.importDirective.isAllUnder }
    return bindings.takeIf { it.isNotEmpty() }?.let { starBindings ->
        CfirExplicitStarImportingScope(starBindings, symbolProvider)
    }
}

/** 同一 [ScopeSession] 内文件结构 scope 的缓存身份。 */
private data class CfirFileLookupScopesKey(
    val session: CfirSession,
    val file: CfirFile,
)

/** 文件结构 scope 缓存键；结果不包含任何 access-sensitive 结论。 */
private val FILE_LOOKUP_SCOPES: ScopeSessionKey<CfirFileLookupScopesKey, CfirFileLookupScopes> =
    scopeSessionKey()
