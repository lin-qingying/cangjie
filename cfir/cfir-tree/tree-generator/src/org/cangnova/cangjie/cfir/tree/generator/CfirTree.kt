/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.cfir.tree.generator

import org.cangnova.cangjie.cfir.source.CjSourceElement
import org.cangnova.cangjie.cfir.tree.generator.context.AbstractCfirTreeBuilder
import org.cangnova.cangjie.cfir.tree.generator.model.Element
import org.cangnova.cangjie.cfir.tree.generator.model.Element.Kind.*
import org.cangnova.cangjie.cfir.tree.generator.util.generatedType
import org.cangnova.cangjie.cfir.tree.generator.util.type
import org.cangnova.cangjie.generators.tree.ImplementationKind
import org.cangnova.cangjie.generators.tree.TypeKind
import org.cangnova.cangjie.generators.tree.TypeRef as TreeTypeRef
import org.cangnova.cangjie.generators.tree.withArgs

object CfirTree : AbstractCfirTreeBuilder() {
    val sourceElementType = type<CjSourceElement>()

    private val moduleDataType = type("common", "CfirModuleData")
    private val declarationOriginType = generatedType("declarations", "CfirDeclarationOrigin", TypeKind.Class)
    private val declarationAttributesType = generatedType("declarations", "CfirDeclarationAttributes", TypeKind.Class)
    private val declarationStatusType = generatedType("declarations", "CfirDeclarationStatus", TypeKind.Interface)
    private val visibilityType = type("org.cangnova.cangjie.descriptors", "Visibility", exactPackage = true, kind = TypeKind.Class)
    private val modalityType = type("org.cangnova.cangjie.descriptors", "Modality", exactPackage = true, kind = TypeKind.Class)
    private val resolvePhaseType = generatedType("declarations", "CfirResolvePhase", TypeKind.Class)
    private val symbolType = type("symbols", "CfirSymbol").withArgs(TreeTypeRef.Star)
    private val coneTypeType = type("types", "ConeCangjieType")
    private val nameType = type("org.cangnova.cangjie.name", "Name", exactPackage = true, kind = TypeKind.Class)
    private val fqNameType = type("org.cangnova.cangjie.name", "FqName", exactPackage = true, kind = TypeKind.Class)
    private val classKindType = generatedType("declarations", "CfirClassKind", TypeKind.Class)
    private val literalKindType = generatedType("expressions", "CfirLiteralKind", TypeKind.Class)
    private val binaryOpKindType = generatedType("expressions", "CfirBinaryOpKind", TypeKind.Class)
    private val comparisonOpType = generatedType("expressions", "CfirComparisonOp", TypeKind.Class)
    private val typeOperationKindType = generatedType("expressions", "CfirTypeOperationKind", TypeKind.Class)
    private val jumpKindType = generatedType("expressions", "CfirJumpKind", TypeKind.Class)
    private val stringType = type("kotlin", "String", exactPackage = true, kind = TypeKind.Class)
    private val booleanType = type("kotlin", "Boolean", exactPackage = true, kind = TypeKind.Class)
    private val anyType = type("kotlin", "Any", exactPackage = true, kind = TypeKind.Class)

    override val rootElement: Element by element(Other, name = "Element") {
        kind = ImplementationKind.Interface
        hasAcceptChildrenMethod = true
        hasTransformChildrenMethod = true
        +field("source", sourceElementType, nullable = true)
    }

    val packageDirective: Element by element(Declaration, name = "PackageDirective") {
        parent(rootElement)
        +field("packageFqName", fqNameType)
    }

    val importDirective: Element by element(Declaration, name = "Import") {
        parent(rootElement)
        +field("importedFqName", fqNameType)
        +field("isAllUnder", booleanType)
        +field("aliasName", nameType, nullable = true)
    }

    val annotation: Element by element(Declaration, name = "Annotation") {
        parent(rootElement)
        +field("typeRef", typeRef, withTransform = true)
        +listField("arguments", rootElement, withTransform = true)
    }

    val statement: Element by element(Expression, name = "Statement") {
        kind = ImplementationKind.Interface
        parent(rootElement)
    }

    val declaration: Element by sealedElement(Declaration) {
        parent(rootElement)
        parent(statement)
        +field("symbol", symbolType)
        +field("origin", declarationOriginType)
        +listField("annotations", annotation, withReplace = true, withTransform = true)
        +field("moduleData", moduleDataType)
        +field("resolvePhase", resolvePhaseType, withReplace = true)
        +field("attributes", declarationAttributesType)
    }

    val memberDeclaration: Element by sealedElement(Declaration, name = "MemberDeclaration") {
        parent(declaration)
    }

    val callableDeclaration: Element by sealedElement(Declaration, name = "CallableDeclaration") {
        parent(memberDeclaration)
    }

    val classLikeDeclaration: Element by sealedElement(Declaration, name = "ClassLikeDeclaration") {
        parent(memberDeclaration)
    }

    val file: Element by element(Declaration, name = "File") {
        parent(declaration)
        +field("name", stringType)
        +field("packageDirective", packageDirective, withTransform = true)
        +listField("imports", importDirective, withTransform = true)
        +listField("declarations", declaration, withTransform = true)
    }
    val classDeclaration: Element by element(Declaration, name = "Class") {
        parent(classLikeDeclaration)
        +field("status", declarationStatusType, withReplace = true, withTransform = true)
        +listField("typeParameters", typeParameter, withTransform = true)
        +listField("superTypeRefs", typeRef, withTransform = true)
        +listField("declarations", declaration, withTransform = true)
        +field("name", nameType)
        +field("classKind", classKindType)
    }
    val enumConstructor: Element by element(Declaration, name = "EnumConstructor") {
        parent(callableDeclaration)
        +field("status", declarationStatusType, withReplace = true, withTransform = true)
        +listField("typeParameters", typeParameter, withTransform = true)
        +field("returnTypeRef", typeRef, withReplace = true, withTransform = true)
        +field("name", nameType)
    }
    val extend: Element by element(Declaration, name = "Extend") {
        parent(classLikeDeclaration)
        +field("status", declarationStatusType, withReplace = true, withTransform = true)
        +listField("typeParameters", typeParameter, withTransform = true)
        +field("extendedTypeRef", typeRef, withTransform = true)
        +listField("superTypeRefs", typeRef, withTransform = true)
        +listField("declarations", declaration, withTransform = true)
    }
    val typeAlias: Element by element(Declaration, name = "TypeAlias") {
        parent(classLikeDeclaration)
        +field("status", declarationStatusType, withReplace = true, withTransform = true)
        +listField("typeParameters", typeParameter, withTransform = true)
        +field("name", nameType)
        +field("expandedTypeRef", typeRef, withReplace = true, withTransform = true)
    }
    val function: Element by element(Declaration, name = "Function") {
        parent(callableDeclaration)
        +field("status", declarationStatusType, withReplace = true, withTransform = true)
        +listField("typeParameters", typeParameter, withTransform = true)
        +field("returnTypeRef", typeRef, withReplace = true, withTransform = true)
        +field("name", nameType)
        +listField("valueParameters", valueParameter, withTransform = true)
        +field("body", block, nullable = true, withTransform = true)
        +field("isMut", booleanType)
    }
    val mainFunction: Element by element(Declaration, name = "MainFunction") {
        parent(callableDeclaration)
        +field("status", declarationStatusType, withReplace = true, withTransform = true)
        +listField("typeParameters", typeParameter, withTransform = true)
        +field("returnTypeRef", typeRef, withReplace = true, withTransform = true)
        +listField("valueParameters", valueParameter, withTransform = true)
        +field("body", block, nullable = true, withTransform = true)
    }
    val macroDeclaration: Element by element(Declaration, name = "MacroDeclaration") {
        parent(callableDeclaration)
        +field("status", declarationStatusType, withReplace = true, withTransform = true)
        +listField("typeParameters", typeParameter, withTransform = true)
        +field("returnTypeRef", typeRef, withReplace = true, withTransform = true)
        +field("name", nameType)
        +listField("valueParameters", valueParameter, withTransform = true)
        +field("body", block, nullable = true, withTransform = true)
    }
    val finalizer: Element by element(Declaration, name = "Finalizer") {
        parent(callableDeclaration)
        +field("status", declarationStatusType, withReplace = true, withTransform = true)
        +listField("typeParameters", typeParameter, withTransform = true)
        +field("returnTypeRef", typeRef, withReplace = true, withTransform = true)
        +listField("valueParameters", valueParameter, withTransform = true)
        +field("body", block, nullable = true, withTransform = true)
    }
    val constructor: Element by element(Declaration, name = "Constructor") {
        parent(callableDeclaration)
        +field("status", declarationStatusType, withReplace = true, withTransform = true)
        +listField("typeParameters", typeParameter, withTransform = true)
        +field("returnTypeRef", typeRef, withReplace = true, withTransform = true)
        +listField("valueParameters", valueParameter, withTransform = true)
        +field("body", block, nullable = true, withTransform = true)
        +field("isPrimary", booleanType)
    }
    val invalidDeclaration: Element by element(Declaration, name = "InvalidDeclaration") {
        parent(declaration)
        +field("reason", stringType)
    }
    val property: Element by element(Declaration, name = "Property") {
        parent(callableDeclaration)
        +field("status", declarationStatusType, withReplace = true, withTransform = true)
        +listField("typeParameters", typeParameter, withTransform = true)
        +field("returnTypeRef", typeRef, withReplace = true, withTransform = true)
        +field("name", nameType)
        +field("initializer", expression, nullable = true, withTransform = true)
        +field("getter", function, nullable = true, withTransform = true)
        +field("setter", function, nullable = true, withTransform = true)
        +field("isVar", booleanType)
    }
    val variable: Element by element(Declaration, name = "Variable") {
        parent(callableDeclaration)
        +field("status", declarationStatusType, withReplace = true, withTransform = true)
        +listField("typeParameters", typeParameter, withTransform = true)
        +field("returnTypeRef", typeRef, withReplace = true, withTransform = true)
        +field("name", nameType)
        +field("initializer", expression, nullable = true, withTransform = true)
        +field("isVar", booleanType)
    }
    val patternVariable: Element by element(Declaration, name = "PatternVariable") {
        parent(callableDeclaration)
        +field("status", declarationStatusType, withReplace = true, withTransform = true)
        +listField("typeParameters", typeParameter, withTransform = true)
        +field("returnTypeRef", typeRef, withReplace = true, withTransform = true)
        +field("pattern", pattern, withTransform = true)
        +field("initializer", expression, nullable = true, withTransform = true)
        +field("isVar", booleanType)
    }
    val valueParameter: Element by element(Declaration, name = "ValueParameter") {
        parent(callableDeclaration)
        +field("status", declarationStatusType, withReplace = true, withTransform = true)
        +listField("typeParameters", typeParameter, withTransform = true)
        +field("returnTypeRef", typeRef, withReplace = true, withTransform = true)
        +field("name", nameType)
        +field("defaultValue", expression, nullable = true, withTransform = true)
    }
    val typeParameter: Element by element(Declaration, name = "TypeParameter") {
        parent(declaration)
        +field("name", nameType)
        +listField("bounds", typeRef, withTransform = true)
    }

    val declarationStatus: Element by element(Declaration, name = "DeclarationStatus") {
        kind = ImplementationKind.Interface
        +field("visibility", visibilityType)
        +field("modality", modalityType, nullable = true)
        generateBooleanFields(
            "override",
            "operator",
            "static",
            "const",
            "mut",
            "unsafe",
            "foreign",
            "common",
            "specific",
            "redef",
            "abstract",
            "open",
            "sealed",
        )
    }

    val expression: Element by sealedElement(Expression) {
        parent(rootElement)
        parent(statement)
        +field("coneTypeOrNull", coneTypeType, nullable = true, withReplace = true)
    }

    val block: Element by element(Expression, name = "Block") {
        parent(expression)
        +listField("statements", rootElement, withTransform = true)
    }
    val literalExpression: Element by element(Expression, name = "LiteralExpression") {
        parent(expression)
        +field("kind", literalKindType)
        +field("value", anyType, nullable = true)
    }
    val stringInterpolation: Element by element(Expression, name = "StringInterpolation") {
        parent(expression)
        +listField("parts", expression, withTransform = true)
    }
    val functionCall: Element by element(Expression, name = "FunctionCall") {
        parent(expression)
        +field("calleeReference", reference, withTransform = true)
        +field("explicitReceiver", expression, nullable = true, withTransform = true)
        +listField("arguments", expression, withTransform = true)
        +listField("typeArguments", typeRef, withTransform = true)
    }
    val propertyAccess: Element by element(Expression, name = "PropertyAccess") {
        parent(expression)
        +field("calleeReference", reference, withTransform = true)
        +field("explicitReceiver", expression, nullable = true, withTransform = true)
    }
    val qualifiedAccess: Element by element(Expression, name = "QualifiedAccess") {
        parent(expression)
        +field("calleeReference", reference, withTransform = true)
        +field("explicitReceiver", expression, nullable = true, withTransform = true)
        +listField("typeArguments", typeRef, withTransform = true)
    }
    val assignment: Element by element(Expression, name = "Assignment") {
        parent(expression)
        +field("lValue", expression, withTransform = true)
        +field("rValue", expression, withTransform = true)
    }
    val binaryOp: Element by element(Expression, name = "BinaryOp") {
        parent(expression)
        +field("kind", binaryOpKindType)
        +field("left", expression, withTransform = true)
        +field("right", expression, withTransform = true)
    }
    val comparisonExpression: Element by element(Expression, name = "ComparisonExpression") {
        parent(expression)
        +field("operation", comparisonOpType)
        +field("left", expression, withTransform = true)
        +field("right", expression, withTransform = true)
    }
    val typeOperator: Element by element(Expression, name = "TypeOperator") {
        parent(expression)
        +field("operation", typeOperationKindType)
        +field("argument", expression, withTransform = true)
        +field("typeRef", typeRef, withTransform = true)
    }
    val ifExpression: Element by element(Expression, name = "IfExpression") {
        parent(expression)
        +field("condition", expression, withTransform = true)
        +field("thenBranch", block, withTransform = true)
        +field("elseBranch", expression, nullable = true, withTransform = true)
    }
    val matchExpression: Element by element(Expression, name = "MatchExpression") {
        parent(expression)
        +field("subject", expression, withTransform = true)
        +listField("branches", matchBranch, withTransform = true)
    }
    val matchBranch: Element by element(Expression, name = "MatchBranch") {
        parent(expression)
        +field("pattern", pattern, withTransform = true)
        +field("guard", expression, nullable = true, withTransform = true)
        +field("body", block, withTransform = true)
    }
    val catchClause: Element by element(Expression, name = "Catch") {
        parent(expression)
        +field("parameter", valueParameter, withTransform = true)
        +field("body", block, withTransform = true)
    }
    val loopExpression: Element by element(Expression, name = "LoopExpression") {
        parent(expression)
        +field("condition", expression, withTransform = true)
        +field("body", block, withTransform = true)
        +field("isDoWhile", booleanType)
    }
    val forInExpression: Element by element(Expression, name = "ForInExpression") {
        parent(loopExpression)
        +field("variable", variable, withTransform = true)
        +field("iterable", expression, withTransform = true)
        +field("body", block, withTransform = true)
    }
    val tryExpression: Element by element(Expression, name = "TryExpression") {
        parent(expression)
        +field("tryBlock", block, withTransform = true)
        +listField("catches", catchClause, withTransform = true)
        +field("finallyBlock", block, nullable = true, withTransform = true)
    }
    val throwExpression: Element by element(Expression, name = "ThrowExpression") {
        parent(expression)
        +field("exception", expression, withTransform = true)
    }
    val returnExpression: Element by element(Expression, name = "ReturnExpression") {
        parent(expression)
        +field("result", expression, nullable = true, withTransform = true)
    }
    val jumpExpression: Element by element(Expression, name = "JumpExpression") {
        parent(expression)
        +field("kind", jumpKindType)
    }
    val lambdaExpression: Element by element(Expression, name = "LambdaExpression") {
        parent(expression)
        +field("anonymousFunction", function, withTransform = true)
    }
    val rangeExpression: Element by element(Expression, name = "RangeExpression") {
        parent(expression)
        +field("start", expression, withTransform = true)
        +field("end", expression, withTransform = true)
        +field("isInclusive", booleanType)
    }
    val arrayLiteral: Element by element(Expression, name = "ArrayLiteral") {
        parent(expression)
        +listField("elements", expression, withTransform = true)
    }
    val tupleLiteral: Element by element(Expression, name = "TupleLiteral") {
        parent(expression)
        +listField("elements", expression, withTransform = true)
    }
    val spawnExpression: Element by element(Expression, name = "SpawnExpression") {
        parent(expression)
        +field("body", block, withTransform = true)
    }
    val subscriptExpression: Element by element(Expression, name = "SubscriptExpression") {
        parent(expression)
        +field("receiver", expression, withTransform = true)
        +listField("indices", expression, withTransform = true)
    }
    val errorExpression: Element by element(Expression, name = "ErrorExpression") {
        parent(expression)
        +field("reason", stringType)
    }

    val pattern: Element by sealedElement(Pattern) {
        parent(rootElement)
    }

    val constPattern: Element by element(Pattern, name = "ConstPattern") {
        parent(pattern)
        +field("expression", expression, withTransform = true)
    }
    val wildcardPattern: Element by element(Pattern, name = "WildcardPattern") { parent(pattern) }
    val bindingPattern: Element by element(Pattern, name = "BindingPattern") {
        parent(pattern)
        +field("name", nameType)
        +field("typeRef", typeRef, nullable = true, withTransform = true)
        +field("nestedPattern", pattern, nullable = true, withTransform = true)
    }
    val tuplePattern: Element by element(Pattern, name = "TuplePattern") {
        parent(pattern)
        +listField("elements", pattern, withTransform = true)
    }
    val enumPattern: Element by element(Pattern, name = "EnumPattern") {
        parent(pattern)
        +field("constructorReference", reference, withTransform = true)
        +listField("arguments", pattern, withTransform = true)
    }
    val typePattern: Element by element(Pattern, name = "TypePattern") {
        parent(pattern)
        +field("typeRef", typeRef, withTransform = true)
        +field("bindingName", nameType, nullable = true)
    }

    val typeRef: Element by sealedElement(TypeRef) {
        parent(rootElement)
    }

    val resolvedTypeRef: Element by element(TypeRef, name = "ResolvedTypeRef") {
        parent(typeRef)
        +field("coneType", coneTypeType)
    }
    val userTypeRef: Element by element(TypeRef, name = "UserTypeRef") {
        parent(typeRef)
        +listField("qualifier", nameType)
        +listField("typeArguments", typeRef, withTransform = true)
    }
    val basicTypeRef: Element by element(TypeRef, name = "BasicTypeRef") {
        parent(typeRef)
        +field("name", nameType)
    }
    val implicitTypeRef: Element by element(TypeRef, name = "ImplicitTypeRef") { parent(typeRef) }
    val functionTypeRef: Element by element(TypeRef, name = "FunctionTypeRef") {
        parent(typeRef)
        +listField("parameterTypeRefs", typeRef, withTransform = true)
        +field("returnTypeRef", typeRef, withTransform = true)
    }
    val tupleTypeRef: Element by element(TypeRef, name = "TupleTypeRef") {
        parent(typeRef)
        +listField("elementTypeRefs", typeRef, withTransform = true)
    }
    val varrayTypeRef: Element by element(TypeRef, name = "VArrayTypeRef") {
        parent(typeRef)
        +field("elementTypeRef", typeRef, withTransform = true)
        +field("sizeLiteral", stringType)
    }
    val errorTypeRef: Element by element(TypeRef, name = "ErrorTypeRef") {
        parent(typeRef)
        +field("reason", stringType)
    }

    val reference: Element by sealedElement(Reference) {
        parent(rootElement)
    }

    val namedReference: Element by element(Reference, name = "NamedReference") {
        parent(reference)
        +field("name", nameType)
    }
    val resolvedNamedReference: Element by element(Reference, name = "ResolvedNamedReference") {
        parent(reference)
        +field("name", nameType)
        +field("resolvedSymbol", symbolType)
    }
    val errorReference: Element by element(Reference, name = "ErrorReference") {
        parent(reference)
        +field("reason", stringType)
    }
}
