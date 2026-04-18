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
import org.cangnova.cangjie.cfir.patterns.bindingVariables
import org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirMacroDeclarationSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPatternVariableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPatternBindingSymbol
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
            state.classifierMap[classId] as? CfirClassLikeSymbol<*>

        override fun getTopLevelClassifierSymbols(packageFqName: FqName, name: Name): List<CfirClassLikeSymbol<*>> =
            state.topLevelClassifierMap[CallableId(packageFqName, name)].orEmpty()
                .filterIsInstance<CfirClassLikeSymbol<*>>()

        override fun getClassIdBySymbol(classSymbol: CfirClassSymbol): ClassId? =
            state.classIdBySymbol[classSymbol]

        override fun getEnumConstructorOwnerClassId(symbol: CfirEnumConstructorSymbol): ClassId? =
            state.enumConstructorOwnerClassIdMap[symbol]

        override fun getContainingFile(symbol: org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol<*>): CfirFile? = when (val normalizedSymbol = symbol.unwrapForDeclarationMetadataLookup()) {
            is CfirClassLikeSymbol<*> -> state.classifierContainerFileBySymbol[normalizedSymbol]
            is CfirCallableSymbol<*> -> state.callableContainerFileMap[normalizedSymbol]
            else -> null
        }

        override fun getContainingClassId(symbol: CfirCallableSymbol<*>): ClassId? =
            state.callableOwnerClassIdMap[symbol.unwrapCallableForDeclarationMetadataLookup()]

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
                if (!isTopLevel) return
                val classId = computeClassId(packageFqName, declaration.name)
                recordClassLikeClassifier(
                    symbol = declaration.symbol as? CfirClassSymbol,
                    packageFqName = packageFqName,
                    shortName = declaration.name,
                    containingFile = containingFile,
                    isTopLevel = true,
                )
                recordMemberDeclarations(
                    declarations = declaration.declarations,
                    packageFqName = packageFqName,
                    ownerClassId = classId,
                    containingFile = containingFile,
                )
            }

            is CfirInterface -> {
                if (!isTopLevel) return
                val classId = computeClassId(packageFqName, declaration.name)
                recordClassLikeClassifier(
                    symbol = declaration.symbol as? CfirInterfaceSymbol,
                    packageFqName = packageFqName,
                    shortName = declaration.name,
                    containingFile = containingFile,
                    isTopLevel = true,
                )
                // 接口成员只采集直接 callable，不采集任何 class-like。
                recordMemberDeclarations(
                    declarations = declaration.declarations ,
                    packageFqName = packageFqName,
                    ownerClassId = classId,
                    containingFile = containingFile,
                )
            }

            is CfirStruct -> {
                if (!isTopLevel) return
                val classId = computeClassId(packageFqName, declaration.name)
                recordClassLikeClassifier(
                    symbol = declaration.symbol as? CfirStructSymbol,
                    packageFqName = packageFqName,
                    shortName = declaration.name,
                    containingFile = containingFile,
                    isTopLevel = true,
                )
                recordMemberDeclarations(
                    declarations = declaration.declarations,
                    packageFqName = packageFqName,
                    ownerClassId = classId,
                    containingFile = containingFile,
                )
            }

            is CfirEnum -> {
                if (!isTopLevel) return
                val classId = computeClassId(packageFqName, declaration.name)
                recordClassLikeClassifier(
                    symbol = declaration.symbol as? CfirEnumSymbol,
                    packageFqName = packageFqName,
                    shortName = declaration.name,
                    containingFile = containingFile,
                    isTopLevel = true,
                )
                // 枚举构造器在包级可见，但 enum 内部 class-like 仍不进入公开索引。
                recordTopLevelEnumConstructors(
                    declaration = declaration,
                    ownerClassId = classId,
                    containingFile = containingFile,
                )
                recordMemberDeclarations(
                    declarations = declaration.declarations,
                    packageFqName = packageFqName,
                    ownerClassId = classId,
                    containingFile = containingFile,
                )
            }

            is CfirTypeAlias -> {
                if (!isTopLevel) return
                recordClassLikeClassifier(
                    symbol = declaration.symbol as? CfirClassLikeSymbol<*>,
                    packageFqName = packageFqName,
                    shortName = declaration.name,
                    containingFile = containingFile,
                    isTopLevel = true,
                )
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
                for (bindingVariable in declaration.pattern.bindingVariables()) {
                    val bindingSymbol = bindingVariable.symbol as? CfirPatternBindingSymbol ?: continue
                    state.callableContainerFileMap[bindingSymbol] = containingFile
                    state.callableOwnerClassIdMap[bindingSymbol] = containingClass
                    val callableId = CallableId(packageFqName, bindingVariable.name)
                    state.callableMap.getOrPut(callableId, ::mutableListOf).add(bindingSymbol)
                    state.callableNamesInPackage.getOrPut(packageFqName, ::mutableSetOf).add(bindingVariable.name)
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
        containingFile: CfirFile,
        isTopLevel: Boolean,
    ) {
        val classId = computeClassId(packageFqName, shortName)
        if (symbol != null) {
            state.classIdBySymbol[symbol] = classId
            state.classifierContainerFileBySymbol[symbol] = containingFile
            val previousSymbol = state.classifierMap[classId]
            if (previousSymbol == null) {
                state.classifierMap[classId] = symbol
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
            if (symbol != null) {
                val callableId = CallableId(packageFqName, shortName)
                val symbols = state.topLevelClassifierMap.getOrPut(callableId, ::mutableListOf)
                if (symbol !in symbols) {
                    symbols += symbol
                }
            }
        }
    }

    private fun computeClassId(
        packageFqName: FqName,
        shortName: Name,
    ): ClassId {
        return ClassId(packageFqName, shortName)
    }

    /**
     * 成员作用域只递归直接 callable。
     *
     * class-like 声明不会继续向成员索引层展开，
     * 从而保证源码符号索引与顶层类型标识规则保持一致。
     */
    private fun recordMemberDeclarations(
        declarations: Collection<CfirDeclaration>,
        packageFqName: FqName,
        ownerClassId: ClassId,
        containingFile: CfirFile,
    ) {
        for (member in declarations) {
            if (member is CfirClassLikeDeclaration) continue
            recordDeclaration(
                declaration = member,
                packageFqName = packageFqName,
                containingClass = ownerClassId,
                containingFile = containingFile,
                isTopLevel = false,
            )
        }
    }

    private fun recordTopLevelEnumConstructors(
        declaration: CfirEnum,
        ownerClassId: ClassId,
        containingFile: CfirFile,
    ) {
        declaration.declarations.asSequence()
            .filterIsInstance<CfirEnumConstructor>()
            .forEach { enumConstructor ->
                val symbol = enumConstructor.symbol ?: return@forEach
                val callableId = CallableId(ownerClassId.packageFqName, enumConstructor.name)
                state.callableMap.getOrPut(callableId, ::mutableListOf).add(symbol)
                state.callableNamesInPackage.getOrPut(ownerClassId.packageFqName, ::mutableSetOf)
                    .add(enumConstructor.name)
                state.enumConstructorOwnerClassIdMap[symbol] = ownerClassId
                state.callableContainerFileMap[symbol] = containingFile
                state.callableOwnerClassIdMap[symbol] = ownerClassId
            }
    }

    private class State {
        val fileMap: MutableMap<FqName, MutableList<CfirFile>> = hashMapOf()
        val allSubPackages: MutableSet<FqName> = hashSetOf()

        val classifierMap: MutableMap<ClassId, CfirClassLikeSymbol<*>> = hashMapOf()
        val topLevelClassifierMap: MutableMap<CallableId, MutableList<CfirClassLikeSymbol<*>>> = hashMapOf()
        val classIdBySymbol: MutableMap<CfirClassLikeSymbol<*>, ClassId> = hashMapOf()
        val classifierContainerFileBySymbol: MutableMap<CfirClassLikeSymbol<*>, CfirFile> = hashMapOf()
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
