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
        noImpl(statement)
        noImpl(jump)
        noImpl(loopJump)
        noImpl(memberDeclaration)
        noImpl(callableDeclaration)
        noImpl(classLikeDeclaration)
        noImpl(function)
        noImpl(variable)
        noImpl(annotationArgumentMapping)
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
            publicImplementation()
        }
        impl(functionCall) {
            kind = ImplementationKind.OpenClass
        }
        impl(lazyExpression) {
            val error = """error("CfirLazyExpression should be resolved before accessing")"""
            default("source") { value = error; withGetter = true }
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
                value = "org.cangnova.cangjie.cfir.patterns.wholeValueInitializer(patternTreeOwner, this)"
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
                            $fieldName = $fieldName?.transform(transformer, data)
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
            configurePatternTreeChild("bindingVariable")
        }
        impl(commandTypePattern) { configurePatternTreeNode() }
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
            configurePatternTreeNode()
            configurePatternTreeChild("arguments", listField = true)
        }
        impl(typePattern) {
            configurePatternTreeNode()
            configurePatternTreeChild("bindingVariable")
        }
        impl(orPattern) {
            configurePatternTreeNode()
            configurePatternTreeChild("alternatives", listField = true)
        }

        impl(letPatternExpression) { configurePatternTreeOwner("pattern") }
        impl(matchBranch) { configurePatternTreeOwner("pattern") }
        impl(catchClause) { configurePatternTreeOwner("pattern") }
        impl(handleClause) { configurePatternTreeOwner("commandPattern") }

// resolvedImportDirective：委托字段给 delegate
        impl(resolvedImportDirective) {
            publicImplementation()
            // organizationName、aliasName、aliasSource、importedFqName、isAllUnder 全部委托给原始节点
            delegateFields(listOf("organizationName", "aliasName", "aliasSource", "importedFqName", "isAllUnder", "visibility"), "delegate")
            // source 也委托给原始节点
            default("source") {
                delegate = "delegate"
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
                fun hasDeclSupertype(element: Element): Boolean =
                    element == CfirTree.declaration ||
                            element.allParents.any { hasDeclSupertype(it) }
                hasDeclSupertype(impl.element)
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
        patternVariable,
        patternBindingVariable,
        valueParameter,
        typeParameter,

        // -------- 语句 / 表达式节点 --------
        ifAvailableExpression,
        block,
        // lazyBlock 已单独配置
        // lazyExpression 已单独配置
        literalExpression,
        stringInterpolation,

        namedAccessExpression,
        qualifiedAccessExpression,
        assignment,
        binaryOp,
        comparisonExpression,
        typeOperator,
        typeConversion,
        letPatternExpression,
        ifExpression,
        matchExpression,
        matchBranch,
        catchPattern,
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

        // -------- 类型引用节点 --------
        qualifierPart,
        resolvedTypeRef,
        userTypeRef,
        basicTypeRef,
        implicitTypeRef,
        functionTypeRef,
        tupleTypeRef,
        varrayTypeRef,

        // -------- 引用节点 --------
        namedReference,
        resolvedNamedReference,
        errorReference,
        thisReference,
    )
}
