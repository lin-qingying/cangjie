package org.cangnova.cangjie.chir.cfir2chir

import org.cangnova.cangjie.chir.core.declaration.DefaultChirClassDeclaration
import org.cangnova.cangjie.chir.core.declaration.DefaultChirEnumDeclaration
import org.cangnova.cangjie.chir.core.declaration.DefaultChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.declaration.DefaultChirPropertyDeclaration
import org.cangnova.cangjie.chir.core.declaration.DefaultChirStructDeclaration
import org.cangnova.cangjie.chir.core.declaration.DefaultChirVariableDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirVariableDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirDeclaration as ChirDeclaration
import org.cangnova.cangjie.chir.core.model.ChirModule
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration as CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirMainFunction
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter

internal class Cfir2ChirFileConverter(
    private val components: Cfir2ChirComponents,
    private val packageName: String,
) {
    private val storage: Cfir2ChirDeclarationStorage = components.declarationStorage
    private val typeMapper: Cfir2ChirTypeMapper = components.typeMapper

    fun registerFileAndDeclarations(file: CfirFile) {
        storage.registerModule(
            file,
            ChirModule(
                semanticId = Cfir2ChirIds.moduleId(file),
                name = Cfir2ChirIds.moduleName(file),
                declarations = emptyList(),
            ),
        )
        file.declarations.forEach(::registerDeclarationHeaders)
    }

    fun convertFile(file: CfirFile): ChirModule {
        val declarations = file.declarations.map(::convertDeclaration)
        return ChirModule(
            semanticId = Cfir2ChirIds.moduleId(file),
            name = Cfir2ChirIds.moduleName(file),
            declarations = declarations,
        )
    }

    private fun registerDeclarationHeaders(declaration: CfirDeclaration) {
        when (declaration) {
            is CfirNamedFunction -> registerFunctionHeader(declaration)
            is CfirMainFunction -> registerFunctionHeader(declaration)
            is CfirClass -> declaration.declarations.forEach(::registerDeclarationHeaders)
            is CfirStruct -> declaration.declarations.forEach(::registerDeclarationHeaders)
            is CfirEnum -> declaration.declarations.forEach(::registerDeclarationHeaders)
            else -> Unit
        }
    }

    private fun registerFunctionHeader(function: CfirFunction) {
        val functionId = Cfir2ChirIds.callableId(function)
        val parameters = function.valueParameters.map { parameter ->
            convertValueParameter(functionId, parameter)
        }
        val header = ChirFunctionHeader(
            semanticId = functionId,
            name = function.nameForChir(),
            returnType = typeMapper.mapTypeRef(function.returnTypeRef),
            parameters = parameters,
        )
        storage.registerFunctionHeader(function.symbol, header)
    }

    private fun convertValueParameter(
        ownerFunctionId: org.cangnova.cangjie.chir.core.identity.ChirSemanticId,
        parameter: CfirValueParameter,
    ): ChirVariableDeclaration {
        val declaration = DefaultChirVariableDeclaration(
            semanticId = Cfir2ChirIds.parameterId(parameter),
            name = parameter.name.asString(),
            type = typeMapper.mapTypeRef(parameter.returnTypeRef),
            mutable = parameter.isVar,
        )
        storage.registerParameter(
            parameter.symbol,
            ChirParameterHeader(
                declaration = declaration,
                ownerFunctionId = ownerFunctionId,
            ),
        )
        return declaration
    }

    private fun convertDeclaration(declaration: CfirDeclaration): ChirDeclaration {
        return when (declaration) {
            is CfirNamedFunction -> convertFunction(declaration)
            is CfirMainFunction -> convertFunction(declaration)
            is CfirProperty -> DefaultChirPropertyDeclaration(
                semanticId = Cfir2ChirIds.declarationId(declaration.symbol),
                name = declaration.name.asString(),
                type = typeMapper.mapTypeRef(declaration.returnTypeRef),
                mutable = declaration.setter != null,
            )
            is CfirFieldVariable -> DefaultChirVariableDeclaration(
                semanticId = Cfir2ChirIds.declarationId(declaration.symbol),
                name = declaration.name.asString(),
                type = typeMapper.mapTypeRef(declaration.returnTypeRef),
                mutable = declaration.isVar,
            )
            is CfirClass -> DefaultChirClassDeclaration(
                semanticId = Cfir2ChirIds.declarationId(declaration.symbol),
                name = declaration.name.asString(),
                typeParameters = declaration.typeParameters.map { it.name.asString() },
                superTypes = declaration.superTypeRefs.map(typeMapper::mapTypeRef),
                memberDeclarations = declaration.declarations.map(::convertDeclaration),
            )
            is CfirStruct -> {
                val members = declaration.declarations.map(::convertDeclaration)
                DefaultChirStructDeclaration(
                    semanticId = Cfir2ChirIds.declarationId(declaration.symbol),
                    name = declaration.name.asString(),
                    typeParameters = declaration.typeParameters.map { it.name.asString() },
                    fieldDeclarations = members.filterIsInstance<ChirVariableDeclaration>(),
                    memberDeclarations = members,
                )
            }
            is CfirEnum -> DefaultChirEnumDeclaration(
                semanticId = Cfir2ChirIds.declarationId(declaration.symbol),
                name = declaration.name.asString(),
                typeParameters = declaration.typeParameters.map { it.name.asString() },
                memberDeclarations = declaration.declarations.map(::convertDeclaration),
            )
            else -> throw Cfir2ChirConversionException(
                "unsupported top-level CFIR declaration for CHIR lowering: ${declaration::class.qualifiedName}",
                declaration,
            )
        }
    }

    private fun convertFunction(function: CfirFunction): DefaultChirFunctionDeclaration {
        val header = storage.getFunctionHeader(function.symbol)
        val declaration = Cfir2ChirFunctionBodyConverter(
            components = components,
            packageName = packageName,
            function = function,
            header = header,
        ).convert()
        storage.registerFunction(function.symbol, declaration)
        return declaration
    }

    private fun CfirFunction.nameForChir(): String {
        return when (this) {
            is CfirNamedFunction -> name.asString()
            is CfirMainFunction -> "main"
            else -> throw Cfir2ChirConversionException("unsupported CFIR function kind for CHIR header: ${this::class.qualifiedName}", this)
        }
    }
}
