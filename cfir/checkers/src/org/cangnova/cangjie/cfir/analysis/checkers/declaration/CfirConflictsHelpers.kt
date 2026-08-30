package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.lookupTracker
import org.cangnova.cangjie.cfir.nameConflictsTracker
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameterRef
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.declarations.callableNameOrNull
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.patterns.visibleBindingVariables
import org.cangnova.cangjie.cfir.scopes.CfirPackageScope
import org.cangnova.cangjie.cfir.scopes.CfirTypeScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassMemberScopeKind
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassUseSiteMemberScope
import org.cangnova.cangjie.cfir.resolve.providers.getContainingClass
import org.cangnova.cangjie.cfir.resolve.providers.getContainingFile
import org.cangnova.cangjie.cfir.session.cangjieScopeProvider
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.session.directSupertypeProviderOrNull
import org.cangnova.cangjie.cfir.session.extendProvider
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirEnumConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirErrorCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.renderForDebugging
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.SpecialNames
import org.cangnova.cangjie.source.CjFakeSourceElementKind
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.utils.SmartSet

/**
 * 同一名称下的顶层声明分桶。
 */
private class DeclarationBuckets {
    /**
     * 普通函数声明及其稳定签名展示。
     */
    val simpleFunctions = mutableListOf<Pair<CfirFunctionSymbol<*>, String>>()

    /**
     * 构造器声明及其稳定签名展示。
     */
    val constructors = mutableListOf<Pair<CfirConstructorSymbol, String>>()

    /**
     * classifier 声明及其展示名。
     */
    val classLikes = mutableListOf<Pair<CfirClassLikeSymbol<*>, String>>()

    /**
     * 非扩展属性/字段声明及其展示名。
     */
    val properties = mutableListOf<Pair<CfirCallableSymbol<*>, String>>()

    /**
     * 扩展属性声明及其展示名。
     */
    val extensionProperties = mutableListOf<Pair<CfirCallableSymbol<*>, String>>()
}

/**
 * 将顶层声明按可见诊断名称分组。
 */
context(context: CheckerContext)
private fun groupTopLevelByName(declarations: List<CfirDeclaration>): Map<Name, DeclarationBuckets> {
    val groups = mutableMapOf<Name, DeclarationBuckets>()

    for (declaration in declarations) {
        when (declaration) {
            is CfirFunction -> {
                val symbol = declaration.symbol as? CfirFunctionSymbol<*> ?: continue
                if (!symbol.isCollectable()) continue

                val name = declaration.callableNameOrNull() ?: symbol.name
                val presentation = CfirRedeclarationPresenter.represent(symbol) ?: continue
                groups.getOrPut(name, ::DeclarationBuckets).simpleFunctions += symbol to presentation
            }

            is CfirProperty -> {
                val symbol = declaration.symbol as? CfirPropertySymbol ?: continue
                if (!symbol.isCollectable()) continue

                val presentation = CfirRedeclarationPresenter.represent(symbol) ?: continue
                val group = groups.getOrPut(declaration.name, ::DeclarationBuckets)
                if (declaration.dispatchReceiverType != null) {
                    group.extensionProperties += symbol to presentation
                } else {
                    group.properties += symbol to presentation
                }
            }

            is CfirFieldVariable -> {
                val symbol = declaration.symbol as? CfirCallableSymbol<*> ?: continue
                if (!symbol.isCollectable()) continue

                val presentation = CfirRedeclarationPresenter.represent(symbol) ?: continue
                groups.getOrPut(symbol.name, ::DeclarationBuckets).properties += symbol to presentation
            }

            is CfirPatternVariable -> {
                for (bindingVariable in declaration.pattern.visibleBindingVariables()) {
                    val symbol = bindingVariable.symbol as? CfirCallableSymbol<*> ?: continue
                    if (!symbol.isCollectable()) continue
                    val presentation = bindingVariable.name.asString()
                    groups.getOrPut(bindingVariable.name, ::DeclarationBuckets).properties += symbol to presentation
                }
            }

            is CfirClassLikeDeclaration -> {
                val symbol = declaration.symbol as? CfirClassLikeSymbol<*> ?: continue
                if (!symbol.isCollectable()) continue

                val presentation = CfirRedeclarationPresenter.represent(symbol) ?: continue
                val group = groups.getOrPut(declaration.name, ::DeclarationBuckets)
                group.classLikes += symbol to presentation
            }

            else -> Unit
        }
    }

    return groups
}

/**
 * 声明冲突收集器。
 *
 * @property context 当前检查上下文。
 */
internal class CfirDeclarationCollector<D : CfirBasedSymbol<*>>(
    /**
     * 当前检查上下文。
     */
    internal val context: CheckerContext,
) {
    /**
     * 每个声明符号对应的冲突符号集合。
     */
    val declarationConflictingSymbols: HashMap<D, SmartSet<CfirBasedSymbol<*>>> = hashMapOf()
}

/**
 * 收集文件顶层声明与 package scope 中已有声明之间的冲突。
 */
context(context: CheckerContext)
internal fun CfirDeclarationCollector<CfirBasedSymbol<*>>.collectTopLevel(
    file: CfirFile,
    packageMemberScope: CfirPackageScope,
) {
    for ((declarationName, group) in groupTopLevelByName(file.declarations)) {
        val groupHasClassLikesOrProperties = group.classLikes.isNotEmpty() || group.properties.isNotEmpty()
        val groupHasSimpleFunctions = group.simpleFunctions.isNotEmpty()

        fun collect(
            declarations: List<Pair<CfirBasedSymbol<*>, String>>,
            conflictingSymbol: CfirBasedSymbol<*>,
            conflictingPresentation: String? = null,
            conflictingFile: CfirFile? = null,
        ) {
            declarations.forEach { (declaration, declarationPresentation) ->
                collectTopLevelConflict(
                    declaration = declaration,
                    declarationPresentation = declarationPresentation,
                    containingFile = file,
                    conflictingSymbol = conflictingSymbol,
                    conflictingPresentation = conflictingPresentation,
                    conflictingFile = conflictingFile,
                )

                context.session.lookupTracker?.recordLookup(
                    declarationName.asString(),
                    file.packageDirective.packageFqName.asString(),
                    declaration.boundSourceOrNull(),
                    file.source,
                )
            }
        }

        fun collectFromClassifierSource(
            conflictingSymbol: CfirClassLikeSymbol<*>,
            conflictingPresentation: String? = null,
            conflictingFile: CfirFile? = null,
        ) {
            collect(group.classLikes, conflictingSymbol, conflictingPresentation, conflictingFile)
            collect(group.properties, conflictingSymbol, conflictingPresentation, conflictingFile)

            if (groupHasSimpleFunctions) {
                collectConstructorsForClassLike(conflictingSymbol).forEach { (constructorSymbol, constructorPresentation) ->
                    collect(group.simpleFunctions, constructorSymbol, constructorPresentation, conflictingFile)
                }
            }
        }

        if (groupHasSimpleFunctions || group.constructors.isNotEmpty()) {
            packageMemberScope.processFunctionsByName(declarationName) {
                collect(group.simpleFunctions, it)
                collect(group.constructors, it)
            }
        }

        if (groupHasClassLikesOrProperties || groupHasSimpleFunctions) {
            packageMemberScope.processClassifiersByName(declarationName) {
                collectFromClassifierSource(it)
            }

            context.session.nameConflictsTracker
                ?.getClassifierRedeclarations(ClassId(file.packageDirective.packageFqName, declarationName))
                ?.forEach { redeclaration ->
                    val symbol = redeclaration.classifierSymbol as? CfirClassLikeSymbol<*> ?: return@forEach
                    collectFromClassifierSource(
                        conflictingSymbol = symbol,
                        conflictingFile = redeclaration.containingFile,
                    )
                }

            group.classLikes.forEach { (classLike, representation) ->
                collectFromClassifierSource(classLike, representation, file)
            }
        }

        if (groupHasClassLikesOrProperties || group.extensionProperties.isNotEmpty()) {
            packageMemberScope.processPropertiesByName(declarationName) {
                collect(group.classLikes, conflictingSymbol = it)
                collect(group.properties, conflictingSymbol = it)
                collect(group.extensionProperties, conflictingSymbol = it)
            }
        }

        collectFunctionNameRedeclarations(group)
    }
}

/**
 * 收集 class-like 声明成员之间以及成员与继承 scope 之间的冲突。
 */
context(context: CheckerContext)
internal fun CfirDeclarationCollector<CfirBasedSymbol<*>>.collectClassMembers(classDeclaration: CfirClassLikeDeclaration) {
    val otherDeclarations = mutableMapOf<String, MutableSet<CfirBasedSymbol<*>>>()
    val functionDeclarations = mutableMapOf<String, MutableSet<CfirBasedSymbol<*>>>()
    val useSiteScope = createUseSiteMemberScope(classDeclaration)

    fun processClassifier(symbol: CfirClassLikeSymbol<*>) {
        if (!symbol.isCollectable() || !symbol.isVisibleInClass(classDeclaration)) return

        val representation = CfirRedeclarationPresenter.represent(symbol) ?: return
        collect(symbol, representation, otherDeclarations)

        collectConstructorsForClassLike(symbol).forEach { (constructorSymbol, constructorPresentation) ->
            collect(constructorSymbol, constructorPresentation, functionDeclarations)
        }
    }

    for (declaration in classDeclaration.declarations) {
        when (declaration) {
            is CfirFunction -> {
                val declaredFunction = declaration.symbol as? CfirFunctionSymbol<*> ?: continue
                if (!declaredFunction.isCollectable()) continue

                val representation = CfirRedeclarationPresenter.represent(declaredFunction) ?: continue
                collect(declaredFunction, representation, functionDeclarations)
                collect(declaredFunction, representation, otherDeclarations)

                useSiteScope.processFunctionsByName(declaredFunction.name) { anotherFunction ->
                    if (
                        anotherFunction != declaredFunction &&
                        anotherFunction.isCollectable() &&
                        !anotherFunction.isScopeGeneratedSubstitutionOverride() &&
                        anotherFunction.isVisibleInClass(classDeclaration, context)
                    ) {
                        val anotherRepresentation = CfirRedeclarationPresenter.represent(anotherFunction) ?: return@processFunctionsByName
                        collect(anotherFunction, anotherRepresentation, functionDeclarations)
                    }
                }
            }

            is CfirProperty -> {
                val declaredProperty = declaration.symbol as? CfirPropertySymbol ?: continue
                if (!declaredProperty.isCollectable()) continue

                val representation = CfirRedeclarationPresenter.represent(declaredProperty) ?: continue
                collect(declaredProperty, representation, otherDeclarations)

                useSiteScope.processPropertiesByName(declaredProperty.name) { anotherProperty ->
                    if (
                        anotherProperty != declaredProperty &&
                        anotherProperty.isCollectable() &&
                        !anotherProperty.isScopeGeneratedSubstitutionOverride() &&
                        anotherProperty.isVisibleInClass(classDeclaration, context)
                    ) {
                        val anotherRepresentation = CfirRedeclarationPresenter.represent(anotherProperty) ?: return@processPropertiesByName
                        collect(anotherProperty, anotherRepresentation, otherDeclarations)
                    }
                }
            }

            is CfirFieldVariable -> {
                val declaredVariable = declaration.symbol as? CfirCallableSymbol<*> ?: continue
                if (!declaredVariable.isCollectable()) continue

                val representation = CfirRedeclarationPresenter.represent(declaredVariable) ?: continue
                collect(declaredVariable, representation, otherDeclarations)

                useSiteScope.processCallablesByName(declaredVariable.name) { anotherCallable ->
                    if (
                        anotherCallable != declaredVariable &&
                        anotherCallable !is CfirFunctionSymbol<*> &&
                        anotherCallable.isCollectable() &&
                        !anotherCallable.isScopeGeneratedSubstitutionOverride() &&
                        anotherCallable.isVisibleInClass(classDeclaration, context)
                    ) {
                        val anotherRepresentation = CfirRedeclarationPresenter.represent(anotherCallable) ?: return@processCallablesByName
                        collect(anotherCallable, anotherRepresentation, otherDeclarations)
                    }
                }
            }

            is CfirPatternVariable -> {
                for (bindingVariable in declaration.pattern.visibleBindingVariables()) {
                    val declaredVariable = bindingVariable.symbol as? CfirCallableSymbol<*> ?: continue
                    if (!declaredVariable.isCollectable()) continue

                    val representation = CfirRedeclarationPresenter.represent(declaredVariable) ?: continue
                    collect(declaredVariable, representation, otherDeclarations)

                    useSiteScope.processCallablesByName(bindingVariable.name) { anotherCallable ->
                        if (
                            anotherCallable != declaredVariable &&
                            anotherCallable !is CfirFunctionSymbol<*> &&
                            anotherCallable.isCollectable() &&
                            !anotherCallable.isScopeGeneratedSubstitutionOverride() &&
                            anotherCallable.isVisibleInClass(classDeclaration, context)
                        ) {
                            val anotherRepresentation = CfirRedeclarationPresenter.represent(anotherCallable) ?: return@processCallablesByName
                            collect(anotherCallable, anotherRepresentation, otherDeclarations)
                        }
                    }
                }
            }

            is CfirClassLikeDeclaration -> {
                val declaredClassifier = declaration.symbol as? CfirClassLikeSymbol<*> ?: continue
                processClassifier(declaredClassifier)

                useSiteScope.processClassifiersByName(declaredClassifier.name) { anotherClassifier ->
                    if (anotherClassifier != declaredClassifier) {
                        processClassifier(anotherClassifier)
                    }
                }
            }

            else -> Unit
        }
    }
}

/**
 * 收集单个 `extend` 声明自身成员函数的同签名冲突。
 *
 * `extend` 是独立的声明所有者，不能借用被扩展类型的 class-member collector：后者会把目标类型
 * 及其他 extend 的成员混入当前作用域，而这里的 `CONFLICTING_OVERLOADS` 只描述同一 extend 块内
 * 的重复声明。继承成员冲突仍由 `CfirInheritanceDeepChecker` 统一处理。
 */
context(context: CheckerContext)
internal fun CfirDeclarationCollector<CfirBasedSymbol<*>>.collectExtendMembers(extend: CfirExtend) {
    val functionDeclarations = mutableMapOf<String, MutableSet<CfirBasedSymbol<*>>>()
    val functionNamesBySignature = mutableMapOf<String, Name>()
    val staticSignatures = mutableMapOf<String, Boolean>()
    val nonStaticFunctionNames = mutableSetOf<Name>()

    for (declaration in extend.declarations) {
        if (declaration !is CfirFunction) continue

        val declaredFunction = declaration.symbol as? CfirFunctionSymbol<*> ?: continue
        if (!declaredFunction.isCollectable()) continue

        // static 属性属于函数重定义签名；static/non-static 同名规则由
        // CfirFunctionOverloadChecker 单独报告，不能在此降格为 CONFLICTING_OVERLOADS。
        val representation = CfirRedeclarationPresenter.represent(declaredFunction)
            ?.let { signature -> "${declaration.status.isStatic}:$signature" }
            ?: continue
        functionNamesBySignature[representation] = declaredFunction.name
        staticSignatures[representation] = declaration.status.isStatic
        if (!declaration.status.isStatic) {
            nonStaticFunctionNames += declaredFunction.name
        }
        // `collect` 会在加入成员的同时把冲突边写入 declarationConflictingSymbols。
        // static/non-static 规则依赖完整的同名组，必须先完成纯分组，再决定该签名组
        // 是否应参与 CONFLICTING_OVERLOADS；否则后续排除无法撤销已经写入的边。
        functionDeclarations.getOrPut(representation) { linkedSetOf() } += declaredFunction
    }

    // extend 内的同签名函数共享一个声明所有者；冲突图必须对每个参与者保留边，
    // 以便诊断层能够在每个源码声明位置渲染该冲突。
    for ((signature, sameSignatureFunctions) in functionDeclarations) {
        // 官方 static/non-static 函数冲突以同名组为单位，而不是以签名组为单位；
        // 同名 non-static 函数存在时，静态函数仅由 CfirFunctionOverloadChecker 报告。
        if (staticSignatures[signature] == true && functionNamesBySignature[signature] in nonStaticFunctionNames) {
            continue
        }

        for (function in sameSignatureFunctions) {
            val conflicts = SmartSet.create<CfirBasedSymbol<*>>()
            for (anotherFunction in sameSignatureFunctions) {
                if (
                    anotherFunction != function &&
                    getConflictState(function, anotherFunction) == ConflictState.Conflict
                ) {
                    conflicts += anotherFunction
                }
            }
            declarationConflictingSymbols.mergeConflicts(function, conflicts)
        }
    }
}

/**
 * 两个声明的冲突判定结果。
 */
private enum class ConflictState {
    /**
     * 两个声明形成冲突。
     */
    Conflict,

    /**
     * 两个声明不形成冲突。
     */
    NoConflict,
}

/**
 * 判断两个符号在当前收集器语义下是否冲突。
 */
private fun CfirDeclarationCollector<*>.getConflictState(
    declaration: CfirBasedSymbol<*>,
    conflicting: CfirBasedSymbol<*>,
): ConflictState {
    if (declaration is CfirCallableSymbol<*> && conflicting is CfirCallableSymbol<*>) {
        val declarationPresentation = CfirRedeclarationPresenter.represent(declaration)
        val conflictingPresentation = CfirRedeclarationPresenter.represent(conflicting)
        if (declarationPresentation != conflictingPresentation) {
            return ConflictState.NoConflict
        }
    }
    return ConflictState.Conflict
}

/**
 * 将声明加入按表示文本分组的冲突集合。
 */
private fun <D : CfirBasedSymbol<*>, S : D> CfirDeclarationCollector<D>.collect(
    declaration: S,
    representation: String,
    map: MutableMap<String, MutableSet<S>>,
) {
    map.getOrPut(representation, ::mutableSetOf).also { declarations ->
        if (!declarations.add(declaration)) return@also

        val conflicts = SmartSet.create<CfirBasedSymbol<*>>()
        for (otherDeclaration in declarations) {
            if (
                otherDeclaration != declaration &&
                declaration.shouldReportRedeclarationWith(otherDeclaration) &&
                getConflictState(declaration, otherDeclaration) == ConflictState.Conflict
            ) {
                conflicts += otherDeclaration
            }
        }

        declarationConflictingSymbols.mergeConflicts(declaration, conflicts)
    }
}

/**
 * 合并声明已有冲突集合和新发现的冲突集合。
 */
private fun <D : CfirBasedSymbol<*>> MutableMap<D, SmartSet<CfirBasedSymbol<*>>>.mergeConflicts(
    declaration: D,
    conflicts: SmartSet<CfirBasedSymbol<*>>,
) {
    if (conflicts.isEmpty()) {
        getOrPut(declaration) { SmartSet.create() }
        return
    }

    val current = getOrPut(declaration) { SmartSet.create() }
    conflicts.forEach { current += it }
}

/**
 * 收集单个顶层声明与另一个顶层符号之间的冲突。
 */
private fun CfirDeclarationCollector<CfirBasedSymbol<*>>.collectTopLevelConflict(
    declaration: CfirBasedSymbol<*>,
    declarationPresentation: String,
    containingFile: CfirFile,
    conflictingSymbol: CfirBasedSymbol<*>,
    conflictingPresentation: String? = null,
    conflictingFile: CfirFile? = null,
) {
    if (conflictingSymbol == declaration) return
    if (declaration is CfirFunctionSymbol<*> && conflictingSymbol is CfirConstructorSymbol) return

    if (declaration.isBound && conflictingSymbol.isBound) {
        val declarationModule = declaration.cfir.moduleData
        val conflictingModule = conflictingSymbol.cfir.moduleData
        if (
            declarationModule != conflictingModule &&
            !shouldCheckForMultiplatformRedeclaration(declaration, conflictingSymbol)
        ) {
            return
        }
    }

    val actualConflictingPresentation = conflictingPresentation ?: CfirRedeclarationPresenter.represent(conflictingSymbol) ?: return
    if (actualConflictingPresentation != declarationPresentation) {
        return
    }

    val actualConflictingFile = conflictingFile ?: conflictingSymbol.getContainingFile()
    if (!conflictingSymbol.isCollectable()) {
        return
    }
    if (areCompatibleMainFunctions(declaration, containingFile, conflictingSymbol, actualConflictingFile)) {
        return
    }

    val conflictingDeclaration = if (conflictingSymbol.isBound) conflictingSymbol.cfir else return
    if (
        conflictingDeclaration is CfirCallableDeclaration &&
        conflictingDeclaration.status.visibility == Visibilities.Private &&
        actualConflictingFile != containingFile
    ) {
        return
    }

    if (
        declaration.shouldReportRedeclarationWith(conflictingSymbol) &&
        getConflictState(declaration, conflictingSymbol) == ConflictState.Conflict
    ) {
        declarationConflictingSymbols.getOrPut(declaration) { SmartSet.create() }.add(conflictingSymbol)
    }
}

/**
 * 收集函数名与先前非函数声明之间的 redeclaration 关系。
 */
private fun CfirDeclarationCollector<CfirBasedSymbol<*>>.collectFunctionNameRedeclarations(group: DeclarationBuckets) {
    val nonFunctionDeclarations = (group.classLikes + group.properties + group.extensionProperties)
        .sortedBy { (symbol, _) -> symbol.boundSourceOrNull()?.startOffset ?: Int.MAX_VALUE }

    val functions = group.simpleFunctions
        .sortedBy { (symbol, _) -> symbol.boundSourceOrNull()?.startOffset ?: Int.MAX_VALUE }

    for ((function, _) in functions) {
        val functionSource = function.boundSourceOrNull() ?: continue
        if (functions.any { (otherFunction, _) ->
                otherFunction != function &&
                    (otherFunction.boundSourceOrNull()?.startOffset ?: Int.MAX_VALUE) < functionSource.startOffset
            }
        ) {
            continue
        }

        nonFunctionDeclarations
            .asSequence()
            .map { (symbol, _) -> symbol }
            .filter { (it.boundSourceOrNull()?.startOffset ?: Int.MAX_VALUE) < functionSource.startOffset }
            .forEach { previous ->
                declarationConflictingSymbols.getOrPut(function) { SmartSet.create() }.add(previous)
            }
    }
}

/**
 * 判断当前声明是否应作为与另一个声明冲突的报告主体。
 */
private fun CfirBasedSymbol<*>.shouldReportRedeclarationWith(conflicting: CfirBasedSymbol<*>): Boolean {
    val declarationSource = boundSourceOrNull() ?: return true
    val conflictingSource = conflicting.boundSourceOrNull() ?: return true
    return declarationSource.startOffset >= conflictingSource.startOffset
}

/**
 * 判断跨模块符号是否需要检查多平台 redeclaration。
 */
private fun shouldCheckForMultiplatformRedeclaration(
    dependency: CfirBasedSymbol<*>,
    dependent: CfirBasedSymbol<*>,
): Boolean {
    if (!dependency.isBound || !dependent.isBound) return true

    val dependencyModule = dependency.cfir.moduleData
    val dependentModule = dependent.cfir.moduleData
    if (dependencyModule == dependentModule) return true

    return dependencyModule in dependentModule.dependencies ||
        dependencyModule in dependentModule.allRefinementDependencies ||
        dependentModule in dependencyModule.dependencies ||
        dependentModule in dependencyModule.allRefinementDependencies
}

/**
 * 判断两个 main 函数是否属于允许跨文件共存的入口重载形态。
 */
private fun areCompatibleMainFunctions(
    declaration1: CfirBasedSymbol<*>,
    file1: CfirFile,
    declaration2: CfirBasedSymbol<*>,
    file2: CfirFile?,
): Boolean {
    if (file1 == file2) return false
    if (declaration1 !is CfirNamedFunctionSymbol || declaration2 !is CfirNamedFunctionSymbol) return false
    return declaration1.representsMainFunctionAllowingConflictingOverloads() &&
        declaration2.representsMainFunctionAllowingConflictingOverloads()
}

/**
 * 判断函数符号是否是允许跨文件共存的 main 签名。
 */
private fun CfirNamedFunctionSymbol.representsMainFunctionAllowingConflictingOverloads(): Boolean {
    if (!isBound) return false
    if (name.asString() != "main") return false
    if (callableId.className != null) return false

    val function = cfir
    if (function.dispatchReceiverType != null) return false
    if (function.typeParameters.isNotEmpty()) return false

    val returnType = function.returnTypeRef.toStableSignatureKey()
    if (!returnType.contains("Unit")) return false

    val valueParameters = function.valueParameters
    if (valueParameters.isEmpty()) return true
    if (valueParameters.size != 1) return false

    val parameterType = valueParameters.single().returnTypeRef.toStableSignatureKey()
    return parameterType.contains("Array") && parameterType.contains("String")
}

/**
 * 检查同一局部声明列表中的重声明。
 */
context(context: CheckerContext, reporter: DiagnosticReporter)
internal fun checkForLocalRedeclarations(elements: List<org.cangnova.cangjie.cfir.CfirElement>) {
    if (elements.size <= 1) return

    val groupedByName = linkedMapOf<Name, MutableList<CfirBasedSymbol<*>>>()

    for (element in elements) {
        val (symbol, name) = when (element) {
            is CfirVariable -> element.symbol to element.symbol.name
            is CfirClassLikeDeclaration -> {
                val symbol = element.symbol as? CfirClassLikeSymbol<*> ?: continue
                symbol to symbol.name
            }
            is CfirTypeParameterRef -> element.symbol to element.symbol.name
            else -> continue
        }

        if (!name.isSpecial) {
            groupedByName.getOrPut(name, ::mutableListOf).add(symbol)
        }
    }

    groupedByName.values.forEach { conflictingSymbols ->
        if (conflictingSymbols.size <= 1) return@forEach
        val rendered = conflictingSymbols.renderNames()
        conflictingSymbols.sortedBy { symbol ->
            symbol.boundSourceOrNull()?.startOffset ?: Int.MAX_VALUE
        }.drop(1).forEach { conflictingSymbol ->
            reporter.reportOn(conflictingSymbol.boundSourceOrNull(), CfirErrors.REDECLARATION, rendered)
        }
    }
}

/**
 * 为 class-like 声明创建用于成员冲突检查的 declaration-site member scope。
 */
context(context: CheckerContext)
private fun createUseSiteMemberScope(classDeclaration: CfirClassLikeDeclaration): CfirTypeScope {
    val classLikeSymbol = classDeclaration.symbol as? CfirClassLikeSymbol<*> ?: return CfirTypeScope.Empty
    return when (classDeclaration) {
        is CfirClass -> context.session.cangjieScopeProvider.getDeclarationSiteMemberScope(
            classDeclaration,
            context.session,
            context.scopeSession,
        )
        else -> CfirClassUseSiteMemberScope(
            session = context.session,
            classLikeSymbol,
            context.session.symbolProvider,
            context.session.extendProvider,
            context.session.directSupertypeProviderOrNull,
            scopeKind = CfirClassMemberScopeKind.DECLARATION_SITE,
        )
    }
}

/**
 * 判断 callable 符号从当前 class 声明内是否可见。
 */
private fun CfirCallableSymbol<*>.isVisibleInClass(
    classDeclaration: CfirClassLikeDeclaration,
    context: CheckerContext,
): Boolean {
    if (!isBound) return true
    if (cfir.status.visibility != Visibilities.Private) return true

    val ownerClassId = getContainingClass()?.classId ?: return true
    val currentClassId = (classDeclaration.symbol as? CfirClassLikeSymbol<*>)?.classId ?: return true
    return ownerClassId == currentClassId
}

/**
 * 判断 callable 是否是 scope 生成的 substitution override。
 */
private fun CfirCallableSymbol<*>.isScopeGeneratedSubstitutionOverride(): Boolean =
    isBound && cfir.origin is CfirDeclarationOrigin.SubstitutionOverride

/**
 * 判断 classifier 符号从当前 class 声明内是否可见。
 */
private fun CfirClassLikeSymbol<*>.isVisibleInClass(classDeclaration: CfirClassLikeDeclaration): Boolean {
    if (!isBound) return true
    if (cfir.status.visibility != Visibilities.Private) return true

    val currentClassId = (classDeclaration.symbol as? CfirClassLikeSymbol<*>)?.classId ?: return true
    return classId == currentClassId
}

/**
 * 收集 class-like 声明的构造器及其冲突表示文本。
 */
private fun collectConstructorsForClassLike(classLikeSymbol: CfirClassLikeSymbol<*>): List<Pair<CfirConstructorSymbol, String>> {
    if (!classLikeSymbol.isBound) return emptyList()
    val declaration = classLikeSymbol.cfir
    val constructors = mutableListOf<Pair<CfirConstructorSymbol, String>>()

    declaration.declarations
        .filterIsInstance<CfirConstructor>()
        .forEach { constructor ->
            val symbol = constructor.symbol
            val representation = CfirRedeclarationPresenter.represent(symbol, classLikeSymbol) ?: return@forEach
            constructors += symbol to representation
        }

    return constructors
}

/**
 * 判断符号是否应该纳入冲突收集。
 */
private fun CfirBasedSymbol<*>.isCollectable(): Boolean {
    if (!isBound) return true

    if (this is CfirCallableSymbol<*>) {
        if (this is CfirErrorCallableSymbol<*>) return false
        if (typeParameterSymbolsHaveNoName()) return false
    }

    return when (this) {
        is CfirFunctionSymbol<*> -> isCollectableAccordingToSource && name != SpecialNames.NO_NAME_PROVIDED
        is CfirClassLikeSymbol<*> -> name != SpecialNames.NO_NAME_PROVIDED
        is CfirPropertySymbol -> cfir.source?.kind != CjFakeSourceElementKind.EnumGeneratedDeclaration
        else -> cfir.source?.kind != CjFakeSourceElementKind.ClassDelegationField
    }
}

/**
 * 判断 callable 的类型参数中是否存在无名参数。
 */
private fun CfirCallableSymbol<*>.typeParameterSymbolsHaveNoName(): Boolean {
    if (!isBound) return false
    return cfir.typeParameters.any { it.symbol.name == SpecialNames.NO_NAME_PROVIDED }
}

/**
 * 判断函数符号按源码来源是否可收集。
 */
private val CfirFunctionSymbol<*>.isCollectableAccordingToSource: Boolean
    get() = if (!isBound) {
        true
    } else {
        cfir.source?.kind !is CjFakeSourceElementKind || cfir.source?.kind == CjFakeSourceElementKind.DataClassGeneratedMembers
    }

/**
 * 返回已绑定符号的源码范围。
 */
private fun CfirBasedSymbol<*>.boundSourceOrNull(): CjSourceElement? =
    if (isBound) cfir.source else null

/**
 * redeclaration 诊断展示名渲染器。
 */
internal object CfirRedeclarationPresenter {
    /**
     * 渲染任意可支持符号的 redeclaration 表示。
     */
    fun represent(symbol: CfirBasedSymbol<*>): String? = when (symbol) {
        is CfirClassLikeSymbol<*> -> represent(symbol)
        is CfirCallableSymbol<*> -> represent(symbol)
        else -> null
    }

    /**
     * 渲染 classifier 符号。
     */
    fun represent(symbol: CfirClassLikeSymbol<*>): String? {
        return symbol.classId.shortClassName.asString()
    }

    /**
     * 渲染 callable 符号的稳定签名表示。
     */
    fun represent(symbol: CfirCallableSymbol<*>): String? {
        if (!symbol.isBound) return symbol.name.asString()

        return when (symbol) {
            is CfirConstructorSymbol -> {
                val ownerClassName = symbol.callableId.classId?.shortClassName ?: symbol.name
                val constructor = symbol.cfir
                // 静态初始化器 `static init()`（官方建模为 `static.init`）与普通构造器 `init`
                // 语义不同：同签名不应互相冲突，只应与同签名静态初始化器冲突。因此用独立的
                // static 前缀区分签名，避免把 `static init()` 与普通 `init()` 误判为重声明。
                val ownerText = if (constructor.status.isStatic) {
                    "static." + ownerClassName.asString()
                } else {
                    ownerClassName.asString()
                }
                constructorRepresentation(Name.identifier(ownerText), constructor.valueParameters.map(CfirVariable::returnTypeRef))
            }

            is CfirEnumConstructorSymbol -> {
                val enumConstructor = symbol.cfir
                constructorRepresentation(symbol.name, enumConstructor.valueParameters.map(CfirVariable::returnTypeRef))
            }

            is CfirFunctionSymbol<*> -> {
                val function = symbol.cfir
                val receiverPrefix = function.dispatchReceiverType?.renderForDebugging()?.let { "$it." }.orEmpty()
                val parameters = function.valueParameters.joinToString(",") { parameter ->
                    parameter.returnTypeRef.toStableSignatureKey()
                }
                receiverPrefix + symbol.name.asString() + "(" + parameters + ")"
            }

            else -> {
                val callable = symbol.cfir
                val receiverPrefix = callable.dispatchReceiverType?.renderForDebugging()?.let { "$it." }.orEmpty()
                receiverPrefix + constructorRepresentation(symbol.name, emptyList())
            }
        }
    }

    /**
     * 使用指定 owner class 渲染构造器符号。
     */
    fun represent(symbol: CfirConstructorSymbol, containingClass: CfirClassLikeSymbol<*>): String? {
        if (!symbol.isBound) return null
        val constructor = symbol.cfir
        return constructorRepresentation(
            containingClass.classId.shortClassName,
            constructor.valueParameters.map(CfirVariable::returnTypeRef),
        )
    }

    /**
     * 渲染构造器样式的 `Name(paramTypes)` 表示。
     */
    fun constructorRepresentation(name: Name, parameterTypeRefs: List<CfirTypeRef>): String {
        val parameterTypes = parameterTypeRefs.joinToString(",") { it.toStableSignatureKey() }
        return "${name.asString()}($parameterTypes)"
    }

    /**
     * 渲染诊断参数中的短名称。
     */
    fun diagnosticName(symbol: CfirBasedSymbol<*>): String? = when (symbol) {
        is CfirClassLikeSymbol<*> -> symbol.classId.shortClassName.asString()
        is CfirCallableSymbol<*> -> symbol.name.asString()
        else -> symbol.debugName
    }
}

/**
 * 将类型引用渲染为稳定签名 key。
 */
private fun CfirTypeRef.toStableSignatureKey(): String = when (this) {
    is CfirResolvedTypeRef -> coneType.renderForDebugging()
    else -> toString()
}

/**
 * 渲染符号集合的诊断名称列表。
 */
private fun Collection<CfirBasedSymbol<*>>.renderNames(): List<String> =
    asSequence().mapNotNull(CfirRedeclarationPresenter::diagnosticName).distinct().sorted().toList()
