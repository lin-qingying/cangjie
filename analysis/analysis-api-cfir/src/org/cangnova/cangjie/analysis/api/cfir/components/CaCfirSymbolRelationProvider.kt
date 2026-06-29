package org.cangnova.cangjie.analysis.api.cfir.components

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirSymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirTypeParameterSymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirValueParameterSymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.publicSymbolCacheKeyOrNull
import org.cangnova.cangjie.analysis.api.components.CaSymbolRelationProvider
import org.cangnova.cangjie.analysis.api.impl.base.components.CaBaseSessionComponent
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.projectStructure.CaDanglingFileModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaDanglingFileResolutionMode
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaAnonymousFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaFileSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaLocalVariableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPackageSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPropertyAccessorSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolOrigin
import org.cangnova.cangjie.analysis.api.symbols.isTopLevel
import org.cangnova.cangjie.analysis.api.symbols.symbol
import org.cangnova.cangjie.analysis.low.level.api.cfir.projectStructure.llCfirModuleData
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.originalDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.resolve.getContainingClassSymbol
import org.cangnova.cangjie.cfir.resolve.CfirTypeResolutionConfiguration
import org.cangnova.cangjie.cfir.resolve.SupertypeSupplier
import org.cangnova.cangjie.cfir.scopes.CfirTypeScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassSubstitutionScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassUseSiteMemberScope
import org.cangnova.cangjie.cfir.scopes.unsubstitutedScope
import org.cangnova.cangjie.cfir.session.ProcessorAction
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.session.directSupertypeProviderOrNull
import org.cangnova.cangjie.cfir.session.extendProvider
import org.cangnova.cangjie.cfir.session.extendIndexStore
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.session.typeResolver
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.cfir.types.classIdOrPrimitiveClassId
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.unwrapSubstitutionOverrides
import org.cangnova.cangjie.psi.CjCodeFragment
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjModifierList
import org.cangnova.cangjie.psi.CjParameter
import org.cangnova.cangjie.psi.CjPrimaryConstructor
import org.cangnova.cangjie.psi.CjBindingPattern
import org.cangnova.cangjie.psi.CjTypeStatement
import org.cangnova.cangjie.psi.psiUtil.parentOfType
import org.cangnova.cangjie.psi.psiUtil.parents
import org.cangnova.cangjie.source.CjFakeSourceElementKind
import org.cangnova.cangjie.source.CjRealSourceElementKind
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.source.psi
import org.cangnova.cangjie.utils.exceptions.errorWithAttachment
import org.cangnova.cangjie.utils.exceptions.withPsiEntry

/**
 * 符号关系组件。
 */
internal class CaCfirSymbolRelationProvider(
    /**
     * 延迟取得当前 CFIR Analysis session，保证组件访问始终落在有效生命周期内。
     */
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaSymbolRelationProvider {
    /**
     * 返回符号的语义包含声明。
     *
     * 该属性优先处理 accessor、参数、类型参数等依赖所属声明的符号，
     * 再回退到 CFIR owner 或 PSI 父级，确保源码符号、悬空文件符号和合成符号拥有一致的公开父子关系。
     */
    override val CaSymbol.containingDeclaration: CaDeclarationSymbol?
        get() = withValidityAssertion {
            if (!hasParentSymbol(this@containingDeclaration)) {
                return@withValidityAssertion null
            }

            getContainingDeclarationForDependentDeclaration(this@containingDeclaration)?.let { return@withValidityAssertion it }

            val cfirSymbol = (this@containingDeclaration as? CaCfirSymbol<*>)?.cfirSymbol ?: return@withValidityAssertion null
            val symbolModule = cfirSymbol.llCfirModuleData.caModule

            if (symbolModule is CaDanglingFileModule && symbolModule.resolutionMode == CaDanglingFileResolutionMode.IGNORE_SELF) {
                if (hasParentPsi(this@containingDeclaration)) {
                    return@withValidityAssertion getContainingDeclarationByPsi(this@containingDeclaration)
                }
            }

            when (this@containingDeclaration) {
                is CaLocalVariableSymbol,
                is CaAnonymousFunctionSymbol,
                    -> {
                    return@withValidityAssertion getContainingDeclarationByPsi(this@containingDeclaration)
                }

                is CaCallableSymbol -> {
                    (cfirSymbol as? CfirCallableSymbol<*>)?.let { callableSymbol ->
                        analysisSession.cfirSession.extendIndexStore.containingExtendOf(callableSymbol.unwrapSubstitutionOverrides())?.let { extend ->
                            return@withValidityAssertion analysisSession.cfirSymbolBuilder.buildExtendSymbol(extend.symbol)
                        }
                    }

                    cfirSymbol.getContainingClassSymbol()?.let { outerClass ->
                        return@withValidityAssertion analysisSession.cfirSymbolBuilder.buildSymbol(outerClass) as? CaDeclarationSymbol
                    }
                }

                is CaClassLikeSymbol -> {
                    cfirSymbol.getContainingClassSymbol()?.let { outerClass ->
                        return@withValidityAssertion analysisSession.cfirSymbolBuilder.buildSymbol(outerClass) as? CaDeclarationSymbol
                    }
                }
            }

            return@withValidityAssertion getContainingDeclarationByPsi(this@containingDeclaration)
        }

    /**
     * 解析必须依附其他声明存在的符号的直接宿主声明。
     */
    private fun getContainingDeclarationForDependentDeclaration(symbol: CaSymbol): CaDeclarationSymbol? {
        return when (symbol) {
            is CaPropertyAccessorSymbol -> symbol.owningProperty

            is CaCfirValueParameterSymbol ->
                symbol.ownerSymbol as? CaDeclarationSymbol
                    ?: analysisSession.cfirSymbolBuilder.buildSymbol(symbol.cfirSymbol.containingDeclarationSymbol) as? CaDeclarationSymbol

            is CaCfirTypeParameterSymbol ->
                analysisSession.cfirSymbolBuilder.buildSymbol(symbol.cfirSymbol.containingDeclarationSymbol) as? CaDeclarationSymbol

            else -> null
        }
    }

    /**
     * 判断符号是否理论上应该拥有公开的父声明。
     */
    private fun hasParentSymbol(symbol: CaSymbol): Boolean {
        return when (symbol) {
            is CaPackageSymbol,
            is CaFileSymbol,
                -> false

            !is CaDeclarationSymbol -> false
            else -> !symbol.isTopLevel
        }
    }

    /**
     * 通过源码 PSI 父级恢复符号的公开包含声明。
     */
    private fun getContainingDeclarationByPsi(symbol: CaSymbol): CaDeclarationSymbol? {
        val containingDeclaration = getContainingPsi(symbol) ?: return null
        return with(analysisSession) { containingDeclaration.symbol }
    }

    /**
     * 从 CFIR source 信息中取得用于推导包含关系的 PSI 声明。
     */
    private fun getContainingPsi(symbol: CaSymbol): CjDeclaration? {
        val source = (symbol as? CaCfirSymbol<*>)?.cfirSymbol?.cfir?.source
            ?: errorWithAttachment("PSI should present for declaration built by CangJie code") {
                withPsiEntry("symbolPsi", (symbol as? CaDeclarationSymbol)?.psi)
            }

        return getContainingPsi(symbol, source)
    }

    /**
     * 根据 source kind 将真实源码、假 source 和合成声明映射为包含声明 PSI。
     */
    private fun getContainingPsi(symbol: CaSymbol, source: CjSourceElement): CjDeclaration? {
        getContainingPsiForFakeSource(source)?.let { return it }

        val psi = source.psi
            ?: errorWithAttachment("PSI not found for source kind '${source.kind}'") {}

        if (source.kind != CjRealSourceElementKind) {
            errorWithAttachment("Cannot compute containing PSI for unknown source kind '${source.kind}' (${psi::class.simpleName})") {
                withPsiEntry("psi", psi)
            }
        }

        if (isSyntheticSymbolWithParentSource(symbol)) {
            return psi as CjDeclaration
        }

        if (isOrdinarySymbolWithSource(symbol)) {
            val result = psi.getContainingPsiDeclaration()
            if (result == null) {
                val containingFile = psi.containingFile
                if (containingFile is CjCodeFragment) {
                    return null
                }

                if (psi.parentOfType<CjModifierList>() != null) {
                    return null
                }

                errorWithAttachment("Containing declaration should present for nested declaration ${psi::class}") {
                    withPsiEntry("psi", psi)
                }
            }

            return result
        }

        errorWithAttachment("Unsupported declaration origin ${symbol.origin} ${psi::class}") {
            withPsiEntry("psi", psi)
        }
    }

    /**
     * 判断符号是否携带足够的 PSI 信息来恢复父级声明。
     */
    private fun hasParentPsi(symbol: CaSymbol): Boolean {
        val source = (symbol as? CaCfirSymbol<*>)?.cfirSymbol?.cfir?.source?.takeIf { it.psi is CjElement } ?: return false

        return getContainingPsiForFakeSource(source) != null ||
            isSyntheticSymbolWithParentSource(symbol) ||
            isOrdinarySymbolWithSource(symbol)
    }

    /**
     * 判断合成符号是否可以直接把 source PSI 视为包含声明。
     */
    private fun isSyntheticSymbolWithParentSource(symbol: CaSymbol): Boolean {
        return when (symbol.origin) {
            else -> false
        }
    }

    /**
     * 判断符号是否是普通源码声明，可按 PSI 父链寻找包含声明。
     */
    private fun isOrdinarySymbolWithSource(symbol: CaSymbol): Boolean {
        return symbol.origin == CaSymbolOrigin.SOURCE
    }

    /**
     * 为 CFIR 假 source kind 恢复它们在公开 API 中应呈现的源码容器。
     */
    private fun getContainingPsiForFakeSource(source: CjSourceElement): CjDeclaration? {
        return when (source.kind) {
            CjFakeSourceElementKind.ImplicitConstructor -> source.psi as CjDeclaration
            CjFakeSourceElementKind.PropertyFromParameter -> (source.psi as CjParameter).ownerFunction as? CjPrimaryConstructor
            CjFakeSourceElementKind.EnumGeneratedDeclaration -> source.psi as CjDeclaration
            CjFakeSourceElementKind.PatternBindingVariable -> {
                val bindingPattern = source.psi as? CjBindingPattern ?: return null
                val patternVariable = bindingPattern.variable ?: return null
                patternVariable.parents.filterIsInstance<CjDeclaration>().firstOrNull { it != patternVariable }
            }
            CjFakeSourceElementKind.DataClassGeneratedMembers -> when (val psi = source.psi) {
                is CjTypeStatement -> psi
                is CjParameter -> (psi.ownerFunction as? CjPrimaryConstructor)?.getContainingTypeStatement()
                is CjPrimaryConstructor -> psi.getContainingTypeStatement()
                else -> null
            }
            else -> null
        }
    }

    /**
     * 沿 PSI 父链查找最近的仓颉声明，并优先返回原始声明。
     */
    private fun PsiElement.getContainingPsiDeclaration(): CjDeclaration? {
        for (parent in parents) {
            if (parent is CjDeclaration) {
                return parent.originalDeclaration ?: parent
            }
        }

        return null
    }

    /**
     * 比较两个公开符号是否代表同一个底层声明实体。
     */
    override fun CaSymbol.isEquivalentTo(other: CaSymbol): Boolean = withValidityAssertion {
        this@isEquivalentTo === other ||
            (this@isEquivalentTo.publicSymbolCacheKeyOrNull() != null &&
                this@isEquivalentTo.publicSymbolCacheKeyOrNull() == other.publicSymbolCacheKeyOrNull())
    }

    /**
     * 计算 callable 直接覆盖的父级 callable 符号序列。
     */
    override val CaCallableSymbol.directlyOverriddenSymbols: Sequence<CaCallableSymbol>
        get() = withValidityAssertion {
            if (!mayHaveOverriddenSymbols()) {
                return@withValidityAssertion emptySequence()
            }

            val backingSymbol = (this@directlyOverriddenSymbols as? CaCfirSymbol<*>)
                ?.cfirSymbol as? org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol<*>
                ?: return@withValidityAssertion emptySequence()

            analysisSession.collectDirectlyOverriddenCallableSymbols(backingSymbol)
                .map { symbol -> analysisSession.cfirSymbolBuilder.buildSymbol(symbol) }
                .filterIsInstance<CaCallableSymbol>()
                .distinctStableCallables()
                .asSequence()
        }

    /**
     * 返回交叉类型或多继承合并产生的覆盖符号。
     */
    override val CaCallableSymbol.intersectionOverriddenSymbols: List<CaCallableSymbol>
        get() = withValidityAssertion { emptyList() }

    /**
     * 广度遍历 callable 的完整覆盖链，并按稳定身份去重。
     */
    override val CaCallableSymbol.allOverriddenSymbols: Sequence<CaCallableSymbol>
        get() = withValidityAssertion {
            if (!mayHaveOverriddenSymbols()) {
                return@withValidityAssertion emptySequence()
            }

            val visited = linkedSetOf<String>()
            val result = mutableListOf<CaCallableSymbol>()
            val queue = ArrayDeque(this@allOverriddenSymbols.directlyOverriddenSymbols.toList())

            while (queue.isNotEmpty()) {
                val current = queue.removeFirst()
                val key = current.stableCallableIdentity() ?: continue
                if (!visited.add(key)) continue
                result += current
                queue.addAll(current.directlyOverriddenSymbols)
            }

            result.asSequence()
        }

    /**
     * 判断当前类是否直接或间接继承指定父类。
     */
    override fun CaClassSymbol.isSubClassOf(superClass: CaClassSymbol): Boolean = withValidityAssertion {
        isSubclassOf(superClass, allowIndirect = true)
    }

    /**
     * 判断当前类是否把指定类列为直接父类。
     */
    override fun CaClassSymbol.isDirectSubClassOf(superClass: CaClassSymbol): Boolean = withValidityAssertion {
        isSubclassOf(superClass, allowIndirect = false)
    }

    /**
     * 返回 actual 声明对应的 expect 声明集合。
     */
    override fun CaDeclarationSymbol.getExpectsForActual(): List<CaDeclarationSymbol> = withValidityAssertion {
        emptyList()
    }

    /**
     * 快速过滤语义上可能参与 override 关系的 callable 类型。
     */
    private fun CaCallableSymbol.mayHaveOverriddenSymbols(): Boolean {
        return this is org.cangnova.cangjie.analysis.api.symbols.CaNamedFunctionSymbol ||
            this is org.cangnova.cangjie.analysis.api.symbols.CaPropertySymbol
    }

    /**
     * 使用公开缓存键或 callableId 对覆盖结果做稳定去重。
     */
    private fun List<CaCallableSymbol>.distinctStableCallables(): List<CaCallableSymbol> {
        return distinctBy { callable ->
            callable.stableCallableIdentity() ?: "${callable::class.qualifiedName}@${System.identityHashCode(callable)}"
        }
    }

    /**
     * 生成 callable 在覆盖关系中可复用的稳定身份字符串。
     */
    private fun CaCallableSymbol.stableCallableIdentity(): String? {
        return publicSymbolCacheKeyOrNull()?.toString() ?: callableId?.toString()
    }

    /**
     * 在直接或传递模式下执行类继承关系遍历。
     */
    private fun CaClassSymbol.isSubclassOf(
        superClass: CaClassSymbol,
        allowIndirect: Boolean,
    ): Boolean {
        if (isSameClassAs(superClass)) {
            return false
        }

        val directSuperSymbols = superTypes.mapNotNull { type ->
            with(analysisSession) { type.classLikeSymbol as? CaClassSymbol }
        }
        if (directSuperSymbols.any { symbol -> symbol.isSameClassAs(superClass) }) {
            return true
        }
        if (!allowIndirect) {
            return false
        }

        val visited = linkedSetOf<String>()
        val queue = ArrayDeque(directSuperSymbols)
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (!visited.add(current.classRelationIdentity())) continue
            if (current.isSameClassAs(superClass)) {
                return true
            }
            queue.addAll(current.superTypes.mapNotNull { type ->
                with(analysisSession) { type.classLikeSymbol as? CaClassSymbol }
            })
        }
        return false
    }

    /**
     * subclass relation 既要覆盖带 `ClassId` 的稳定声明，也要覆盖当前 session 中仅由源码承载的局部类。
     *
     * 因此这里统一按“ClassId 优先，其次源码 PSI 身份”来比较两个 class symbol，
     * 避免把 local class 关系硬退化成一律 `false`。
     */
    private fun CaClassSymbol.isSameClassAs(other: CaClassSymbol): Boolean {
        val thisClassId = classId
        val otherClassId = other.classId
        if (thisClassId != null && otherClassId != null) {
            return thisClassId == otherClassId
        }

        val thisPsi = psi
        val otherPsi = other.psi
        return thisPsi != null && otherPsi != null && thisPsi == otherPsi
    }

    /**
     * 生成继承遍历去重使用的类身份。
     */
    private fun CaClassSymbol.classRelationIdentity(): String {
        classId?.let { return "classId:${it.asString()}" }

        val declarationPsi = psi
        if (declarationPsi != null) {
            val filePath = declarationPsi.containingFile?.virtualFile?.path ?: declarationPsi.containingFile?.name.orEmpty()
            return "psi:$filePath:${declarationPsi.textOffset}"
        }

        return "${this::class.qualifiedName}@${System.identityHashCode(this)}"
    }

    /**
     * 通过 CFIR owner scope 收集底层 callable 的直接 overridden 符号。
     */
    private fun CaCfirSession.collectDirectlyOverriddenCallableSymbols(
        backingSymbol: org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol<*>,
    ): List<org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol<*>> {
        val memberScope = overrideOwnerScope(backingSymbol)
            ?: return emptyList()
        memberScope.processCallableByName(backingSymbol.cfir)

        return when (backingSymbol) {
            is CfirNamedFunctionSymbol -> buildList {
                memberScope.processDirectOverriddenFunctionsWithBaseScope(backingSymbol) { overridden, _ ->
                    add(overridden)
                    ProcessorAction.NEXT
                }
            }

            is CfirPropertySymbol -> buildList {
                memberScope.processDirectOverriddenPropertiesWithBaseScope(backingSymbol) { overridden, _ ->
                    add(overridden)
                    ProcessorAction.NEXT
                }
            }

            else -> emptyList()
        }
    }

    /**
     * 取得用于查询 override 关系的 owner scope。
     */
    private fun CaCfirSession.overrideOwnerScope(
        backingSymbol: org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol<*>,
    ): CfirTypeScope? {
        val originalSymbol = backingSymbol.unwrapSubstitutionOverrides()
        cfirSession.extendProvider.getContainingExtend(originalSymbol)?.let { extend ->
            return targetUseSiteMemberScope(extend)
        }

        val ownerClassId = originalSymbol.callableId.classId
            ?: cfirSession.cfirProvider.getContainingClass(originalSymbol)?.classId
            ?: return null
        val ownerClass = cfirSession.symbolProvider.getClassLikeSymbolByClassId(ownerClassId)
            ?: return null
        return ownerClass.cfir.unsubstitutedScope(
            cfirSession,
            getScopeSessionFor(cfirSession),
            withForcedTypeCalculator = false,
            memberRequiredPhase = CfirResolvePhase.STATUS,
        )
    }

    /**
     * 仓颉 `extend` 成员的 override owner 是被扩展类型的 use-site scope。
     *
     * Kotlin 没有语言级 `extend` 容器，因此这里只复用 Kotlin 的 scope 入口形状：
     * 先确定真实 owner type，再在 owner 的 use-site member scope 上查询 direct overridden。
     */
    private fun CaCfirSession.targetUseSiteMemberScope(
        extend: CfirExtend,
    ): CfirTypeScope? {
        val extendedType = extend.extendedTypeRef.coneTypeOrNull
            ?: cfirSession.typeResolver.resolveType(
                typeRef = extend.extendedTypeRef,
                configuration = CfirTypeResolutionConfiguration.EMPTY
                    .withTopContainer(extend)
                    .withAdditionalTypeParameters(extend.typeParameters),
                areBareTypesAllowed = false,
                isOperandOfIsOperator = false,
                resolveDeprecations = false,
                supertypeSupplier = SupertypeSupplier.Default,
            ).type
        val targetClassId = extendedType.classIdOrPrimitiveClassId ?: return null
        val targetSymbol = cfirSession.symbolProvider.getClassLikeSymbolByClassId(targetClassId) ?: return null
        val rawScope = CfirClassUseSiteMemberScope(
            session = cfirSession,
            classSymbol = targetSymbol,
            symbolProvider = cfirSession.symbolProvider,
            extendProvider = cfirSession.extendProvider,
            directSupertypeProvider = cfirSession.directSupertypeProviderOrNull,
            ownerType = extendedType,
        )
        return CfirClassSubstitutionScope(cfirSession, rawScope, extendedType)
    }

    /**
     * 预热 scope 中与声明同名的 callable 查询路径。
     */
    private fun CfirTypeScope.processCallableByName(
        declaration: org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration,
    ) {
        when (declaration) {
            is CfirNamedFunction -> processFunctionsByName(declaration.name) {}
            is CfirProperty -> processPropertiesByName(declaration.name) {}
            else -> Unit
        }
    }
}
