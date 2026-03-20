package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.cfir.nameConflictsTracker
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirMacroDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.patterns.CfirBindingPattern
import org.cangnova.cangjie.cfir.patterns.CfirConstPattern
import org.cangnova.cangjie.cfir.patterns.CfirEnumPattern
import org.cangnova.cangjie.cfir.patterns.CfirExpressionPattern
import org.cangnova.cangjie.cfir.patterns.CfirOrPattern
import org.cangnova.cangjie.cfir.patterns.CfirPattern
import org.cangnova.cangjie.cfir.patterns.CfirTuplePattern
import org.cangnova.cangjie.cfir.patterns.CfirTypePattern
import org.cangnova.cangjie.cfir.patterns.CfirWildcardPattern
import org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirMacroDeclarationSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPatternVariableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.cfir.symbols.CfirFieldVariableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirEnumConstructorSymbol
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/**
 * Source-side provider aligned with Kotlin's `FirProviderImpl`.
 *
 * It owns source indexes and exposes them through both:
 * - [CfirProvider] APIs for declaration lookup.
 * - [symbolProvider] for class/callable symbol resolution.
 */
class CfirProviderImpl(
    val session: CfirSession,
    val cangjieScopeProvider: CfirCangJieScopeProvider = CfirCangJieScopeProvider(),
) : CfirProvider() {

    override val symbolProvider: CfirSymbolProvider = SourceSymbolProvider()

    private val state = State()

    /** 返回所有已注册的源码文件。 */
    fun getAllFiles(): List<CfirFile> = state.fileMap.values.flatten()

    fun recordFile(file: CfirFile) {
        val packageName = file.packageDirective.packageFqName
        state.fileMap.getOrPut(packageName, ::mutableListOf).add(file)
        recordPackageAndParents(packageName)

        for (declaration in file.declarations) {
            recordDeclaration(
                declaration = declaration,
                packageFqName = packageName,
                containingClass = null,
                containingFile = file,
                isTopLevel = true,
            )
        }
    }

    override fun getCfirFilesByPackage(fqName: FqName): List<CfirFile> =
        state.fileMap[fqName].orEmpty()

    override fun getClassByClassId(classId: ClassId): CfirClass? {
        val symbol = state.classifierMap[classId] ?: return null
        return if (symbol.isBound) symbol.cfir else null
    }

    override fun getClassIdBySymbol(classSymbol: CfirClassSymbol): ClassId? =
        state.classIdBySymbol[classSymbol]

    override fun getEnumConstructorOwnerClassId(symbol: CfirEnumConstructorSymbol): ClassId? =
        state.enumConstructorOwnerClassIdMap[symbol]

    override fun getClassNamesInPackage(fqName: FqName): Set<Name> =
        state.classesInPackage[fqName].orEmpty()

    private inner class SourceSymbolProvider : CfirSymbolProvider(session) {
        override val symbolNamesProvider: CfirSymbolNamesProvider = object : CfirSymbolNamesProvider {
            override fun getPackageNames(): Set<FqName> = state.allSubPackages

            override fun getTopLevelClassifierNamesInPackage(packageFqName: FqName): Set<Name> =
                state.classifierInPackage[packageFqName].orEmpty()

            override fun getTopLevelCallableNamesInPackage(packageFqName: FqName): Set<Name> =
                state.callableNamesInPackage[packageFqName].orEmpty()
        }

        override fun getClassLikeSymbolByClassId(classId: ClassId): CfirClassSymbol? =
            state.classifierMap[classId]

        override fun getClassIdBySymbol(classSymbol: CfirClassSymbol): ClassId? =
            state.classIdBySymbol[classSymbol]

        override fun getEnumConstructorOwnerClassId(symbol: CfirEnumConstructorSymbol): ClassId? =
            state.enumConstructorOwnerClassIdMap[symbol]

        override fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): List<CfirCallableSymbol<*>> =
            state.callableMap[CallableId(packageFqName, name)].orEmpty()

        override fun getTopLevelFunctionSymbols(packageFqName: FqName, name: Name): List<CfirFunctionSymbol> =
            state.functionMap[CallableId(packageFqName, name)].orEmpty()

        override fun getTopLevelPropertySymbols(packageFqName: FqName, name: Name): List<CfirPropertySymbol> =
            state.propertyMap[CallableId(packageFqName, name)].orEmpty()

        override fun hasPackage(fqName: FqName): Boolean =
            fqName in state.allSubPackages
    }

    private fun recordPackageAndParents(packageName: FqName) {
        var current = packageName
        while (true) {
            state.allSubPackages.add(current)
            if (current.isRoot) break
            current = current.parent()
        }
    }

    private fun recordDeclaration(
        declaration: CfirDeclaration,
        packageFqName: FqName,
        containingClass: ClassId?,
        containingFile: CfirFile,
        isTopLevel: Boolean,
    ) {
        when (declaration) {
            is CfirClass -> {
                val classId = computeClassId(packageFqName, declaration.name, containingClass)
                val classSymbol = declaration.symbol as? CfirClassSymbol
                recordClassLikeClassifier(
                    symbol = classSymbol,
                    packageFqName = packageFqName,
                    shortName = declaration.name,
                    containingClass = containingClass,
                    containingFile = containingFile,
                    isTopLevel = isTopLevel,
                )

                if (declaration.classKind == org.cangnova.cangjie.cfir.declarations.CfirClassKind.ENUM && isTopLevel) {
                    recordTopLevelEnumConstructors(
                        declaration = declaration,
                        ownerClassId = classId,
                    )
                }

                for (nested in declaration.declarations) {
                    recordDeclaration(
                        declaration = nested,
                        packageFqName = packageFqName,
                        containingClass = classId,
                        containingFile = containingFile,
                        isTopLevel = false,
                    )
                }
            }

            is CfirTypeAlias -> {
                val classId = computeClassId(packageFqName, declaration.name, containingClass)
                if (isTopLevel) {
                    state.classifierInPackage.getOrPut(packageFqName, ::mutableSetOf).add(classId.shortClassName)
                }
            }

            is CfirFunction -> {
                if (!isTopLevel) return
                val symbol = declaration.symbol as? CfirFunctionSymbol ?: return
                val callableId = CallableId(packageFqName, declaration.name)
                state.functionMap.getOrPut(callableId, ::mutableListOf).add(symbol)
                state.callableMap.getOrPut(callableId, ::mutableListOf).add(symbol)
                state.callableNamesInPackage.getOrPut(packageFqName, ::mutableSetOf).add(declaration.name)
            }

            is CfirProperty -> {
                if (!isTopLevel) return
                val symbol = declaration.symbol as? CfirPropertySymbol ?: return
                val callableId = CallableId(packageFqName, declaration.name)
                state.propertyMap.getOrPut(callableId, ::mutableListOf).add(symbol)
                state.callableMap.getOrPut(callableId, ::mutableListOf).add(symbol)
                state.callableNamesInPackage.getOrPut(packageFqName, ::mutableSetOf).add(declaration.name)
            }

            is CfirPatternVariable -> {
                if (!isTopLevel) return
                val symbol = declaration.symbol as? CfirPatternVariableSymbol ?: return
                val bindingNames = collectBindingNames(declaration.pattern)
                for (name in bindingNames) {
                    val callableId = CallableId(packageFqName, name)
                    state.callableMap.getOrPut(callableId, ::mutableListOf).add(symbol)
                    state.callableNamesInPackage.getOrPut(packageFqName, ::mutableSetOf).add(name)
                }
            }

            is CfirFieldVariable -> {
                if (!isTopLevel) return
                val symbol = declaration.symbol as? CfirFieldVariableSymbol ?: return
                val callableId = CallableId(packageFqName, declaration.name)
                state.callableMap.getOrPut(callableId, ::mutableListOf).add(symbol)
                state.callableNamesInPackage.getOrPut(packageFqName, ::mutableSetOf).add(declaration.name)
            }

            is CfirMacroDeclaration -> {
                if (!isTopLevel) return
                val symbol = declaration.symbol as? CfirMacroDeclarationSymbol ?: return
                val callableId = CallableId(packageFqName, declaration.name)
                state.callableMap.getOrPut(callableId, ::mutableListOf).add(symbol)
                state.callableNamesInPackage.getOrPut(packageFqName, ::mutableSetOf).add(declaration.name)
            }

            else -> Unit
        }
    }

    private fun recordClassLikeClassifier(
        symbol: CfirClassSymbol?,
        packageFqName: FqName,
        shortName: Name,
        containingClass: ClassId?,
        containingFile: CfirFile,
        isTopLevel: Boolean,
    ) {
        val classId = computeClassId(packageFqName, shortName, containingClass)
        if (symbol != null) {
            val previousSymbol = state.classifierMap[classId]
            if (previousSymbol == null) {
                state.classifierMap[classId] = symbol
                state.classIdBySymbol[symbol] = classId
                state.classifierContainerFileMap[classId] = containingFile
            } else if (previousSymbol != symbol) {
                session.nameConflictsTracker?.registerClassifierRedeclaration(
                    classId = classId,
                    newSymbol = symbol,
                    newSymbolFile = containingFile,
                    prevSymbol = previousSymbol,
                    prevSymbolFile = state.classifierContainerFileMap[classId],
                )
            }
        }

        if (isTopLevel) {
            state.classifierInPackage.getOrPut(packageFqName, ::mutableSetOf).add(classId.shortClassName)
            state.classesInPackage.getOrPut(packageFqName, ::mutableSetOf).add(classId.shortClassName)
        }
    }

    private fun computeClassId(
        packageFqName: FqName,
        shortName: Name,
        containingClass: ClassId?,
    ): ClassId {
        return if (containingClass == null) {
            ClassId(packageFqName, shortName)
        } else {
            ClassId(packageFqName, containingClass.relativeClassName.child(shortName), isLocal = false)
        }
    }

    private fun collectBindingNames(pattern: CfirPattern): List<Name> {
        return when (pattern) {
            is CfirBindingPattern -> buildList {
                add(pattern.name)
                pattern.nestedPattern?.let { addAll(collectBindingNames(it)) }
            }
            is CfirTuplePattern -> pattern.elements.flatMap(::collectBindingNames)
            is CfirEnumPattern -> pattern.arguments.flatMap(::collectBindingNames)
            is CfirTypePattern -> listOfNotNull(pattern.bindingName)
            is CfirOrPattern -> pattern.alternatives.firstOrNull()?.let(::collectBindingNames).orEmpty()
            is CfirWildcardPattern, is CfirConstPattern , is CfirExpressionPattern -> emptyList()
        }
    }

    private fun recordTopLevelEnumConstructors(
        declaration: CfirClass,
        ownerClassId: ClassId,
    ) {
        declaration.declarations.asSequence()
            .filterIsInstance<CfirEnumConstructor>()
            .forEach { enumConstructor ->
                val symbol = enumConstructor.symbol as? CfirEnumConstructorSymbol ?: return@forEach
                val callableId = CallableId(ownerClassId.packageFqName, enumConstructor.name)
                state.callableMap.getOrPut(callableId, ::mutableListOf).add(symbol)
                state.callableNamesInPackage.getOrPut(ownerClassId.packageFqName, ::mutableSetOf)
                    .add(enumConstructor.name)
                state.enumConstructorOwnerClassIdMap[symbol] = ownerClassId
            }
    }

    private class State {
        val fileMap: MutableMap<FqName, MutableList<CfirFile>> = hashMapOf()
        val allSubPackages: MutableSet<FqName> = hashSetOf()

        val classifierMap: MutableMap<ClassId, CfirClassSymbol> = hashMapOf()
        val classIdBySymbol: MutableMap<CfirClassSymbol, ClassId> = hashMapOf()
        val classifierContainerFileMap: MutableMap<ClassId, CfirFile> = hashMapOf()
        val classifierInPackage: MutableMap<FqName, MutableSet<Name>> = hashMapOf()
        val classesInPackage: MutableMap<FqName, MutableSet<Name>> = hashMapOf()
        val enumConstructorOwnerClassIdMap: MutableMap<CfirEnumConstructorSymbol, ClassId> = hashMapOf()

        val callableMap: MutableMap<CallableId, MutableList<CfirCallableSymbol<*>>> = hashMapOf()
        val functionMap: MutableMap<CallableId, MutableList<CfirFunctionSymbol>> = hashMapOf()
        val propertyMap: MutableMap<CallableId, MutableList<CfirPropertySymbol>> = hashMapOf()
        val callableNamesInPackage: MutableMap<FqName, MutableSet<Name>> = hashMapOf()
    }
}
