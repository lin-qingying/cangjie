package org.cangnova.cangjie.cfir.tree.generator

import org.cangnova.cangjie.cfir.tree.generator.util.generatedType
import org.cangnova.cangjie.cfir.tree.generator.util.type
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeSimpleCangJieType
import org.cangnova.cangjie.generators.tree.TypeKind
import org.cangnova.cangjie.generators.tree.imports.ArbitraryImportable
import org.cangnova.cangjie.source.CjSourceFileLinesMapping

val cfirVisitorType = generatedType("visitors", "CfirVisitor")
val cfirVisitorVoidType = generatedType("visitors", "CfirVisitorVoid")
val cfirDefaultVisitorType = generatedType("visitors", "CfirDefaultVisitor")
val cfirDefaultVisitorVoidType = generatedType("visitors", "CfirDefaultVisitorVoid")
val cfirTransformerType = generatedType("visitors", "CfirTransformer")
val sourceFileLinesMappingType = type<CjSourceFileLinesMapping>()
val functionCallOrigin = type("expressions", "CfirFunctionCallOrigin")
val matchExhaustivenessStatusType = type("expressions", "CfirMatchExhaustivenessStatus")
val errorFunctionSymbolType = type("symbols", "CfirErrorFunctionSymbol")
val errorNamedValueSymbolType = type("symbols", "CfirErrorNamedValueSymbol")
val emptyArgumentListType = type("expressions", "CfirEmptyArgumentList")
val patternBindingVariableSymbolType = type("symbols", "CfirPatternBindingSymbol")

val cfirElementType = generatedType("CfirElement")
val pureAbstractElementType = generatedType("CfirPureAbstractElement")
val cfirImplementationDetailType = generatedType("CfirImplementationDetail", kind = TypeKind.Class)
val cfirRendererType = type("renderer", "CfirRenderer")
val cfirBuilderDslAnnotation = generatedType("builder", "CfirBuilderDsl", kind = TypeKind.Class)
val coneDiagnosticType = generatedType("types", "ConeDiagnostic", kind = TypeKind.Interface)
val coneErrorTypeType = type<ConeErrorType>()
val coneUnreportedDuplicateDiagnosticType = generatedType("types", "ConeUnreportedDuplicateDiagnostic")
val jumpTargetType = type(BASE_PACKAGE, "CfirTarget", exactPackage = true, kind = TypeKind.Interface)
val cfirSymbolType = type("symbols", "CfirBasedSymbol")
val cfirThisOwnerSymbolType = type("symbols", "CfirThisOwnerSymbol")
val coneTypeOrNull = type("types","coneTypeOrNull")
val coneSimpleCangJieTypeType = type<ConeSimpleCangJieType>()
val errorTypeRefImplType = type("types.impl", "CfirErrorTypeRefImpl")
val deprecationsProviderType = type("declarations", "DeprecationsProvider")
val defaultStatusForStatuslessDeclarationsType = type("declarations", "DEFAULT_STATUS_FOR_STATUSLESS_DECLARATIONS")
val propertyBodyResolveStateType = type("declarations", "CfirPropertyBodyResolveState")
val unresolvedDeprecationsProviderType = type("declarations", "UnresolvedDeprecationProvider")
val toMutableOrEmptyImport = type(BASE_PACKAGE, "toMutableOrEmpty",exactPackage = true)
val transformInPlaceImport = ArbitraryImportable(VISITOR_PACKAGE, "transformInplace")
