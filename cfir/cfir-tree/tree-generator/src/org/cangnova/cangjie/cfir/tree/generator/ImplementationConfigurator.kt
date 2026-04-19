package org.cangnova.cangjie.cfir.tree.generator

import org.cangnova.cangjie.cfir.tree.generator.context.AbstractCfirTreeImplementationConfigurator
import org.cangnova.cangjie.cfir.tree.generator.model.Element
import org.cangnova.cangjie.cfir.tree.generator.model.Field
import org.cangnova.cangjie.cfir.tree.generator.model.Implementation
import org.cangnova.cangjie.generators.tree.ImplementationKind
import org.cangnova.cangjie.generators.tree.config.AbstractImplementationConfigurator

object ImplementationConfigurator : AbstractCfirTreeImplementationConfigurator() {

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

// resolvedImportDirective：委托字段给 delegate
        impl(resolvedImportDirective) {
            publicImplementation()
            // aliasName、aliasSource、importedFqName、isAllUnder 全部委托给原始节点
            delegateFields(listOf("aliasName", "aliasSource", "importedFqName", "isAllUnder"), "delegate")
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
        macroExpression,
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
