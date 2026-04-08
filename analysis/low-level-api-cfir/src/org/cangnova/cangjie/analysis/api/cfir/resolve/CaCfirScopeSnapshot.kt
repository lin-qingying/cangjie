package org.cangnova.cangjie.analysis.api.cfir.resolve

import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.ScopeSessionKey
import org.cangnova.cangjie.cfir.scopes.CfirContainingNamesAwareScope
import org.cangnova.cangjie.cfir.scopes.CfirScope
import org.cangnova.cangjie.cfir.scopes.CfirTypeScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassDeclaredMemberScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassUseSiteMemberScope
import org.cangnova.cangjie.cfir.session.cangjieScopeProvider
import org.cangnova.cangjie.cfir.session.directSupertypeProviderOrNull
import org.cangnova.cangjie.cfir.session.extendProviderOrNull
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFileSymbol
import org.cangnova.cangjie.cfir.symbols.CfirSymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.classIdOrPrimitiveClassId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjNamedDeclaration
import org.cangnova.cangjie.source.psi
import java.util.concurrent.ConcurrentHashMap

/**
 * low-level 作用域快照。
 *
 * 这里对齐 Kotlin `low-level-api-fir` 的职责边界：
 * 把“当前作用域已经索引出的稳定名字集合”和“按名字查询权威底层符号”
 * 统一收敛在 low-level 层。Analysis API 上层只负责把这里的 CFIR 符号
 * 映射成公开 `CaSymbol`，不再直接接触具体 scope 实现。
 */
interface CaCfirScopeSnapshot {
    val availableNames: Set<Name>

    fun getSymbols(name: Name): List<CfirSymbol<*>>

    fun getCallableSymbols(name: Name): List<CfirCallableSymbol<*>>

    fun getClassifierSymbols(name: Name): List<CfirClassLikeSymbol<*>>
}

/**
 * use-site 模块对应的 low-level scope snapshot provider。
 *
 * 当前仓颉 Analysis API 只建模语言真实存在的作用域边界：
 * `package / file / declared-member / use-site-member / type`。
 * 这里不会额外构造层级化的类型声明视图，只维护上述稳定作用域模型。
 */
internal class CaCfirScopeSnapshotProvider(
    private val moduleResolveComponents: CaCfirModuleResolveComponents,
) {
    private val scopeSession = ScopeSession()

    private val fileScopeCache = ConcurrentHashMap<CjFile, CaCfirScopeSnapshot>()
    private val packageScopeCache = ConcurrentHashMap<FqName, CaCfirScopeSnapshot>()
    private val declaredMemberScopeCache = ConcurrentHashMap<ClassId, CaCfirScopeSnapshot>()
    private val memberScopeCache = ConcurrentHashMap<ClassId, CaCfirScopeSnapshot>()
    private val memberTypeScopeCache = ConcurrentHashMap<ClassId, CfirTypeScope>()

    fun getFileScope(file: CjFile): CaCfirScopeSnapshot {
        return fileScopeCache.computeIfAbsent(file, ::buildFileScope)
    }

    fun getPackageScope(packageFqName: FqName): CaCfirScopeSnapshot? {
        packageScopeCache[packageFqName]?.let { return it }

        val snapshot = buildPackageScope(packageFqName) ?: return null
        val existing = packageScopeCache.putIfAbsent(packageFqName, snapshot)
        return existing ?: snapshot
    }

    fun getDeclaredMemberScope(classId: ClassId): CaCfirScopeSnapshot? {
        declaredMemberScopeCache[classId]?.let { return it }

        val classSymbol = visibleSymbolProvider.getClassLikeSymbolByClassId(classId) ?: return null
        val snapshot = buildScopeSnapshot(
            scope = scopeSession.getOrBuild(classId, DeclaredMemberScopeKey) {
                CfirClassDeclaredMemberScope(classSymbol)
            },
        )
        val existing = declaredMemberScopeCache.putIfAbsent(classId, snapshot)
        return existing ?: snapshot
    }

    fun getMemberScope(classId: ClassId): CaCfirScopeSnapshot? {
        memberScopeCache[classId]?.let { return it }

        val scope = getMemberTypeScope(classId) ?: return null
        val snapshot = buildScopeSnapshot(scope = scope)
        val existing = memberScopeCache.putIfAbsent(classId, snapshot)
        return existing ?: snapshot
    }

    /**
     * 返回 use-site member scope 的底层 `CfirTypeScope`。
     *
     * 该入口供 low-level 继承/override 查询复用，
     * 确保 relation 查询与公开 `memberScope` 使用的是同一份底层 scope。
     */
    fun getMemberTypeScope(classId: ClassId): CfirTypeScope? {
        memberTypeScopeCache[classId]?.let { return it }

        val classSymbol = visibleSymbolProvider.getClassLikeSymbolByClassId(classId) ?: return null
        val scope = scopeSession.getOrBuild(classId, UseSiteMemberScopeKey) {
            CfirClassUseSiteMemberScope(
                classSymbol = classSymbol,
                symbolProvider = symbolProvider,
                extendProvider = session.extendProviderOrNull,
                directSupertypeProvider = session.directSupertypeProviderOrNull,
            )
        }
        val existing = memberTypeScopeCache.putIfAbsent(classId, scope)
        return existing ?: scope
    }

    /**
     * 为类型构建对应的 low-level type scope。
     *
     * 当前仓颉里只有能稳定映射到 class-like 的类型才拥有公开成员作用域。
     */
    fun getTypeScope(type: ConeCangJieType): CaCfirScopeSnapshot? {
        val classId = type.classIdOrPrimitiveClassId ?: return null
        return getMemberScope(classId)
    }

    private val session
        get() = moduleResolveComponents.session

    private val cfirFiles
        get() = moduleResolveComponents.cfirFiles

    private val visibleSymbolProvider
        get() = moduleResolveComponents.visibleSymbolProvider

    private val symbolProvider
        get() = session.symbolProvider

    private fun buildFileScope(file: CjFile): CaCfirScopeSnapshot {
        val packageScope = getPackageScope(file.packageFqName)
        val declaredNames = collectDeclaredNames(file)

        return when (packageScope) {
            null -> CaCfirScopeSnapshotImpl(
                availableNames = declaredNames,
                symbolLookup = { emptyList() },
            )

            else -> packageScope.withAdditionalNames(declaredNames)
        }
    }

    private fun buildPackageScope(packageFqName: FqName): CaCfirScopeSnapshot? {
        val packageExists = visibleSymbolProvider.hasPackage(packageFqName)
        val packageScope = session.cangjieScopeProvider.getPackageMemberScope(
            packageFqName = packageFqName,
            symbolProvider = symbolProvider,
            useSiteSession = session,
            scopeSession = scopeSession,
        )
        val declaredNames = collectDeclaredNames(packageFqName)
        val availableNames = linkedSetOf<Name>().apply {
            addAll(declaredNames)
            addAll(packageScope.getClassifierNames())
            addAll(packageScope.getCallableNames())
        }

        if (!packageExists && availableNames.isEmpty()) {
            return null
        }

        return buildScopeSnapshot(packageScope, additionalNames = availableNames)
    }

    /**
     * 统一把底层 `CfirContainingNamesAwareScope` 适配成 low-level scope snapshot。
     *
     * 这里保留按名字查询底层 scope 的能力，不把 scope 拍平成一次性符号列表。
     * 因此 `availableNames` 与 `getSymbols(name)` 共享同一套缓存和去重语义。
     */
    private fun buildScopeSnapshot(
        scope: CfirContainingNamesAwareScope,
        additionalNames: Set<Name> = emptySet(),
        additionalScopes: List<CfirContainingNamesAwareScope> = emptyList(),
    ): CaCfirScopeSnapshot {
        val availableNames = linkedSetOf<Name>().apply {
            addAll(additionalNames)
            addAll(scope.getClassifierNames())
            addAll(scope.getCallableNames())
            additionalScopes.forEach { extraScope ->
                addAll(extraScope.getClassifierNames())
                addAll(extraScope.getCallableNames())
            }
        }

        val scopes = listOf(scope) + additionalScopes
        return CaCfirScopeSnapshotImpl(
            availableNames = availableNames,
            symbolLookup = { name -> resolveScopeSymbols(scopes, name) },
        )
    }

    private fun resolveScopeSymbols(
        scopes: List<CfirScope>,
        name: Name,
    ): List<CfirSymbol<*>> {
        val classifiers = buildList {
            scopes.forEach { scope ->
                scope.processClassifiersByName(name) { symbol ->
                    add(symbol)
                }
            }
        }
        val callables = buildList {
            scopes.forEach { scope ->
                scope.processCallablesByName(name) { symbol ->
                    add(symbol)
                }
            }
        }
        return (classifiers + callables).distinctBy { symbol -> symbol.scopeKey() }
    }

    private fun collectDeclaredNames(packageFqName: FqName): Set<Name> {
        return cfirFiles.asSequence()
            .mapNotNull { cfirFile -> cfirFile.source?.psi as? CjFile }
            .filter { file -> file.packageFqName == packageFqName }
            .flatMap { file -> collectDeclaredNames(file).asSequence() }
            .toCollection(linkedSetOf())
    }

    private fun collectDeclaredNames(file: CjFile): Set<Name> {
        return file.declarations.asSequence()
            .filterIsInstance<CjNamedDeclaration>()
            .mapNotNull { declaration -> declaration.name }
            .map(Name::identifier)
            .toCollection(linkedSetOf())
    }

    private fun CaCfirScopeSnapshot.withAdditionalNames(additionalNames: Set<Name>): CaCfirScopeSnapshot {
        if (additionalNames.isEmpty()) {
            return this
        }

        return CaCfirScopeSnapshotImpl(
            availableNames = linkedSetOf<Name>().apply {
                addAll(additionalNames)
                addAll(this@withAdditionalNames.availableNames)
            },
            symbolLookup = { name -> this@withAdditionalNames.getSymbols(name) },
        )
    }

    private object DeclaredMemberScopeKey : ScopeSessionKey<ClassId, CfirClassDeclaredMemberScope>()

    private object UseSiteMemberScopeKey : ScopeSessionKey<ClassId, CfirClassUseSiteMemberScope>()
}

private class CaCfirScopeSnapshotImpl(
    override val availableNames: Set<Name>,
    private val symbolLookup: (Name) -> List<CfirSymbol<*>>,
) : CaCfirScopeSnapshot {
    private val cachedSymbolsByName = linkedMapOf<Name, List<CfirSymbol<*>>>()

    override fun getSymbols(name: Name): List<CfirSymbol<*>> {
        return cachedSymbolsByName.getOrPut(name) {
            symbolLookup(name)
        }
    }

    override fun getCallableSymbols(name: Name): List<CfirCallableSymbol<*>> {
        return getSymbols(name).filterIsInstance<CfirCallableSymbol<*>>()
    }

    override fun getClassifierSymbols(name: Name): List<CfirClassLikeSymbol<*>> {
        return getSymbols(name).filterIsInstance<CfirClassLikeSymbol<*>>()
    }
}

/**
 * 为 low-level scope snapshot 提供稳定的去重键。
 *
 * low-level 层不能依赖包装后的 Analysis API 符号相等性，
 * 因此这里直接按底层语义标识去重。
 */
private fun CfirSymbol<*>.scopeKey(): String = when (this) {
    is CfirClassLikeSymbol<*> -> "class:${classId.asString()}"
    is CfirCallableSymbol<*> -> "callable:${callableId?.toString() ?: name.asString()}"
    is CfirFileSymbol -> "file:${cfir.source?.psi?.containingFile?.name ?: "<unknown>"}"
    else -> "${this::class.qualifiedName}:$debugName"
}
