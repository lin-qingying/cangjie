/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.cfir.tree.generator

import org.cangnova.cangjie.cfir.tree.generator.CfirTree.anonymousFunction
import org.cangnova.cangjie.cfir.tree.generator.CfirTree.function
import org.cangnova.cangjie.cfir.tree.generator.CfirTree.typeRef
import org.cangnova.cangjie.cfir.tree.generator.context.AbstractCfirTreeBuilder
import org.cangnova.cangjie.cfir.tree.generator.model.Field
import org.cangnova.cangjie.cfir.tree.generator.model.Element
import org.cangnova.cangjie.cfir.tree.generator.model.Element.Kind.*
import org.cangnova.cangjie.cfir.tree.generator.model.fieldSet
import org.cangnova.cangjie.cfir.tree.generator.util.generatedType
import org.cangnova.cangjie.cfir.tree.generator.util.type
import org.cangnova.cangjie.generators.tree.AbstractField
import org.cangnova.cangjie.generators.tree.ImplementationKind
import org.cangnova.cangjie.generators.tree.TypeKind
import org.cangnova.cangjie.generators.tree.withArgs
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.generators.tree.TypeRef as TreeTypeRef

/**
 * CFIR tree 的完整元模型定义。
 */
object CfirTree : AbstractCfirTreeBuilder() {
    /**
     * CFIR source 字段使用的源码元素类型。
     */
    val sourceElementType = type<CjSourceElement>()
    private val conditionalCompilationFailureType = type(
        "org.cangnova.cangjie.cfir.session",
        "CfirConditionalCompilationFailure",
        exactPackage = true,
    )

    /**
     * 模块数据类型引用。
     */
    private val moduleDataType = type("common", "CfirModuleData")
    /**
     * 声明来源类型引用。
     */
    private val declarationOriginType = generatedType("declarations", "CfirDeclarationOrigin", TypeKind.Class)
    /**
     * 声明属性集合类型引用。
     */
    private val declarationAttributesType = generatedType("declarations", "CfirDeclarationAttributes", TypeKind.Class)
    /**
     * 描述符可见性类型引用。
     */
    private val visibilityType =
        type("org.cangnova.cangjie.descriptors", "Visibility", exactPackage = true, kind = TypeKind.Class)
    /**
     * 描述符 modality 类型引用。
     */
    private val modalityType =
        type("org.cangnova.cangjie.descriptors", "Modality", exactPackage = true, kind = TypeKind.Class)
    /**
     * CFIR resolve phase 类型引用。
     */
    private val resolvePhaseType = generatedType("declarations", "CfirResolvePhase", TypeKind.Class)
    /**
     * CFIR resolve state 类型引用。
     */
    private val resolveStateType = type("declarations", "CfirResolveState", kind = TypeKind.Class)
    /**
     * 直接访问 resolve state 的 opt-in 注解类型引用。
     */
    private val resolveStateAccessType = type("declarations", "ResolveStateAccess", kind = TypeKind.Class)
    /** 公共官方 AnnotationKind 类型引用。 */
    private val annotationKindType = type(
        "org.cangnova.cangjie.annotations",
        "BuiltInAnnotationKind",
        exactPackage = true,
        kind = TypeKind.Class,
    )
    /** 公共注解来源类型引用。 */
    private val annotationOriginType = type(
        "org.cangnova.cangjie.annotations",
        "CangjieAnnotationOrigin",
        exactPackage = true,
        kind = TypeKind.Class,
    )
    /** annotation resolve 后保存的唯一身份；禁止消费者再次从 callee 短名猜测。 */
    private val annotationIdentityType = type(
        "org.cangnova.cangjie.annotations",
        "CangjieAnnotationIdentity",
        exactPackage = true,
        kind = TypeKind.Class,
    )
    /** annotation call 状态机类型引用；状态由宿主 declaration processor 管理。 */
    internal val annotationResolveStateType = type(
        "expressions",
        "CfirAnnotationResolveState",
        exactPackage = false,
        kind = TypeKind.Class,
    )
    /** annotation 参数只读视图类型引用。 */
    private val annotationArgumentViewType = type(
        "expressions",
        "CfirAnnotationArgumentView",
        exactPackage = false,
        kind = TypeKind.Class,
    )
    private val annotationTargetType = type(
        "org.cangnova.cangjie.annotations", "CangjieAnnotationTarget", exactPackage = true, kind = TypeKind.Class,
    )
    internal val emptyAnnotationArgumentMappingType = type(
        "expressions.impl", "CfirEmptyAnnotationArgumentMapping", kind = TypeKind.Class,
    )
    /**
     * 任意 CFIR 符号基类类型引用。
     */
    private val symbolType = type("symbols", "CfirBasedSymbol").withArgs(TreeTypeRef.Star)

    // ---- 分类器符号类型 ----
    /**
     * class 符号类型引用。
     */
    val classSymbolType = type("symbols", "CfirClassSymbol")
    /**
     * interface 符号类型引用。
     */
    val interfaceSymbolType = type("symbols", "CfirInterfaceSymbol")
    /**
     * struct 符号类型引用。
     */
    val structSymbolType = type("symbols", "CfirStructSymbol")
    /**
     * enum 符号类型引用。
     */
    val enumSymbolType = type("symbols", "CfirEnumSymbol")
    /**
     * class-like 符号基类类型引用。
     */
    val classLikeSymbolType = type("symbols", "CfirClassLikeSymbol").withArgs(TreeTypeRef.Star)
//    val cfirClassifierSymbolWithClassId = type("symbols", "CfirClassifierSymbolWithClassId").withArgs(TreeTypeRef.Star)
/**
 * CFIR 作用域 provider 类型引用。
 */
val cfirScopeProviderType = type("scopes", "CfirScopeProvider")

    /**
     * typealias 符号类型引用。
     */
    val typeAliasSymbolType = type("symbols", "CfirTypeAliasSymbol")
    /**
     * 类型参数符号类型引用。
     */
    val typeParameterSymbolType = type("symbols", "CfirTypeParameterSymbol")

    // ---- 可调用符号类型 ----
    /**
     * callable 符号基类类型引用。
     */
    val callableSymbolType = type("symbols", "CfirCallableSymbol").withArgs(TreeTypeRef.Star)
    /**
     * function 符号基类类型引用。
     */
    val functionSymbolType = type("symbols", "CfirFunctionSymbol").withArgs(TreeTypeRef.Star)
    /**
     * 具名函数符号类型引用。
     */
    val namedFunctionSymbolType = type("symbols", "CfirNamedFunctionSymbol")
    /**
     * 匿名函数符号类型引用。
     */
    val anonymousFunctionSymbolType = type("symbols", "CfirAnonymousFunctionSymbol")
    /**
     * main 函数符号类型引用。
     */
    val mainFunctionSymbolType = type("symbols", "CfirMainFunctionSymbol")
    /**
     * finalizer 符号类型引用。
     */
    val finalizerSymbolType = type("symbols", "CfirFinalizerSymbol")
    /**
     * 构造器符号类型引用。
     */
    val constructorSymbolType = type("symbols", "CfirConstructorSymbol")
    /**
     * 宏声明符号类型引用。
     */
    val macroDeclarationSymbolType = type("symbols", "CfirMacroDeclarationSymbol")
    /**
     * 属性符号类型引用。
     */
    val propertySymbolType = type("symbols", "CfirPropertySymbol")
    /**
     * 属性访问器符号类型引用。
     */
    val propertyAccessorSymbolType = type("symbols", "CfirPropertyAccessorSymbol")
    /**
     * 变量符号基类类型引用。
     */
    val variableSymbolType = type("symbols", "CfirVariableSymbol").withArgs(TreeTypeRef.Star)
    /**
     * 值参数符号类型引用。
     */
    val valueParameterSymbolType = type("symbols", "CfirValueParameterSymbol")
    /**
     * 字段变量符号类型引用。
     */
    val fieldVariableSymbolType = type("symbols", "CfirFieldVariableSymbol")
    /**
     * 模式变量符号类型引用。
     */
    val patternVariableSymbolType = type("symbols", "CfirPatternVariableSymbol")
    /**
     * 模式绑定变量符号类型引用。
     */
    val patternBindingVariableSymbolType = type("symbols", "CfirPatternBindingSymbol")
    /**
     * enum 构造器符号类型引用。
     */
    val enumConstructorSymbolType = type("symbols", "CfirEnumConstructorSymbol")
    /** 正常字段与 Library 完成内容的非空数据源角色。 */
    private val contentSourceType = type(BASE_PACKAGE, "CfirContentSource", exactPackage = true)
    /**
     * 具名值符号基类类型引用。
     */
    val nameValueSymbolType = type("symbols", "CfirNamedValueSymbol")

    // ---- 其他符号类型 ----
    /**
     * 文件符号类型引用。
     */
    val fileSymbolType = type("symbols", "CfirFileSymbol")
    /**
     * extend 符号类型引用。
     */
    val extendSymbolType = type("symbols", "CfirExtendSymbol")
    /**
     * code fragment 符号类型引用。
     */
    val codeFragmentSymbolType = type("symbols", "CfirCodeFragmentSymbol")

    /**
     * cone 仓颉类型类型引用。
     */
    private val coneTypeType = type("types", "ConeCangJieType")
    /**
     * 名称类型引用。
     */
    private val nameType = type("org.cangnova.cangjie.name", "Name", exactPackage = true, kind = TypeKind.Class)
    /**
     * FqName 类型引用。
     */
    private val fqNameType = type("org.cangnova.cangjie.name", "FqName", exactPackage = true, kind = TypeKind.Class)

    // classKindType 已删除：各具名类型由独立节点承载，不再需要运行时 kind 区分
    /**
     * 字面量 kind 类型引用。
     */
    private val literalKindType = generatedType("expressions", "CfirLiteralKind", TypeKind.Class)
    /**
     * 二元操作 kind 类型引用。
     */
    private val binaryOpKindType = generatedType("expressions", "CfirBinaryOpKind", TypeKind.Class)
    /**
     * 比较操作类型引用。
     */
    private val comparisonOpType = generatedType("expressions", "CfirComparisonOp", TypeKind.Class)
    /**
     * 类型操作 kind 类型引用。
     */
    private val typeOperationKindType = generatedType("expressions", "CfirTypeOperationKind", TypeKind.Class)
    /**
     * Kotlin String 类型引用。
     */
    private val stringType = type("kotlin", "String", exactPackage = true, kind = TypeKind.Class)
    /**
     * Kotlin Boolean 类型引用。
     */
    private val booleanType = type("kotlin", "Boolean", exactPackage = true, kind = TypeKind.Class)
    /**
     * Kotlin Any 类型引用。
     */
    private val anyType = type("kotlin", "Any", exactPackage = true, kind = TypeKind.Class)
    /**
     * 仓颉源文件类型引用。
     */
    private val sourceFileType =
        type("org.cangnova.cangjie", "CjSourceFile", exactPackage = true, kind = TypeKind.Interface)

    /**
     * 可复用字段集合定义。
     */
    private object FieldSets {
        /**
         * 类型实参列表字段集合。
         */
        val typeArguments = fieldSet(
            listField("typeArguments", typeRef, useMutableOrEmpty = true, withReplace = true, withTransform = true)
        )
        /**
         * 名称字段集合。
         */
        val name = fieldSet(field(nameType))

        /**
         * 声明列表字段集合。
         */
        val declarations = fieldSet(
            listField("declarations", declaration, withTransform = true) {
                useInBaseTransformerDetection = false
            })
        /** 自有参数的明确字段类型；多父节点必须显式覆盖 refs owner 的字段。 */
        val typeParameters = fieldSet(
            listField("typeParameters", typeParameter, withTransform = true)
        )
        /**
         * 注解列表字段集合。
         */
        val annotations = fieldSet(
            listField("annotations", annotation, withReplace = true, useMutableOrEmpty = true, withTransform = true) {
                needTransformInOtherChildren = true
            })
    }

    /**
     * 所有 CFIR 元素的根接口。
     */
    override val rootElement: Element by element(Other, name = "Element") {
        kind = ImplementationKind.Interface
        hasAcceptChildrenMethod = true
        hasTransformChildrenMethod = true
        // 仓颉独立target PSI换代时，保留的IR节点与symbol必须通过同一源槽位协议迁移。
        +field("source", sourceElementType, nullable = true, withReplace = true)
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

    /**
     * 携带注解列表的元素接口。
     */
    val annotationContainer: Element by element(Other) {
        kind = ImplementationKind.Interface
        +FieldSets.annotations
    }

    /**
     * 持有控制流图引用的声明元素接口。
     */
    val controlFlowGraphOwner: Element by element(Declaration) {
        +field("controlFlowGraphReference", controlFlowGraphReference, withReplace = true, nullable = true)
    }

    /**
     * 仅包装一个内部表达式的表达式基类。
     */
    val wrappedExpression: Element by element(Expression) {
        parent(expression)

        // 包装节点需要独立转换内部表达式，供已检查接收者与 selector 的分阶段解析使用。
        +field(expression, withTransform = true)
    }

    /**
     * optional 后缀包装节点。
     *
     * 对齐官方 `OptionalExpr`：它只记录一次 `?` 后缀引入的包装语义，
     * 不把 `?.` / `?[` / `?(` 退化为独立安全访问节点。
     */
    val optionalExpression: Element by element(Expression, name = "OptionalExpression") {
        parent(wrappedExpression)
    }

    /**
     * optional chain 根节点。
     *
     * 对齐官方 `OptionalChainExpr`：整条 quest 后缀链在 CFIR 中由单独节点承接，
     * 链内的访问/调用/索引仍保持普通 expression 结构。
     */
    val optionalChainExpression: Element by element(Expression, name = "OptionalChainExpression") {
        parent(wrappedExpression)
    }

    /**
     * `@IfAvailable(name: value, { => ... }, { => ... })` 的特殊表达式节点。
     *
     * 该语法在官方前端中先作为 IfAvailableExpr 独立检查，再解糖为 if；
     * 因此不能把它建模为普通 annotation call 或 construction-only macro surface。
     */
    val ifAvailableExpression: Element by element(Expression, name = "IfAvailableExpression") {
        parent(expression)
        +field("conditionName", nameType)
        +field("conditionArgumentSource", sourceElementType, nullable = true, withReplace = true)
        +field("conditionNameSource", sourceElementType, nullable = true, withReplace = true)
        +field("condition", expression, nullable = true, withTransform = true)
        // 合法分支必须保留匿名函数节点，以便 BODY_RESOLVE 看到参数列表、lambda
        // 作用域和 `() -> Unit` 目标类型；非法分支仍以 expression 承载原始形状。
        +field("thenBranch", expression, withTransform = true)
        +field("elseBranch", expression, withTransform = true)
    }

    /**
     * 文件前导 `features` directive。
     *
     * 它是文件级 metadata，不是声明；注解仍作为独立 annotation children
     * 保留，feature id 只保留源码规范化文本，后续 package checker 负责一致性语义。
     */
    val featuresDirective: Element by element(Other, name = "FeaturesDirective") {
        parent(annotationContainer)
        +listField("featureIds", stringType, withReplace = true, isChild = false)
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

    /**
     * 源文件 package 指令节点。
     */
    val packageDirective: Element by element(Declaration, name = "PackageDirective") {
        parent(rootElement)
        +field("packageFqName", fqNameType)
        +field("organizationName", nameType, nullable = true)
        +field("isMacroPackage", booleanType) {
            defaultValueInBuilder = "false"
        }
    }

    /**
     * 源文件 import 指令抽象节点。
     */
    val importDirective: Element by element(Declaration, name = "Import") {
        kind = ImplementationKind.AbstractClass
        parent(rootElement)
        +field("importedFqName", fqNameType, nullable = true)
        +field("organizationName", nameType, nullable = true)
        +field("isAllUnder", booleanType)
        // 语句级可见性在 raw 构建时传给每个叶项，后续阶段不得反查源码 token。
        +field("visibility", visibilityType)
        +field("aliasName", nameType, nullable = true)
        +field("aliasSource", sourceElementType, nullable = true, withReplace = true)
        /** `@When[...] import` 在 raw-CFIR 裁剪前保留的条件表达式。 */
        +field("condition", expression, nullable = true, withTransform = true, withReplace = true)
    }

    /**
     * 已解析 import 指令节点。
     */
    val resolvedImportDirective: Element by element(Declaration, name = "ResolvedImport") {
        parent(importDirective)
        +field("delegate", importDirective, isChild = false) { sourceTraversalKind = Field.SourceTraversalKind.ELEMENT }
        +field("packageFqName", fqNameType)
        +field("importedName", nameType, nullable = true)
    }

    /**
     * 注解实例节点。
     */
    val annotation: Element by element(Expression, name = "Annotation") {
        parent(expression)

        +field("typeRef", typeRef, withTransform = true, withReplace = true)
        +listField("arguments", expression, isChild = false)
        +field("argumentMapping", annotationArgumentMapping, withReplace = true, isChild = false) {
            sourceTraversalKind = Field.SourceTraversalKind.ELEMENT
        }
        +field("annotationClassId", type("org.cangnova.cangjie.name", "ClassId", exactPackage = true, kind = TypeKind.Class), nullable = true, withReplace = true)
        +field("annotationTarget", annotationTargetType, nullable = true, withReplace = true)
        +field("forcedCustom", booleanType, withReplace = true) { defaultValueInBuilder = "false" }
        // 保留 raw source spelling；它只用于显示、诊断和区分共享 NUMERIC_OVERFLOW kind。
        +field("annotationSourceName", stringType, nullable = true, withReplace = true)
        // 保存实际解析入口的语言模块来源；宏新生成 token 的默认模块为空，不能由宿主文件补授 builtin 身份。
        +field("sourceModuleName", stringType, withReplace = true) { defaultValueInBuilder = "\"\"" }
        // Annotation identity is semantic metadata, not a child node. It is
        // populated by annotation resolution and remains nullable for raw and
        // unresolved annotations.
        +field("annotationKind", annotationKindType, nullable = true, withReplace = true)
        +field("annotationIdentity", annotationIdentityType, nullable = true, withReplace = true)
        // Raw annotations have not yet established their semantic origin or
        // compile-time visibility. Nullable fields make that unresolved state
        // explicit instead of forcing every raw/synthetic builder to invent a
        // semantic default.
        +field("annotationOrigin", annotationOriginType, nullable = true, withReplace = true)
        +field("isCompileTimeVisible", booleanType, nullable = true, withReplace = true)
    }

    /**
     * 可出现在语句位置的表达式抽象节点。
     */
    val statement: Element by element(Expression ) {

        parent(annotationContainer)
    }
    /**
     * 类型参数引用节点。
     */
    val typeParameterRef: Element by element(Declaration) {
        +referencedSymbol(typeParameterSymbolType)
    }
    /**
     * 所有 CFIR 声明节点的抽象基类。
     */
    val declaration: Element by sealedElement(Declaration) {
        kind = ImplementationKind.AbstractClass

        parent(elementWithResolveState)
        parent(annotationContainer)

        +field("contentSource", contentSourceType, isChild = false) {
            implementationDefaultStrategy = AbstractField.ImplementationDefaultStrategy.ConstructorDefaultValue("CfirContentSource.Owned")
            defaultValueInBuilder = "CfirContentSource.Owned"
            skippedInCopy = true
        }
        +listField("annotations", annotation, withReplace = true, useMutableOrEmpty = true, withTransform = true) {
            needTransformInOtherChildren = true
            publishedContentKind = Field.PublishedContentKind.ANNOTATIONS
        }

        +declaredSymbol(symbolType)
        +field("origin", declarationOriginType)
        +field("attributes", declarationAttributesType)
    }
    /**
     * 拥有类型参数引用列表的声明抽象层。
     */
    val typeParameterRefsOwner: Element by sealedElement(Declaration) {
        +listField("typeParameters", typeParameterRef, withTransform = true)
    }
    /**
     * 自身声明类型参数的 owner，对位 Kotlin FirTypeParametersOwner。
     *
     * 构造器借用所属名义类型参数，只继承 refs owner；不能把引用收窄为新声明。
     */
    val typeParametersOwner: Element by sealedElement(Declaration) {
        parent(typeParameterRefsOwner)
        +listField("typeParameters", typeParameter, withTransform = true)
    }
    /**
     * 成员声明抽象层。
     */
    val memberDeclaration: Element by sealedElement(Declaration, name = "MemberDeclaration") {
        parent(declaration)

        parent(typeParameterRefsOwner)

        +field(
            "status",
            declarationStatus, withReplace = true, withTransform = true
        )
    }

    /**
     * callable 声明抽象层。
     */
    val callableDeclaration: Element by sealedElement(Declaration, name = "CallableDeclaration") {
        parent(memberDeclaration)
        +declaredSymbol(callableSymbolType)
        +field("isLocal", boolean)
        +field(
            "returnTypeRef",
            typeRef, withReplace = true, withTransform = true
        )
        +field("deprecationsProvider", deprecationsProviderType, withReplace = true) {
            isMutable = true
            publishedContentKind = Field.PublishedContentKind.DEPRECATIONS
            defaultValueInBuilder = "UnresolvedDeprecationProvider"
            arbitraryImportables += unresolvedDeprecationsProviderType
        }

        +field("dispatchReceiverType", coneSimpleCangJieTypeType, nullable = true)
        +referencedSymbol(callableSymbolType.withArgs(callableDeclaration))

    }

    /**
     * class / interface / struct / enum / typealias 的抽象层。
     */
    val classLikeDeclaration: Element by sealedElement(Declaration, name = "ClassLikeDeclaration") {
        parent(memberDeclaration)
        +declaredSymbol(classLikeSymbolType)
        +field("deprecationsProvider", deprecationsProviderType, withReplace = true) {
            isMutable = true
            publishedContentKind = Field.PublishedContentKind.DEPRECATIONS
            defaultValueInBuilder = "UnresolvedDeprecationProvider"
            arbitraryImportables += unresolvedDeprecationsProviderType
        }
        +FieldSets.declarations
        +field("name", nameType)

        +listField("superTypeRefs", typeRef, withTransform = true)
        +field("scopeProvider", cfirScopeProviderType)

    }

    /**
     * CFIR 源文件节点。
     */
    val file: Element by element(Declaration, name = "File") {
        parent(declaration)
        parent(controlFlowGraphOwner)
        +declaredSymbol(fileSymbolType)
        +field("name", stringType)
        +field("sourceFile", sourceFileType, nullable = true, withReplace = true)
        +field("featuresDirective", featuresDirective, nullable = true, withReplace = true, withTransform = true)
        +field("packageDirective", packageDirective, withTransform = true)
         +listField("imports", importDirective, withReplace = true, withTransform = true)
        +field("sourceFileLinesMapping", sourceFileLinesMappingType, nullable = true, withReplace = true)
        +listField(
            "conditionalCompilationFailures",
            conditionalCompilationFailureType,
            withReplace = true,
            isChild = false,
        )

        +FieldSets.declarations
    }

    /**
     * 代码片段节点。
     */
    val codeFragment: Element by element(Declaration, name = "CodeFragment") {
        parent(declaration)
        +declaredSymbol(codeFragmentSymbolType)
        +field("block", block, withReplace = true, withTransform = true)
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
        parent(typeParametersOwner)
        // 多父节点且不指定 visitor 父：默认 visitor 不把 class 转发到 class-like，沿用既有分派。
        +FieldSets.typeParameters
        +field("status", declarationStatus, withReplace = true, withTransform = true)
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
        parent(typeParametersOwner)
        +FieldSets.typeParameters
        +field("status", declarationStatus, withReplace = true, withTransform = true)
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
        parent(typeParametersOwner)
        +FieldSets.typeParameters
        +field("status", declarationStatus, withReplace = true, withTransform = true)
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
     * isNonExhaustive 保存 enum body 中的 `...`，作为 match 穷尽性判断的声明级语义。
     */
    val enumDeclaration: Element by element(Declaration, name = "Enum") {
        parent(classLikeDeclaration)
        parent(controlFlowGraphOwner)
        parent(typeParametersOwner)
        +FieldSets.typeParameters
        +field("status", declarationStatus, withReplace = true, withTransform = true)
        +declaredSymbol(enumSymbolType)
        +listField("superTypeRefs", typeRef, withTransform = true)
        +FieldSets.declarations
        +field("name", nameType)
        +field("isRefEnum", booleanType)
        +field("isNonExhaustive", booleanType)
    }

    /**
     * enum constructor 既是可调用声明，也是 ADT payload 的唯一声明源。
     *
     * 这里显式保存 `valueParameters`，让调用解析、模式匹配、冲突检测共享同一份
     * 参数真相表，避免继续把 payload 信息折叠进 `returnTypeRef` 后再到处反推。
     */
    val enumConstructor: Element by element(Declaration, name = "EnumConstructor") {
        parent(callableDeclaration)
        parent(functionBodyOwner)
        customParentInVisitor = callableDeclaration
        +declaredSymbol(enumConstructorSymbolType)
        +field("status", declarationStatus, withReplace = true, withTransform = true)
        +field("name", nameType)
    }

    /**
     * extend 声明节点。
     */
    val extend: Element by element(Declaration, name = "Extend") {
        parent(memberDeclaration)
        parent(typeParametersOwner)
        customParentInVisitor = memberDeclaration
        +FieldSets.typeParameters
        +declaredSymbol(extendSymbolType)
        +field("status", declarationStatus, withReplace = true, withTransform = true)
        +field("extendedTypeRef", typeRef, withTransform = true)
        +listField("superTypeRefs", typeRef, withTransform = true)
        +FieldSets.declarations
    }

    /**
     * typealias 声明节点。
     */
    val typeAlias: Element by element(Declaration, name = "TypeAlias") {
        parent(classLikeDeclaration)
        parent(typeParametersOwner)
        customParentInVisitor = classLikeDeclaration
        +FieldSets.typeParameters
        +declaredSymbol(typeAliasSymbolType)
        +field("status", declarationStatus, withReplace = true, withTransform = true)
        +field("name", nameType)
        +field("expandedTypeRef", typeRef, withReplace = true, withTransform = true)
    }

    /**
     * 函数类声明抽象层。
     */
    val function: Element by sealedElement(Declaration, name = "Function") {
        parent(callableDeclaration)
        parent(functionBodyOwner)
        parent(statement)
        customParentInVisitor = callableDeclaration
        +declaredSymbol(functionSymbolType)
        +field("status", declarationStatus, withReplace = true, withTransform = true)
        // 普通函数保留参数向量的 replace 能力；enum F 继承共同 owner 的原参数向量。
        +listField("valueParameters", valueParameter, withReplace = true, withTransform = true)
        /** foreign C function 的 `...` 不是一个值参数，必须作为函数级签名事实保存。 */
        +field("hasVariableLenArg", booleanType) { defaultValueInBuilder = "false" }
    }

    /**
     * 具名函数声明节点。
     */
    val namedFunction: Element by element(Declaration, name = "NamedFunction") {
        parent(function)
        parent(typeParametersOwner)
        customParentInVisitor = function
        +FieldSets.typeParameters
        +declaredSymbol(namedFunctionSymbolType)
        +field("name", nameType)
        +field("isMut", booleanType)
    }

    /**
     * main 函数声明节点。
     */
    val mainFunction: Element by element(Declaration, name = "MainFunction") {
        parent(function)
        parent(typeParametersOwner)
        customParentInVisitor = function
        +FieldSets.typeParameters
        +declaredSymbol(mainFunctionSymbolType)
    }

    /**
     * 宏声明节点。
     */
    val macroDeclaration: Element by element(Declaration, name = "MacroDeclaration") {
        parent(function)
        parent(typeParametersOwner)
        customParentInVisitor = function
        +FieldSets.typeParameters
        +declaredSymbol(macroDeclarationSymbolType)
        +field("name", nameType)
    }

    /**
     * finalizer 声明节点。
     */
    val finalizer: Element by element(Declaration, name = "Finalizer") {
        parent(function)
        parent(typeParametersOwner)
        customParentInVisitor = function
        +FieldSets.typeParameters
        +declaredSymbol(finalizerSymbolType)
    }

    /**
     * 构造器声明节点。
     */
    val constructor: Element by element(Declaration, name = "Constructor") {
        parent(function)
        +declaredSymbol(constructorSymbolType)
        +field("isPrimary", booleanType)
    }

    /**
     * 无效声明占位节点。
     */
    val invalidDeclaration: Element by element(Declaration, name = "InvalidDeclaration") {
        parent(declaration)
        +field("reason", stringType)
    }

    /** 源码中独立的宏声明调用；它在展开前占据真实声明列表槽位。 */
    val macroExpansionDeclaration: Element by element(Declaration, name = "MacroExpansionDeclaration") {
        parent(declaration)
    }

    /**
     * 属性声明节点。
     */
    val property: Element by element(Declaration, name = "Property") {
        parent(callableDeclaration)
        parent(controlFlowGraphOwner)
        parent(typeParametersOwner)
        customParentInVisitor = callableDeclaration
        +FieldSets.typeParameters
        +declaredSymbol(propertySymbolType)
        +field("status", declarationStatus, withReplace = true, withTransform = true)
        +field("returnTypeRef", typeRef, withReplace = true, withTransform = true)
        +field("name", nameType)
        +field("getter", propertyAccessor, nullable = true, withReplace = true, withTransform = true)
        +field("setter", propertyAccessor, nullable = true, withReplace = true, withTransform = true)
        +field("bodyResolveState", propertyBodyResolveStateType, withReplace = true) {
            defaultValueInBuilder = "CfirPropertyBodyResolveState.NOTHING_RESOLVED"
        }
    }

    /**
     * 属性访问器声明节点。
     */
    val propertyAccessor: Element by element(Declaration, name = "PropertyAccessor") {
        parent(function)
        parent(typeParametersOwner)
        customParentInVisitor = function
        +FieldSets.typeParameters
        +declaredSymbol(propertyAccessorSymbolType)
        +referencedSymbol("propertySymbol", propertySymbolType, withReplace = false) {
            withBindThis = false
        }
        +field("isGetter", booleanType)
    }

    /**
     * 变量类声明抽象层。
     */
    val variable: Element by sealedElement(Declaration, name = "Variable") {
        parent(callableDeclaration)
        parent(statement)
        +declaredSymbol(variableSymbolType)
        +field("status", declarationStatus, withReplace = true, withTransform = true)
        +field("contentSource", contentSourceType, isChild = false) {
            implementationDefaultStrategy = AbstractField.ImplementationDefaultStrategy.ConstructorDefaultValue("CfirContentSource.Owned")
            defaultValueInBuilder = "CfirContentSource.Owned"
            skippedInCopy = true
        }
        +field("initializer", expression, nullable = true, withReplace = true, withTransform = true) {
            publishedContentKind = Field.PublishedContentKind.INITIALIZER
        }
        +field("isVar", booleanType)
        // 保留源码是否省略变量类型；resolve 后 returnTypeRef 会变成 resolved，
        // 该事实不能再由 resolved type-ref 的形状或 source 文本反推。
        +field("isTypeImplicit", booleanType) { defaultValueInBuilder = "false" }
    }

    /**
     * 字段变量声明节点。
     */
    val fieldVariable: Element by element(Declaration, name = "FieldVariable") {
        parent(variable)
        parent(typeParametersOwner)
        customParentInVisitor = variable
        +FieldSets.typeParameters
        +declaredSymbol(fieldVariableSymbolType)
        +field("returnTypeRef", typeRef, withReplace = true, withTransform = true)
        +field("name", nameType)
    }

    /**
     * 模式变量声明容器节点。
     */
    val patternVariable: Element by element(Declaration, name = "PatternVariable") {
        parent(variable)
        parent(typeParametersOwner)
        customParentInVisitor = variable
        +FieldSets.typeParameters
        +declaredSymbol(patternVariableSymbolType)
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
        parent(typeParametersOwner)
        customParentInVisitor = variable
        +FieldSets.typeParameters
        +declaredSymbol(patternBindingVariableSymbolType)
        +field("returnTypeRef", typeRef, withReplace = true, withTransform = true)
        +field("name", nameType)
    }

    /**
     * 值参数声明节点。
     */
    val valueParameter: Element by element(Declaration, name = "ValueParameter") {
        parent(variable)
        parent(controlFlowGraphOwner)
        parent(typeParametersOwner)
        // 对位 Kotlin FirValueParameter：默认 visitor 不把形参转发为 variable，
        // 否则局部声明类 visitor 会把 lambda/函数形参当作局部变量处理。
        +FieldSets.typeParameters
        +declaredSymbol(valueParameterSymbolType)
        +referencedSymbol("containingDeclarationSymbol", cfirSymbolType.withArgs(TreeTypeRef.Star)) {
            withBindThis = false
        }
        +field("isNamed",booleanType, withReplace = false)
        +field("status", declarationStatus, withReplace = true, withTransform = true)
        +field("returnTypeRef", typeRef, withReplace = true, withTransform = true)
        +field("name", nameType)
        +field("defaultValue", expression, nullable = true, withReplace = true, withTransform = true) {
            publishedContentKind = Field.PublishedContentKind.DEFAULT_VALUE
        }
    }

    /**
     * 类型参数声明节点。
     */
    val typeParameter: Element by element(Declaration, name = "TypeParameter") {
        parent(typeParameterRef)

        parent(declaration)
        +referencedSymbol("containingDeclarationSymbol", cfirSymbolType.withArgs(TreeTypeRef.Star)) {
            withBindThis = false
        }
        +declaredSymbol(typeParameterSymbolType)
        +field("name", nameType)
        +listField("bounds", typeRef, withReplace = true, withTransform = true)
    }

    /**
     * 构造器对所属 class、struct 或 enum 类型参数的引用。
     * 不拥有声明、不绑定 symbol，也不重复推进原参数的解析阶段或导出其物理声明。
     */
    val constructedClassTypeParameterRef: Element by element(Declaration) {
        parent(typeParameterRef)
    }

    /**
     * 声明状态节点。
     */
    val declarationStatus: Element by element(Declaration, name = "DeclarationStatus") {
        kind = ImplementationKind.Interface
        +field("visibility", visibilityType)
        +field("modality", modalityType, nullable = true)
        generateBooleanFields(
            "visibilityExplicit",
            "modalityExplicit",
            "abstractExplicit",
            "override",
            "operator",
            "static",
            "const",
            "mut",
            "unsafe",
            "foreign",
            "c",
            "common",
            "specific",
            "commonWithDefault",
            "redef",
            "default",
            "abstract",
            "open",
            "sealed",
        )
    }

    /**
     * 所有表达式节点的抽象基类。
     */
    val expression: Element by element(Expression) {
        parent(statement)
        +field("coneTypeOrNull", coneTypeType, nullable = true, withReplace = true)
        // 保存后 map 为借用回边，不沿它扩大普通或 SOURCE 的拥有域。
        +field("mappedExpression", expression, nullable = true, isChild = false, withReplace = true) {
            defaultValueInBuilder = "null"
        }
        +field("overflowStrategy", sourceOverflowStrategyType, nullable = true, withReplace = true) {
            defaultValueInBuilder = "null"
        }
    }


    /** 保存后 NativeMatchExpression 的真实拥有结构；不借用 raw 节点表示。 */
    val nativeMatchExpression: Element by element(Expression, name = "NativeMatchExpression") {
        parent(expression)
        +field("selector", expression, withTransform = true)
        +listField("cases", nativeMatchCase, withTransform = true)
    }

    /** 保存后 NativeMatchCase 的真实拥有结构；不借用 raw 节点表示。 */
    val nativeMatchCase: Element by element(Expression, name = "NativeMatchCase") {
        parent(expression)
        +listField("patterns", pattern, withTransform = true)
        +field("guard", expression, nullable = true, withTransform = true)
        +field("body", block, withTransform = true)
    }

    /** 保存后 NativeConditionMatchExpression 的真实拥有结构；不借用 raw 节点表示。 */
    val nativeConditionMatchExpression: Element by element(Expression, name = "NativeConditionMatchExpression") {
        parent(expression)
        +listField("cases", nativeConditionMatchCase, withTransform = true)
    }

    /** 保存后 NativeConditionMatchCase 的真实拥有结构；不借用 raw 节点表示。 */
    val nativeConditionMatchCase: Element by element(Expression, name = "NativeConditionMatchCase") {
        parent(expression)
        +field("condition", expression, withTransform = true)
        +field("body", block, withTransform = true)
    }

    /** 保存后 LetPatternDestructorExpression 的真实拥有结构；不借用 raw 节点表示。 */
    val letPatternDestructorExpression: Element by element(Expression, name = "LetPatternDestructorExpression") {
        parent(expression)
        +field("initializer", expression, withTransform = true)
        +listField("patterns", pattern, withTransform = true)
    }

    /** Native Try 拥有原 resources/catch P/body，保留嵌套异常模式。 */
    val nativeCatch: Element by element(Expression, name = "NativeCatch") {
        parent(rootElement)
        +field("pattern", pattern, withTransform = true)
        +field("body", block, withTransform = true)
    }

    /** Native Try 拥有原 resources/catch P/body，保留嵌套异常模式。 */
    val nativeTryExpression: Element by element(Expression, name = "NativeTryExpression") {
        parent(expression)
        +listField("resources", fieldVariable, withTransform = true)
        +field("tryBlock", block, withTransform = true)
        +listField("catches", nativeCatch, withTransform = true)
        +field("finallyBlock", block, nullable = true, withTransform = true)
    }

    /** Native PrimitiveTypeExpr 的实际表达式角色，不以 TypeRef 或值字面量代替。 */
    val primitiveTypeExpression: Element by element(Expression, name = "PrimitiveTypeExpression") {
        parent(expression)
    }

    /** Native WildcardExpr 与模式 wildcard 分属 e/P 身份域。 */
    val wildcardExpression: Element by element(Expression, name = "WildcardExpression") {
        parent(expression)
    }

    /** 保存后的 FuncArg：应用后类型属于本节点，default 占位确实没有 value。 */
    val functionArgument: Element by element(Expression, name = "FunctionArgument") {
        parent(expression)
        +field("value", expression, nullable = true, withTransform = true)
        +field("isDefaultValue", booleanType)
        +field("withInout", booleanType)
    }

    /** 保存后 CallExpr 的 base 是独立拥有节点，调用种类不从 raw origin 反推。 */
    val callExpression: Element by element(Expression, name = "CallExpression") {
        parent(expression)
        +field("base", expression, withTransform = true)
        +listField("arguments", functionArgument, withTransform = true)
        +field("callKind", sourceCallKindType)
        +field("hasSideEffect", booleanType)
    }

    /** 保存后的语言运算直接承载真实 opcode 与 operand，不能造未解析的运算函数调用。 */
    val unaryExpression: Element by element(Expression, name = "UnaryExpression") {
        parent(expression)
        +field("operand", expression, withTransform = true)
        +field("operation", sourceOperatorType)
    }

    /** Native BinaryExpr 包含短路与急切运算；左右节点和原运算角色各自保留。 */
    val binaryExpression: Element by element(Expression, name = "BinaryExpression") {
        parent(expression)
        +field("left", expression, withTransform = true)
        +field("right", expression, withTransform = true)
        +field("operation", sourceOperatorType)
    }

    /** ArrayExpr 构造与 ArrayLit 分开；initFunc 是借用目标，不通过它取得函数体。 */
    val arrayCreationExpression: Element by element(Expression, name = "ArrayCreationExpression") {
        parent(expression)
        +listField("arguments", functionArgument, withTransform = true)
        +field("initializerSymbol", functionSymbolType, nullable = true, withReplace = true) {
            withBindThis = false
        }
        +field("isValueArray", booleanType)
    }

    /** Native PointerExpr 保留零或一个 FuncArg，不把 inout 传址或转换混为指针构造。 */
    val pointerExpression: Element by element(Expression, name = "PointerExpression") {
        parent(expression)
        +field("argument", functionArgument, nullable = true, withTransform = true)
    }

    /** 解糖后的 SpawnExpr：原 task 已被消除，拥有真实 future d，不能补空 body。 */
    val resolvedSpawnExpression: Element by element(Expression, name = "ResolvedSpawnExpression") {
        parent(expression)
        +field("argument", functionArgument, nullable = true, withTransform = true)
        +field("future", fieldVariable, withTransform = true)
    }

    /**
     * 块表达式节点。
     */
    val block: Element by element(Expression, name = "Block") {
        needTransformOtherChildren()

        parent(expression)
        +listField("statements", statement, withReplace = true, withTransform = true)
        +field("isUnsafe", booleanType, withReplace = true) { defaultValueInBuilder = "false" }
    }

    /**
     * 延迟解析块占位节点。
     */
    val lazyBlock: Element by element(Expression, name = "LazyBlock") {
        parent(block)
    }

    /**
     * 延迟解析表达式占位节点。
     */
    val lazyExpression: Element by element(Expression, name = "LazyExpression") {
        parent(expression)
    }

    /**
     * 字面量表达式节点。
     */
    val literalExpression: Element by element(Expression, name = "LiteralExpression") {
        parent(expression)
        +field("kind", literalKindType)
        +field("value", anyType, nullable = true)
        +field("stringKind", sourceStringKindType, withReplace = true) {
            defaultValueInBuilder = "CfirSourceStringKind.NORMAL"
        }
    }

    /**
     * 字符串插值表达式节点。
     */
    val stringInterpolation: Element by element(Expression, name = "StringInterpolation") {
        parent(expression)
        +listField("parts", expression, withTransform = true)
    }
    /**
     * 携带候选符号的具名引用基类。
     */
    val namedReferenceWithCandidateBase: Element by element(Reference) {
        parent(namedReference)

        +referencedSymbol("candidateSymbol", cfirSymbolType.withArgs(TreeTypeRef.Star))
    }
    /**
     * 错误主构造器占位节点。
     */
    val errorPrimaryConstructor: Element by element(Declaration) {
        parent(constructor)
        parent(diagnosticHolder)
        // diagnosticHolder 是附加能力；默认 visitor 必须沿构造器语义父链处理 body 和参数。
        customParentInVisitor = constructor
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
        +field("varraySizeLiteral", stringType, nullable = true)
        // 原生 SIDE_EFFECT 是完成 desugar 的语义位，独立于 operator origin 和 callable 名字。
        +field("sourceCallHasSideEffect", boolean, withReplace = true, isChild = false)

    }

    /**
     * 自增/自减表达式。
     *
     * 对齐 Kotlin FIR 的 `FirIncrementDecrementExpression` 与仓颉官方 AST 的
     * `IncOrDecExpr`：raw CFIR 保留语法级节点，resolve 阶段再按赋值语义脱糖。
     */
    val incrementDecrementExpression: Element by element(Expression, name = "IncrementDecrementExpression") {
        parent(expression)

        +field("isPrefix", boolean)
        +field("operationName", nameType)
        +field("expression", expression, withTransform = true)
        +field("operationSource", sourceElementType, nullable = true, withReplace = true)
    }
    /**
     * 解析失败的具名引用节点。
     */
    val errorNamedReference: Element by element(Reference) {
        parent(namedReference)
        parent(diagnosticHolder)
    }
    /**
     * 调用实参列表节点。
     */
    val argumentList: Element by element(Expression) {
        +listField("arguments", expression, withTransform = true)
    }

    /**
     * CFunc 调用中 `inout expr` 语法产生的实参包装节点。
     *
     * 对齐 C++ `FuncArg.withInout`:只影响诊断判断,不改变 wrapped expression 的语义类型。
     * raw-cfir 根据 PSI `CjValueArgument.isInout` 包装。
     */
    val inoutArgumentExpression: Element by element(Expression, name = "InoutArgumentExpression") {
        parent(wrappedExpression)
    }

    /**
     * `name: expression` 语法产生的命名实参包装节点。
     *
     * 参数名称属于调用形状，不应通过 PSI 或源码文本从普通表达式反向恢复。
     * 完整 value-argument source 用于参数级诊断，nameSource 用于名称级诊断。
     */
    val namedArgumentExpression: Element by element(Expression, name = "NamedArgumentExpression") {
        parent(wrappedExpression)

        +field("argumentName", nameType)
        +field("nameSource", sourceElementType, nullable = true, withReplace = true)
    }
    /**
     * 调用表达式抽象层。
     */
    val call: Element by sealedElement(Expression) {
        parent(statement)

        +field(argumentList, withReplace = true)
    }
    /**
     * 注解调用表达式节点。
     */
    val annotationArgumentMapping: Element by element(Expression) {
        +field("mapping", type("kotlin.collections", "Map", exactPackage = true, kind = TypeKind.Interface).withArgs(nameType, expression), isChild = false) {
            sourceTraversalKind = Field.SourceTraversalKind.MAP_VALUES
        }
    }

    val annotationCall: Element by element(Expression) {
        parent(annotation)
        parent(call)
        parent(resolvable)

        // mapping 仍属于 CfirResolvedArgumentList；annotation call 只保存由
        // 宿主 processor 发布的不可变 view，不复制第二套映射。
        +field("argumentView", annotationArgumentViewType, nullable = true, withReplace = true, isChild = false) {
            sourceTraversalKind = Field.SourceTraversalKind.ARGUMENT_VIEW
        }
        +field("annotationResolveState", annotationResolveStateType, withReplace = true) {
            defaultValueInBuilder = "CfirAnnotationResolveState.UNRESOLVED"
        }
        +referencedSymbol("containingDeclarationSymbol", cfirSymbolType.withArgs(TreeTypeRef.Star)) {
            withBindThis = false
        }
    }
    /**
     * super 引用节点。
     */
    val superReference: Element by element(Reference) {
        parent(reference)

        +field("superTypeRef", typeRef, withReplace = true)
    }

    /**
     * this 引用节点。
     */
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
    /** 已完成身份的 package/class/typealias qualifier；没有 callable reference 或运行期接收者。 */
    val resolvedQualifier: Element by element(Expression) {
        parent(expression)
        customParentInVisitor = expression
        +field("packageFqName", fqNameType)
        +field("relativeClassFqName", fqNameType, nullable = true)
        +referencedSymbol("qualifierSymbol", classLikeSymbolType, nullable = true)
        +field("explicitParent", resolvedQualifier, nullable = true, withTransform = true, withReplace = true)
        +listField("nonFatalDiagnostics", coneDiagnosticType, useMutableOrEmpty = true, withReplace = true, isChild = false)
        +FieldSets.typeArguments { withReplace = true; withTransform = true }
        +field("organizationName", nameType, nullable = true, isChild = false)
        +field("referenceName", nameType, isChild = false)
    }

    /** 具有真实 qualifier 身份的错误，不用虚构 package 或 callable target。 */
    val errorResolvedQualifier: Element by element(Expression) {
        parent(resolvedQualifier)
        parent(diagnosticHolder)
        customParentInVisitor = expression
    }

    val namedAccessExpression: Element by element(Expression ) {
        parent(qualifiedAccessExpression)

        +field("explicitReceiver", expression, nullable = true, withTransform = true, withReplace = true)
    }

    /**
     * 带类型参数的限定访问表达式（如 foo<T>、Foo.bar<T>）。
     */
    val qualifiedAccessExpression: Element by element(Expression ) {
        parent(expression)
        parent(resolvable)
        +field("dispatchReceiver", expression, nullable = true, withReplace = true)

        +field("explicitReceiver", expression, nullable = true, withTransform = true, withReplace = true)
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
    /**
     * 错误函数声明占位节点。
     */
    val errorFunction: Element by element(Declaration) {
        parent(function)
        parent(diagnosticHolder)

        +declaredSymbol(errorFunctionSymbolType)
    }
    /**
     * 错误具名值声明占位节点。
     */
    val errorNamedValue: Element by element(Declaration) {
        parent(callableDeclaration)
        parent(diagnosticHolder)

        +field("name", nameType)
        +declaredSymbol(errorNamedValueSymbolType)
    }
    /**
     * 赋值表达式节点。
     */
    val assignment: Element by element(Expression, name = "Assignment") {
        parent(expression)
        +field("lValue", expression, withTransform = true)
        +field("rValue", expression, withTransform = true)
        +field("augmentedOperation", nameType, nullable = true)
        +field("operation", sourceOperatorType, withReplace = true) { defaultValueInBuilder = "CfirSourceOperator.ASSIGN" }
        +field("isCompound", booleanType, withReplace = true) { defaultValueInBuilder = "false" }
        +field("typeMismatchOutcome", assignmentTypeMismatchOutcomeType, nullable = true, withReplace = true)
    }

    /**
     * 复合赋值表达式节点。
     *
     * raw CFIR 保留操作符及左右操作数，body resolve 再根据候选选择构造普通赋值或
     * operator 调用。这避免在 raw builder 阶段把 `a <op>= b` 退化为 `a = a <op> b`，
     * 从而丢失 compound-assignment 专有的类型检查和诊断归属。
     */
    val augmentedAssignment: Element by element(Expression, name = "AugmentedAssignment") {
        parent(expression)
        +field("operation", nameType)
        +field("operationSource", sourceElementType, nullable = true, withReplace = true)
        +field("leftArgument", expression, withTransform = true)
        +field("rightArgument", expression, withTransform = true)
    }

    /**
     * 二元操作表达式节点。
     */
    val binaryOp: Element by element(Expression, name = "BinaryOp") {
        parent(expression)
        +field("kind", binaryOpKindType)
        +field("left", expression, withTransform = true)
        +field("right", expression, withTransform = true)
    }

    /**
     * 比较表达式节点。
     */
    val comparisonExpression: Element by element(Expression, name = "ComparisonExpression") {
        parent(expression)
        +field("operation", comparisonOpType)
        +field("left", expression, withTransform = true)
        +field("right", expression, withTransform = true)
    }

    /**
     * 类型操作表达式节点。
     */
    val typeOperator: Element by element(Expression, name = "TypeOperator") {
        parent(expression)
        +field("operation", typeOperationKindType)
        +field("argument", expression, withTransform = true)
        +field("typeRef", typeRef, withTransform = true)
    }

    /**
     * 基本类型转换表达式。
     *
     * 对齐官方 AST `TypeConvExpr`：`Int64(x)` 是语言级类型转换表达式，
     * 不参与普通函数调用、构造器调用或内置成员解析。
     */
    val typeConversion: Element by element(Expression, name = "TypeConversion") {
        parent(expression)
        +field("argument", expression, withTransform = true)
        +field("targetTypeRef", typeRef, withTransform = true)
    }

    /**
     * let pattern 条件表达式。
     *
     * 对齐官方 AST `LetPatternDestructor`：`if/while (let p <- initializer)` 本身是一个
     * 条件表达式，initializer 先独立解析，pattern 绑定只进入条件对应的 then/body 作用域。
     */
    val letPatternExpression: Element by element(Expression, name = "LetPatternExpression") {
        parent(expression)
        +field("initializer", expression, withTransform = true)
        +field("pattern", pattern, withTransform = true)
    }

    /**
     * if 表达式节点。
     */
    val ifExpression: Element by element(Expression, name = "IfExpression") {
        parent(expression)
        +field("condition", expression, withTransform = true)
        +field("thenBranch", block, withTransform = true)
        +field("elseBranch", expression, nullable = true, withTransform = true)
    }

    /**
     * match 表达式节点。
     */
    val matchExpression: Element by element(Expression, name = "MatchExpression") {
        parent(expression)
        +field("subject", expression, withTransform = true, nullable = true)
        +listField("branches", matchBranch, withTransform = true)
        +field("exhaustiveness", matchExhaustivenessStatusType, withReplace = true) {
            defaultValueInBuilder = "CfirMatchExhaustivenessStatus.Unknown"
        }
    }

    /**
     * or-pattern 节点。
     */
    val orPattern: Element by element(Pattern, name = "OrPattern") {
        parent(pattern)
        +listField("alternatives", pattern, withTransform = true)
    }

    /**
     * 表达式模式节点。
     */
    val expressionPattern: Element by element(Pattern, name = "ExpressionPattern") {
        parent(pattern)
        +field("expression", expression, withTransform = true)
    }

    /**
     * match 分支节点。
     */
    val matchBranch: Element by element(Expression, name = "MatchBranch") {
        parent(expression)
        +field("pattern", pattern, withTransform = true)
        +field("guard", expression, nullable = true, withTransform = true)
        +field("body", block, withTransform = true)
    }

    /**
     * 官方 ExceptTypePattern 的共享模式节点。
     *
     * 整体语义类型使用 Pattern.semanticTypeRef；typeRefs 仅保存原异常 Type 尾槽。
     * 嵌套模式拥有自己的绑定声明；异常上下文的合法性仍由 catch resolver/checker 负责。
     */
    val catchPattern: Element by element(Pattern, name = "CatchPattern") {
        parent(pattern)
        +field("nestedPattern", pattern, withReplace = true, withTransform = true)
        +listField("typeRefs", typeRef, withReplace = true, withTransform = true)
    }

    /**
     * catch 子句节点。
     */
    val catchClause: Element by element(Expression, name = "Catch") {
        parent(expression)
        // catch (_) 直接拥有共享 WildcardPattern；带类型列表才拥有 CatchPattern。
        +field("pattern", pattern, withReplace = true, withTransform = true)
        +field("body", block, withTransform = true)
    }

    /**
     * 官方 CommandTypePattern 的共享模式节点。
     *
     * 原 Type 尾槽与整体 P 类型分开，inner P 保留真实拥有结构和声明身份。
     * handle 上下文负责 command 语义，进入共同 Pattern 不改变合法使用位置。
     */
    val commandTypePattern: Element by element(Pattern, name = "CommandTypePattern") {
        parent(pattern)
        +field("nestedPattern", pattern, withReplace = true, withTransform = true)
        +listField("typeRefs", typeRef, withReplace = true, withTransform = true)
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

    /**
     * loop / while / do-while 表达式节点。
     */
    val loopExpression: Element by element(Expression, name = "LoopExpression") {
        parent(expression)
        parent(iterationOwner)
        customParentInVisitor = expression
        +field("condition", expression, withTransform = true)
        +field("body", block, withTransform = true)
        +field("isDoWhile", booleanType)
    }

    /** 原生 For 的完整拥有结构；迭代检查属于实际 kind 的语义阶段，不生成 condition 或包装变量。 */
    val forInExpression: Element by element(Expression, name = "ForInExpression") {
        parent(expression)
        parent(iterationOwner)
        customParentInVisitor = expression
        +field("pattern", pattern, withReplace = true, withTransform = true)
        +field("iterable", expression, withTransform = true)
        +field("patternGuard", expression, nullable = true, withTransform = true)
        +field("body", block, withTransform = true)
        +field("iterationKind", sourceForInKindType)
    }

    /**
     * try 表达式节点。
     */
    val tryExpression: Element by element(Expression, name = "TryExpression") {
        parent(expression)
        +listField("resources", fieldVariable, withReplace = true, withTransform = true)
        +field("tryBlock", block, withTransform = true)
        +listField("handlers", handleClause, withTransform = true)
        +listField("catches", catchClause, withTransform = true)
        +field("finallyBlock", block, nullable = true, withTransform = true)
    }

    /**
     * throw 表达式节点。
     */
    val throwExpression: Element by element(Expression, name = "ThrowExpression") {
        parent(expression)
        +field("exception", expression, withTransform = true)
    }
    /**
     * 完成解析后的声明状态节点。
     */
    val resolvedDeclarationStatus: Element by element(Declaration) {
        kind = ImplementationKind.Interface

        parent( declarationStatus)

        +field(modalityType, nullable = false)
//        +field("effectiveVisibility", effectiveVisibilityType)
    }
    /**
     * effect perform 表达式节点。
     */
    val performExpression: Element by element(Expression, name = "PerformExpression") {
        parent(expression)
        +field("expression", expression, withTransform = true)
    }

    /**
     * effect resume 表达式节点。
     */
    val resumeExpression: Element by element(Expression, name = "ResumeExpression") {
        parent(expression)
        +field("withExpression", expression, nullable = true, withTransform = true)
        +field("throwingExpression", expression, nullable = true, withTransform = true)
    }

    /**
     * return 表达式节点。
     */
    val returnExpression: Element by element(Expression, name = "ReturnExpression") {
        needTransformOtherChildren()

        parent(jump.withArgs("E" to functionBodyOwner))

        +field("result", expression, withReplace = true, withTransform = true)
    }

    /** 统一控制转移；target 是稳定借用身份，Return.result 才是拥有子节点。 */
    val jump: Element by sealedElement(Expression, name = "Jump") {
        val e = +param("E", targetElement)
        parent(expression)
        +field("target", jumpTargetType.withArgs(e), isChild = false)
    }

    /**
     * 循环跳转抽象层。
     *
     * 这里只负责携带“跳到哪个循环”的 target；具体是 `break` 还是 `continue`
     * 交由不同 concrete 节点表达，而不是再回退到枚举字段。
     */
    val loopJump: Element by sealedElement(Expression, name = "LoopJump") {
        parent(jump.withArgs("E" to iterationOwner))
    }

    /**
     * break 表达式节点。
     */
    val breakExpression: Element by element(Expression, name = "BreakExpression") {
        parent(loopJump)
    }

    /**
     * continue 表达式节点。
     */
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
        parent(typeParametersOwner)
        customParentInVisitor = function
        +FieldSets.typeParameters
        +declaredSymbol(anonymousFunctionSymbolType)
        +field("hasExplicitParameterList", booleanType)
        +field("isLambda", booleanType)
        +field("typeRef", typeRef, withReplace = true)
        +field("matchingParameterFunctionType", coneTypeType, nullable = true, withReplace = true)
    }

    /**
     * 已解析但携带错误诊断的引用节点。
     */
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
        +field("supportMock", booleanType, withReplace = true) { defaultValueInBuilder = "false" }
    }

    /**
     * range 表达式节点。
     */
    val rangeExpression: Element by element(Expression, name = "RangeExpression") {
        parent(expression)
        +field("start", expression, withTransform = true)
        +field("end", expression, withTransform = true)
        +field("step", expression, nullable = true, withTransform = true)
        +field("isInclusive", booleanType)
    }

    /**
     * array 字面量表达式节点。
     */
    val arrayLiteral: Element by element(Expression, name = "ArrayLiteral") {
        parent(expression)
        +listField("elements", expression, withTransform = true, withReplace = true)
        // initFunc 是引用目标；不能 bind 到 ArrayLit，也不能通过它扩大拥有域。
        +field("initializerSymbol", functionSymbolType, nullable = true, withReplace = true) {
            withBindThis = false
        }
    }

    /**
     * tuple 字面量表达式节点。
     */
    val tupleLiteral: Element by element(Expression, name = "TupleLiteral") {
        parent(expression)
        +listField("elements", expression, withTransform = true, withReplace = true)
    }

    /** 真实 F 的共同参数、返回类型、正文和 CFG 能力；具体声明继续持有自身种类的 symbol。 */
    val functionBodyOwner: Element by element(Other, name = "FunctionBodyOwner") {
        kind = ImplementationKind.Interface
        parent(targetElement)
        parent(controlFlowGraphOwner)
        customParentInVisitor = targetElement
        +field("contentSource", contentSourceType, isChild = false) {
            implementationDefaultStrategy = AbstractField.ImplementationDefaultStrategy.ConstructorDefaultValue("CfirContentSource.Owned")
            defaultValueInBuilder = "CfirContentSource.Owned"
            skippedInCopy = true
        }
        // 共同接口只暴露身份；具体 function/enumConstructor 的 declaredSymbol 在构造时绑定一次。
        +field("symbol", callableSymbolType, isChild = false) { withBindThis = false }
        +field("returnTypeRef", typeRef, withReplace = true, withTransform = true)
        +listField("valueParameters", valueParameter, withReplace = true, withTransform = true)
        +field("body", block, nullable = true, withReplace = true, withTransform = true) {
            publishedContentKind = Field.PublishedContentKind.BODY
        }
    }

    /** while、do-while 和 For 共用实际迭代目标；此接口不声明 condition。 */
    val iterationOwner: Element by element(Other, name = "IterationOwner") {
        kind = ImplementationKind.Interface
        parent(targetElement)
    }

    val targetElement: Element by element(Other, name = "TargetElement") {
        kind = ImplementationKind.Interface
    }

    /**
     * spawn 表达式节点。
     */
    val spawnExpression: Element by element(Expression, name = "SpawnExpression") {
        parent(expression)
        +field("body", block, withTransform = true)
        +field("threadContextArgument", expression, nullable = true, withTransform = true)
    }

    /**
     * synchronized 表达式节点。
     */
    val synchronizedExpression: Element by element(Expression, name = "SynchronizedExpression") {
        parent(expression)
        +field("monitor", expression, withTransform = true)
        +field("body", block, withTransform = true)
    }

    /**
     * unsafe 表达式节点。
     */
    val unsafeExpression: Element by element(Expression, name = "UnsafeExpression") {
        parent(expression)
        +field("body", expression, withTransform = true)
    }

    /**
     * quote 表达式节点。
     */
    val quoteExpression: Element by element(Expression, name = "QuoteExpression") {
        parent(expression)
        +field("rawText", stringType)
        +listField("interpolations", expression, withTransform = true)
    }


    /**
     * 下标访问表达式节点。
     */
    val subscriptExpression: Element by element(Expression, name = "SubscriptExpression") {
        parent(expression)
        +field("receiver", expression, withTransform = true)
        +listField("indices", expression, withTransform = true)
        +field("isTupleAccess", booleanType, withReplace = true) { defaultValueInBuilder = "false" }
        // 解析成功的 operator 调用保留完整绑定，供调用检查器和语义消费者使用。
        // 操作数已由下标/赋值节点拥有；这些引用不是重复的语法子节点。
        +field("resolvedGetCall", functionCall, nullable = true, withReplace = true, isChild = false) {
            sourceTraversalKind = Field.SourceTraversalKind.ELEMENT
        }
        +field("resolvedSetCall", functionCall, nullable = true, withReplace = true, isChild = false) {
            sourceTraversalKind = Field.SourceTraversalKind.ELEMENT
        }
    }

    /**
     * 携带 cone 诊断的通用节点。
     */
    val diagnosticHolder: Element by element(Diagnostics) {
        +field("diagnostic", coneDiagnosticType)
    }

    /**
     * 错误表达式占位节点。
     */
    val errorExpression: Element by element(Expression) {
        parent(expression)
        parent(diagnosticHolder)
        +field("expression", expression, nullable = true)
        +field("nonExpressionElement", rootElement, nullable = true)
    }

    /**
     * 所有模式节点的抽象基类。
     */
    val pattern: Element by sealedElement(Pattern) {
        parent(rootElement)
        // raw 尚未取得整体类型；正常解析或保存后 reader 必须填入 P 自己的真实类型。
        +field("semanticTypeRef", typeRef, nullable = true, withReplace = true, withTransform = true)
        +listField("constantValues", constantValueType, withReplace = true)
        +field("contextExpression", expression, nullable = true, isChild = false, withReplace = true) {
            defaultValueInBuilder = "null"
        }
    }

    /**
     * 常量模式节点。
     */
    val constPattern: Element by element(Pattern, name = "ConstPattern") {
        parent(pattern)
        +field("expression", expression, withReplace = true, withTransform = true)
        // 拥有真实 == 调用；raw FunctionCall 与保存后 CallExpression 共用 Expression 抽象。
        +field("operatorCall", expression, nullable = true, withReplace = true, withTransform = true)
    }

    /**
     * 通配符模式节点。
     */
    val wildcardPattern: Element by element(Pattern, name = "WildcardPattern") { parent(pattern) }

    /**
     * 绑定模式节点。
     */
    val bindingPattern: Element by element(Pattern, name = "BindingPattern") {
        parent(pattern)
        // raw 名字保留原语法；已绑定身份来自实际 declaration 槽，不按名字重新创建声明。
        +field("name", nameType)
        +field("typeRef", typeRef, nullable = true, withReplace = true, withTransform = true)
        +field("bindingVariable", patternBindingVariable, nullable = true, withReplace = true, withTransform = true)
        +field("nestedPattern", pattern, nullable = true, withReplace = true, withTransform = true)
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
        +field("bindingVariable", patternBindingVariable, nullable = true, withTransform = true)
    }

    /**
     * tuple 模式节点。
     */
    val tuplePattern: Element by element(Pattern, name = "TuplePattern") {
        parent(pattern)
        +listField("elements", pattern, withReplace = true, withTransform = true)
    }

    /**
     * enum 构造器模式节点。
     */
    val enumPattern: Element by element(Pattern, name = "EnumPattern") {
        parent(pattern)
        // 原 constructor expression 是唯一拥有入口，receiver/type arguments/回边均留在此节点。
        +field("constructor", expression, withReplace = true, withTransform = true)
        // reference 是原 constructor 或调用 base 的阶段视图；不重复拥有、存储或独立 transform。
        +field("constructorReference", reference, nullable = true, isChild = false)
        +listField("arguments", pattern, withReplace = true, withTransform = true)
    }

    /**
     * 类型模式节点。
     */
    val typePattern: Element by element(Pattern, name = "TypePattern") {
        parent(pattern)
        // matchingKind 保留现有 raw/checker 阶段状态；两个原生位由语义生产者独立填写。
        +field("matchingKind", typePatternMatchingKindType, withReplace = true) {
            defaultValueInBuilder = "CfirTypePatternMatchingKind.UNKNOWN"
        }
        +field("typeRef", typeRef, withReplace = true, withTransform = true)
        // inner P 自己拥有绑定变量和位置；不在 Type 上再次存储绑定名或变量。
        +field("nestedPattern", pattern, withReplace = true, withTransform = true)
        +field("matchBeforeRuntime", booleanType, withReplace = true)
        +field("needRuntimeTypeCheck", booleanType, withReplace = true)
    }

    /**
     * 所有类型引用节点的抽象基类。
     */
    val typeRef: Element by sealedElement(TypeRef) {

        parent(annotationContainer)
        +FieldSets.annotations
        +field("customRenderer", boolean) {
            defaultValueInBuilder = "false"
        }

        +field("typeUseOrigin", type("types", "CfirTypeRefOrigin"), withReplace = true) {
            defaultValueInBuilder = "CfirTypeRefOrigin.SEMANTIC"
        }

    }

    /**
     * 已解析类型引用节点。
     */
    val resolvedTypeRef: Element by element(TypeRef, name = "ResolvedTypeRef") {
        parent(typeRef)
        +field("coneType", coneTypeType)
        +field("delegatedTypeRef", typeRef, nullable = true, isChild = false) {
            sourceTraversalKind = Field.SourceTraversalKind.ELEMENT
        }
    }
    /**
     * 未解析类型引用抽象层。
     */
    val unresolvedTypeRef: Element by sealedElement(TypeRef) {
        parent(typeRef)

        +field("source", sourceElementType, nullable = false, withReplace = true)
    }

    /**
     * 用户类型限定名片段节点。
     */
    val qualifierPart: Element by element(Other, name = "QualifierPart") {
        +field("source", sourceElementType, nullable = true, isChild = false, withReplace = true)
        +field("name", nameType)
        +field("typeUseOrigin", type("types", "CfirTypeRefOrigin"), withReplace = true) {
            defaultValueInBuilder = "CfirTypeRefOrigin.SEMANTIC"
        }
        +FieldSets.typeArguments
    }

    /** 原生ParenType拥有独立来源/flags，不能在raw阶段直接删除外层节点。 */
    val parenthesizedTypeRef: Element by element(TypeRef, name = "ParenthesizedTypeRef") {
        parent(typeRef)
        +field("innerTypeRef", typeRef, withTransform = true)
    }

    /**
     * 用户类型引用节点。
     */
    val userTypeRef: Element by element(TypeRef, name = "UserTypeRef") {
        parent(unresolvedTypeRef)

        +listField("qualifier", qualifierPart, withReplace = true, withTransform = true, useMutableOrEmpty = true)
    }

    /**
     * 基础类型引用节点。
     */
    val basicTypeRef: Element by element(TypeRef, name = "BasicTypeRef") {
        parent(typeRef)
        +field("name", nameType)
    }

    /**
     * 隐式类型引用节点。
     */
    val implicitTypeRef: Element by element(TypeRef ) { parent(typeRef) }

    /**
     * 函数类型引用节点。
     */
    val functionTypeRef: Element by element(TypeRef, name = "FunctionTypeRef") {
        parent(typeRef)
        +listField("parameterTypeRefs", typeRef, withTransform = true)
        +field("returnTypeRef", typeRef, withTransform = true)
    }

    /**
     * `?T` 的语法糖类型引用。
     *
     * 这里显式保留 optional type 的语法来源，后续 resolve 再映射到 `Option<T>`，
     * 避免 raw CFIR 阶段过早退化成普通 user type。
     */
    val optionTypeRef: Element by element(TypeRef, name = "OptionTypeRef") {
        parent(unresolvedTypeRef)
        +field("componentTypeRef", typeRef, withTransform = true)
    }

    /**
     * tuple 类型引用节点。
     */
    val tupleTypeRef: Element by element(TypeRef, name = "TupleTypeRef") {
        parent(typeRef)
        +listField("elementTypeRefs", typeRef, withTransform = true)
    }

    /**
     * VArray 类型引用节点。
     */
    val varrayTypeRef: Element by element(TypeRef, name = "VArrayTypeRef") {
        parent(typeRef)
        +field("elementTypeRef", typeRef, withTransform = true)
        +field("sizeLiteral", stringType)
    }


    /**
     * 错误类型引用节点。
     */
    val errorTypeRef: Element by element(TypeRef) {
        parent(resolvedTypeRef)
        parent(diagnosticHolder)

        +field(
            "partiallyResolvedTypeRef",
            typeRef, nullable = true, withTransform = true
        )
    }

    /**
     * 所有引用节点的抽象基类。
     */
    val reference: Element by sealedElement(Reference) {
        parent(rootElement)
    }

    /**
     * 控制流图引用节点。
     */
    val controlFlowGraphReference: Element by element(Reference) {
        parent(reference)
    }

    /**
     * 未解析具名引用节点。
     */
    val namedReference: Element by element(Reference, name = "NamedReference") {
        parent(reference)
        +field("name", nameType)
    }

    /**
     * 已解析具名引用节点。
     */
    val resolvedNamedReference: Element by element(Reference, name = "ResolvedNamedReference") {
        parent(namedReference)
        +field("resolvedSymbol", symbolType)
    }

    /** 已绑定的仓颉内建运算引用；原生 primitive 运算没有 FuncDecl 符号。 */
    val resolvedBuiltinOperatorReference: Element by element(Reference, name = "ResolvedBuiltinOperatorReference") {
        parent(namedReference)
        +field("signature", type("types", "BuiltinPrimitiveOperatorSignature", kind = TypeKind.Class))
    }

    /**
     * 错误引用节点。
     */
    val errorReference: Element by element(Reference, name = "ErrorReference") {
        parent(reference)
        +field("reason", stringType)
    }
}
