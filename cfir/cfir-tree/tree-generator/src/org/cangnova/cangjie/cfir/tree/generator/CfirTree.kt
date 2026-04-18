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
    private val symbolType = type("symbols", "CfirBasedSymbol").withArgs(TreeTypeRef.Star)

    // ---- 分类器符号类型 ----
    val classSymbolType = type("symbols", "CfirClassSymbol")
    val interfaceSymbolType = type("symbols", "CfirInterfaceSymbol")
    val structSymbolType = type("symbols", "CfirStructSymbol")
    val enumSymbolType = type("symbols", "CfirEnumSymbol")
    val classLikeSymbolType = type("symbols", "CfirClassLikeSymbol").withArgs(TreeTypeRef.Star)
//    val cfirClassifierSymbolWithClassId = type("symbols", "CfirClassifierSymbolWithClassId").withArgs(TreeTypeRef.Star)

    val typeAliasSymbolType = type("symbols", "CfirTypeAliasSymbol")
    val typeParameterSymbolType = type("symbols", "CfirTypeParameterSymbol")

    // ---- 可调用符号类型 ----
    val callableSymbolType = type("symbols", "CfirCallableSymbol").withArgs(TreeTypeRef.Star)
    val functionSymbolType = type("symbols", "CfirFunctionSymbol").withArgs(TreeTypeRef.Star)
    val namedFunctionSymbolType = type("symbols", "CfirNamedFunctionSymbol")
    val anonymousFunctionSymbolType = type("symbols", "CfirAnonymousFunctionSymbol")
    val mainFunctionSymbolType = type("symbols", "CfirMainFunctionSymbol")
    val finalizerSymbolType = type("symbols", "CfirFinalizerSymbol")
    val constructorSymbolType = type("symbols", "CfirConstructorSymbol")
    val macroDeclarationSymbolType = type("symbols", "CfirMacroDeclarationSymbol")
    val propertySymbolType = type("symbols", "CfirPropertySymbol")
    val variableSymbolType = type("symbols", "CfirVariableSymbol").withArgs(TreeTypeRef.Star)
    val valueParameterSymbolType = type("symbols", "CfirValueParameterSymbol")
    val fieldVariableSymbolType = type("symbols", "CfirFieldVariableSymbol")
    val patternVariableSymbolType = type("symbols", "CfirPatternVariableSymbol")
    val patternBindingVariableSymbolType = type("symbols", "CfirPatternBindingSymbol")
    val enumConstructorSymbolType = type("symbols", "CfirEnumConstructorSymbol")
    val nameValueSymbolType = type("symbols", "CfirNamedValueSymbol")

    // ---- 其他符号类型 ----
    val fileSymbolType = type("symbols", "CfirFileSymbol")
    val extendSymbolType = type("symbols", "CfirExtendSymbol")

    private val coneTypeType = type("types", "ConeCangJieType")
    private val nameType = type("org.cangnova.cangjie.name", "Name", exactPackage = true, kind = TypeKind.Class)
    private val fqNameType = type("org.cangnova.cangjie.name", "FqName", exactPackage = true, kind = TypeKind.Class)

    // classKindType 已删除：各具名类型由独立节点承载，不再需要运行时 kind 区分
    private val literalKindType = generatedType("expressions", "CfirLiteralKind", TypeKind.Class)
    private val binaryOpKindType = generatedType("expressions", "CfirBinaryOpKind", TypeKind.Class)
    private val comparisonOpType = generatedType("expressions", "CfirComparisonOp", TypeKind.Class)
    private val typeOperationKindType = generatedType("expressions", "CfirTypeOperationKind", TypeKind.Class)
    private val stringType = type("kotlin", "String", exactPackage = true, kind = TypeKind.Class)
    private val booleanType = type("kotlin", "Boolean", exactPackage = true, kind = TypeKind.Class)
    private val anyType = type("kotlin", "Any", exactPackage = true, kind = TypeKind.Class)
    private val sourceFileType =
        type("org.cangnova.cangjie", "CjSourceFile", exactPackage = true, kind = TypeKind.Interface)

    private object FieldSets {
        val typeArguments = fieldSet(
            listField("typeArguments", typeRef, useMutableOrEmpty = true, withReplace = true, withTransform = true)
        )
        val name = fieldSet(field(nameType))

        val declarations = fieldSet(
            listField("declarations", declaration, withTransform = true) {
                useInBaseTransformerDetection = false
            })
        val typeParameters = fieldSet(
            listField(
                "typeParameters",
                typeParameter
            )
        )

        val annotations = fieldSet(
            listField("annotations", annotation, withReplace = true, useMutableOrEmpty = true, withTransform = true) {
                needTransformInOtherChildren = true
            })
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
    val elementWithResolveState: Element by element(Other ) {
        kind = ImplementationKind.AbstractClass
        +field("moduleData", moduleDataType)
        +field("resolvePhase", resolvePhaseType) { isParameter = true; }

        +field("resolveState", resolveStateType) {
            isMutable = true; isVolatile = true; isFinal = true
            implementationDefaultStrategy = AbstractField.ImplementationDefaultStrategy.Lateinit
            customInitializationCall = "resolvePhase.asResolveState()"
            arbitraryImportables += phaseAsResolveStateExtentionImport
            optInAnnotation = resolveStateAccessType
        }
    }

    val annotationContainer: Element by element(Other) {
        kind = ImplementationKind.Interface
        +FieldSets.annotations
    }

    val controlFlowGraphOwner: Element by element(Declaration) {
        +field("controlFlowGraphReference", controlFlowGraphReference, withReplace = true, nullable = true)
    }

    val wrappedExpression: Element by element(Expression) {
        parent(expression)

        +field(expression)
    }

    /**
     * 可解析节点接口。
     *
     * 所有持有 calleeReference 的节点都应实现此接口，以便：
     * 1. 解析阶段可以统一替换 calleeReference（NamedReference → ResolvedNamedReference）
     * 2. Visitor/Transformer 可以通过 visitResolvable / transformResolvable 统一处理
     * 3. 避免在 functionCall、propertyAccess、qualifiedAccessExpression 等节点中重复定义相同字段
     */
    val resolvable: Element by sealedElement(Expression ) {


        +field("calleeReference", reference, withReplace = true, withTransform = true)
    }

    val packageDirective: Element by element(Declaration, name = "PackageDirective") {
        parent(rootElement)
        +field("packageFqName", fqNameType)
    }

    val importDirective: Element by element(Declaration, name = "Import") {
        kind = ImplementationKind.AbstractClass
        parent(rootElement)
        +field("importedFqName", fqNameType, nullable = true)
        +field("isAllUnder", booleanType)
        +field("aliasName", nameType, nullable = true)
        +field("aliasSource", sourceElementType, nullable = true)
    }

    val resolvedImportDirective: Element by element(Declaration, name = "ResolvedImport") {
        parent(importDirective)
        +field("delegate", importDirective, isChild = false)
        +field("packageFqName", fqNameType)
        +field("importedName", nameType, nullable = true)
    }

    val annotation: Element by element(Expression, name = "Annotation") {
        parent(expression)

        +field("typeRef", typeRef, withTransform = true)
        +listField("arguments", rootElement, withTransform = true)
    }

    val statement: Element by element(Expression ) {

        parent(annotationContainer)
    }
    val typeParameterRef: Element by element(Declaration) {
        +referencedSymbol(typeParameterSymbolType)
    }
    val declaration: Element by sealedElement(Declaration) {
        kind = ImplementationKind.AbstractClass

        parent(elementWithResolveState)
        parent(annotationContainer)

        +declaredSymbol(symbolType)
        +field("origin", declarationOriginType)
        +field("attributes", declarationAttributesType)
    }
    val typeParameterRefsOwner: Element by sealedElement(Declaration) {
        +listField("typeParameters", typeParameterRef, withTransform = true)
    }
    val memberDeclaration: Element by sealedElement(Declaration, name = "MemberDeclaration") {
        parent(declaration)

        parent(typeParameterRefsOwner)

        +field(
            "status",
            declarationStatus, withReplace = true, withTransform = true
        )
    }

    val callableDeclaration: Element by sealedElement(Declaration, name = "CallableDeclaration") {
        parent(memberDeclaration)
        +declaredSymbol(callableSymbolType)
        +field("isLocal", boolean)
        +field(
            "returnTypeRef",
            typeRef, withReplace = true, withTransform = true
        )

        +field("dispatchReceiverType", coneSimpleCangJieTypeType, nullable = true)
        +referencedSymbol(callableSymbolType.withArgs(callableDeclaration))

    }

    val classLikeDeclaration: Element by sealedElement(Declaration, name = "ClassLikeDeclaration") {
        parent(memberDeclaration)
        +declaredSymbol(classLikeSymbolType)
        +FieldSets.declarations
        +field("name", nameType)

        +listField("superTypeRefs", typeRef, withTransform = true)

    }

    val file: Element by element(Declaration, name = "File") {
        parent(declaration)
        parent(controlFlowGraphOwner)
        +declaredSymbol(fileSymbolType)
        +field("name", stringType)
        +field("sourceFile", sourceFileType, nullable = true)
        +field("packageDirective", packageDirective, withTransform = true)
        +listField("imports", importDirective, withTransform = true)
        +field("sourceFileLinesMapping", sourceFileLinesMappingType, nullable = true)

        +FieldSets.declarations
    }

    /**
     * class 声明节点，对应仓颉中的顶层引用语义具名类型。
     *
     * class 成员只包含构造器、函数、属性和字段变量。
     * 这里的成员列表只描述当前 class 的直接成员，不承载额外的类型声明层级。
     */
    val classDeclaration: Element by element(Declaration, name = "Class") {
        parent(classLikeDeclaration)
        parent(controlFlowGraphOwner)
        +field("status", declarationStatusType, withReplace = true, withTransform = true)
        +FieldSets.typeParameters
        +declaredSymbol(classSymbolType)
        +listField("superTypeRefs", typeRef, withTransform = true)
        +FieldSets.declarations
        +field("name", nameType)
        // classKind 已删除：节点类型本身即为区分依据
    }

    /**
     * interface 声明节点。
     *
     * 语义限制：
     * - 不能包含构造器（constructor）
     * - 不能包含字段变量（fieldVariable）
     * - 成员只能是属性（property）和方法（function），均可为抽象或带默认实现
     *
     * 这里统一使用 declarations 作为接口成员的唯一树形存储。
     * interface 专属的“属性/函数分类”只能是派生视图，不能再作为并行子节点列表，
     * 否则 visitor/transformer/renderer 会把同一成员遍历多次，破坏整棵 CFIR 树的单一子节点语义。
     */
    val interfaceDeclaration: Element by element(Declaration, name = "Interface") {
        parent(classLikeDeclaration)
        parent(controlFlowGraphOwner)
        +field("status", declarationStatusType, withReplace = true, withTransform = true)
        +FieldSets.typeParameters
        +declaredSymbol(interfaceSymbolType)
        +listField("superTypeRefs", typeRef, withTransform = true)
        +FieldSets.declarations
        +field("name", nameType)
    }

    /**
     * struct 声明节点，对应仓颉中值语义的具名类型。
     *
     * 赋值时复制整个结构体而非共享引用。
     * 与 class 的区别体现在类型系统的值语义处理上。
     */
    val structDeclaration: Element by element(Declaration, name = "Struct") {
        parent(classLikeDeclaration)
        parent(controlFlowGraphOwner)
        +field("status", declarationStatusType, withReplace = true, withTransform = true)
        +FieldSets.typeParameters
        +declaredSymbol(structSymbolType)
        +listField("superTypeRefs", typeRef, withTransform = true)
        +FieldSets.declarations
        +field("name", nameType)
    }

    /**
     * enum 声明节点，对应仓颉中的代数数据类型枚举。
     *
     * 支持带参数的构造器（ADT 风格）。
     * isRefEnum 区分值枚举（EnumTy）和引用枚举（RefEnumTy）。
     */
    val enumDeclaration: Element by element(Declaration, name = "Enum") {
        parent(classLikeDeclaration)
        parent(controlFlowGraphOwner)
        +field("status", declarationStatusType, withReplace = true, withTransform = true)
        +FieldSets.typeParameters
        +declaredSymbol(enumSymbolType)
        +listField("superTypeRefs", typeRef, withTransform = true)
        +FieldSets.declarations
        +field("name", nameType)
        +field("isRefEnum", booleanType)
    }

    /**
     * enum constructor 既是可调用声明，也是 ADT payload 的唯一声明源。
     *
     * 这里显式保存 `valueParameters`，让调用解析、模式匹配、冲突检测共享同一份
     * 参数真相表，避免继续把 payload 信息折叠进 `returnTypeRef` 后再到处反推。
     */
    val enumConstructor: Element by element(Declaration, name = "EnumConstructor") {
        parent(callableDeclaration)
        +declaredSymbol(enumConstructorSymbolType)
        +field("status", declarationStatusType, withReplace = true, withTransform = true)
        +FieldSets.typeParameters
        +field("returnTypeRef", typeRef, withReplace = true, withTransform = true)
        +listField("valueParameters", valueParameter, withTransform = true)
        +field("name", nameType)
    }

    val extend: Element by element(Declaration, name = "Extend") {
        parent(memberDeclaration)
        +declaredSymbol(extendSymbolType)
        +field("status", declarationStatusType, withReplace = true, withTransform = true)
        +FieldSets.typeParameters
        +field("extendedTypeRef", typeRef, withTransform = true)
        +listField("superTypeRefs", typeRef, withTransform = true)
        +FieldSets.declarations
    }

    val typeAlias: Element by element(Declaration, name = "TypeAlias") {
        parent(classLikeDeclaration)
        +declaredSymbol(typeAliasSymbolType)
        +field("status", declarationStatusType, withReplace = true, withTransform = true)
        +FieldSets.typeParameters
        +field("name", nameType)
        +field("expandedTypeRef", typeRef, withReplace = true, withTransform = true)
    }

    val function: Element by sealedElement(Declaration, name = "Function") {
        parent(callableDeclaration)
        parent(targetElement)
        parent(controlFlowGraphOwner)
        customParentInVisitor = callableDeclaration
        +declaredSymbol(functionSymbolType)
        +field("status", declarationStatusType, withReplace = true, withTransform = true)
        +FieldSets.typeParameters
        +field("returnTypeRef", typeRef, withReplace = true, withTransform = true)
        +listField("valueParameters", valueParameter, withTransform = true)
        +field("body", block, nullable = true, withTransform = true)
    }

    val namedFunction: Element by element(Declaration, name = "NamedFunction") {
        parent(function)
        +declaredSymbol(namedFunctionSymbolType)
        +field("name", nameType)
        +field("isMut", booleanType)
    }

    val mainFunction: Element by element(Declaration, name = "MainFunction") {
        parent(function)
        +declaredSymbol(mainFunctionSymbolType)
    }

    val macroDeclaration: Element by element(Declaration, name = "MacroDeclaration") {
        parent(function)
        +declaredSymbol(macroDeclarationSymbolType)
        +field("name", nameType)
    }

    val finalizer: Element by element(Declaration, name = "Finalizer") {
        parent(function)
        +declaredSymbol(finalizerSymbolType)
    }

    val constructor: Element by element(Declaration, name = "Constructor") {
        parent(function)
        +declaredSymbol(constructorSymbolType)
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
        +declaredSymbol(propertySymbolType)
        +field("status", declarationStatusType, withReplace = true, withTransform = true)
        +FieldSets.typeParameters
        +field("returnTypeRef", typeRef, withReplace = true, withTransform = true)
        +field("name", nameType)
        +field("getter", function, nullable = true, withTransform = true)
        +field("setter", function, nullable = true, withTransform = true)
    }

    val variable: Element by sealedElement(Declaration, name = "Variable") {
        parent(callableDeclaration)
        +declaredSymbol(variableSymbolType)
        +field("status", declarationStatusType, withReplace = true, withTransform = true)
        +field("initializer", expression, nullable = true, withTransform = true)
        +field("isVar", booleanType)
    }

    val fieldVariable: Element by element(Declaration, name = "FieldVariable") {
        parent(variable)
        +declaredSymbol(fieldVariableSymbolType)
        +FieldSets.typeParameters
        +field("returnTypeRef", typeRef, withReplace = true, withTransform = true)
        +field("name", nameType)
    }

    val patternVariable: Element by element(Declaration, name = "PatternVariable") {
        parent(variable)
        +declaredSymbol(patternVariableSymbolType)
        +FieldSets.typeParameters
        +field("returnTypeRef", typeRef, withReplace = true, withTransform = true)
        +field("pattern", pattern, withTransform = true)
    }

    /**
     * 模式内部的具名绑定变量。
     *
     * 该节点对应官方 C++ `VarPattern` / 具名 `TypePattern` 中真正进入作用域的绑定，
     * 与外层 `PatternVariable` 容器分离，避免多个绑定名共用同一个 symbol。
     */
    val patternBindingVariable: Element by element(Declaration, name = "PatternBindingVariable") {
        parent(variable)
        +declaredSymbol(patternBindingVariableSymbolType)
        +FieldSets.typeParameters
        +field("returnTypeRef", typeRef, withReplace = true, withTransform = true)
        +field("name", nameType)
    }

    val valueParameter: Element by element(Declaration, name = "ValueParameter") {
        parent(variable)
        parent(controlFlowGraphOwner)
        +declaredSymbol(valueParameterSymbolType)
        +referencedSymbol("containingDeclarationSymbol", cfirSymbolType.withArgs(TreeTypeRef.Star)) {
            withBindThis = false
        }
        +field("isNamed",booleanType, withReplace = false)
        +field("status", declarationStatusType, withReplace = true, withTransform = true)
        +FieldSets.typeParameters
        +field("returnTypeRef", typeRef, withReplace = true, withTransform = true)
        +field("name", nameType)
        +field("defaultValue", expression, nullable = true, withTransform = true)
    }

    val typeParameter: Element by element(Declaration, name = "TypeParameter") {
        parent(typeParameterRef)

        parent(declaration)
        +referencedSymbol("containingDeclarationSymbol", cfirSymbolType.withArgs(TreeTypeRef.Star)) {
            withBindThis = false
        }
        +declaredSymbol(typeParameterSymbolType)
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

    val expression: Element by element(Expression) {
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
    val namedReferenceWithCandidateBase: Element by element(Reference) {
        parent(namedReference)

        +referencedSymbol("candidateSymbol", cfirSymbolType.withArgs(TreeTypeRef.Star))
    }

    /**
     * 函数调用表达式。
     *
     * 继承 resolvable，calleeReference 由父接口提供，无需重复声明。
     * 解析阶段会将 calleeReference 从 NamedReference 替换为 ResolvedNamedReference。
     */
    val functionCall: Element by element(Expression ) {
        parent(qualifiedAccessExpression)
        parent(call)

        +field("origin", functionCallOrigin)
        +field("hasTrailingLambda", boolean)

    }
    val errorNamedReference: Element by element(Reference) {
        parent(namedReference)
        parent(diagnosticHolder)
    }
    val argumentList: Element by element(Expression) {
        +listField("arguments", expression, withTransform = true)
    }
    val call: Element by sealedElement(Expression) {
        parent(statement)

        +field(argumentList, withReplace = true)
    }
    val annotationCall: Element by element(Expression) {
        parent(annotation)
        parent(call)
        parent(resolvable)

//        +field("argumentMapping", annotationArgumentMapping, withReplace = true, isChild = false)
//        +field("annotationResolvePhase", annotationResolvePhaseType, withReplace = true)
        +referencedSymbol("containingDeclarationSymbol", cfirSymbolType.withArgs(TreeTypeRef.Star)) {
            withBindThis = false
        }
    }
    val superReference: Element by element(Reference) {
        parent(reference)

        +field("superTypeRef", typeRef, withReplace = true)
    }

    val thisReference: Element by element(Reference, name = "ThisReference") {
        parent(reference)
        +referencedSymbol(
            "boundSymbol",
            cfirThisOwnerSymbolType.withArgs(TreeTypeRef.Star),
            nullable = true,
            withReplace = true
        )
        +field("isImplicit", booleanType)
        +field("diagnostic", coneDiagnosticType, nullable = true, withReplace = true)
    }

    /**
     * 名称访问表达式（不带类型参数的成员访问）。
     */
    val namedAccessExpression: Element by element(Expression ) {
        parent(qualifiedAccessExpression)

        +field("explicitReceiver", expression, nullable = true, withTransform = true)
    }

    /**
     * 带类型参数的限定访问表达式（如 foo<T>、Foo.bar<T>）。
     */
    val qualifiedAccessExpression: Element by element(Expression ) {
        parent(expression)
        parent(resolvable)
        +field("dispatchReceiver", expression, nullable = true, withReplace = true)

        +field("explicitReceiver", expression, nullable = true, withTransform = true)
        +FieldSets.typeArguments {
            withTransform = true
        }
    }

    /**
     * `super` 接收者表达式。
     *
     * 将 `super` 从普通名字访问中独立出来，
     * 以便在 body resolve 阶段统一解析当前 dispatch receiver 对应的直接父类型。
     */
    val superReceiverExpression: Element by element(Expression) {
        parent(qualifiedAccessExpression)

        +field("calleeReference", superReference)
    }
    val errorFunction: Element by element(Declaration) {
        parent(function)
        parent(diagnosticHolder)

        +declaredSymbol(errorFunctionSymbolType)
    }
    val errorNamedValue: Element by element(Declaration) {
        parent(callableDeclaration)
        parent(diagnosticHolder)

        +field("name", nameType)
        +declaredSymbol(errorNamedValueSymbolType)
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

    /**
     * effect command pattern。
     *
     * 它和 match/catch pattern 同样带有“绑定名 + 多个类型”的结构，
     * 但语义只服务于 try/handle，因此不接到通用 pattern 分派上，避免污染现有模式匹配路径。
     */
    val commandTypePattern: Element by element(Pattern, name = "CommandTypePattern") {
        parent(rootElement)
        +field("bindingName", nameType, nullable = true)
        +field("isWildcard", booleanType)
        +listField("typeRefs", typeRef, withTransform = true)
    }

    /**
     * try expression 下的 handle 子句。
     *
     * 这里保持和官方 AST 一致，作为 TryExpression 的直属分支，
     * 不把 handle 退化成 catch 或独立表达式。
     */
    val handleClause: Element by element(Expression, name = "HandleClause") {
        parent(expression)
        +field("commandPattern", commandTypePattern, withTransform = true)
        +field("body", block, withTransform = true)
    }

    val loopExpression: Element by element(Expression, name = "LoopExpression") {
        parent(expression)
        parent(targetElement)
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
        +listField("handlers", handleClause, withTransform = true)
        +listField("catches", catchClause, withTransform = true)
        +field("finallyBlock", block, nullable = true, withTransform = true)
    }

    val throwExpression: Element by element(Expression, name = "ThrowExpression") {
        parent(expression)
        +field("exception", expression, withTransform = true)
    }
    val resolvedDeclarationStatus: Element by element(Declaration) {
        kind = ImplementationKind.Interface

        parent( declarationStatus)

        +field(modalityType, nullable = false)
//        +field("effectiveVisibility", effectiveVisibilityType)
    }
    val performExpression: Element by element(Expression, name = "PerformExpression") {
        parent(expression)
        +field("expression", expression, withTransform = true)
    }

    val resumeExpression: Element by element(Expression, name = "ResumeExpression") {
        parent(expression)
        +field("withExpression", expression, nullable = true, withTransform = true)
        +field("throwingExpression", expression, nullable = true, withTransform = true)
    }

    val returnExpression: Element by element(Expression, name = "ReturnExpression") {
        needTransformOtherChildren()

        parent(jump.withArgs("E" to function))

        +field("result", expression,  withTransform = true)
    }

    /**
     * jump 基类。
     *
     * 当前先对齐 loop jump 体系，把 `break` / `continue` 从单节点枚举分派
     * 提升为独立 CFIR 节点，避免 target 已经框架化后仍然依赖 `kind` 做语义分叉。
     *
     * `return` 后续也可以继续接到这套 target 体系上，但第一步先把循环跳转拆实。
     */
    val jump: Element by sealedElement(Expression, name = "Jump") {
        val e = +param("E", targetElement)
        parent(expression)
        +field("target", jumpTargetType.withArgs(e))
    }

    /**
     * 循环跳转抽象层。
     *
     * 这里只负责携带“跳到哪个循环”的 target；具体是 `break` 还是 `continue`
     * 交由不同 concrete 节点表达，而不是再回退到枚举字段。
     */
    val loopJump: Element by sealedElement(Expression, name = "LoopJump") {
        parent(jump.withArgs("E" to loopExpression))
    }

    val breakExpression: Element by element(Expression, name = "BreakExpression") {
        parent(loopJump)
    }

    val continueExpression: Element by element(Expression, name = "ContinueExpression") {
        parent(loopJump)
    }

    /**
     * 匿名函数声明，对应仓颉中 lambda 表达式内部的函数体。
     *
     * 与普通 [function] 的区别：
     * - 无名称（始终为 `<anonymous>`）
     * - 携带 [isLambda]、[hasExplicitParameterList] 等语义标记
     * - 持有 [typeRef] 用于推断 lambda 整体类型
     * - [matchingParameterFunctionType] 记录参数推断匹配的函数类型
     */
    val anonymousFunction: Element by element(Declaration, name = "AnonymousFunction") {
        parent(function)
        +declaredSymbol(anonymousFunctionSymbolType)
        +field("hasExplicitParameterList", booleanType)
        +field("isLambda", booleanType)
        +field("typeRef", typeRef, withReplace = true)
        +field("matchingParameterFunctionType", coneTypeType, nullable = true, withReplace = true)
    }

    val resolvedErrorReference: Element by element(Reference) {
        customParentInVisitor = resolvedNamedReference

        parent(resolvedNamedReference)
        parent(diagnosticHolder)
    }

    /**
     * 匿名函数表达式，包装 [anonymousFunction] 作为表达式节点。
     *
     * 替代旧的 `CfirLambdaExpression`，增加 [isTrailingLambda] 标记。
     */
    val anonymousFunctionExpression: Element by element(Expression, name = "AnonymousFunctionExpression") {
        parent(expression)
        +field("anonymousFunction", anonymousFunction, withTransform = true, withReplace = true)
        +field("isTrailingLambda", booleanType, withReplace = true)
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

    val targetElement: Element by element(Other, name = "TargetElement") {
        kind = ImplementationKind.Interface
    }

    val spawnExpression: Element by element(Expression, name = "SpawnExpression") {
        parent(expression)
        +field("body", block, withTransform = true)
        +field("threadContextArgument", expression, nullable = true, withTransform = true)
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

    val diagnosticHolder: Element by element(Diagnostics) {
        +field("diagnostic", coneDiagnosticType)
    }

    val errorExpression: Element by element(Expression) {
        parent(expression)
        parent(diagnosticHolder)
        +field("expression", expression, nullable = true)
        +field("nonExpressionElement", rootElement, nullable = true)
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
        +field("bindingVariable", patternBindingVariable, nullable = true, withTransform = false, isChild = false)
        +field("nestedPattern", pattern, nullable = true, withTransform = true)
    }

    /**
     * 对齐官方 parser 的 `VarOrEnumPattern` 中间态。
     *
     * 语法阶段先保留裸名字的歧义信息，resolve 阶段再决定它究竟是 binding pattern
     * 还是 enum constructor pattern，避免 parser 过早降级成单一路径。
     */
    val varOrEnumPattern: Element by element(Pattern, name = "VarOrEnumPattern") {
        parent(pattern)
        +field("name", nameType)
        +field("bindingVariable", patternBindingVariable, nullable = true, withTransform = false, isChild = false)
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
        +field("bindingVariable", patternBindingVariable, nullable = true, withTransform = false, isChild = false)
    }

    val typeRef: Element by sealedElement(TypeRef) {

        parent(annotationContainer)
        +FieldSets.annotations

    }

    val resolvedTypeRef: Element by element(TypeRef, name = "ResolvedTypeRef") {
        parent(typeRef)
        +field("coneType", coneTypeType)
        +field("delegatedTypeRef", typeRef, nullable = true, isChild = false)
    }
    val unresolvedTypeRef: Element by sealedElement(TypeRef) {
        parent(typeRef)

        +field("source", sourceElementType, nullable = false)
    }

    val userTypeRef: Element by element(TypeRef, name = "UserTypeRef") {
        parent(unresolvedTypeRef)

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


    val errorTypeRef: Element by element(TypeRef) {
        parent(resolvedTypeRef)
        parent(diagnosticHolder)

        +field(
            "partiallyResolvedTypeRef",
            typeRef, nullable = true, withTransform = true
        )
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
        parent(namedReference)
        +field("resolvedSymbol", symbolType)
    }

    val errorReference: Element by element(Reference, name = "ErrorReference") {
        parent(reference)
        +field("reason", stringType)
    }
}
