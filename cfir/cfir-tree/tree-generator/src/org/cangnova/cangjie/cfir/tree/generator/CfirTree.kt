package org.cangnova.cangjie.cfir.tree.generator

import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.cfir.tree.generator.context.AbstractCfirTreeBuilder
import org.cangnova.cangjie.cfir.tree.generator.model.Element
import org.cangnova.cangjie.cfir.tree.generator.model.Element.Kind.*
import org.cangnova.cangjie.cfir.tree.generator.model.fieldSet
import org.cangnova.cangjie.cfir.tree.generator.util.generatedType
import org.cangnova.cangjie.cfir.tree.generator.util.type
import org.cangnova.cangjie.generators.tree.AbstractField
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
    private val visibilityType =
        type("org.cangnova.cangjie.descriptors", "Visibility", exactPackage = true, kind = TypeKind.Class)
    private val modalityType =
        type("org.cangnova.cangjie.descriptors", "Modality", exactPackage = true, kind = TypeKind.Class)
    private val resolvePhaseType = generatedType("declarations", "CfirResolvePhase", TypeKind.Class)
    private val resolveStateType = type("declarations", "CfirResolveState", kind = TypeKind.Class)
    private val resolveStateAccessType = type("declarations", "ResolveStateAccess", kind = TypeKind.Class)
    private val symbolType = type("symbols", "CfirSymbol").withArgs(TreeTypeRef.Star)
    val classSymbolType = type("symbols", "CfirClassSymbol")

    private val coneTypeType = type("types", "ConeCangJieType")
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
    private val sourceFileType =
        type("org.cangnova.cangjie", "CjSourceFile", exactPackage = true, kind = TypeKind.Interface)

    private object FieldSets {
        val typeArguments = fieldSet(
            listField("typeArguments", typeRef, useMutableOrEmpty = true, withReplace = true, withTransform = true)
        )
        val declarations = fieldSet(
            listField("declarations", declaration, withTransform = true) {
                useInBaseTransformerDetection = false
            }
        )
        val typeParameters = fieldSet(
            listField("typeParameters", typeParameter, withTransform = true)
        )
        val annotations = fieldSet(
            listField("annotations", annotation, withReplace = true, useMutableOrEmpty = true, withTransform = true) {
                needTransformInOtherChildren = true
            }
        )
    }

    override val rootElement: Element by element(Other, name = "Element") {
        kind = ImplementationKind.Interface
        hasAcceptChildrenMethod = true
        hasTransformChildrenMethod = true
        +field("source", sourceElementType, nullable = true)
    }

    /**
     * 支持懒加载解析的元素基类。
     *
     * 包含 resolveState 状态机，用于管理 lazy resolve。
     */
    val elementWithResolveState: Element by element(Other, name = "ElementWithResolveState") {
        kind = ImplementationKind.AbstractClass
        parent(rootElement)
        +field("moduleData", moduleDataType)
        // resolveState: 懒加载解析的核心状态字段
        // isFinal = true → 在抽象类中生成为具体字段（非 abstract），子类实现中不重复声明
        // isVolatile → @Volatile（并发安全的内存可见性）
        // Lateinit → lateinit var（延迟初始化，由 Builder 在 build() 中设置）
        // ResolveStateAccess 注解 → 限制直接访问，需 @OptIn
        +field("resolveState", resolveStateType, isChild = false) {
            isFinal = true
            isVolatile = true
            isMutable = true
            implementationDefaultStrategy = AbstractField.ImplementationDefaultStrategy.Lateinit
            additionalAnnotations.add(resolveStateAccessType)
        }
    }

    // expression、typeRef、statement 都继承此接口，统一"可被注解"的语义边界
    val annotationContainer: Element by element(Other) {
        kind = ImplementationKind.Interface
        +FieldSets.annotations
    }

    val controlFlowGraphOwner: Element by element(Declaration) {
        +field("controlFlowGraphReference", controlFlowGraphReference, withReplace = true, nullable = true)
    }

    /**
     * 可解析节点接口（对应 Kotlin FIR 中的 FirResolvable）。
     *
     * 所有持有 calleeReference 的节点都应实现此接口，以便：
     * 1. 解析阶段可以统一替换 calleeReference（NamedReference → ResolvedNamedReference）
     * 2. Visitor/Transformer 可以通过 visitResolvable / transformResolvable 统一处理
     * 3. 避免在 functionCall、propertyAccess、qualifiedAccess 等节点中重复定义相同字段
     *
     * 典型解析流程：
     *   NamedReference("foo") → resolve() → ResolvedNamedReference("foo", symbol = FooSymbol)
     */
    val resolvable: Element by element(Other, name = "Resolvable") {
        kind = ImplementationKind.Interface
        parent(rootElement)
        // withReplace = true  → 生成 replaceCalleeReference()，解析阶段替换引用时使用
        // withTransform = true → 生成 transformCalleeReference()，Transformer 遍历时使用
        +field("calleeReference", reference, withReplace = true, withTransform = true)
    }

    val packageDirective: Element by element(Declaration, name = "PackageDirective") {
        parent(rootElement)
        +field("packageFqName", fqNameType)
    }

    // 改为抽象基类，不直接实例化
    val importDirective: Element by element(Declaration, name = "Import") {
        kind = ImplementationKind.AbstractClass
        parent(rootElement)
        +field("importedFqName", fqNameType, nullable = true)  // 解析前可能为 null
        +field("isAllUnder", booleanType)
        +field("aliasName", nameType, nullable = true)
        +field("aliasSource", sourceElementType, nullable = true)  // 新增，对应 FIR
    }

    // 语义分析后的导入节点，内部持有原始 importDirective 作为 delegate
    val resolvedImportDirective: Element by element(Declaration, name = "ResolvedImport") {
        parent(importDirective)
        +field("delegate", importDirective, isChild = false)  // 指向原始节点，不作为子节点遍历
        +field("packageFqName", fqNameType)
        +field("importedName", nameType, nullable = true)     // 具体导入的名称（非 * 时有值）
    }

    val annotation: Element by element(Declaration, name = "Annotation") {
        parent(rootElement)
        +field("typeRef", typeRef, withTransform = true)
        +listField("arguments", rootElement, withTransform = true)
    }

    val statement: Element by element(Expression, name = "Statement") {
        kind = ImplementationKind.Interface
        parent(rootElement)
        parent(annotationContainer)
    }

    val declaration: Element by sealedElement(Declaration) {
        parent(elementWithResolveState)
        parent(statement)
        +field("symbol", symbolType)
        +field("origin", declarationOriginType)
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
        parent(controlFlowGraphOwner)
        +field("name", stringType)
        +field("sourceFile", sourceFileType, nullable = true)
        +field("packageDirective", packageDirective, withTransform = true)
        +listField("imports", importDirective, withTransform = true)
        +FieldSets.declarations
    }

    val classDeclaration: Element by element(Declaration, name = "Class") {
        parent(classLikeDeclaration)
        parent(controlFlowGraphOwner)
        +field("status", declarationStatusType, withReplace = true, withTransform = true)
        +FieldSets.typeParameters
        +field("symbol", classSymbolType)

        +listField("superTypeRefs", typeRef, withTransform = true)
        +FieldSets.declarations
        +field("name", nameType)
        +field("classKind", classKindType)
    }

    val enumConstructor: Element by element(Declaration, name = "EnumConstructor") {
        parent(callableDeclaration)
        +field("status", declarationStatusType, withReplace = true, withTransform = true)
        +FieldSets.typeParameters
        +field("returnTypeRef", typeRef, withReplace = true, withTransform = true)
        +field("name", nameType)
    }

    val extend: Element by element(Declaration, name = "Extend") {
        parent(classLikeDeclaration)
        +field("status", declarationStatusType, withReplace = true, withTransform = true)
        +FieldSets.typeParameters
        +field("extendedTypeRef", typeRef, withTransform = true)
        +listField("superTypeRefs", typeRef, withTransform = true)
        +FieldSets.declarations
    }

    val typeAlias: Element by element(Declaration, name = "TypeAlias") {
        parent(classLikeDeclaration)
        +field("status", declarationStatusType, withReplace = true, withTransform = true)
        +FieldSets.typeParameters
        +field("name", nameType)
        +field("expandedTypeRef", typeRef, withReplace = true, withTransform = true)
    }

    val function: Element by element(Declaration, name = "Function") {
        parent(callableDeclaration)
        parent(controlFlowGraphOwner)
        customParentInVisitor = callableDeclaration
        +field("status", declarationStatusType, withReplace = true, withTransform = true)
        +FieldSets.typeParameters
        +field("returnTypeRef", typeRef, withReplace = true, withTransform = true)
        +field("name", nameType)
        +listField("valueParameters", valueParameter, withTransform = true)
        +field("body", block, nullable = true, withTransform = true)
        +field("isMut", booleanType)
    }

    val mainFunction: Element by element(Declaration, name = "MainFunction") {
        parent(callableDeclaration)
        parent(controlFlowGraphOwner)
        customParentInVisitor = callableDeclaration
        +field("status", declarationStatusType, withReplace = true, withTransform = true)
        +FieldSets.typeParameters
        +field("returnTypeRef", typeRef, withReplace = true, withTransform = true)
        +listField("valueParameters", valueParameter, withTransform = true)
        +field("body", block, nullable = true, withTransform = true)
    }

    val macroDeclaration: Element by element(Declaration, name = "MacroDeclaration") {
        parent(callableDeclaration)
        parent(controlFlowGraphOwner)
        customParentInVisitor = callableDeclaration
        +field("status", declarationStatusType, withReplace = true, withTransform = true)
        +FieldSets.typeParameters
        +field("returnTypeRef", typeRef, withReplace = true, withTransform = true)
        +field("name", nameType)
        +listField("valueParameters", valueParameter, withTransform = true)
        +field("body", block, nullable = true, withTransform = true)
    }

    val finalizer: Element by element(Declaration, name = "Finalizer") {
        parent(callableDeclaration)
        parent(controlFlowGraphOwner)
        customParentInVisitor = callableDeclaration
        +field("status", declarationStatusType, withReplace = true, withTransform = true)
        +FieldSets.typeParameters
        +field("returnTypeRef", typeRef, withReplace = true, withTransform = true)
        +listField("valueParameters", valueParameter, withTransform = true)
        +field("body", block, nullable = true, withTransform = true)
    }

    val constructor: Element by element(Declaration, name = "Constructor") {
        parent(callableDeclaration)
        parent(controlFlowGraphOwner)
        customParentInVisitor = callableDeclaration
        +field("status", declarationStatusType, withReplace = true, withTransform = true)
        +FieldSets.typeParameters
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
        parent(controlFlowGraphOwner)
        customParentInVisitor = callableDeclaration
        +field("status", declarationStatusType, withReplace = true, withTransform = true)
        +FieldSets.typeParameters
        +field("returnTypeRef", typeRef, withReplace = true, withTransform = true)
        +field("name", nameType)

        +field("getter", function, nullable = true, withTransform = true)
        +field("setter", function, nullable = true, withTransform = true)
    }
    val variable: Element by element(Declaration, name = "Variable") {
        parent(callableDeclaration)

        +field("status", declarationStatusType, withReplace = true, withTransform = true)

        +field("initializer", expression, nullable = true, withTransform = true)
        +field("isVar", booleanType)

    }
    val fieldVariable: Element by element(Declaration, name = "FieldVariable") {
        parent(variable)
        +FieldSets.typeParameters
        +field("returnTypeRef", typeRef, withReplace = true, withTransform = true)
        +field("name", nameType)
    }

    val patternVariable: Element by element(Declaration, name = "PatternVariable") {
        parent(variable)
        +FieldSets.typeParameters
        +field("returnTypeRef", typeRef, withReplace = true, withTransform = true)
        +field("pattern", pattern, withTransform = true)

    }

    val valueParameter: Element by element(Declaration, name = "ValueParameter") {
        parent(callableDeclaration)
        +field("status", declarationStatusType, withReplace = true, withTransform = true)
        +FieldSets.typeParameters
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
            "visibilityExplicit",
            "modalityExplicit",
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
        parent(statement)
        +field("coneTypeOrNull", coneTypeType, nullable = true, withReplace = true)
    }

    val block: Element by element(Expression, name = "Block") {
        parent(expression)
        +listField("statements", rootElement, withTransform = true)
    }

    val lazyBlock: Element by element(Expression, name = "LazyBlock") {
        parent(block)
    }

    val lazyExpression: Element by element(Expression, name = "LazyExpression") {
        parent(expression)
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

    /**
     * 函数调用表达式。
     *
     * 继承 resolvable，calleeReference 由父接口提供，无需重复声明。
     * 解析阶段会将 calleeReference 从 NamedReference 替换为 ResolvedNamedReference。
     */
    val functionCall: Element by element(Expression, name = "FunctionCall") {
        parent(expression)
        parent(resolvable)  // calleeReference 来自 resolvable，不在此重复声明
        +field("explicitReceiver", expression, nullable = true, withTransform = true)
        +listField("arguments", expression, withTransform = true)
        +FieldSets.typeArguments
    }

    /**
     * 属性访问表达式（不带类型参数的成员访问）。
     *
     * 继承 resolvable，calleeReference 由父接口提供。
     */
    val propertyAccess: Element by element(Expression, name = "PropertyAccess") {
        parent(expression)
        parent(resolvable)  // calleeReference 来自 resolvable
        +field("explicitReceiver", expression, nullable = true, withTransform = true)
    }

    /**
     * 带类型参数的限定访问表达式（如 foo<T>、Foo.bar<T>）。
     *
     * 继承 resolvable，calleeReference 由父接口提供。
     */
    val qualifiedAccess: Element by element(Expression, name = "QualifiedAccess") {
        parent(expression)
        parent(resolvable)  // calleeReference 来自 resolvable
        +field("explicitReceiver", expression, nullable = true, withTransform = true)
        +FieldSets.typeArguments
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
        +field("subject", expression, withTransform = true, nullable = true)
        +listField("branches", matchBranch, withTransform = true)
    }

    val orPattern: Element by element(Pattern, name = "OrPattern") {
        parent(pattern)
        +listField("alternatives", pattern, withTransform = true)
    }

    val expressionPattern: Element by element(Pattern, name = "ExpressionPattern") {
        parent(pattern)
        +field("expression", expression, withTransform = true)
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
        +field("variable", patternVariable, withTransform = true)
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

    val synchronizedExpression: Element by element(Expression, name = "SynchronizedExpression") {
        parent(expression)
        +field("monitor", expression, withTransform = true)
        +field("body", block, withTransform = true)
    }

    val unsafeExpression: Element by element(Expression, name = "UnsafeExpression") {
        parent(expression)
        +field("body", expression, withTransform = true)
    }

    val quoteExpression: Element by element(Expression, name = "QuoteExpression") {
        parent(expression)
        +field("rawText", stringType)
        +listField("interpolations", expression, withTransform = true)
    }

    val macroExpression: Element by element(Expression, name = "MacroExpression") {
        parent(expression)
        +field("name", nameType, nullable = true)
        +field("inputText", stringType, nullable = true)
        +field("attrText", stringType, nullable = true)
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
        parent(annotationContainer)
    }

    val resolvedTypeRef: Element by element(TypeRef, name = "ResolvedTypeRef") {
        parent(typeRef)
        +field("coneType", coneTypeType)
        +field("delegatedTypeRef", typeRef, nullable = true, isChild = false)
    }

    val userTypeRef: Element by element(TypeRef, name = "UserTypeRef") {
        parent(typeRef)
        +listField("qualifier", nameType)
        +FieldSets.typeArguments
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

    val controlFlowGraphReference: Element by element(Reference) {
        parent(reference)
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
