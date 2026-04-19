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
import org.cangnova.cangjie.cfir.patterns.bindingVariables
import org.cangnova.cangjie.cfir.scopes.CfirPackageScope
import org.cangnova.cangjie.cfir.scopes.CfirTypeScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassMemberScopeKind
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassUseSiteMemberScope
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

private class DeclarationBuckets {
    val simpleFunctions = mutableListOf<Pair<CfirFunctionSymbol<*>, String>>()
    val constructors = mutableListOf<Pair<CfirConstructorSymbol, String>>()
    val classLikes = mutableListOf<Pair<CfirClassLikeSymbol<*>, String>>()
    val properties = mutableListOf<Pair<CfirCallableSymbol<*>, String>>()
    val extensionProperties = mutableListOf<Pair<CfirCallableSymbol<*>, String>>()
}

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
                for (bindingVariable in declaration.pattern.bindingVariables()) {
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
                group.constructors += collectConstructorsForClassLike(symbol)
            }

            else -> Unit
        }
    }

    return groups
}

internal class CfirDeclarationCollector<D : CfirBasedSymbol<*>>(
    internal val context: CheckerContext,
) {
    val declarationConflictingSymbols: HashMap<D, SmartSet<CfirBasedSymbol<*>>> = hashMapOf()
}

context(context: CheckerContext)
internal fun CfirDeclarationCollector<CfirBasedSymbol<*>>.collectTopLevel(
    file: CfirFile,
    packageMemberScope: CfirPackageScope,
) {
    for ((declarationName, group) in groupTopLevelByName(file.declarations)) {
        val groupHasClassLikesOrProperties = group.classLikes.isNotEmpty() || group.properties.isNotEmpty()
        val groupHasSimpleFunctions = group.simpleFunctions.isNotEmpty()

        fun collect(
            declarations: List<Pair<out CfirBasedSymbol<*>, String>>,
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
    }
}

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

                useSiteScope.processFunctionsByName(declaredFunction.name) { anotherFunction ->
                    if (
                        anotherFunction != declaredFunction &&
                        anotherFunction.isCollectable() &&
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
                        anotherCallable.isVisibleInClass(classDeclaration, context)
                    ) {
                        val anotherRepresentation = CfirRedeclarationPresenter.represent(anotherCallable) ?: return@processCallablesByName
                        collect(anotherCallable, anotherRepresentation, otherDeclarations)
                    }
                }
            }

            is CfirPatternVariable -> {
                for (bindingVariable in declaration.pattern.bindingVariables()) {
                    val declaredVariable = bindingVariable.symbol as? CfirCallableSymbol<*> ?: continue
                    if (!declaredVariable.isCollectable()) continue

                    val representation = CfirRedeclarationPresenter.represent(declaredVariable) ?: continue
                    collect(declaredVariable, representation, otherDeclarations)

                    useSiteScope.processCallablesByName(bindingVariable.name) { anotherCallable ->
                        if (
                            anotherCallable != declaredVariable &&
                            anotherCallable !is CfirFunctionSymbol<*> &&
                            anotherCallable.isCollectable() &&
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

private enum class ConflictState {
    Conflict,
    NoConflict,
}

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

private fun <D : CfirBasedSymbol<*>, S : D> CfirDeclarationCollector<D>.collect(
    declaration: S,
    representation: String,
    map: MutableMap<String, MutableSet<S>>,
) {
    map.getOrPut(representation, ::mutableSetOf).also { declarations ->
        if (!declarations.add(declaration)) return@also

        val conflicts = SmartSet.create<CfirBasedSymbol<*>>()
        for (otherDeclaration in declarations) {
            if (otherDeclaration != declaration && getConflictState(declaration, otherDeclaration) == ConflictState.Conflict) {
                conflicts += otherDeclaration
                declarationConflictingSymbols.getOrPut(otherDeclaration) { SmartSet.create() }.add(declaration)
            }
        }

        declarationConflictingSymbols[declaration] = conflicts
    }
}

private fun CfirDeclarationCollector<CfirBasedSymbol<*>>.collectTopLevelConflict(
    declaration: CfirBasedSymbol<*>,
    declarationPresentation: String,
    containingFile: CfirFile,
    conflictingSymbol: CfirBasedSymbol<*>,
    conflictingPresentation: String? = null,
    conflictingFile: CfirFile? = null,
) {
    if (conflictingSymbol == declaration) return

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

    val actualConflictingFile = conflictingFile ?: context.session.cfirProvider.getContainingFile(conflictingSymbol)
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

    if (getConflictState(declaration, conflictingSymbol) == ConflictState.Conflict) {
        declarationConflictingSymbols.getOrPut(declaration) { SmartSet.create() }.add(conflictingSymbol)
    }
}

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
        conflictingSymbols.forEach { conflictingSymbol ->
            reporter.reportOn(conflictingSymbol.boundSourceOrNull(), CfirErrors.REDECLARATION, rendered)
        }
    }
}

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

private fun CfirCallableSymbol<*>.isVisibleInClass(
    classDeclaration: CfirClassLikeDeclaration,
    context: CheckerContext,
): Boolean {
    if (!isBound) return true
    if (cfir.status.visibility != Visibilities.Private) return true

    val ownerClassId = context.session.cfirProvider.getContainingClass(this)?.classId ?: return true
    val currentClassId = (classDeclaration.symbol as? CfirClassLikeSymbol<*>)?.classId ?: return true
    return ownerClassId == currentClassId
}

private fun CfirClassLikeSymbol<*>.isVisibleInClass(classDeclaration: CfirClassLikeDeclaration): Boolean {
    if (!isBound) return true
    if (cfir.status.visibility != Visibilities.Private) return true

    val currentClassId = (classDeclaration.symbol as? CfirClassLikeSymbol<*>)?.classId ?: return true
    return classId == currentClassId
}

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

private fun CfirCallableSymbol<*>.typeParameterSymbolsHaveNoName(): Boolean {
    if (!isBound) return false
    return cfir.typeParameters.any { it.symbol.name == SpecialNames.NO_NAME_PROVIDED }
}

private val CfirFunctionSymbol<*>.isCollectableAccordingToSource: Boolean
    get() = if (!isBound) {
        true
    } else {
        cfir.source?.kind !is CjFakeSourceElementKind || cfir.source?.kind == CjFakeSourceElementKind.DataClassGeneratedMembers
    }

private fun CfirBasedSymbol<*>.boundSourceOrNull(): CjSourceElement? =
    if (isBound) cfir.source else null

internal object CfirRedeclarationPresenter {
    fun represent(symbol: CfirBasedSymbol<*>): String? = when (symbol) {
        is CfirClassLikeSymbol<*> -> represent(symbol)
        is CfirCallableSymbol<*> -> represent(symbol)
        else -> null
    }

    fun represent(symbol: CfirClassLikeSymbol<*>): String? {
        return symbol.classId.shortClassName.asString()
    }

    fun represent(symbol: CfirCallableSymbol<*>): String? {
        if (!symbol.isBound) return symbol.name.asString()

        return when (symbol) {
            is CfirConstructorSymbol -> {
                val ownerClassName = symbol.callableId.classId?.shortClassName ?: symbol.name
                val constructor = symbol.cfir
                constructorRepresentation(ownerClassName, constructor.valueParameters.map(CfirVariable::returnTypeRef))
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

    fun represent(symbol: CfirConstructorSymbol, containingClass: CfirClassLikeSymbol<*>): String? {
        if (!symbol.isBound) return null
        val constructor = symbol.cfir
        return constructorRepresentation(
            containingClass.classId.shortClassName,
            constructor.valueParameters.map(CfirVariable::returnTypeRef),
        )
    }

    fun constructorRepresentation(name: Name, parameterTypeRefs: List<CfirTypeRef>): String {
        val parameterTypes = parameterTypeRefs.joinToString(",") { it.toStableSignatureKey() }
        return "${name.asString()}($parameterTypes)"
    }

    fun diagnosticName(symbol: CfirBasedSymbol<*>): String? = when (symbol) {
        is CfirClassLikeSymbol<*> -> symbol.classId.shortClassName.asString()
        is CfirCallableSymbol<*> -> symbol.name.asString()
        else -> symbol.debugName
    }
}

private fun CfirTypeRef.toStableSignatureKey(): String = when (this) {
    is CfirResolvedTypeRef -> coneType.renderForDebugging()
    else -> toString()
}

private fun Collection<CfirBasedSymbol<*>>.renderNames(): List<String> =
    asSequence().mapNotNull(CfirRedeclarationPresenter::diagnosticName).distinct().sorted().toList()
