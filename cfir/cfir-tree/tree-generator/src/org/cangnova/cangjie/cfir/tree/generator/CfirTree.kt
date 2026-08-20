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
     * 声明状态接口类型引用。
     */
    private val declarationStatusType = generatedType("declarations", "CfirDeclarationStatus", TypeKind.Interface)
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
        /**
         * 类型参数列表字段集合。
         */
        val typeParameters = fieldSet(
            listField(
                "typeParameters",
                typeParameter
            )
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

        +field(expression)
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
        +field("isAllUnder", booleanType)
        +field("aliasName", nameType, nullable = true)
        +field("aliasSource", sourceElementType, nullable = true)
    }

    /**
     * 已解析 import 指令节点。
     */
    val resolvedImportDirective: Element by element(Declaration, name = "ResolvedImport") {
        parent(importDirective)
        +field("delegate", importDirective, isChild = false)
        +field("packageFqName", fqNameType)
        +field("importedName", nameType, nullable = true)
    }

    /**
     * 注解实例节点。
     */
    val annotation: Element by element(Expression, name = "Annotation") {
        parent(expression)

        +field("typeRef", typeRef, withTransform = true)
        +listField("arguments", rootElement, withTransform = true)
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
        +field("sourceFile", sourceFileType, nullable = true)
        +field("packageDirective", packageDirective, withTransform = true)
        +listField("imports", importDirective, withTransform = true)
        +field("sourceFileLinesMapping", sourceFileLinesMappingType, nullable = true)

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
     * isNonExhaustive 保存 enum body 中的 `...`，作为 match 穷尽性判断的声明级语义。
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
        +declaredSymbol(enumConstructorSymbolType)
        +field("status", declarationStatusType, withReplace = true, withTransform = true)
        +FieldSets.typeParameters
        +field("returnTypeRef", typeRef, withReplace = true, withTransform = true)
        +listField("valueParameters", valueParameter, withTransform = true)
        +field("name", nameType)
    }

    /**
     * extend 声明节点。
     */
    val extend: Element by element(Declaration, name = "Extend") {
        parent(memberDeclaration)
        +declaredSymbol(extendSymbolType)
        +field("status", declarationStatusType, withReplace = true, withTransform = true)
        +FieldSets.typeParameters
        +field("extendedTypeRef", typeRef, withTransform = true)
        +listField("superTypeRefs", typeRef, withTransform = true)
        +FieldSets.declarations
    }

    /**
     * typealias 声明节点。
     */
    val typeAlias: Element by element(Declaration, name = "TypeAlias") {
        parent(classLikeDeclaration)
        +declaredSymbol(typeAliasSymbolType)
        +field("status", declarationStatusType, withReplace = true, withTransform = true)
        +FieldSets.typeParameters
        +field("name", nameType)
        +field("expandedTypeRef", typeRef, withReplace = true, withTransform = true)
    }

    /**
     * 函数类声明抽象层。
     */
    val function: Element by sealedElement(Declaration, name = "Function") {
        parent(callableDeclaration)
        parent(targetElement)
        parent(controlFlowGraphOwner)
        parent(statement)
        customParentInVisitor = callableDeclaration
        +declaredSymbol(functionSymbolType)
        +field("status", declarationStatusType, withReplace = true, withTransform = true)
        +FieldSets.typeParameters
        +field("returnTypeRef", typeRef, withReplace = true, withTransform = true)
        +listField("valueParameters", valueParameter, withReplace = true, withTransform = true)
        +field("body", block, nullable = true, withReplace = true, withTransform = true)
    }

    /**
     * 具名函数声明节点。
     */
    val namedFunction: Element by element(Declaration, name = "NamedFunction") {
        parent(function)
        +declaredSymbol(namedFunctionSymbolType)
        +field("name", nameType)
        +field("isMut", booleanType)
    }

    /**
     * main 函数声明节点。
     */
    val mainFunction: Element by element(Declaration, name = "MainFunction") {
        parent(function)
        +declaredSymbol(mainFunctionSymbolType)
    }

    /**
     * 宏声明节点。
     */
    val macroDeclaration: Element by element(Declaration, name = "MacroDeclaration") {
        parent(function)
        +declaredSymbol(macroDeclarationSymbolType)
        +field("name", nameType)
    }

    /**
     * finalizer 声明节点。
     */
    val finalizer: Element by element(Declaration, name = "Finalizer") {
        parent(function)
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

    /**
     * 属性声明节点。
     */
    val property: Element by element(Declaration, name = "Property") {
        parent(callableDeclaration)
        parent(controlFlowGraphOwner)
        customParentInVisitor = callableDeclaration
        +declaredSymbol(propertySymbolType)
        +field("status", declarationStatusType, withReplace = true, withTransform = true)
        +FieldSets.typeParameters
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
        customParentInVisitor = function
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
        +field("status", declarationStatusType, withReplace = true, withTransform = true)
        +field("initializer", expression, nullable = true, withReplace = true, withTransform = true)
        +field("isVar", booleanType)
    }

    /**
     * 字段变量声明节点。
     */
    val fieldVariable: Element by element(Declaration, name = "FieldVariable") {
        parent(variable)
        +declaredSymbol(fieldVariableSymbolType)
        +FieldSets.typeParameters
        +field("returnTypeRef", typeRef, withReplace = true, withTransform = true)
        +field("name", nameType)
    }

    /**
     * 模式变量声明容器节点。
     */
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

    /**
     * 值参数声明节点。
     */
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
        +field("defaultValue", expression, nullable = true, withReplace = true, withTransform = true)
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
        +listField("bounds", typeRef, withTransform = true)
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
            "common",
            "specific",
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
    }

    /**
     * 块表达式节点。
     */
    val block: Element by element(Expression, name = "Block") {
        needTransformOtherChildren()

        parent(expression)
        +listField("statements", statement, withTransform = true)
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
        +field("operationSource", sourceElementType, nullable = true)
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
        +field("nameSource", sourceElementType, nullable = true)
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
        +field("typeMismatchOutcome", assignmentTypeMismatchOutcomeType, nullable = true, withReplace = true)
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
     * catch 子句的异常模式。
     *
     * 官方仓颉 AST 中 `catch (_)` 是 `WildcardPattern`，`catch (e: E | F)` 是
     * `ExceptTypePattern(pattern = VarPattern(e), types = [E, F])`。这里保留
     * “绑定名 + 多个异常类型”的结构，但不接到通用 match pattern，避免把异常模式
     * 混入普通模式匹配的合法性和穷尽性分析。
     */
    val catchPattern: Element by element(Pattern, name = "CatchPattern") {
        parent(rootElement)
        +field("bindingName", nameType, nullable = true)
        +field("isWildcard", booleanType)
        +listField("typeRefs", typeRef, withTransform = true)
        +field("bindingVariable", patternBindingVariable, nullable = true, withTransform = true)
    }

    /**
     * catch 子句节点。
     */
    val catchClause: Element by element(Expression, name = "Catch") {
        parent(expression)
        +field("pattern", catchPattern, withTransform = true)
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

    /**
     * loop / while / do-while 表达式节点。
     */
    val loopExpression: Element by element(Expression, name = "LoopExpression") {
        parent(expression)
        parent(targetElement)
        +field("condition", expression, withTransform = true)
        +field("body", block, withTransform = true)
        +field("isDoWhile", booleanType)
    }

    /**
     * for-in 循环表达式节点。
     */
    val forInExpression: Element by element(Expression, name = "ForInExpression") {
        parent(loopExpression)
        +field("variable", patternVariable, withTransform = true)
        +field("iterable", expression, withTransform = true)
        +field("body", block, withTransform = true)
    }

    /**
     * try 表达式节点。
     */
    val tryExpression: Element by element(Expression, name = "TryExpression") {
        parent(expression)
        +listField("resources", fieldVariable, withTransform = true)
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
        +listField("elements", expression, withTransform = true)
    }

    /**
     * tuple 字面量表达式节点。
     */
    val tupleLiteral: Element by element(Expression, name = "TupleLiteral") {
        parent(expression)
        +listField("elements", expression, withTransform = true)
    }

    /**
     * 跳转 target 元素接口。
     */
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
    }

    /**
     * 常量模式节点。
     */
    val constPattern: Element by element(Pattern, name = "ConstPattern") {
        parent(pattern)
        +field("expression", expression, withTransform = true)
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
        +field("name", nameType)
        +field("typeRef", typeRef, nullable = true, withTransform = true)
        +field("bindingVariable", patternBindingVariable, nullable = true, withTransform = true)
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
        +field("bindingVariable", patternBindingVariable, nullable = true, withTransform = true)
    }

    /**
     * tuple 模式节点。
     */
    val tuplePattern: Element by element(Pattern, name = "TuplePattern") {
        parent(pattern)
        +listField("elements", pattern, withTransform = true)
    }

    /**
     * enum 构造器模式节点。
     */
    val enumPattern: Element by element(Pattern, name = "EnumPattern") {
        parent(pattern)
        +field("constructorReference", reference, withTransform = true)
        +listField("arguments", pattern, withTransform = true)
    }

    /**
     * 类型模式节点。
     */
    val typePattern: Element by element(Pattern, name = "TypePattern") {
        parent(pattern)
        +field("typeRef", typeRef, withTransform = true)
        +field("bindingName", nameType, nullable = true)
        +field("bindingVariable", patternBindingVariable, nullable = true, withTransform = true)
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

    }

    /**
     * 已解析类型引用节点。
     */
    val resolvedTypeRef: Element by element(TypeRef, name = "ResolvedTypeRef") {
        parent(typeRef)
        +field("coneType", coneTypeType)
        +field("delegatedTypeRef", typeRef, nullable = true, isChild = false)
    }
    /**
     * 未解析类型引用抽象层。
     */
    val unresolvedTypeRef: Element by sealedElement(TypeRef) {
        parent(typeRef)

        +field("source", sourceElementType, nullable = false)
    }

    /**
     * 用户类型限定名片段节点。
     */
    val qualifierPart: Element by element(Other, name = "QualifierPart") {
        +field("source", sourceElementType, nullable = true, isChild = false)
        +field("name", nameType)
        +FieldSets.typeArguments
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

    /**
     * 错误引用节点。
     */
    val errorReference: Element by element(Reference, name = "ErrorReference") {
        parent(reference)
        +field("reason", stringType)
    }
}
