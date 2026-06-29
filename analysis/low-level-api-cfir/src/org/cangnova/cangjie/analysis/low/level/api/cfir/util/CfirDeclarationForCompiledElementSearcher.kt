@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)



package org.cangnova.cangjie.analysis.low.level.api.cfir.util

import org.cangnova.cangjie.analysis.api.platform.CaDeserializedDeclarationsOrigin
import org.cangnova.cangjie.analysis.api.platform.CaPlatformSettings
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CangJieProjectStructureProvider
import org.cangnova.cangjie.analysis.api.util.withClassEntry
import org.cangnova.cangjie.analysis.api.util.withPsiEntry
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.services.LLCfirElementByPsiElementChooser
import org.cangnova.cangjie.analysis.low.level.api.cfir.element.builder.containingDeclaration
import org.cangnova.cangjie.analysis.low.level.api.cfir.projectStructure.llCfirModuleData
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSession
import org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.LLModuleWithDependenciesSymbolProvider
import org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.LLModuleSpecificSymbolProviderAccess
import org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.getClassLikeSymbolByClassIdWithoutDependencies
import org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.getClassLikeSymbolByPsiWithoutDependencies
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
import org.cangnova.cangjie.utils.exceptions.ExceptionAttachmentBuilder
import org.cangnova.cangjie.utils.exceptions.withCfirEntry

/**
 * 根据反编译或 stub PSI 声明查找对应 CFIR 声明的工具。
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
    fun findNonLocalDeclaration(cjDeclaration: CjDeclaration): CfirDeclaration = when (cjDeclaration) {
        is CjClassLikeDeclaration -> findNonLocalClassLikeDeclaration(cjDeclaration)
        is CjConstructor<*> -> findConstructorOfNonLocalClass(cjDeclaration)
        is CjNamedFunction -> findNonLocalFunction(cjDeclaration)
        is CjProperty -> findNonLocalProperty(cjDeclaration)
        is CjParameter -> findParameter(cjDeclaration)
        is CjPropertyAccessor -> findNonLocalPropertyAccessor(cjDeclaration)
        is CjTypeParameter -> findNonLocalTypeParameter(cjDeclaration)

        else -> errorWithCfirSpecificEntries("Unsupported compiled declaration of type", psi = cjDeclaration)
    }

    /**
     * 查找 [function] 对应的函数符号候选。
     */
    private fun findFunctionCandidates(function: CjNamedFunction): List<CfirFunctionSymbol<*>> =
        findCallableCandidates(function, function.parent is CjFile).filterIsInstance<CfirFunctionSymbol<*>>()

    /**
     * 查找 [property] 对应的属性符号候选。
     */
    private fun findPropertyCandidates(property: CjProperty): List<CfirPropertySymbol> =
        findCallableCandidates(property, property.parent is CjFile).filterIsInstance<CfirPropertySymbol>()

    /**
     * 查找 compiled callable [declaration] 对应的 CFIR callable 符号候选。
     */
    private fun findCallableCandidates(
        declaration: CjCallableDeclaration,
        isTopLevel: Boolean,
    ): List<CfirCallableSymbol<*>> {
        val shortName = declaration.nameAsSafeName

        if (isTopLevel) {
            val packageFqName = declaration.containingCjFile.packageFqName

            return when (val symbolProvider = session.symbolProvider) {
                is LLModuleWithDependenciesSymbolProvider ->
                    symbolProvider.getTopLevelDeserializedCallableSymbolsWithoutDependencies(packageFqName, shortName, declaration)

                else -> symbolProvider.getTopLevelCallableSymbols(packageFqName, shortName)
            }
        }

        val containingClass = declaration.containingTypeStatement?.let(::findNonLocalClassLikeDeclaration)
            ?: errorWithCfirSpecificEntries("No containing non-local declaration found for", psi = declaration)

        return when (declaration) {
            is CjProperty -> containingClass.declarations
                .filterIsInstance<CfirProperty>()
                .filter { it.name == shortName }
                .mapTo(mutableListOf()) { it.symbol }
            is CjNamedFunction -> containingClass.declarations
                .filterIsInstance<CfirFunction>()
                .filter { it.symbol.name == shortName }
                .mapTo(mutableListOf()) { it.symbol }
            else -> errorWithCfirSpecificEntries("Unexpected callable ${declaration::class.simpleName}") {
                withEntry("isTopLevel", isTopLevel.toString())
                withPsiEntry("declaration", declaration)
            }
        }
    }

    /**
     * 查找 compiled 类型参数 [param] 对应的 CFIR 类型参数声明。
     */
    private fun findNonLocalTypeParameter(param: CjTypeParameter): CfirDeclaration {
        val owner = param.containingDeclaration ?: errorWithCfirSpecificEntries("Unsupported compiled type parameter", psi = param)
        val cfirDeclaration = findNonLocalDeclaration(owner)
        val cfirTypeParameterRefOwner = cfirDeclaration as? CfirTypeParameterRefsOwner ?: errorWithCfirSpecificEntries(
            "No cfir found by $owner",
            psi = owner,
            cfir = cfirDeclaration,
        )

        return cfirTypeParameterRefOwner.typeParameters.find { typeParameterRef ->
            cfirElementByPsiElementChooser.isMatchingTypeParameter(param, typeParameterRef.symbol.cfir)
        } as CfirDeclaration
    }

    /**
     * 查找 compiled 值参数 [param] 对应的 CFIR 值参数声明。
     */
    private fun findParameter(param: CjParameter): CfirDeclaration {
        val ownerDeclaration = param.ownerFunction ?: errorWithCfirSpecificEntries("Unsupported compiled parameter", psi = param)
        val cfirDeclaration = findNonLocalDeclaration(ownerDeclaration)
        val cfirFunction = cfirDeclaration as? CfirFunction ?: errorWithCfirSpecificEntries(
            "${CfirFunction::class.simpleName} expected but ${cfirDeclaration::class.simpleName} found",
            psi = ownerDeclaration,
            cfir = cfirDeclaration,
        )

        return cfirFunction.valueParameters.find { cfirElementByPsiElementChooser.isMatchingValueParameter(param, it) }
            ?: errorWithCfirSpecificEntries("No cfir value parameter found", psi = param, cfir = cfirFunction)
    }

    /**
     * 查找 compiled class-like [declaration] 对应的 CFIR class-like 声明。
     */
    private fun findNonLocalClassLikeDeclaration(declaration: CjClassLikeDeclaration): CfirClassLikeDeclaration {
        val classId = declaration.getClassId() ?: errorWithCfirSpecificEntries("Non-local class should have classId", psi = declaration)

        // With the `BINARIES` origin, deserialized CFIR declarations don't have associated PSI elements. Hence, we cannot use `*ByPsi*`
        // functions, as they check the candidate's associated PSI.
        val classLikeSymbol = when (CaPlatformSettings.getInstance(project).deserializedDeclarationsOrigin) {
            CaDeserializedDeclarationsOrigin.BINARIES -> findBinaryClassLikeSymbol(classId)
            CaDeserializedDeclarationsOrigin.STUBS -> findStubClassLikeSymbol(classId, declaration)
            else -> null
        }

        classLikeSymbol?.let { return it.cfir }

        errorWithCfirSpecificEntries(
            "We should be able to find a symbol for class-like declaration",
            psi = declaration,
        ) {
            withEntry("classId", classId) { it.asString() }

            val contextualModule = session.llCfirModuleData.caModule
            val moduleForFile = projectStructureProvider.getModule(declaration, contextualModule)
            withEntry("caModule", moduleForFile) { it.moduleDescription }
        }
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
    private fun findConstructorOfNonLocalClass(declaration: CjConstructor<*>): CfirConstructor {
        val containingClass = declaration.containingTypeStatement
            ?: errorWithCfirSpecificEntries("Constructor must have outer class", psi = declaration)

        val containingCfirClass = findNonLocalClassLikeDeclaration(containingClass)
        val constructorCandidate = containingCfirClass.declarations
            .filterIsInstance<CfirConstructor>()
            .singleOrNull { cfirElementByPsiElementChooser.isMatchingCallableDeclaration(declaration, it) }
            ?: errorWithCfirSpecificEntries("We should be able to find a constructor", psi = declaration, cfir = containingCfirClass)

        return constructorCandidate
    }

    /**
     * 查找非局部函数 [declaration] 对应的 CFIR 函数声明。
     */
    private fun findNonLocalFunction(declaration: CjNamedFunction): CfirFunction {
        require(!declaration.isLocal)

        val candidates = findFunctionCandidates(declaration)
        val functionCandidate = candidates.firstOrNull { cfirElementByPsiElementChooser.isMatchingCallableDeclaration(declaration, it.cfir) }
            ?: errorWithCfirSpecificEntries("We should be able to find a symbol for function", psi = declaration) {
                withCandidates(candidates)
            }

        return functionCandidate.cfir
    }

    /**
     * 查找非局部属性 [declaration] 对应的 CFIR 属性声明。
     */
    private fun findNonLocalProperty(declaration: CjProperty): CfirProperty {
        require(!declaration.isLocal)

        val candidates = findPropertyCandidates(declaration)
        val propertyCandidate = candidates.firstOrNull { cfirElementByPsiElementChooser.isMatchingCallableDeclaration(declaration, it.cfir) }
            ?: errorWithCfirSpecificEntries("We should be able to find a symbol for property", psi = declaration) {
                withCandidates(candidates)
            }

        return propertyCandidate.cfir
    }

    /**
     * 查找属性访问器 [declaration] 对应的 CFIR 访问器声明。
     */
    private fun findNonLocalPropertyAccessor(declaration: CjPropertyAccessor): CfirPropertyAccessor {
        val cfirProperty = findNonLocalProperty(declaration.property)

        return (if (declaration.isGetter) cfirProperty.getter else cfirProperty.setter)
            ?: errorWithCfirSpecificEntries("We should be able to find a symbol for property accessor", psi = declaration)
    }
}

/**
 * 把候选符号集合写入异常附件。
 */
private fun ExceptionAttachmentBuilder.withCandidates(candidates: List<CfirBasedSymbol<*>>) {
    withEntry("Candidates count", candidates.size.toString())
    for ((index, candidate) in candidates.withIndex()) {
        val caModule = candidate.llCfirModuleData.caModule
        withEntryGroup(index.toString()) {
            withClassEntry("candidateClass", candidate)
            withEntry("module", caModule) { it.moduleDescription }
            withEntry("origin", candidate.origin.toString())
            withCfirEntry("candidateCfir", candidate.cfir)

        }
    }
}
