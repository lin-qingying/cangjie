@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)



package org.cangnova.cangjie.analysis.low.level.api.cfir.util

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.platform.CaDeserializedDeclarationsOrigin
import org.cangnova.cangjie.analysis.api.platform.CaPlatformSettings
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CangJieProjectStructureProvider
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.services.LLCfirElementByPsiElementChooser
import org.cangnova.cangjie.analysis.low.level.api.cfir.element.builder.containingDeclaration
import org.cangnova.cangjie.analysis.low.level.api.cfir.projectStructure.llCfirModuleData
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSession
import org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.LLModuleWithDependenciesSymbolProvider
import org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.LLModuleSpecificSymbolProviderAccess
import org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.getClassLikeSymbolByClassIdWithoutDependencies
import org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.getClassLikeSymbolByPsiWithoutDependencies
import org.cangnova.cangjie.analysis.low.level.api.cfir.symbols.CfirResolution
import org.cangnova.cangjie.analysis.low.level.api.cfir.symbols.ResolutionDiagnostics
import org.cangnova.cangjie.analysis.low.level.api.cfir.symbols.UnresolvedReason
import org.cangnova.cangjie.analysis.low.level.api.cfir.symbols.flatMap
import org.cangnova.cangjie.analysis.low.level.api.cfir.symbols.map
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.psiUtil.containingTypeStatement

/**
 * 根据反编译或 stub PSI 声明查找对应 CFIR 声明的工具。
 *
 * 该搜索器只在 recovered 声明**存在**的模块上工作，但“存在”本身是部分函数：
 * 库类型可能尚未进入索引、反编译 PSI 可能失效、局部声明本来就没有全局身份。
 * 因此除 genuinely 违反框架契约的情况外，所有入口都返回 [CfirResolution]，
 * 由调用方决定降级方式，而不是把环境状态编码成异常。
 */
internal class CfirDeclarationForCompiledElementSearcher(private val session: LLCfirSession) {
    /**
     * 当前 low-level 会话所属工程。
     */
    private val project get() = session.project

    /**
     * 工程结构提供器，用于错误附件中描述 PSI 所属模块。
     */
    private val projectStructureProvider by lazy(LazyThreadSafetyMode.PUBLICATION) {
        CangJieProjectStructureProvider.getInstance(project)
    }

    /**
     * PSI 与 CFIR 元素匹配策略。
     */
    private val cfirElementByPsiElementChooser by lazy(LazyThreadSafetyMode.PUBLICATION) {
        LLCfirElementByPsiElementChooser.getInstance(project)
    }

    /**
     * 查找 [cjDeclaration] 对应的非局部 CFIR 声明。
     */
    fun findNonLocalDeclaration(cjDeclaration: CjDeclaration): CfirResolution<CfirDeclaration> = when (cjDeclaration) {
        is CjClassLikeDeclaration -> findNonLocalClassLikeDeclaration(cjDeclaration)
        is CjConstructor<*> -> findConstructorOfNonLocalClass(cjDeclaration)
        is CjNamedFunction -> findNonLocalFunction(cjDeclaration)
        is CjProperty -> findNonLocalProperty(cjDeclaration)
        is CjParameter -> findParameter(cjDeclaration)
        is CjPropertyAccessor -> findNonLocalPropertyAccessor(cjDeclaration)
        is CjTypeParameter -> findNonLocalTypeParameter(cjDeclaration)

        else -> unresolved(
            reason = UnresolvedReason.UNHANDLED_DECLARATION_KIND,
            message = "Unsupported compiled declaration of type ${cjDeclaration::class.simpleName}",
            psi = cjDeclaration,
        )
    }

    /**
     * 查找 [function] 对应的函数符号候选。
     */
    private fun findFunctionCandidates(function: CjNamedFunction): CfirResolution<List<CfirFunctionSymbol<*>>> =
        findCallableCandidates(function, function.isTopLevel).map { candidates ->
            candidates.filterIsInstance<CfirFunctionSymbol<*>>()
        }

    /**
     * 查找 [property] 对应的属性符号候选。
     */
    private fun findPropertyCandidates(property: CjProperty): CfirResolution<List<CfirPropertySymbol>> =
        findCallableCandidates(property, property.parent is CjFile).map { candidates ->
            candidates.filterIsInstance<CfirPropertySymbol>()
        }

    /**
     * 查找 compiled callable [declaration] 对应的 CFIR callable 符号候选。
     */
    private fun findCallableCandidates(
        declaration: CjCallableDeclaration,
        isTopLevel: Boolean,
    ): CfirResolution<List<CfirCallableSymbol<*>>> {
        val shortName = declaration.nameAsSafeName

        if (isTopLevel) {
            val packageFqName = declaration.containingCjFile.packageFqName

            return CfirResolution.Resolved(
                when (val symbolProvider = session.symbolProvider) {
                    is LLModuleWithDependenciesSymbolProvider ->
                        symbolProvider.getTopLevelDeserializedCallableSymbolsWithoutDependencies(
                            packageFqName,
                            shortName,
                            declaration,
                        )

                    else -> symbolProvider.getTopLevelCallableSymbols(packageFqName, shortName)
                },
            )
        }

        // 非顶层 callable 必然有外层类型：这里为空说明调用方传入了局部声明，
        // 而上游 `findNonLocalDeclaration` 只接受非局部声明。属于编程错误，不降级。
        val containingClass = requireNotNull(declaration.containingTypeStatement) {
            "Non-top-level callable must have a containing type statement: ${declaration::class.simpleName}"
        }

        return findNonLocalClassLikeDeclaration(containingClass).map { cfirClass ->
            when (declaration) {
                is CjProperty -> cfirClass.declarations
                    .filterIsInstance<CfirProperty>()
                    .filter { it.name == shortName }
                    .mapTo(mutableListOf<CfirCallableSymbol<*>>()) { it.symbol }

                is CjNamedFunction -> cfirClass.declarations
                    .filterIsInstance<CfirFunction>()
                    .filter { it.symbol.name == shortName }
                    .mapTo(mutableListOf<CfirCallableSymbol<*>>()) { it.symbol }

                else -> error("Unexpected callable ${declaration::class.simpleName}")
            }
        }
    }

    /**
     * 查找 compiled 类型参数 [param] 对应的 CFIR 类型参数声明。
     */
    private fun findNonLocalTypeParameter(param: CjTypeParameter): CfirResolution<CfirDeclaration> {
        val owner = requireNotNull(param.containingDeclaration) {
            "Type parameter must have a containing declaration"
        }

        return findNonLocalDeclaration(owner).flatMap { ownerDeclaration ->
            val typeParameterRefsOwner = ownerDeclaration as? CfirTypeParameterRefsOwner
                ?: return@flatMap unresolved(
                    reason = UnresolvedReason.MISSING_DECLARATION,
                    message = "Declaration ${ownerDeclaration::class.simpleName} does not own type parameters",
                    cfir = ownerDeclaration,
                )

            val typeParameter = typeParameterRefsOwner.typeParameters.firstOrNull { typeParameterRef ->
                cfirElementByPsiElementChooser.isMatchingTypeParameter(param, typeParameterRef.symbol.cfir)
            } ?: return@flatMap unresolved(
                reason = UnresolvedReason.MISSING_DECLARATION,
                message = "No cfir type parameter found",
                psi = param,
                cfir = ownerDeclaration,
            )

            CfirResolution.Resolved(typeParameter as CfirDeclaration)
        }
    }

    /**
     * 查找 compiled 值参数 [param] 对应的 CFIR 值参数声明。
     */
    private fun findParameter(param: CjParameter): CfirResolution<CfirDeclaration> {
        // 形参必然有 owner（函数或属性访问器）；为空说明 PSI 已破损，属于编程错误。
        val ownerDeclaration = requireNotNull(param.ownerFunction) {
            "Value parameter must have an owning function"
        }

        return findNonLocalDeclaration(ownerDeclaration).flatMap { cfirDeclaration ->
            val cfirFunction = cfirDeclaration as? CfirFunction
                ?: return@flatMap unresolved(
                    reason = UnresolvedReason.MISSING_DECLARATION,
                    message = "${CfirFunction::class.simpleName} expected but ${cfirDeclaration::class.simpleName} found",
                    psi = ownerDeclaration,
                    cfir = cfirDeclaration,
                )

            val valueParameter = cfirFunction.valueParameters.firstOrNull { cfirElementByPsiElementChooser.isMatchingValueParameter(param, it) }
                ?: return@flatMap unresolved(
                    reason = UnresolvedReason.MISSING_DECLARATION,
                    message = "No cfir value parameter found",
                    psi = param,
                    cfir = cfirFunction,
                )

            CfirResolution.Resolved(valueParameter)
        }
    }

    /**
     * 查找 compiled class-like [declaration] 对应的 CFIR class-like 声明。
     */
    private fun findNonLocalClassLikeDeclaration(declaration: CjClassLikeDeclaration): CfirResolution<CfirClassLikeDeclaration> {
        // 该搜索器只服务非局部声明，而非局部 class-like 必然有 ClassId。
        // 这里为空说明调用方传入了局部类，属于编程错误——不要把它混进「环境缺失」里。
        val classId = requireNotNull(declaration.getClassId()) {
            "Non-local class-like declaration must have a ClassId: ${declaration::class.simpleName}"
        }

        // With the `BINARIES` origin, deserialized CFIR declarations don't have associated PSI elements. Hence, we cannot use `*ByPsi*`
        // functions, as they check the candidate's associated PSI.
        val origin = CaPlatformSettings.getInstance(project).deserializedDeclarationsOrigin

        val classLikeSymbol: CfirClassLikeSymbol<*>? = when (origin) {
            CaDeserializedDeclarationsOrigin.BINARIES -> findBinaryClassLikeSymbol(classId)
            CaDeserializedDeclarationsOrigin.STUBS -> findStubClassLikeSymbol(classId, declaration)
        }

        if (classLikeSymbol != null) {
            return CfirResolution.Resolved(classLikeSymbol.cfir)
        }

        // 名称索引对 classId 的有效性是「近似」的（能通过过滤但不保证真存在），
        // 所以 miss 是正常状态而非不变量破坏。
        // 非物理 PSI 说明反编译/stub 视图已失效（重建后会重新 physical），
        // 这是需要被看见的生命周期问题，不能与「索引暂未就绪」混为一类。
        val reason = when {
            !declaration.isPhysical -> UnresolvedReason.CORRUPTED_COMPILED_PSI
            else -> UnresolvedReason.MISSING_DECLARATION
        }

        val contextualModule = session.llCfirModuleData.caModule
        val moduleForFile = projectStructureProvider.getModule(declaration, contextualModule)

        return unresolved(
            reason = reason,
            message = "We should be able to find a symbol for class-like declaration",
            psi = declaration,
            entries = buildList {
                add("classId" to classId.asString())
                add("caModule" to moduleForFile.moduleDescription)
                add("deserializedDeclarationsOrigin" to origin.toString())
                add("isDeclarationPhysical" to declaration.isPhysical.toString())
            },
        )
    }

    /**
     * 在二进制来源模式下按 [classId] 查找 class-like 符号。
     */
    private fun findBinaryClassLikeSymbol(classId: ClassId): CfirClassLikeSymbol<*>? =
        session.symbolProvider.getClassLikeSymbolByClassIdWithoutDependencies(classId)

    /**
     * 按 stub PSI 精确查找 class-like 符号。
     *
     * [CfirDeclarationForCompiledElementSearcher] 只应接收属于当前 compiled element searcher 模块的 PSI，因此允许调用模块专属
     * 符号提供器访问入口。
     */
    @OptIn(LLModuleSpecificSymbolProviderAccess::class)
    private fun findStubClassLikeSymbol(classId: ClassId, declaration: CjClassLikeDeclaration): CfirClassLikeSymbol<*>? =
        session.symbolProvider.getClassLikeSymbolByPsiWithoutDependencies(classId, declaration)

    /**
     * 查找非局部 class 中 [declaration] 对应的构造器 CFIR 声明。
     */
    private fun findConstructorOfNonLocalClass(declaration: CjConstructor<*>): CfirResolution<CfirConstructor> {
        // 构造器必然有外层类型；为空说明 PSI 已破损，属于编程错误。
        val containingClass = requireNotNull(declaration.containingTypeStatement) {
            "Constructor must have a containing type statement"
        }

        return findNonLocalClassLikeDeclaration(containingClass).flatMap { containingCfirClass ->
            val constructorCandidate = containingCfirClass.declarations
                .filterIsInstance<CfirConstructor>()
                .singleOrNull { cfirElementByPsiElementChooser.isMatchingCallableDeclaration(declaration, it) }
                ?: return@flatMap unresolved(
                    reason = UnresolvedReason.MISSING_DECLARATION,
                    message = "We should be able to find a constructor",
                    psi = declaration,
                    cfir = containingCfirClass,
                )

            CfirResolution.Resolved(constructorCandidate)
        }
    }

    /**
     * 查找非局部函数 [declaration] 对应的 CFIR 函数声明。
     */
    private fun findNonLocalFunction(declaration: CjNamedFunction): CfirResolution<CfirFunction> {
        require(!declaration.isLocal)

        return findFunctionCandidates(declaration).flatMap { candidates ->
            val functionCandidate = candidates.firstOrNull { cfirElementByPsiElementChooser.isMatchingCallableDeclaration(declaration, it.cfir) }
                ?: return@flatMap unresolved(
                    reason = UnresolvedReason.MISSING_DECLARATION,
                    message = "We should be able to find a symbol for function",
                    psi = declaration,
                    entries = candidates.toDiagnosticEntries(),
                )

            CfirResolution.Resolved(functionCandidate.cfir)
        }
    }

    /**
     * 查找非局部属性 [declaration] 对应的 CFIR 属性声明。
     */
    private fun findNonLocalProperty(declaration: CjProperty): CfirResolution<CfirProperty> {
        require(!declaration.isLocal)

        return findPropertyCandidates(declaration).flatMap { candidates ->
            val propertyCandidate = candidates.firstOrNull { cfirElementByPsiElementChooser.isMatchingCallableDeclaration(declaration, it.cfir) }
                ?: return@flatMap unresolved(
                    reason = UnresolvedReason.MISSING_DECLARATION,
                    message = "We should be able to find a symbol for property",
                    psi = declaration,
                    entries = candidates.toDiagnosticEntries(),
                )

            CfirResolution.Resolved(propertyCandidate.cfir)
        }
    }

    /**
     * 查找属性访问器 [declaration] 对应的 CFIR 访问器声明。
     */
    private fun findNonLocalPropertyAccessor(declaration: CjPropertyAccessor): CfirResolution<CfirPropertyAccessor> =
        findNonLocalProperty(declaration.property).flatMap { cfirProperty ->
            val accessor = if (declaration.isGetter) cfirProperty.getter else cfirProperty.setter
            if (accessor == null) {
                return@flatMap unresolved(
                    reason = UnresolvedReason.MISSING_DECLARATION,
                    message = "We should be able to find a symbol for property accessor",
                    psi = declaration,
                    cfir = cfirProperty,
                )
            }

            CfirResolution.Resolved(accessor)
        }

    /**
     * 构造一次“不可恢复”的结果。
     */
    private fun unresolved(
        reason: UnresolvedReason,
        message: String,
        psi: PsiElement? = null,
        cfir: CfirElement? = null,
        entries: List<Pair<String, String>> = emptyList(),
    ): CfirResolution.Unresolved = CfirResolution.Unresolved(
        reason = reason,
        diagnostics = ResolutionDiagnostics(message = message, psi = psi, cfir = cfir, extraEntries = entries),
    )
}

/**
 * 把候选符号集合压缩成诊断条目。
 *
 * 只依赖 `toString()`：候选来自不同符号族，各自的名称/来源访问器层级不一致，
 * 在诊断路径上强转反而可能引入二次失败。
 */
private fun List<CfirBasedSymbol<*>>.toDiagnosticEntries(): List<Pair<String, String>> = buildList {
    add("candidatesCount" to size.toString())
    forEachIndexed { index, candidate ->
        add("candidate#$index" to "${candidate::class.simpleName}: $candidate")
    }
}
