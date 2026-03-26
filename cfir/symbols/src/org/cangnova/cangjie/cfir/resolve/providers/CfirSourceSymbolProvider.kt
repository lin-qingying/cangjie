package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.cfir.nameConflictsTracker
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirMacroDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.declarations.callableNameOrNull
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
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirMacroDeclarationSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPatternVariableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.cfir.symbols.CfirFieldVariableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirEnumConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirEnumSymbol
import org.cangnova.cangjie.cfir.symbols.CfirInterfaceSymbol
import org.cangnova.cangjie.cfir.symbols.CfirStructSymbol
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

    override fun getClassByClassId(classId: ClassId): CfirClassLikeDeclaration? {
        val symbol = state.classifierMap[classId] ?: return null
        return if (symbol.isBound) symbol.cfir as? CfirClassLikeDeclaration else null
    }

    override fun getClassIdBySymbol(classSymbol: CfirClassSymbol): ClassId? =
        state.classIdBySymbol[classSymbol]

    override fun getEnumConstructorOwnerClassId(symbol: CfirEnumConstructorSymbol): ClassId? =
        state.enumConstructorOwnerClassIdMap[symbol]

    override fun getCfirClassifierContainerFile(fqName: ClassId): CfirFile =
        state.classifierContainerFileMap[fqName]
            ?: error("No containing file recorded for classifier $fqName")

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

        override fun getClassLikeSymbolByClassId(classId: ClassId): CfirClassLikeSymbol<*>? =
            state.classifierMap[classId]

        override fun getClassIdBySymbol(classSymbol: CfirClassSymbol): ClassId? =
            state.classIdBySymbol[classSymbol]

        override fun getEnumConstructorOwnerClassId(symbol: CfirEnumConstructorSymbol): ClassId? =
            state.enumConstructorOwnerClassIdMap[symbol]

        override fun getContainingFile(symbol: org.cangnova.cangjie.cfir.symbols.CfirSymbol<*>): CfirFile? = when (symbol) {
            is CfirClassLikeSymbol<*> -> state.classIdBySymbol[symbol]?.let(state.classifierContainerFileMap::get)
            is CfirCallableSymbol<*> -> state.callableContainerFileMap[symbol]
            else -> null
        }

        override fun getContainingClassId(symbol: CfirCallableSymbol<*>): ClassId? =
            state.callableOwnerClassIdMap[symbol]

        override fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): List<CfirCallableSymbol<*>> =
            state.callableMap[CallableId(packageFqName, name)].orEmpty()

        override fun getTopLevelFunctionSymbols(packageFqName: FqName, name: Name): List<CfirFunctionSymbol<*>> =
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
            // ---- 具名分类器类型 ----

            is CfirClass -> {
                val classId = computeClassId(packageFqName, declaration.name, containingClass)
                recordClassLikeClassifier(
                    symbol = declaration.symbol as? CfirClassSymbol,
                    packageFqName = packageFqName,
                    shortName = declaration.name,
                    containingClass = containingClass,
                    containingFile = containingFile,
                    isTopLevel = isTopLevel,
                )
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

            is CfirInterface -> {
                val classId = computeClassId(packageFqName, declaration.name, containingClass)
                recordClassLikeClassifier(
                    symbol = declaration.symbol as? CfirInterfaceSymbol,
                    packageFqName = packageFqName,
                    shortName = declaration.name,
                    containingClass = containingClass,
                    containingFile = containingFile,
                    isTopLevel = isTopLevel,
                )
                // 接口成员只有 property 和 function，分别遍历
                for (nested in declaration.properties + declaration.functions) {
                    recordDeclaration(
                        declaration = nested,
                        packageFqName = packageFqName,
                        containingClass = classId,
                        containingFile = containingFile,
                        isTopLevel = false,
                    )
                }
            }

            is CfirStruct -> {
                val classId = computeClassId(packageFqName, declaration.name, containingClass)
                recordClassLikeClassifier(
                    symbol = declaration.symbol as? CfirStructSymbol,
                    packageFqName = packageFqName,
                    shortName = declaration.name,
                    containingClass = containingClass,
                    containingFile = containingFile,
                    isTopLevel = isTopLevel,
                )
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

            is CfirEnum -> {
                val classId = computeClassId(packageFqName, declaration.name, containingClass)
                recordClassLikeClassifier(
                    symbol = declaration.symbol as? CfirEnumSymbol,
                    packageFqName = packageFqName,
                    shortName = declaration.name,
                    containingClass = containingClass,
                    containingFile = containingFile,
                    isTopLevel = isTopLevel,
                )
                // 顶层枚举需要额外注册其构造器（枚举构造器在包级别可见）
                if (isTopLevel) {
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
                    state.classifierInPackage
                        .getOrPut(packageFqName, ::mutableSetOf)
                        .add(classId.shortClassName)
                }
            }

            // ---- 可调用声明（仅顶层注册）----


            is CfirProperty -> {
                val symbol = declaration.symbol as? CfirPropertySymbol ?: return
                state.callableContainerFileMap[symbol] = containingFile
                state.callableOwnerClassIdMap[symbol] = containingClass
                if (!isTopLevel) return
                val callableId = CallableId(packageFqName, declaration.name)
                state.propertyMap.getOrPut(callableId, ::mutableListOf).add(symbol)
                state.callableMap.getOrPut(callableId, ::mutableListOf).add(symbol)
                state.callableNamesInPackage.getOrPut(packageFqName, ::mutableSetOf).add(declaration.name)
            }

            is CfirPatternVariable -> {
                val symbol = declaration.symbol as? CfirPatternVariableSymbol ?: return
                state.callableContainerFileMap[symbol] = containingFile
                state.callableOwnerClassIdMap[symbol] = containingClass
                if (!isTopLevel) return
                for (name in collectBindingNames(declaration.pattern)) {
                    val callableId = CallableId(packageFqName, name)
                    state.callableMap.getOrPut(callableId, ::mutableListOf).add(symbol)
                    state.callableNamesInPackage.getOrPut(packageFqName, ::mutableSetOf).add(name)
                }
            }

            is CfirFieldVariable -> {
                val symbol = declaration.symbol as? CfirFieldVariableSymbol ?: return
                state.callableContainerFileMap[symbol] = containingFile
                state.callableOwnerClassIdMap[symbol] = containingClass
                if (!isTopLevel) return
                val callableId = CallableId(packageFqName, declaration.name)
                state.callableMap.getOrPut(callableId, ::mutableListOf).add(symbol)
                state.callableNamesInPackage.getOrPut(packageFqName, ::mutableSetOf).add(declaration.name)
            }

            is CfirMacroDeclaration -> {
                val symbol = declaration.symbol as? CfirMacroDeclarationSymbol ?: return
                state.callableContainerFileMap[symbol] = containingFile
                state.callableOwnerClassIdMap[symbol] = containingClass
                if (!isTopLevel) return
                val callableId = CallableId(packageFqName, declaration.name)
                state.callableMap.getOrPut(callableId, ::mutableListOf).add(symbol)
                state.callableNamesInPackage.getOrPut(packageFqName, ::mutableSetOf).add(declaration.name)
            }

            is CfirFunction -> {
                val symbol = declaration.symbol as? CfirFunctionSymbol<*> ?: return
                state.callableContainerFileMap[symbol] = containingFile
                state.callableOwnerClassIdMap[symbol] = containingClass
                if (!isTopLevel) return
                val callableName = declaration.callableNameOrNull() ?: return
                val callableId = CallableId(packageFqName, callableName)
                state.functionMap.getOrPut(callableId, ::mutableListOf).add(symbol)
                state.callableMap.getOrPut(callableId, ::mutableListOf).add(symbol)
                state.callableNamesInPackage.getOrPut(packageFqName, ::mutableSetOf).add(callableName)
            }
            else -> Unit
        }
    }

    private fun recordClassLikeClassifier(
        symbol: CfirClassLikeSymbol<*>?,
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
            ClassId(packageFqName, containingClass.relativeClassName.child(shortName))
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
        declaration: CfirEnum,
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

        val classifierMap: MutableMap<ClassId, CfirClassLikeSymbol<*>> = hashMapOf()
        val classIdBySymbol: MutableMap<CfirClassLikeSymbol<*>, ClassId> = hashMapOf()
        val classifierContainerFileMap: MutableMap<ClassId, CfirFile> = hashMapOf()
        val classifierInPackage: MutableMap<FqName, MutableSet<Name>> = hashMapOf()
        val classesInPackage: MutableMap<FqName, MutableSet<Name>> = hashMapOf()
        val enumConstructorOwnerClassIdMap: MutableMap<CfirEnumConstructorSymbol, ClassId> = hashMapOf()
        val callableContainerFileMap: MutableMap<CfirCallableSymbol<*>, CfirFile> = hashMapOf()
        val callableOwnerClassIdMap: MutableMap<CfirCallableSymbol<*>, ClassId?> = hashMapOf()

        val callableMap: MutableMap<CallableId, MutableList<CfirCallableSymbol<*>>> = hashMapOf()
        val functionMap: MutableMap<CallableId, MutableList<CfirFunctionSymbol<*>>> = hashMapOf()
        val propertyMap: MutableMap<CallableId, MutableList<CfirPropertySymbol>> = hashMapOf()
        val callableNamesInPackage: MutableMap<FqName, MutableSet<Name>> = hashMapOf()
    }
}
