package org.cangnova.cangjie.chir.cfir2chir

import org.cangnova.cangjie.chir.core.declaration.DefaultChirClassDeclaration
import org.cangnova.cangjie.chir.core.declaration.DefaultChirEnumDeclaration
import org.cangnova.cangjie.chir.core.declaration.DefaultChirExtendDeclaration
import org.cangnova.cangjie.chir.core.declaration.DefaultChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.declaration.DefaultChirPropertyDeclaration
import org.cangnova.cangjie.chir.core.declaration.DefaultChirStructDeclaration
import org.cangnova.cangjie.chir.core.declaration.DefaultChirTypeDeclaration
import org.cangnova.cangjie.chir.core.declaration.DefaultChirVariableDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirVariableDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirDeclaration as ChirDeclaration
import org.cangnova.cangjie.chir.core.attribute.ChirStringAttribute
import org.cangnova.cangjie.chir.core.controlflow.ChirBlock
import org.cangnova.cangjie.chir.core.controlflow.ChirReturnTerminator
import org.cangnova.cangjie.chir.core.expression.ChirOtherExpression
import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.chir.core.model.ChirModule
import org.cangnova.cangjie.chir.core.type.ChirPrimitiveType
import org.cangnova.cangjie.chir.core.type.ChirResolvedTypeRef
import org.cangnova.cangjie.chir.core.value.ChirLocalValue
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirCodeFragment
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration as CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirErrorFunction
import org.cangnova.cangjie.cfir.declarations.CfirErrorNamedValue
import org.cangnova.cangjie.cfir.declarations.CfirErrorPrimaryConstructor
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirFinalizer
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirInvalidDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirMacroDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirPrimitiveTypeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirPropertyAccessor
import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.declarations.CfirVariable

/**
 * 负责单个 CFIR 文件的声明 header 登记和顶层声明转换。
 */
internal class Cfir2ChirFileConverter(
    /**
     * 本次 package 转换共享的组件集合。
     */
    private val components: Cfir2ChirComponents,
    /**
     * 当前转换所属的包名。
     */
    private val packageName: String,
) {
    /**
     * 共享声明存储，负责跨文件/跨函数查询 CHIR header。
     */
    private val storage: Cfir2ChirDeclarationStorage = components.declarationStorage

    /**
     * 共享类型映射器。
     */
    private val typeMapper: Cfir2ChirTypeMapper = components.typeMapper

    /**
     * 注册文件 module 骨架以及该文件内所有可引用声明 header。
     */
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

    /**
     * 将已经登记 header 的 CFIR 文件转换为完整 CHIR module。
     */
    fun convertFile(file: CfirFile): ChirModule {
        val declarations = file.declarations.map(::convertDeclaration)
        return ChirModule(
            semanticId = Cfir2ChirIds.moduleId(file),
            name = Cfir2ChirIds.moduleName(file),
            declarations = declarations,
        )
    }

    /**
     * 递归登记声明 header，保证函数体转换前所有可调用/变量符号已经可查。
     */
    private fun registerDeclarationHeaders(declaration: CfirDeclaration) {
        when (declaration) {
            is CfirFunction -> registerFunctionHeader(declaration)
            is CfirEnumConstructor -> registerEnumConstructorHeader(declaration)
            is CfirProperty -> {
                registerVariableHeader(declaration)
                declaration.getter?.let(::registerDeclarationHeaders)
                declaration.setter?.let(::registerDeclarationHeaders)
            }
            is CfirVariable -> registerVariableHeader(declaration)
            is CfirCodeFragment -> Unit
            is CfirClassLikeDeclaration -> declaration.declarations.forEach(::registerDeclarationHeaders)
            is CfirExtend -> declaration.declarations.forEach(::registerDeclarationHeaders)
            else -> Unit
        }
    }

    /**
     * 登记普通 CFIR 函数的 CHIR 函数 header。
     */
    internal fun registerFunctionHeader(function: CfirFunction) {
        if (storage.hasFunctionHeader(function.symbol)) return
        val functionId = Cfir2ChirIds.callableId(function)
        val parameters = function.valueParameters.map { parameter ->
            convertValueParameter(functionId, parameter)
        }
        val header = ChirFunctionHeader(
            semanticId = functionId,
            name = function.nameForChir(),
            returnType = typeMapper.mapTypeRef(function.returnTypeRef),
            parameters = parameters,
            receiverType = function.dispatchReceiverType?.let(typeMapper::mapConeTypeRef),
        )
        storage.registerFunctionHeader(function.symbol, header)
    }

    /**
     * 登记 enum constructor 的 CHIR 函数 header。
     */
    private fun registerEnumConstructorHeader(constructor: CfirEnumConstructor) {
        if (storage.hasFunctionHeader(constructor.symbol)) return
        val functionId = Cfir2ChirIds.declarationId(constructor.symbol)
        val parameters = constructor.valueParameters.map { parameter ->
            convertValueParameter(functionId, parameter)
        }
        storage.registerFunctionHeader(
            constructor.symbol,
            ChirFunctionHeader(
                semanticId = functionId,
                name = constructor.name.asString(),
                returnType = typeMapper.mapTypeRef(constructor.returnTypeRef),
                parameters = parameters,
                receiverType = constructor.dispatchReceiverType?.let(typeMapper::mapConeTypeRef),
            ),
        )
    }

    /**
     * 登记属性、字段或局部变量的 CHIR 变量 header。
     */
    private fun registerVariableHeader(variable: CfirCallableDeclaration) {
        val declaration = when (variable) {
            is CfirProperty -> variable.toChirPropertyDeclaration()
            is CfirVariable -> variable.toChirVariableDeclaration()
            is CfirErrorNamedValue -> throw Cfir2ChirConversionException(
                "error CFIR named value cannot be lowered to CHIR: ${variable.diagnostic.reason}",
                variable,
            )
            else -> return
        }
        storage.registerVariable(variable.symbol, ChirVariableHeader(declaration))
    }

    /**
     * 将 CFIR 值参数转换为 CHIR 变量声明并登记参数 header。
     */
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

    /**
     * 将单个 CFIR 声明转换为 CHIR 声明。
     */
    private fun convertDeclaration(declaration: CfirDeclaration): ChirDeclaration {
        return when (declaration) {
            is CfirErrorFunction -> throw Cfir2ChirConversionException(
                "error CFIR function cannot be lowered to CHIR: ${declaration.diagnostic.reason}",
                declaration,
            )
            is CfirErrorPrimaryConstructor -> throw Cfir2ChirConversionException(
                "error CFIR primary constructor cannot be lowered to CHIR: ${declaration.diagnostic.reason}",
                declaration,
            )
            is CfirFunction -> convertFunction(declaration)
            is CfirEnumConstructor -> convertEnumConstructor(declaration)
            is CfirProperty -> declaration.toChirPropertyDeclaration()
            is CfirVariable -> declaration.toChirVariableDeclaration()
            is CfirCodeFragment -> convertCodeFragment(declaration)
            is CfirPrimitiveTypeDeclaration -> DefaultChirTypeDeclaration(
                semanticId = Cfir2ChirIds.declarationId(declaration.symbol),
                name = declaration.name.asString(),
                typeParameters = declaration.typeParameters.map { it.symbol.name.asString() },
                attributes = setOf(
                    ChirStringAttribute("cfir.kind", "primitiveType"),
                    ChirStringAttribute("cfir.primitive.kind", declaration.kind.name),
                ),
            )
            is CfirInterface -> DefaultChirClassDeclaration(
                semanticId = Cfir2ChirIds.declarationId(declaration.symbol),
                name = declaration.name.asString(),
                typeParameters = declaration.typeParameters.map { it.name.asString() },
                implementedTypes = declaration.superTypeRefs.map(typeMapper::mapTypeRef),
                memberDeclarations = declaration.declarations.mapNotNull(::convertMemberDeclaration),
                attributes = setOf(ChirStringAttribute("cfir.kind", "interface")),
            )
            is org.cangnova.cangjie.cfir.declarations.CfirClass -> DefaultChirClassDeclaration(
                semanticId = Cfir2ChirIds.declarationId(declaration.symbol),
                name = declaration.name.asString(),
                typeParameters = declaration.typeParameters.map { it.name.asString() },
                superTypes = declaration.superTypeRefs.map(typeMapper::mapTypeRef),
                memberDeclarations = declaration.declarations.mapNotNull(::convertMemberDeclaration),
            )
            is CfirStruct -> {
                val members = declaration.declarations.mapNotNull(::convertMemberDeclaration)
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
                cases = declaration.declarations.filterIsInstance<CfirEnumConstructor>().map { it.name.asString() },
                memberDeclarations = declaration.declarations.mapNotNull(::convertMemberDeclaration),
                attributes = setOf(ChirStringAttribute("cfir.enum.kind", if (declaration.isRefEnum) "ref" else "value")),
            )
            is CfirExtend -> DefaultChirExtendDeclaration(
                semanticId = Cfir2ChirIds.declarationId(declaration.symbol),
                name = declaration.extendedTypeRef.renderNameForDeclaration(),
                targetType = typeMapper.mapTypeRef(declaration.extendedTypeRef),
                extendedTypes = declaration.superTypeRefs.map(typeMapper::mapTypeRef),
                typeParameters = declaration.typeParameters.map { it.name.asString() },
                memberDeclarations = declaration.declarations.mapNotNull(::convertMemberDeclaration),
            )
            is CfirTypeAlias -> DefaultChirTypeDeclaration(
                semanticId = Cfir2ChirIds.declarationId(declaration.symbol),
                name = declaration.name.asString(),
                typeParameters = declaration.typeParameters.map { it.name.asString() },
                attributes = setOf(ChirStringAttribute("cfir.expandedType", declaration.expandedTypeRef.renderNameForDeclaration())),
            )
            is CfirTypeParameter -> DefaultChirTypeDeclaration(
                semanticId = Cfir2ChirIds.declarationId(declaration.symbol),
                name = declaration.name.asString(),
                typeParameters = emptyList(),
                attributes = declaration.bounds.mapIndexedTo(linkedSetOf()) { index, bound ->
                    ChirStringAttribute("cfir.bound.$index", bound.renderNameForDeclaration())
                },
            )
            is CfirInvalidDeclaration -> throw Cfir2ChirConversionException(
                "invalid CFIR declaration cannot be lowered to CHIR: ${declaration.reason}",
                declaration,
            )
            else -> throw Cfir2ChirConversionException(
                "unsupported top-level CFIR declaration for CHIR lowering: ${declaration::class.qualifiedName}",
                declaration,
            )
        }
    }

    /**
     * 转换 class-like 或 extend 的成员声明，enum constructor 已在 enum case 列表中表达。
     */
    private fun convertMemberDeclaration(declaration: CfirDeclaration): ChirDeclaration? =
        if (declaration is CfirEnumConstructor) null else convertDeclaration(declaration)

    /**
     * 将 CFIR 函数及其 body 转换为 CHIR 函数声明。
     */
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

    /**
     * 将 CFIR code fragment 转换为合成 CHIR 函数声明。
     */
    private fun convertCodeFragment(codeFragment: CfirCodeFragment): DefaultChirFunctionDeclaration {
        val returnType = codeFragment.block.coneTypeOrNull?.let(typeMapper::mapConeTypeRef)
            ?: throw Cfir2ChirConversionException("code fragment block must carry resolved Cone type before CHIR lowering", codeFragment.block)
        val header = ChirFunctionHeader(
            semanticId = Cfir2ChirIds.declarationId(codeFragment.symbol),
            name = "<code-fragment>",
            returnType = returnType,
            parameters = emptyList(),
        )
        return Cfir2ChirFunctionBodyConverter(
            components = components,
            packageName = packageName,
            header = header,
            body = codeFragment.block,
            bodySource = codeFragment,
        ).convert().copy(
            attributes = setOf(ChirStringAttribute("cfir.kind", "codeFragment")),
        )
    }

    /**
     * 为不同 CFIR 函数形态生成 CHIR 函数名。
     */
    private fun CfirFunction.nameForChir(): String {
        return when (this) {
            is org.cangnova.cangjie.cfir.declarations.CfirNamedFunction -> name.asString()
            is org.cangnova.cangjie.cfir.declarations.CfirMainFunction -> "main"
            is CfirMacroDeclaration -> name.asString()
            is CfirFinalizer -> "finalize"
            is CfirConstructor -> if (isPrimary) "<init>" else symbol.name.asString()
            is CfirPropertyAccessor -> {
                val accessorName = if (isGetter) "get" else "set"
                "$accessorName:${propertySymbol.callableId.callableName.asString()}"
            }
            is org.cangnova.cangjie.cfir.declarations.CfirAnonymousFunction -> "anonymous"
            else -> symbol.name.asString()
        }
    }

    /**
     * 将 CFIR 变量声明转换为 CHIR 变量声明。
     */
    private fun CfirVariable.toChirVariableDeclaration(): DefaultChirVariableDeclaration =
        DefaultChirVariableDeclaration(
            semanticId = Cfir2ChirIds.declarationId(symbol),
            name = when (this) {
                is CfirFieldVariable -> name.asString()
                is org.cangnova.cangjie.cfir.declarations.CfirPatternBindingVariable -> name.asString()
                is org.cangnova.cangjie.cfir.declarations.CfirPatternVariable -> pattern::class.simpleName ?: "<pattern>"
                is CfirValueParameter -> symbol.name.asString()
            },
            type = typeMapper.mapTypeRef(returnTypeRef),
            mutable = isVar,
        )

    /**
     * 将 CFIR property 转换为 CHIR property 声明。
     */
    private fun CfirProperty.toChirPropertyDeclaration(): DefaultChirPropertyDeclaration =
        DefaultChirPropertyDeclaration(
            semanticId = Cfir2ChirIds.declarationId(symbol),
            name = name.asString(),
            type = typeMapper.mapTypeRef(returnTypeRef),
            mutable = setter != null,
        )

    /**
     * 将 enum constructor 转换为返回 enum case value 的 CHIR 函数。
     */
    private fun convertEnumConstructor(constructor: CfirEnumConstructor): DefaultChirFunctionDeclaration {
        val header = storage.getFunctionHeader(constructor.symbol)
        val expressionId = Cfir2ChirIds.generatedId("enum_ctor", header.semanticId, 0)
        val resultValue = ChirLocalValue(
            semanticId = expressionId,
            type = header.returnType,
            name = expressionId.value,
        )
        val expression = ChirOtherExpression(
            semanticId = expressionId,
            operation = Cfir2ChirOperation.CFIR_ENUM_CONSTRUCTOR.canonicalName,
            operands = header.parameters.map { parameter ->
                org.cangnova.cangjie.chir.core.value.ChirParameterValue(
                    semanticId = parameter.semanticId,
                    type = parameter.type,
                    name = parameter.name,
                    ownerFunctionId = header.semanticId,
                )
            },
            resultType = header.returnType,
            attributes = setOf(ChirStringAttribute("cfir.enumConstructor", constructor.name.asString())),
        )
        return DefaultChirFunctionDeclaration(
            semanticId = header.semanticId,
            name = header.name,
            returnType = header.returnType,
            parameters = header.parameters,
            blocks = listOf(
                ChirBlock(
                    semanticId = Cfir2ChirIds.generatedId("entry", header.semanticId, 1),
                    name = "entry",
                    expressions = listOf(expression),
                    terminator = ChirReturnTerminator(
                        semanticId = Cfir2ChirIds.generatedId("return", header.semanticId, 2),
                        returnValue = resultValue,
                    ),
                ),
            ),
            entryBlockId = Cfir2ChirIds.generatedId("entry", header.semanticId, 1),
            attributes = setOf(ChirStringAttribute("cfir.kind", "enumConstructor")),
        )
    }

    /**
     * 将 CFIR 类型引用渲染为声明属性中可读的类型名称。
     */
    private fun org.cangnova.cangjie.cfir.types.CfirTypeRef.renderNameForDeclaration(): String =
        typeMapper.mapTypeRef(this).renderName
}
