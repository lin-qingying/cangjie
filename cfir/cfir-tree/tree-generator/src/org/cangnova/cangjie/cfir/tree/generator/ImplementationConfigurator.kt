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

import org.cangnova.cangjie.cfir.tree.generator.context.AbstractCfirTreeImplementationConfigurator
import org.cangnova.cangjie.cfir.tree.generator.model.Element
import org.cangnova.cangjie.cfir.tree.generator.model.Field
import org.cangnova.cangjie.cfir.tree.generator.model.Implementation
import org.cangnova.cangjie.generators.tree.AbstractField
import org.cangnova.cangjie.generators.tree.ImplementationKind
import org.cangnova.cangjie.generators.tree.Visibility
import org.cangnova.cangjie.generators.tree.config.AbstractImplementationConfigurator

/**
 * CFIR tree 具体实现类生成配置器。
 */
object ImplementationConfigurator : AbstractCfirTreeImplementationConfigurator() {

    /**
     * 配置各类 CFIR 元素是否生成实现类以及实现类字段默认行为。
     */
    override fun configure(model: Model) = with(CfirTree) {
        // ---------- 抽象节点：不生成具体实现类 ----------
        noImpl(elementWithResolveState)
        noImpl(declaration)
        noImpl(declarationStatus)
        noImpl(expression)
        noImpl(pattern)
        noImpl(typeRef)
        noImpl(reference)
        noImpl(controlFlowGraphReference)
        noImpl(annotationContainer)
        noImpl(controlFlowGraphOwner)
        noImpl(targetElement)
        noImpl(functionBodyOwner)
        noImpl(iterationOwner)
        noImpl(pattern)
        noImpl(statement)
        noImpl(jump)
        noImpl(loopJump)
        noImpl(memberDeclaration)
        noImpl(callableDeclaration)
        noImpl(classLikeDeclaration)
        noImpl(typeParametersOwner)
        noImpl(function)
        noImpl(variable)
        noImpl(annotationArgumentMapping)
        // 对位 Kotlin 的 expression 具名实现：签名默认值只携带类型和存在性，不拥有原 assignment 子图。
        impl(expression, "CfirExpressionStub") {
            publicImplementation()
        }
        impl(annotationCall) {
            default("arguments", "argumentList.arguments", withGetter = true)
        }

        // ---------- constructor 拆分为两个具名实现 ----------
        impl(constructor) {
            publicImplementation()
            defaultFalse("isPrimary", withGetter = true)
        }

        impl(constructor, "CfirPrimaryConstructor") {
            publicImplementation()
            defaultTrue("isPrimary", withGetter = true)
        }

        // ---------- Lazy 节点：访问任何字段时抛错 ----------

        impl(lazyBlock) {
            val error = """error("CfirLazyBlock should be resolved before accessing")"""
            default("source") { value = error; withGetter = true }
            default("coneTypeOrNull") { value = error; withGetter = true }
            default("annotations") { value = error; withGetter = true }
            default("statements") { value = error; withGetter = true }
            default("isUnsafe") { value = error; withGetter = true }
            publicImplementation()
        }
        impl(functionCall) {
            kind = ImplementationKind.OpenClass
        }
        impl(lazyExpression) {
            val error = """error("CfirLazyExpression should be resolved before accessing")"""
            default("coneTypeOrNull") { value = error; withGetter = true }
            default("annotations") { value = error; withGetter = true }
            publicImplementation()
        }


        // ---------- 字段精细配置 ----------


        impl(errorExpression) {
            default("coneTypeOrNull", "expression?.coneTypeOrNull ?: ConeErrorType(ConeUnreportedDuplicateDiagnostic(diagnostic))", withGetter = true)
            additionalImports(coneErrorTypeType, coneUnreportedDuplicateDiagnosticType)
        }

        // anonymousFunctionExpression：类型和注解代理到内部匿名函数
        impl(anonymousFunctionExpression) {
            additionalImports(coneTypeOrNull)
            default("coneTypeOrNull") {
                delegate = "anonymousFunction"
                delegateCall = "typeRef.coneTypeOrNull"
                withGetter = true
            }
            default("annotations") {
                delegate = "anonymousFunction"
                withGetter = true
            }
        }
        impl(implicitTypeRef) {
            noSource()
        }
        // resolvedTypeRef：public 可见性，供外部直接使用
        impl(resolvedTypeRef) {
            publicImplementation()
        }
// importDirective 是抽象基类，不生成实现
        impl(importDirective)
        impl(typeAlias) {

            additionalImports(visibilitiesImport)
        }
        /**
         * 配置 value parameter 与相关节点共享的字段默认值。
         */
        fun AbstractImplementationConfigurator<Implementation, Element, Field>.ImplementationContext.configureCommonValueParameter() {
            defaultFalse("isVar", withGetter = true)
            defaultNull(
                "initializer",
                withGetter = true
            )

        }
        impl(valueParameter){
            configureCommonValueParameter()
        }

        fun AbstractImplementationConfigurator<Implementation, Element, Field>.ImplementationContext.configurePatternTreeOwner(
            fieldName: String,
        ) {
            default(fieldName) {
                customSetterVisibility = Visibility.PRIVATE
                customSetter = """
                    if (org.cangnova.cangjie.cfir.patterns.isPatternTreeRootRestoreInProgress()) {
                        field = value
                    } else {
                        val previousValue = field
                        val previousTree = org.cangnova.cangjie.cfir.patterns.capturePatternTree(previousValue)
                        org.cangnova.cangjie.cfir.patterns.validatePatternTreeReplacement(this, value)
                        field = value
                        try {
                            org.cangnova.cangjie.cfir.patterns.reconcilePatternTree(this, previousTree, patternTreeRoot())
                        } catch (failure: Throwable) {
                            field = previousValue
                            throw failure
                        }
                    }
                """.trimIndent()
            }
            initializationStatements(
                "org.cangnova.cangjie.cfir.patterns.bindPatternTree(this, patternTreeRoot())",
            )
        }

        impl(patternVariable) {
            default("initializer") {
                customSetterVisibility = Visibility.PRIVATE
                customSetter = """
                    field = value
                """.trimIndent()
            }
            configurePatternTreeOwner("pattern")
        }
        impl(patternBindingVariable) {
            default("initializer") {
                value = "ownedInitializer ?: org.cangnova.cangjie.cfir.patterns.wholeValueInitializer(patternTreeOwner, this)"
                withGetter = true
                customSetterVisibility = Visibility.PRIVATE
                customSetter = "error(\"CfirPatternBindingVariable.initializer is derived from its owner and cannot be changed.\")"
                customTransform = "error(\"CfirPatternBindingVariable.initializer is derived from its owner and cannot be transformed independently.\")"
            }
            default("patternTreeOwner") {
                value = "null"
                customSetterVisibility = Visibility.PRIVATE
                customSetter = """
                    if (org.cangnova.cangjie.cfir.patterns.isPatternTreeRootRestoreInProgress()) {
                        field = value
                    } else {
                        org.cangnova.cangjie.cfir.patterns.validatePatternTreeNodeOwnerChange(this, field, value)
                        field = value
                    }
                """.trimIndent()
            }
        }

        fun AbstractImplementationConfigurator<Implementation, Element, Field>.ImplementationContext.configurePatternTreeNode() {
            default("patternTreeOwner") {
                value = "null"
                customSetterVisibility = Visibility.PRIVATE
                customSetter = """
                    if (org.cangnova.cangjie.cfir.patterns.isPatternTreeRootRestoreInProgress()) {
                        field = value
                    } else {
                        org.cangnova.cangjie.cfir.patterns.validatePatternTreeNodeOwnerChange(this, field, value)
                        field = value
                    }
                """.trimIndent()
            }
        }

        fun AbstractImplementationConfigurator<Implementation, Element, Field>.ImplementationContext.configurePatternTreeChild(
            fieldName: String,
            listField: Boolean = false,
            nullable: Boolean = true,
        ) {
            default(fieldName) {
                if (listField) {
                    // 模式节点列表只能通过生成的 replace/transform API 改写，避免调用方绕过 owner 同步。
                    readOnlyImplementationList = true
                }
                customSetterVisibility = Visibility.PRIVATE
                customSetter = """
                    val owner = patternTreeOwner
                    val previousTree = owner?.let { org.cangnova.cangjie.cfir.patterns.capturePatternTree(it.patternTreeRoot()) }
                    val previousValue = field
                    field = ${if (listField) "java.util.Collections.unmodifiableList(value.toList())" else "value"}
                    try {
                        if (owner != null) {
                            org.cangnova.cangjie.cfir.patterns.reconcilePatternTree(owner, previousTree!!, owner.patternTreeRoot())
                        } else {
                            org.cangnova.cangjie.cfir.patterns.validateDetachedPatternTree(this)
                        }
                    } catch (failure: Throwable) {
                        field = previousValue
                        throw failure
                    }
                """.trimIndent()
                customTransform = if (listField) {
                    """
                        val owner = patternTreeOwner
                        val previousTree = owner?.let { org.cangnova.cangjie.cfir.patterns.capturePatternTree(it.patternTreeRoot()) }
                        try {
                            $fieldName = $fieldName.map { it.transform(transformer, data) }
                        } finally {
                            if (owner != null) {
                                org.cangnova.cangjie.cfir.patterns.reconcilePatternTree(owner, previousTree!!, owner.patternTreeRoot())
                            } else {
                                org.cangnova.cangjie.cfir.patterns.validateDetachedPatternTree(this)
                            }
                        }
                    """.trimIndent()
                } else {
                    """
                        val owner = patternTreeOwner
                        val previousTree = owner?.let { org.cangnova.cangjie.cfir.patterns.capturePatternTree(it.patternTreeRoot()) }
                        try {
                            $fieldName = $fieldName${if (nullable) "?." else "."}transform(transformer, data)
                        } finally {
                            if (owner != null) {
                                org.cangnova.cangjie.cfir.patterns.reconcilePatternTree(owner, previousTree!!, owner.patternTreeRoot())
                            } else {
                                org.cangnova.cangjie.cfir.patterns.validateDetachedPatternTree(this)
                            }
                        }
                    """.trimIndent()
                }
                if (listField) {
                    val newValue = "new${fieldName.replaceFirstChar(Char::uppercaseChar)}"
                    customReplace = """
                        $fieldName = $newValue
                    """.trimIndent()
                }
            }
        }

        impl(constPattern) { configurePatternTreeNode() }
        impl(wildcardPattern) { configurePatternTreeNode() }
        impl(expressionPattern) { configurePatternTreeNode() }
        impl(catchPattern) {
            configurePatternTreeNode()
            configurePatternTreeChild("nestedPattern", nullable = false)
        }
        impl(commandTypePattern) {
            configurePatternTreeNode()
            configurePatternTreeChild("nestedPattern", nullable = false)
        }
        impl(bindingPattern) {
            configurePatternTreeNode()
            configurePatternTreeChild("bindingVariable")
            configurePatternTreeChild("nestedPattern")
        }
        impl(varOrEnumPattern) {
            configurePatternTreeNode()
            configurePatternTreeChild("bindingVariable")
        }
        impl(tuplePattern) {
            configurePatternTreeNode()
            configurePatternTreeChild("elements", listField = true)
        }
        impl(enumPattern) {
            // 只读取实际 expression 的 callee；未解析 NamedReference 也留在原访问节点中。
            // CallExpression 从实际 base 读取，错误/非访问 expression 没有 reference 视图。
            additionalImports(resolvable, callExpression, errorExpression)
            default("constructorReference", """
                when (val expression = constructor) {
                    is CfirResolvable -> expression.calleeReference
                    is CfirCallExpression -> checkNotNull(expression.base as? CfirResolvable) {
                        "Enum constructor call must retain its actual reference-bearing base"
                    }.calleeReference
                    is CfirErrorExpression -> null
                    else -> error("Enum constructor must retain its actual reference-bearing expression")
                }
            """.trimIndent(), withGetter = true)
            configurePatternTreeNode()
            configurePatternTreeChild("arguments", listField = true)
        }
        impl(typePattern) {
            configurePatternTreeNode()
            configurePatternTreeChild("nestedPattern", nullable = false)
        }
        impl(orPattern) {
            configurePatternTreeNode()
            configurePatternTreeChild("alternatives", listField = true)
        }

        impl(letPatternExpression) { configurePatternTreeOwner("pattern") }
        impl(forInExpression) { configurePatternTreeOwner("pattern") }
        impl(matchBranch) { configurePatternTreeOwner("pattern") }
        impl(catchClause) { configurePatternTreeOwner("pattern") }
        impl(handleClause) { configurePatternTreeOwner("commandPattern") }

// resolvedImportDirective：委托字段给 delegate
        impl(resolvedImportDirective) {
            publicImplementation()
            optInToInternals()
            // organizationName、aliasName、aliasSource、importedFqName、isAllUnder 全部委托给原始节点
            delegateFields(listOf("organizationName", "aliasName", "aliasSource", "importedFqName", "isAllUnder", "visibility"), "delegate")
            // source 也委托给原始节点
            default("source") {
                delegate = "delegate"
                customSetter = "delegate.replaceSource(value)"
            }
            default("aliasSource") {
                delegate = "delegate"
                customSetter = "delegate.replaceAliasSource(value)"
            }
            // importedName：从 importedFqName 取最后一段短名
            default("importedName") {
                delegate = "importedFqName"
                delegateCall = "shortName()"
                withGetter = true
            }
        }

        impl(errorNamedValue) {


            default("returnTypeRef", "CfirErrorTypeRefImpl(source, MutableOrEmptyList.empty(), null, null, diagnostic)")
            default("isLocal") {
                value = "false"
                withGetter = true
            }
            additionalImports(errorTypeRefImplType)
        }
        impl(errorFunction) {
            default("returnTypeRef", "CfirErrorTypeRefImpl(null, MutableOrEmptyList.empty(), null, null, diagnostic)")
            default("isLocal") {
                value = "false"
                withGetter = true
            }
            additionalImports(errorTypeRefImplType)
        }
        // ---------- 具体节点：生成公开实现类 ----------
        concreteElements().forEach { element ->
            impl(element) {
                publicImplementation()

            }
        }
    }

    /**
     * 配置所有实现类共同继承的字段默认值和内部 API opt-in。
     */
    override fun configureAllImplementations(model: Model) {
        fun hasDeclarationSupertype(element: Element): Boolean =
            element == CfirTree.declaration || element.allParents.any(::hasDeclarationSupertype)

        // 新target源迁移不能破坏Source-origin声明始终有源的构造不变量；直接var写入也必须受同一约束。
        configureAllImplementations(
            implementationPredicate = { implementation ->
                val source = implementation.allFields.firstOrNull { it.name == "source" }
                source != null && source.isMutable && source.typeRef.nullable &&
                    hasDeclarationSupertype(implementation.element)
            },
        ) {
            val source = implementation.allFields.single { it.name == "source" }
            source.customSetter = """
                require(value != null || origin != CfirDeclarationOrigin.Source) {
                    "A Source-origin declaration must retain its source element."
                }
                field = value
            """.trimIndent()
        }
        // 数据源在真实声明首次构造时绑定；普通字段保留原后备存储，Library getter 只读完成快照。
        configureAllImplementations(
            implementationPredicate = { implementation -> implementation.allFields.any { it.name == "contentSource" } },
        ) {
            val contentSource = implementation.allFields.single { it.name == "contentSource" }
            contentSource.isMutable = false
            contentSource.skippedInCopy = true
            contentSource.defaultValueInBuilder = "CfirContentSource.Owned"
            contentSource.implementationDefaultStrategy =
                AbstractField.ImplementationDefaultStrategy.ConstructorDefaultValue("CfirContentSource.Owned")
            val contents = implementation.allFields.filter {
                it.publishedContentKind != null && it.implementationDefaultStrategy?.withGetter != true
            }
            // 一个实现只持有每种发布槽一次；共同 F owner 不创建第二个 body 存储或 setter。
            check(contents.map { it.publishedContentKind }.distinct().size == contents.size) {
                "Published content roles must have a single storage field"
            }
            contents.forEach { content ->
                val read = when (content.publishedContentKind!!) {
                    Field.PublishedContentKind.BODY -> "body"
                    Field.PublishedContentKind.INITIALIZER -> "initializer"
                    Field.PublishedContentKind.DEFAULT_VALUE -> "defaultValue"
                    Field.PublishedContentKind.ANNOTATIONS -> "annotations"
                    Field.PublishedContentKind.DEPRECATIONS -> "deprecations"
                }
                val selectedValue = "selectedSource.snapshot().$read(this)" +
                    if (content.publishedContentKind == Field.PublishedContentKind.ANNOTATIONS) ".values" else ""
                content.customGetter = """
                    when (val selectedSource = contentSource) {
                        CfirContentSource.Owned -> field
                        is CfirContentSource.Published -> $selectedValue
                    }
                """.trimIndent()
                content.customSetter = """
                    check(contentSource === CfirContentSource.Owned) {
                        "Published Library content cannot be changed through an ordinary setter"
                    }
                    field = value
                """.trimIndent()
                content.isMutable = true
            }
        }
        // annotations 全局兜底：默认空列表
        // Lazy 节点已单独配置为抛错，排除在外
//        configureFieldInAllImplementations(
//            fieldName = "annotations",
//            implementationPredicate = {
//                it.typeName != "CfirLazyBlockImpl" &&
//                        it.typeName != "CfirLazyExpressionImpl"
//            }
//        ) {
//            defaultEmptyList(it, withGetter = false)
//        }

        // controlFlowGraphReference 默认 null
        // anonymousFunctionExpression 的 cfg 由控制流分析阶段填充，排除在外
        configureFieldInAllImplementations(
            fieldName = "controlFlowGraphReference",
            implementationPredicate = { it.typeName != "CfirAnonymousFunctionExpressionImpl" }
        ) {
            defaultNull(it)
        }

        // Declaration 子类统一加 @OptIn
        // 仓颉编译器内部声明类 API 需要 opt-in 标注
        configureAllImplementations(
            implementationPredicate = { impl ->
                hasDeclarationSupertype(impl.element)
            }
        ) {
            optInToInternals()
        }
    }

    /**
     * 需要生成公开实现类的具体 CFIR 元素列表。
     */
    private fun CfirTree.concreteElements(): List<Element> = listOf(

        // -------- 顶层结构 --------
        packageDirective,
        importDirective,

        // -------- 声明节点 --------
        file,
        codeFragment,
        classDeclaration,
        interfaceDeclaration,
        structDeclaration,
        enumDeclaration,
        enumConstructor,
        extend,
        typeAlias,
        namedFunction,
        anonymousFunction,
        mainFunction,
        macroDeclaration,
        finalizer,
        // constructor 已单独配置为两个具名实现
        invalidDeclaration,
        property,
        propertyAccessor,
        fieldVariable,
        resourceVariable,
        patternVariable,
        patternBindingVariable,
        valueParameter,
        typeParameter,
        constructedClassTypeParameterRef,

        // -------- 语句 / 表达式节点 --------
        nativeMatchExpression,
        nativeMatchCase,
        nativeConditionMatchExpression,
        nativeConditionMatchCase,
        letPatternDestructorExpression,
        nativeCatch,
        nativeTryExpression,
        primitiveTypeExpression,
        wildcardExpression,
        functionArgument,
        callExpression,
        unaryExpression,
        binaryExpression,
        arrayCreationExpression,
        pointerExpression,
        resolvedSpawnExpression,
        ifAvailableExpression,
        block,
        // lazyBlock 已单独配置
        // lazyExpression 已单独配置
        literalExpression,
        stringInterpolation,

        namedAccessExpression,
        qualifiedAccessExpression,
        resolvedQualifier,
        errorResolvedQualifier,
        assignment,
        binaryOp,
        comparisonExpression,
        typeOperator,
        typeConversion,
        letPatternExpression,
        ifExpression,
        matchExpression,
        matchBranch,
        catchClause,
        loopExpression,
        forInExpression,
        tryExpression,
        throwExpression,
        returnExpression,
        breakExpression,
        continueExpression,
        anonymousFunctionExpression,
        rangeExpression,
        arrayLiteral,
        tupleLiteral,
        spawnExpression,
        inoutArgumentExpression,
        synchronizedExpression,
        unsafeExpression,
        quoteExpression,
        subscriptExpression,
        errorExpression,

        // -------- 模式节点 --------
        constPattern,
        wildcardPattern,
        bindingPattern,
        tuplePattern,
        enumPattern,
        typePattern,
        catchPattern,
        commandTypePattern,

        // -------- 类型引用节点 --------
        qualifierPart,
        resolvedTypeRef,
        parenthesizedTypeRef,
        userTypeRef,
        basicTypeRef,
        implicitTypeRef,
        functionTypeRef,
        tupleTypeRef,
        varrayTypeRef,

        // -------- 引用节点 --------
        namedReference,
        resolvedNamedReference,
        resolvedBuiltinOperatorReference,
        errorReference,
        thisReference,
    )
}
