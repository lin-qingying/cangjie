package org.cangnova.cangjie.cfir.tree.generator

import org.cangnova.cangjie.cfir.tree.generator.context.AbstractCfirTreeImplementationConfigurator
import org.cangnova.cangjie.cfir.tree.generator.model.Element
import org.cangnova.cangjie.cfir.tree.generator.util.type

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
        noImpl(statement)
        noImpl(memberDeclaration)
        noImpl(callableDeclaration)
        noImpl(classLikeDeclaration)

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

        impl(lazyExpression) {
            val error = """error("CfirLazyExpression should be resolved before accessing")"""
            default("source") { value = error; withGetter = true }
            default("coneTypeOrNull") { value = error; withGetter = true }
            default("annotations") { value = error; withGetter = true }
            publicImplementation()
        }

        // ---------- 字段精细配置 ----------



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
        // errorExpression：coneTypeOrNull 携带错误类型信息
        impl(errorExpression) {
            default("coneTypeOrNull") {
                value = "ConeErrorType(reason)"
                withGetter = true
            }
            additionalImports(type("types", "ConeErrorType"))
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
        // lambdaExpression 的 cfg 由控制流分析阶段填充，排除在外
        configureFieldInAllImplementations(
            fieldName = "controlFlowGraphReference",
            implementationPredicate = { it.typeName != "CfirLambdaExpressionImpl" }
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
        classDeclaration,
        enumConstructor,
        extend,
        typeAlias,
        function,
        mainFunction,
        macroDeclaration,
        finalizer,
        // constructor 已单独配置为两个具名实现
        invalidDeclaration,
        property,
        fieldVariable,
        patternVariable,
        valueParameter,
        typeParameter,

        // -------- 语句 / 表达式节点 --------
        block,
        // lazyBlock 已单独配置
        // lazyExpression 已单独配置
        literalExpression,
        stringInterpolation,
        functionCall,
        propertyAccess,
        qualifiedAccess,
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
        jumpExpression,
        lambdaExpression,
        rangeExpression,
        arrayLiteral,
        tupleLiteral,
        spawnExpression,
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
        errorTypeRef,

        // -------- 引用节点 --------
        namedReference,
        resolvedNamedReference,
        errorReference,
    )
}
