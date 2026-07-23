package org.cangnova.cangjie.cfir.tree.generator

import org.cangnova.cangjie.cfir.tree.generator.util.generatedType
import org.cangnova.cangjie.cfir.tree.generator.util.type
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeSimpleCangJieType
import org.cangnova.cangjie.generators.tree.TypeKind
import org.cangnova.cangjie.generators.tree.imports.ArbitraryImportable
import org.cangnova.cangjie.source.CjSourceFileLinesMapping

/**
 * 生成的泛型 CFIR visitor 类型引用。
 */
val cfirVisitorType = generatedType("visitors", "CfirVisitor")
/**
 * 生成的无返回值 CFIR visitor 类型引用。
 */
val cfirVisitorVoidType = generatedType("visitors", "CfirVisitorVoid")
/**
 * 生成的默认泛型 CFIR visitor 类型引用。
 */
val cfirDefaultVisitorType = generatedType("visitors", "CfirDefaultVisitor")
/**
 * 生成的默认无返回值 CFIR visitor 类型引用。
 */
val cfirDefaultVisitorVoidType = generatedType("visitors", "CfirDefaultVisitorVoid")
/**
 * 生成的 CFIR transformer 类型引用。
 */
val cfirTransformerType = generatedType("visitors", "CfirTransformer")
/**
 * 源文件行映射类型引用。
 */
val sourceFileLinesMappingType = type<CjSourceFileLinesMapping>()
/**
 * 函数调用来源枚举类型引用。
 */
val functionCallOrigin = type("expressions", "CfirFunctionCallOrigin")
/**
 * match 穷尽性状态类型引用。
 */
val matchExhaustivenessStatusType = type("expressions", "CfirMatchExhaustivenessStatus")
/**
 * 普通赋值类型不匹配语义结果类型引用。
 */
val assignmentTypeMismatchOutcomeType = type("expressions", "CfirAssignmentTypeMismatchOutcome")
/**
 * 错误函数符号类型引用。
 */
val errorFunctionSymbolType = type("symbols", "CfirErrorFunctionSymbol")
/**
 * 错误具名值符号类型引用。
 */
val errorNamedValueSymbolType = type("symbols", "CfirErrorNamedValueSymbol")
/**
 * 空参数列表类型引用。
 */
val emptyArgumentListType = type("expressions", "CfirEmptyArgumentList")
/**
 * 模式绑定变量符号类型引用。
 */
val patternBindingVariableSymbolType = type("symbols", "CfirPatternBindingSymbol")

/**
 * CFIR 根元素类型引用。
 */
val cfirElementType = generatedType("CfirElement")
/**
 * CFIR 纯抽象元素类型引用。
 */
val pureAbstractElementType = generatedType("CfirPureAbstractElement")
/**
 * CFIR 内部实现细节注解类型引用。
 */
val cfirImplementationDetailType = generatedType("CfirImplementationDetail", kind = TypeKind.Class)
/**
 * CFIR renderer 类型引用。
 */
val cfirRendererType = type("renderer", "CfirRenderer")
/**
 * CFIR builder DSL 注解类型引用。
 */
val cfirBuilderDslAnnotation = generatedType("builder", "CfirBuilderDsl", kind = TypeKind.Class)
/**
 * cone 诊断接口类型引用。
 */
val coneDiagnosticType = generatedType("types", "ConeDiagnostic", kind = TypeKind.Interface)
/**
 * cone 错误类型引用。
 */
val coneErrorTypeType = type<ConeErrorType>()
/**
 * 未报告重复诊断类型引用。
 */
val coneUnreportedDuplicateDiagnosticType = generatedType("types", "ConeUnreportedDuplicateDiagnostic")
/**
 * CFIR 跳转目标接口类型引用。
 */
val jumpTargetType = type(BASE_PACKAGE, "CfirTarget", exactPackage = true, kind = TypeKind.Interface)
/**
 * CFIR 符号基类类型引用。
 */
val cfirSymbolType = type("symbols", "CfirBasedSymbol")
/**
 * 拥有 this 的 CFIR 符号类型引用。
 */
val cfirThisOwnerSymbolType = type("symbols", "CfirThisOwnerSymbol")
/**
 * coneTypeOrNull 扩展导入引用。
 */
val coneTypeOrNull = type("types","coneTypeOrNull")
/**
 * cone 简单类型类型引用。
 */
val coneSimpleCangJieTypeType = type<ConeSimpleCangJieType>()
/**
 * 错误类型引用实现类型引用。
 */
val errorTypeRefImplType = type("types.impl", "CfirErrorTypeRefImpl")
/**
 * 弃用信息 provider 类型引用。
 */
val deprecationsProviderType = type("declarations", "DeprecationsProvider")
/**
 * 无 status 声明默认 status 常量引用。
 */
val defaultStatusForStatuslessDeclarationsType = type("declarations", "DEFAULT_STATUS_FOR_STATUSLESS_DECLARATIONS")
/**
 * property body resolve 状态类型引用。
 */
val propertyBodyResolveStateType = type("declarations", "CfirPropertyBodyResolveState")
/**
 * 未解析弃用信息 provider 类型引用。
 */
val unresolvedDeprecationsProviderType = type("declarations", "UnresolvedDeprecationProvider")
/**
 * MutableOrEmptyList 转换扩展导入引用。
 */
val toMutableOrEmptyImport = type(BASE_PACKAGE, "toMutableOrEmpty",exactPackage = true)
/**
 * transformInplace 扩展导入引用。
 */
val transformInPlaceImport = ArbitraryImportable(VISITOR_PACKAGE, "transformInplace")
