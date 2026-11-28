@file:OptIn(org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProviderInternals::class,
    org.cangnova.cangjie.cfir.CfirImplementationDetail::class)

package org.cangnova.cangjie.cfir.serialization.cjo

import org.cangnova.cangjie.cfir.patterns.bindingVariables
import org.cangnova.cangjie.cfir.CfirElementWithResolveState
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.utils.explicitUpperBounds
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall
import org.cangnova.cangjie.cfir.expressions.CfirErrorExpression
import org.cangnova.cangjie.cfir.expressions.CfirLazyExpression
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.resolve.providers.*
import org.cangnova.cangjie.cfir.serialization.provider.CfirDeserializedSymbolProvider
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.source.*
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/** 只观察正式 provider 的调用；不实现 lookup、header completion、类型展开或 SOURCE 解码。 */
internal class R38ExportPreparationGuard(
    private val session: CfirSession,
    private val provider: CfirDeserializedSymbolProvider,
) {
    private var reading = false
    val headers = mutableListOf<CfirDeclaration>()
    private val headerOwners = java.util.Collections.newSetFromMap(java.util.IdentityHashMap<CfirImportedDeclarationHeaderSource, Boolean>())

    fun observeOwners(declarations: Collection<CfirDeclaration>) {
        declarations.mapNotNull { it.contentSource as? CfirImportedDeclarationHeaderSource }.forEach(headerOwners::add)
    }

    /** 已完成产品是准入证据；Owned header 使用实际字段，Published 必须是 Complete，不能靠请求次数证明。 */
    fun assertHeaderReady(declaration: CfirDeclaration) {
        observeOwners(listOf(declaration))
        kotlin.test.assertTrue(declaration.symbol.isBound, "Header 必须使用已绑定的 canonical symbol")
        kotlin.test.assertSame(declaration, declaration.symbol.cfir)
        when (val source = declaration.contentSource) {
            org.cangnova.cangjie.cfir.CfirContentSource.Owned -> {
                declaration.requireOwnedGraphAnalysisReady()
                assertOwnedHeaderFields(declaration)
            }
            is org.cangnova.cangjie.cfir.CfirContentSource.Published -> kotlin.test.assertIs<org.cangnova.cangjie.cfir.CfirPublishedAnnotations.Complete>(
                source.snapshot().annotations(declaration))
        }
    }

    /** 只核验实际存在的 header 槽；空 annotations/bounds 和没有 default 都是合法完成内容。 */
    private fun assertOwnedHeaderFields(declaration: CfirDeclaration) {
        fun type(ref: CfirTypeRef) {
            val resolved = kotlin.test.assertIs<CfirResolvedTypeRef>(ref)
            kotlin.test.assertFalse(resolved.coneType is ConeErrorType, "Header 类型不能是错误产品")
        }
        when (declaration) {
            is CfirCallableDeclaration -> type(declaration.returnTypeRef)
            is CfirTypeAlias -> type(declaration.expandedTypeRef)
            is CfirExtend -> type(declaration.extendedTypeRef)
            else -> Unit
        }
        when (declaration) {
            is CfirClassLikeDeclaration -> declaration.superTypeRefs.forEach(::type)
            is CfirExtend -> declaration.superTypeRefs.forEach(::type)
            else -> Unit
        }
        if (declaration is CfirTypeParameter) {
            kotlin.test.assertTrue(declaration.containingDeclarationSymbol.isBound)
            declaration.bounds.forEach(::type)
            declaration.explicitUpperBounds.forEach(::type)
        }
        if (declaration is CfirTypeParameterRefsOwner) {
            declaration.typeParameters.forEach { ref ->
                kotlin.test.assertTrue(ref.symbol.isBound)
                if (ref is CfirTypeParameter) {
                    kotlin.test.assertSame(ref, ref.symbol.cfir)
                    kotlin.test.assertSame(declaration.symbol, ref.containingDeclarationSymbol)
                }
                assertHeaderReady(ref.symbol.cfir)
            }
        }
        val parameters = when (declaration) {
            is CfirFunction -> declaration.valueParameters
            is CfirEnumConstructor -> declaration.valueParameters
            else -> emptyList()
        }
        parameters.forEach { parameter ->
            kotlin.test.assertSame(declaration.symbol, parameter.containingDeclarationSymbol)
            assertHeaderReady(parameter)
        }
        if (declaration is CfirValueParameter) {
            kotlin.test.assertTrue(declaration.containingDeclarationSymbol.isBound)
            declaration.defaultValue?.let { value ->
                kotlin.test.assertFalse(value is CfirLazyExpression)
                kotlin.test.assertFalse(value is CfirErrorExpression)
                kotlin.test.assertNotNull(value.coneTypeOrNull, "存在的 default 必须是实际完成表达式")
            }
        }
        if (declaration is CfirProperty) {
            listOfNotNull(declaration.getter, declaration.setter).forEach { accessor ->
                kotlin.test.assertSame(declaration.symbol, accessor.propertySymbol)
                assertHeaderReady(accessor)
            }
        }
        declaration.annotations.forEach { annotation ->
            // 内建语言注解没有 nominal 类型/constructor；只对真实 custom target 核验这些槽。
            if (annotation.annotationClassId != null) type(annotation.typeRef)
            if (annotation is CfirAnnotationCall) {
                kotlin.test.assertSame(declaration.symbol, annotation.containingDeclarationSymbol)
                if (annotation.forcedCustom) {
                    val target = kotlin.test.assertIs<CfirResolvedNamedReference>(annotation.calleeReference).resolvedSymbol
                    kotlin.test.assertTrue(target.isBound)
                    kotlin.test.assertNotNull(annotation.argumentView)
                }
            }
        }
    }
    private val resolver = object : CfirLazyDeclarationResolver() {
        override fun startResolvingPhase(phase: CfirResolvePhase) { assertLazyResolveAllowed() }
        override fun finishResolvingPhase(phase: CfirResolvePhase) { assertLazyResolveAllowed() }
        override fun lazyResolveToPhase(element: CfirElementWithResolveState, toPhase: CfirResolvePhase) {
            assertLazyResolveAllowed()
        }
        override fun lazyResolveToPhaseWithCallableMembers(clazz: CfirClassLikeDeclaration, toPhase: CfirResolvePhase) {
            assertLazyResolveAllowed()
        }
        override fun lazyResolveToPhaseWithCallableMembersInSupertypes(
            clazz: CfirClassLikeDeclaration, useSiteSession: CfirSession, toPhase: CfirResolvePhase,
        ) { assertLazyResolveAllowed() }
        override fun lazyResolveToPhaseRecursively(element: CfirElementWithResolveState, toPhase: CfirResolvePhase) {
            assertLazyResolveAllowed()
        }
    }

    private fun query() { check(!reading) { "Capture/validation must consume prepared identities" } }

    init {
        // 与正式库 session 相同的 provider 角色；只提供原符号元数据，不制造源码文件或声明。
        session.register(CfirProvider::class, CfirLibrarySessionProvider(provider))
        // 原 provider 仍拥有包 context；session 入口只增加观察和拒绝权限。
        session.register(CfirLazyDeclarationResolver::class, resolver)
        session.register(CfirSymbolProvider::class, object : CfirSymbolProvider(session) {
            override fun getImportNamespace(context: CfirImportNamespaceContext): CfirImportNamespace {
                query(); return provider.getImportNamespace(context)
            }
            override fun getCjmpLoadDiagnostics(packageFqName: FqName): List<org.cangnova.cangjie.cfir.session.CfirCjmpLoadDiagnostic> {
                query(); return provider.getCjmpLoadDiagnostics(packageFqName)
            }
            override fun getCjmpLoadDiagnostics(
                packageFqName: FqName, consumerSession: CfirSession,
            ): List<org.cangnova.cangjie.cfir.session.CfirCjmpLoadDiagnostic> {
                query(); return provider.getCjmpLoadDiagnostics(packageFqName, consumerSession)
            }
            override val symbolNamesProvider: CfirSymbolNamesProvider
                get() { query(); return provider.symbolNamesProvider }
            override fun getClassLikeSymbolByClassId(classId: ClassId): CfirClassLikeSymbol<*>? {
                query(); return provider.getClassLikeSymbolByClassId(classId)
            }
            override fun getClassLikeSymbolsByClassId(classId: ClassId): List<CfirClassLikeSymbol<*>> {
                query(); return provider.getClassLikeSymbolsByClassId(classId)
            }
            override fun getTopLevelCallableSymbolsTo(
                destination: MutableList<CfirCallableSymbol<*>>, packageFqName: FqName, name: Name,
            ) { query(); provider.getTopLevelCallableSymbolsTo(destination, packageFqName, name) }
            override fun getTopLevelFunctionSymbolsTo(
                destination: MutableList<CfirNamedFunctionSymbol>, packageFqName: FqName, name: Name,
            ) { query(); provider.getTopLevelFunctionSymbolsTo(destination, packageFqName, name) }
            override fun getTopLevelPropertySymbolsTo(
                destination: MutableList<CfirPropertySymbol>, packageFqName: FqName, name: Name,
            ) { query(); provider.getTopLevelPropertySymbolsTo(destination, packageFqName, name) }
            override fun hasPackage(fqName: FqName): Boolean { query(); return provider.hasPackage(fqName) }
        })
        session.register(CfirImportedSourceGraphProvider::class, object : CfirImportedSourceGraphProvider {
            override fun sourceHeaderContentsOf(declaration: CfirDeclaration): Boolean {
                query()
                headers += declaration
                return provider.sourceHeaderContentsOf(declaration)
            }
            override fun sourceHeaderOf(function: CfirCallableDeclaration): CfirSourceImportedFunctionHeader? {
                query(); return provider.sourceHeaderOf(function)
            }
            // 普通 borrowed 准备不授予任何正文、initializer、整包图或局部 owning 槽发现能力。
            override fun sourceGraphOf(function: CfirFunction): CfirSourceGraph? = error("Borrowed body capability")
            override fun packageSourceGraphOf(declaration: CfirDeclaration): CfirSourceGraph? = error("Borrowed package capability")
            override fun discoverSourceFunctionHeadersOf(function: CfirFunction): Boolean = error("Borrowed local header capability")
            override fun sourceBodyOf(function: org.cangnova.cangjie.cfir.CfirFunctionBodyOwner): org.cangnova.cangjie.cfir.expressions.CfirBlock? =
                error("Borrowed body capability")
            override fun sourceInitializerOf(variable: CfirVariable): org.cangnova.cangjie.cfir.expressions.CfirExpression? =
                error("Borrowed initializer capability")
        })
    }

    fun <T> readOnly(action: () -> T): T {
        check(!reading)
        reading = true
        try {
            val owners = headerOwners.toList()
            fun read(index: Int): T = if (index == owners.size) action() else owners[index].withReadOnlyContent { read(index + 1) }
            return resolver.forbidLazyResolveInside { read(0) }
        } finally {
            reading = false
        }
    }

    /** 正控：guard 必须拒绝真实入口，不能使用不检查 forbid 标志的 dummy resolver。 */
    fun proveGuards(declaration: CfirDeclaration) {
        observeOwners(listOf(declaration))
        kotlin.test.assertFailsWith<IllegalStateException> {
            readOnly { session.symbolProvider.getClassLikeSymbolByClassId(ClassId.topLevel(FqName("r38.Absent"))) }
        }
        kotlin.test.assertFailsWith<CfirLazyResolveForbiddenException> {
            readOnly { declaration.symbol.lazyResolveToPhase(CfirResolvePhase.BODY_RESOLVE) }
        }
        kotlin.test.assertFailsWith<IllegalStateException> {
            readOnly { checkNotNull(session.importedSourceGraphProvider).sourceHeaderContentsOf(declaration) }
        }
        // 新的原 context 能力必须实际受只读禁令保护，不能仅证明全局 provider wrapper 拒绝。
        val owner = declaration.contentSource as CfirImportedDeclarationHeaderSource
        kotlin.test.assertFailsWith<IllegalStateException> { readOnly { owner.completeHeader(declaration) } }
    }
}

/** 采用生产声明的物理成员列表；不按输出计划裁掉 borrowed 成员。 */
internal fun r38PhysicalMembers(declaration: CfirDeclaration): List<CfirDeclaration> = when (declaration) {
    is CfirClassLikeDeclaration -> declaration.declarations
    is CfirExtend -> declaration.declarations
    else -> error("Only nominal/extend declarations have member inventories")
}

/** 包作用域暴露 wire binding；它仍属于同一个 d 索引的实际模式声明根。 */
internal fun CfirDeserializedSymbolProvider.packageVariableBinding(packageName: String, name: String): CfirPatternBindingVariable {
    val symbol = getTopLevelCallableSymbols(FqName(packageName), Name.identifier(name)).single()
    val binding = kotlin.test.assertIs<CfirPatternBindingVariable>(symbol.cfir)
    val root = kotlin.test.assertIs<CfirPatternVariable>(binding.patternTreeOwner)
    kotlin.test.assertSame(symbol, binding.symbol)
    kotlin.test.assertSame(binding, root.pattern.bindingVariables().single())
    kotlin.test.assertEquals(name, binding.name.asString())
    kotlin.test.assertEquals(kotlin.test.assertNotNull(root.cjoDeclarationOrigin), binding.cjoDeclarationOrigin)
    return binding
}
