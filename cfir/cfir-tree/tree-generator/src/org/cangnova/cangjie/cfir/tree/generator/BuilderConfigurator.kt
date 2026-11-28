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

import org.cangnova.cangjie.cfir.tree.generator.model.Element
import org.cangnova.cangjie.cfir.tree.generator.model.Field
import org.cangnova.cangjie.cfir.tree.generator.model.Implementation
import org.cangnova.cangjie.generators.tree.config.AbstractBuilderConfigurator

/**
 * CFIR tree builder 生成配置器。
 */
class BuilderConfigurator(model: Model) : AbstractBuilderConfigurator<Element, Implementation, Field>(model) {
    /**
     * 生成 builder 名称使用的统一前缀。
     */
    override val namePrefix: String
        get() = "Cfir"

    /**
     * 默认 builder 输出包名。
     */
    override val defaultBuilderPackage: String
        get() = "org.cangnova.cangjie.cfir.tree.builder"

    /**
     * 配置所有 CFIR 元素 builder、默认字段值和复制策略。
     */
    override fun configureBuilders() = with(CfirTree) {
        val declarationBuilder by builder {
            fields from declaration without "symbol"
        }
        val annotationContainerBuilder by builder {
            fields from annotationContainer without "source"
        }
        // function 的共同 F 字段已由 FunctionBodyOwner 继承；enum 保持自己的 leaf builder 和 symbol 类型。
        val functionBuilder by builder {
            parents += declarationBuilder
            parents += annotationContainerBuilder
            fields from function without listOf("symbol", "resolvePhase", "resolveState", "controlFlowGraphReference", "typeParameters")
        }
        concreteElements().forEach { element ->
            builder(element) {
                withCopy()
            }
        }
        listOf(namedFunction, mainFunction, macroDeclaration, finalizer, anonymousFunction, propertyAccessor).forEach { element ->
            builder(element) {
                parents += functionBuilder
            }
        }

        // constructor 有两个具名实现，需要分别指定
        builder(constructor, "CfirConstructorImpl") {
            parents += functionBuilder
            withCopy()
        }
        builder(constructor, "CfirPrimaryConstructor") {
            parents += functionBuilder
            withCopy()
        }
        // Stub 是既有 Expression 的具名实现，不新增语法种类或独立 visitor；builder 返回抽象 Expression。
        builder(expression, "CfirExpressionStub") {
            parents += annotationContainerBuilder
            defaultNull("source")
            defaultNull("coneTypeOrNull")
        }
        builder(ifExpression) {
            // 未进入 select producer 的 RAW if 只持有无状态的未解析引用。
            default("calleeReference") {
                value = "org.cangnova.cangjie.cfir.references.CfirStubReference"
            }
        }
        val callBuilder by builder {
            fields from call
        }
        val qualifiedAccessExpressionBuilder by builder {
            fields from qualifiedAccessExpression without "calleeReference"
        }
        val abstractFunctionCallBuilder by builder {
            parents += qualifiedAccessExpressionBuilder
            parents += callBuilder
            fields from functionCall
        }
        val configurationForFunctionCallBuilder: LeafBuilderConfigurationContext.() -> Unit = {
            parents += abstractFunctionCallBuilder
            defaultNoReceivers()

            openBuilder()
            default("argumentList") {
                value = "CfirEmptyArgumentList"
            }
            additionalImports(emptyArgumentListType)
        }
        builder(argumentList) {
            withCopy()
        }
        builder(functionCall) {
            configurationForFunctionCallBuilder()
            default("origin") {
                value = "CfirFunctionCallOrigin.Regular"
            }
            defaultFalse("hasTrailingLambda")
            // 原生 Expr 的属性初值没有 SIDE_EFFECT；真实赋值/脱糖生产者显式发布该位。
            defaultFalse("sourceCallHasSideEffect")

            withCopy()
        }
        builder(annotationCall) {
            default("argumentMapping", "CfirEmptyAnnotationArgumentMapping")
            additionalImports(emptyAnnotationArgumentMappingType)
            defaultFalse("forcedCustom")
            // 合成及反序列化节点没有源码 parser 模块；Raw 源码入口必须显式填入解析来源。
            default("sourceModuleName", "\"\"")
            default("annotationResolveState", "CfirAnnotationResolveState.UNRESOLVED")
            additionalImports(annotationResolveStateType)
            withCopy()
        }
        // resolvedImportDirective 单独配置
        builder(importDirective) {
            default("visibility", "org.cangnova.cangjie.descriptors.Visibilities.Private")
        }
        builder(resolvedImportDirective) {
            withCopy()
        }

        builder(property) {
            default("bodyResolveState", "CfirPropertyBodyResolveState.NOTHING_RESOLVED")
        }

        builder(matchExpression) {
            default("calleeReference", "org.cangnova.cangjie.cfir.references.CfirStubReference")
            default("exhaustiveness", "CfirMatchExhaustivenessStatus.Unknown")
        }
        builder(tryExpression) {
            default("calleeReference", "org.cangnova.cangjie.cfir.references.CfirStubReference")
        }

        builder(parenthesizedTypeRef) { withCopy() }

        builder(constPattern) {
            // 缺少 operatorCall 是官方允许的结构状态，不制造默认调用。
            defaultNull("operatorCall")
        }
        builder(typePattern) {
            default("matchingKind", "CfirTypePatternMatchingKind.UNKNOWN")
            // nestedPattern 与两个 runtime 位均必填，由 raw/resolve/reader 的真实阶段填写。
        }
        // Enum.constructor、Catch/Command.nestedPattern 必填；builder 不生成替代节点。
        configureFieldInAllLeafBuilders(field = "semanticTypeRef") {
            // 唯一缺失阶段是 raw 尚未解析；完成阶段的必填检查由正式语义入口执行。
            defaultNull("semanticTypeRef")
        }

        // 对位 Kotlin：参数和匿名函数在 RAW 阶段就拥有稳定的 resolved statusless，独立于解析 phase。
        builder(anonymousFunction) {
            default("status", "DEFAULT_STATUS_FOR_STATUSLESS_DECLARATIONS")
            additionalImports(defaultStatusForStatuslessDeclarationsType)
        }
        builder(valueParameter) {
            default("status", "DEFAULT_STATUS_FOR_STATUSLESS_DECLARATIONS")
            additionalImports(defaultStatusForStatuslessDeclarationsType)
        }

        // lazyBlock 无需携带源码；lazyExpression 的 builder 保留原始 source。


        configureFieldInAllLeafBuilders(
            field = "resolvePhase",
            builderPredicate = { it.wantsCopy },
        ) {
            additionalImports(resolvePhaseExtensionImport)
        }

        // deprecationsProvider: source 构建路径统一默认 UnresolvedDeprecationProvider,
        // 反序列化路径会覆写为具体 provider。对齐 Kotlin FIR 同机制。
        configureFieldInAllLeafBuilders(
            field = "deprecationsProvider",
        ) {
            default("deprecationsProvider", "UnresolvedDeprecationProvider")
        }

        // foreign C 的 `...` 是函数级签名事实；普通函数、lambda 和合成函数
        // 默认没有变长参数，只有 source foreign builder 会显式发布 true。
        configureFieldInAllLeafBuilders(
            field = "hasVariableLenArg",
        ) {
            defaultFalse("hasVariableLenArg")
        }

        // 源码变量是否省略类型是 raw builder 发布的语义事实；其它构造路径默认显式。
        configureFieldInAllLeafBuilders(
            field = "isTypeImplicit",
        ) {
            defaultFalse("isTypeImplicit")
        }

        // TypeRef 体系默认不启用 custom renderer。
        // 纯已知类型构造不制造源码 Type 节点；raw/解析 owner 显式发布并复制自己的来源。
        configureFieldInAllLeafBuilders(field = "typeUseOrigin") {
            default("typeUseOrigin", "CfirTypeRefOrigin.SEMANTIC")
        }

        // TypeRef 体系默认不启用 custom renderer。
        // 这里在所有落地 builder 上统一施加默认值，确保生成代码与运行时构造约定一致。
        configureFieldInAllLeafBuilders(
            field = "customRenderer",
        ) {
            defaultFalse("customRenderer")
        }
    }

    /**
     * 需要生成公开 builder 的具体 CFIR 元素列表。
     */
    private fun CfirTree.concreteElements(): List<Element> = listOf(
        packageDirective,
        // importDirective 已改为抽象基类，由 resolvedImportDirective 替代
        // constructor 已单独配置为两个具名实现

        // -------- 声明节点 --------
        file, classDeclaration, interfaceDeclaration,structDeclaration,enumDeclaration,  enumConstructor, extend, typeAlias, namedFunction, anonymousFunction, mainFunction, macroDeclaration, finalizer,
        codeFragment, invalidDeclaration, property, propertyAccessor, fieldVariable, resourceVariable, patternVariable, patternBindingVariable, valueParameter, typeParameter, constructedClassTypeParameterRef,

        // -------- 语句 / 表达式节点 --------
        nativeMatchExpression,
        nativeMatchCase,
        nativeConditionMatchExpression,
        nativeConditionMatchCase,
        letPatternDestructorExpression,
        nativeCatch,
        nativeTryExpression,
        primitiveTypeExpression, wildcardExpression, functionArgument, callExpression,
        unaryExpression, binaryExpression, arrayCreationExpression, pointerExpression,
        resolvedSpawnExpression,
        annotationCall, ifAvailableExpression, block,
        literalExpression, stringInterpolation, functionCall, namedAccessExpression, qualifiedAccessExpression, resolvedQualifier, errorResolvedQualifier, assignment, binaryOp,
        comparisonExpression,
        typeOperator,
        typeConversion,
        letPatternExpression,
        ifExpression,
        matchExpression,
        matchBranch,
        catchClause,
        handleClause,
        loopExpression,
        forInExpression,
        tryExpression,
        throwExpression, returnExpression, breakExpression, continueExpression, anonymousFunctionExpression, rangeExpression, arrayLiteral, tupleLiteral,
        spawnExpression, synchronizedExpression, unsafeExpression, quoteExpression, subscriptExpression, errorExpression,
        inoutArgumentExpression,

        // -------- 模式节点 --------
        constPattern, wildcardPattern, bindingPattern, varOrEnumPattern, tuplePattern, enumPattern, typePattern,
        catchPattern, commandTypePattern,

        // -------- 类型引用节点 --------
        qualifierPart, resolvedTypeRef, userTypeRef, basicTypeRef, implicitTypeRef, functionTypeRef, tupleTypeRef, varrayTypeRef, parenthesizedTypeRef,

        // -------- 引用节点 --------
        namedReference, resolvedNamedReference, resolvedBuiltinOperatorReference, errorReference, thisReference,
    )

    /**
     * 为访问/调用类 builder 配置默认 receiver 字段。
     */
    protected fun BuilderConfigurationContext.defaultNoReceivers(notNullExplicitReceiver: Boolean = false) {
        if (!notNullExplicitReceiver) {
            defaultNull("explicitReceiver")
        }
        defaultNull("dispatchReceiver")
    }
}
