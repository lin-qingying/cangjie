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

package org.cangnova.cangjie.type.model

import org.cangnova.cangjie.resolve.checkers.EmptyIntersectionTypeChecker
import org.cangnova.cangjie.resolve.checkers.EmptyIntersectionTypeInfo
import org.cangnova.cangjie.type.TypeCheckerState
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

// =====================================================================
// 仓颉类型系统核心标记接口
// =====================================================================

/** 所有仓颉类型的根标记接口 */
interface CangJieTypeMarker

/** 泛型实参的标记接口（仓颉只有普通泛型，无星号投影、无型变注解） */
interface TypeArgumentMarker

/** 类型构造器的标记接口（对应 class/struct/interface 等类型头部） */
interface TypeConstructorMarker

/** 类型参数的标记接口（对应泛型声明中的 T、U 等） */
interface TypeParameterMarker

// =====================================================================
// 类型分类标记
// 仓颉所有类型均为刚性类型（边界确定）
// 无可空类型，可选值通过 Option<T> 表达
// 无弹性类型，C 互操作通过 CType 约束完全确定
// =====================================================================

/** 刚性类型：类型信息确定，是仓颉类型系统中唯一的类型种类 */
interface RigidTypeMarker : CangJieTypeMarker

/**
 * 简单类型：具体的、单一的类型，如 Int、String、MyClass<Int>
 * 对应仓颉中 class 和 struct 实例化后的类型
 */
interface SimpleTypeMarker : RigidTypeMarker


/** 类型实参列表的标记接口 */
interface TypeArgumentListMarker

/** 类型变量的标记接口（类型推断中引入的未知类型） */
interface TypeVariableMarker

/** 类型变量对应的类型构造器标记接口 */
interface TypeVariableTypeConstructorMarker : TypeConstructorMarker



/**
 * 存根类型：类型推断中间状态的占位类型
 * 在推断尚未完成时临时代表某个未知类型变量
 */
interface StubTypeMarker : SimpleTypeMarker

/** 交叉类型（A & B）对应的类型构造器标记接口 */
interface IntersectionTypeConstructorMarker : TypeConstructorMarker

/** 类型替换器的标记接口，用于泛型替换 */
interface TypeSubstitutorMarker

/** 注解标记接口，用于携带类型级别的注解信息（用于反射，不干预类型推断） */
interface AnnotationMarker

// =====================================================================
// 优化上下文
// =====================================================================

/** 类型系统优化上下文，提供快速路径判断以避免重复计算 */
interface TypeSystemOptimizationContext {
    /**
     * 判断两个刚性类型的泛型实参列表是否完全相同（引用相等）
     * @return 若 a.arguments == b.arguments 则返回 true，不支持时返回 false
     */
    fun identicalArguments(a: RigidTypeMarker, b: RigidTypeMarker) = false
}

// =====================================================================
// 内置类型上下文
// =====================================================================

/**
 * 内置基础类型访问上下文
 * 提供对 Any、Nothing 等仓颉内置顶层/底层类型的无关实现访问
 */
interface TypeSystemBuiltInsContext {
    /** 返回底层类型 Nothing（不可达类型，无实例） */
    fun nothingType(): SimpleTypeMarker

    /** 返回顶层类型 Any（所有类型的公共父类型） */
    fun anyType(): SimpleTypeMarker
}

// =====================================================================
// 类型工厂上下文
// =====================================================================

/**
 * 类型构造工厂上下文
 * 负责在编译器内部创建各种类型对象
 */
interface TypeSystemTypeFactoryContext : TypeSystemContext, TypeSystemBuiltInsContext {

    /**
     * 创建简单类型
     * 仓颉中对应 class<T>、struct<T> 等泛型实例化
     * 无型变注解（in/out），形变由编译器内部控制
     */
    fun createSimpleType(
        constructor: TypeConstructorMarker,
        arguments: List<TypeArgumentMarker>,
        attributes: List<AnnotationMarker>? = null,
    ): SimpleTypeMarker

    /**
     * 创建泛型实参
     * 仓颉中泛型实参就是一个具体类型，无 in/out 投影注解
     */
    fun createTypeArgument(type: CangJieTypeMarker): TypeArgumentMarker

    /** 创建错误类型（类型检查失败时的占位类型，携带调试信息） */
    fun createErrorType(debugName: String, delegatedType: RigidTypeMarker?): SimpleTypeMarker

    /** 创建尚未推断出的类型（推断过程中的中间占位） */
    fun createUninferredType(constructor: TypeConstructorMarker): CangJieTypeMarker
}

// =====================================================================
// 类型检查器提供者上下文
// =====================================================================

/**
 * 类型检查状态工厂接口
 * 实现类通常同时实现 TypeSystemContext
 */
interface TypeCheckerProviderContext {
    /**
     * 创建新的类型检查状态
     * @param errorTypesEqualToAnything 错误类型是否与任何类型相等（宽松模式）
     * @param stubTypesEqualToAnything  存根类型是否与任何类型相等（推断阶段宽松模式）
     */
    fun newTypeCheckerState(
        errorTypesEqualToAnything: Boolean,
        stubTypesEqualToAnything: Boolean,
    ): TypeCheckerState
}

// =====================================================================
// 公共父类型计算上下文
// =====================================================================

/**
 * 公共父类型计算扩展上下文
 * 在类型推断需要合并多个候选类型时使用（如 if/when 分支的结果类型）
 */
interface TypeSystemCommonSuperTypesContext : TypeSystemContext, TypeSystemTypeFactoryContext,
    TypeCheckerProviderContext {

    /**
     * 判断当前类型是否存在满足给定谓词的父类型构造器
     * 遍历父类型层级，对每个刚性父类型调用 predicate
     */
    fun CangJieTypeMarker.anySuperTypeConstructor(predicate: (RigidTypeMarker) -> Boolean) =
        newTypeCheckerState(errorTypesEqualToAnything = false, stubTypesEqualToAnything = true)
            .anySupertype(
                asRigidType()!!,
                { predicate(it) },
                { current ->
                    if (current.argumentsCount() == 0) {
                        TypeCheckerState.SupertypesPolicy.Direct
                    } else {
                        substitutionSupertypePolicy(current)
                    }
                }
            )

    /** 获取类型的深度（嵌套泛型层数），用于公共父类型计算的复杂度控制 */
    fun RigidTypeMarker.typeDepth(): Int

    /** 获取任意仓颉类型的深度（仓颉所有类型均为刚性类型） */
    fun CangJieTypeMarker.typeDepth(): Int = asRigidType()!!.typeDepth()

    /** 获取类型近似时使用的深度（可与 typeDepth 不同） */
    fun CangJieTypeMarker.typeDepthForApproximation(): Int = typeDepth()

    /** 查找整型字面量类型列表的公共父类型（用于字面量推断） */
    fun findCommonIntegerLiteralTypesSuperType(explicitSupertypes: List<RigidTypeMarker>): RigidTypeMarker?

    /** 将错误类型构造器转为错误类型（仅在 FIR 前端使用） */
    fun TypeConstructorMarker.toErrorType(): SimpleTypeMarker

    /**
     * 判断类型构造器对应的类型声明是否可访问。
     * 对齐 C++ ImportManager::IsTyAccessible。
     */
    fun TypeConstructorMarker.isTypeAccessible(): Boolean = true

    /** 合并多个类型的注解属性列表（取并集） */
    fun unionTypeAttributes(types: List<CangJieTypeMarker>): List<AnnotationMarker>

    /** 替换类型上的自定义注解属性 */
    fun CangJieTypeMarker.replaceCustomAttributes(newAttributes: List<AnnotationMarker>): CangJieTypeMarker

    // ------------------------------------------------------------------
    // 函数类型与元组类型工具（CST 计算特化路径所需）
    // 对齐官方 C++ JoinAndMeet 中 JoinOrMeetFuncTy / JoinOrMeetTupleTy
    // ------------------------------------------------------------------

    /** 判断该类型是否是函数类型（默认 false，由具体上下文覆盖） */
    override fun CangJieTypeMarker.isFunctionType(): Boolean = false

    /** 提取函数类型的参数类型列表（最后一个元素为返回值类型） */
    override fun CangJieTypeMarker.extractArgumentsForFunctionType(): List<CangJieTypeMarker> =
        error("Not a function type")

    /** 创建函数类型 */
    fun createFunctionType(parameterTypes: List<CangJieTypeMarker>, returnType: CangJieTypeMarker): CangJieTypeMarker =
        error("Function type creation not available in this context")

    /** 判断该类型是否是元组类型（默认 false，由具体上下文覆盖） */
    override fun CangJieTypeMarker.isTupleType(): Boolean = false

    /** 提取元组类型的元素类型列表 */
    override fun CangJieTypeMarker.extractElementsForTupleType(): List<CangJieTypeMarker> =
        error("Not a tuple type")

    /** 判断该类型是否是 VArray 类型（默认 false，由具体上下文覆盖） */
    override fun CangJieTypeMarker.isVArrayType(): Boolean = false

    /** 提取 VArray 的元素类型 */
    override fun CangJieTypeMarker.extractElementTypeForVArrayType(): CangJieTypeMarker =
        error("Not a VArray type")

    /** 提取 VArray 的编译期固定尺寸 */
    override fun CangJieTypeMarker.extractSizeForVArrayType(): Long =
        error("Not a VArray type")

    /** 创建元组类型 */
    fun createTupleType(elementTypes: List<CangJieTypeMarker>): CangJieTypeMarker =
        error("Tuple type creation not available in this context")

}

// =====================================================================
// 类型推断扩展上下文（代理接口）
// =====================================================================

/**
 * 声明实现类作为 TypeSystemInferenceExtensionContext 的组件代理
 * 用于依赖注入场景下的组合模式
 */
interface TypeSystemInferenceExtensionContextDelegate : TypeSystemInferenceExtensionContext

// =====================================================================
// 类型推断扩展上下文（核心）
// =====================================================================

/**
 * 类型推断专用扩展上下文
 * 定义了类型推断算法所需的全部操作
 */
interface TypeSystemInferenceExtensionContext : TypeSystemContext, TypeSystemBuiltInsContext,
    TypeSystemCommonSuperTypesContext {

    /**
     * 判断类型（递归地）是否包含满足谓词的子类型
     * 用于检测类型变量是否出现在某个位置
     */
    fun CangJieTypeMarker.contains(predicate: (CangJieTypeMarker) -> Boolean): Boolean

    /**
     * 将整型字面量类型构造器近似为具体整型类型
     * @param expectedType 期望类型，用于引导近似方向
     */
    fun TypeConstructorMarker.getApproximatedIntegerLiteralType(expectedType: CangJieTypeMarker?): CangJieTypeMarker


    /** 擦除类型中出现的所有含类型参数的类型 */
    fun CangJieTypeMarker.eraseContainingTypeParameters(): CangJieTypeMarker

    /**
     * 从候选类型集合中选出最具代表性的单一类型
     * 用于约束求解时从多个等价候选中取最优解
     */
    fun Collection<CangJieTypeMarker>.singleBestRepresentative(): CangJieTypeMarker?

    /** 判断该类型是否为 Unit（无返回值类型） */
    fun CangJieTypeMarker.isUnit(): Boolean

    /** 判断该类型是否是内置函数类型 */
    override fun CangJieTypeMarker.isFunctionType(): Boolean



    /** 为 Builder 推断创建存根类型 */
    fun createStubTypeForBuilderInference(typeVariable: TypeVariableMarker): StubTypeMarker

    /** 为子类型检查中的类型变量创建存根类型 */
    fun createStubTypeForTypeVariablesInSubtyping(typeVariable: TypeVariableMarker): StubTypeMarker

    /** 用新实参列表替换刚性类型的泛型实参 */
    fun RigidTypeMarker.replaceArguments(newArguments: List<TypeArgumentMarker>): RigidTypeMarker

    /** 用变换函数逐一替换刚性类型的泛型实参 */
    fun RigidTypeMarker.replaceArguments(replacement: (TypeArgumentMarker) -> TypeArgumentMarker): RigidTypeMarker

    /** 对任意仓颉类型替换泛型实参（仓颉所有类型均为刚性类型） */
    fun CangJieTypeMarker.replaceArguments(replacement: (TypeArgumentMarker) -> TypeArgumentMarker): CangJieTypeMarker =
        asRigidType()!!.replaceArguments(replacement)

    /**
     * 深度替换刚性类型中的泛型实参（递归进入嵌套泛型）
     * 例如：List<Map<T, U>> 中的 T、U 都会被替换
     */
    fun RigidTypeMarker.replaceArgumentsDeeply(replacement: (TypeArgumentMarker) -> TypeArgumentMarker): RigidTypeMarker {
        return replaceArguments {
            val type = it.getType() ?: return@replaceArguments it
            val newProjection = if (type.argumentsCount() > 0) {
                it.replaceType(type.replaceArgumentsDeeply(replacement))
            } else it
            replacement(newProjection)
        }
    }

    /** 对任意仓颉类型深度替换泛型实参 */
    fun CangJieTypeMarker.replaceArgumentsDeeply(replacement: (TypeArgumentMarker) -> TypeArgumentMarker): CangJieTypeMarker =
        asRigidType()!!.replaceArgumentsDeeply(replacement)

    /** 判断该类型构造器是否对应一个不可继承的 final class 或 struct */
    fun TypeConstructorMarker.isFinalClassConstructor(): Boolean

    /**
     * 为类型变量创建新鲜的类型构造器
     * 每次推断开始时调用，确保类型变量之间不冲突
     */
    fun TypeVariableMarker.freshTypeConstructor(): TypeVariableTypeConstructorMarker

     /** 获取类型变量的默认类型（用于无法推断时的 fallback） */
    fun TypeVariableMarker.defaultType(): SimpleTypeMarker

    /**
     * 为两个候选类型的交叉结果创建带上界的类型
     * 当交叉类型可能为空集时，使用上界类型进行近似
     */
    fun createTypeWithUpperBoundForIntersectionResult(
        firstCandidate: CangJieTypeMarker,
        secondCandidate: CangJieTypeMarker
    ): CangJieTypeMarker

    /** 获取交叉类型近似时使用的上界（默认为 null，由子类按需覆盖） */
    fun RigidTypeMarker.getUpperBoundForApproximationOfIntersectionType(): CangJieTypeMarker? = null

    /** 判断该类型是否为编译器内部的特殊类型 */
    fun CangJieTypeMarker.isSpecial(): Boolean

    /** 判断该类型构造器是否是类型变量 */
    fun TypeConstructorMarker.isTypeVariable(): Boolean

    /** 判断该类型是否是有符号或无符号数值类型（Int、UInt 等） */
    fun CangJieTypeMarker.isSignedOrUnsignedNumberType(): Boolean

    // ------------------------------------------------------------------
    // 函数类型工具
    // 仓颉函数是一级公民，函数类型是编译器内置类型，只有一种种类
    // 无挂起函数类型、无反射函数类型
    // 扩展通过 extend 语法实现，无扩展函数类型
    // ------------------------------------------------------------------

    /** 获取函数类型的上下文参数数量（context(A, B) 语法） */
    fun CangJieTypeMarker.contextParameterCount(): Int

    /** 提取函数类型的参数类型列表（含返回值类型） */
    override fun CangJieTypeMarker.extractArgumentsForFunctionType(): List<CangJieTypeMarker>

    /** 根据参数数量获取对应的函数类型构造器 */
    fun getFunctionTypeConstructor(parametersNumber: Int): TypeConstructorMarker

    /** 从存根类型中还原出原始类型变量构造器 */
    fun StubTypeMarker.getOriginalTypeVariable(): TypeVariableTypeConstructorMarker

    // ------------------------------------------------------------------
    // 类型提取工具（内部辅助）
    // ------------------------------------------------------------------

    private fun <T> CangJieTypeMarker.extractTypeOf(to: MutableSet<T>, getIfApplicable: (TypeConstructorMarker) -> T?) {
        for (i in 0 until argumentsCount()) {
            val argument = getArgument(i)
            val argumentType = argument.getType() ?: continue
            val argumentTypeConstructor = argumentType.typeConstructor()
            val argumentToAdd = getIfApplicable(argumentTypeConstructor)
            if (argumentToAdd != null) {
                to.add(argumentToAdd)
            } else if (argumentType.argumentsCount() != 0) {
                argumentType.extractTypeOf(to, getIfApplicable)
            }
        }
    }

    /**
     * 提取类型中出现的所有类型变量构造器（递归）
     * 用于确定一个约束涉及哪些类型变量
     */
    fun CangJieTypeMarker.extractTypeVariables(): Set<TypeVariableTypeConstructorMarker> =
        buildSet { extractTypeOf(this) { it as? TypeVariableTypeConstructorMarker } }

    /**
     * 提取类型中出现的所有类型参数（递归）
     * 用于判断类型是否依赖某个泛型形参
     */
    fun CangJieTypeMarker.extractTypeParameters(): Set<TypeParameterMarker> =
        buildSet {
            typeConstructor().getTypeParameterClassifier()?.let(::add)
            extractTypeOf(this) { it.getTypeParameterClassifier() }
        }

    /**
     * 创建用于子类型检查存根类型到类型变量的替换器
     * 推断结束后将存根类型还原为实际类型变量
     */
    fun createSubstitutionFromSubtypingStubTypesToTypeVariables(): TypeSubstitutorMarker

    /**
     * 为自引用（递归）类型参数创建占位类型。
     * 处理如 class Tree<T> where T <: Tree<T> 这类递归泛型约束。
     * 仓颉无 CapturedType，直接返回上界交叉类型作为占位。
     */
    fun createPlaceholderTypeForSelfType(
        typeVariable: TypeVariableTypeConstructorMarker,
        typesForRecursiveTypeParameters: List<CangJieTypeMarker>,
    ): SimpleTypeMarker? {
        val typeParameter = typeVariable.typeParameter ?: return null
        val selfProjection = createTypeArgument(
            createSimpleType(typeParameter.getTypeConstructor(), emptyList())
        )
        val superType = intersectTypes(
            typesForRecursiveTypeParameters.map { type ->
                type.replaceArgumentsDeeply {
                    when (val typeConstructor = it.getType()?.typeConstructor()) {
                        typeVariable -> selfProjection
                        is TypeVariableTypeConstructorMarker -> createTypeArgument(
                            createUninferredType(typeConstructor)
                        )

                        else -> it
                    }
                }
            }
        )
        // 仓颉无 CapturedType，直接用上界类型作为自类型占位
        return superType.asRigidType() as? SimpleTypeMarker
    }

    /** 为给定基础类型的所有父类型构造替换器 */
    fun createSubstitutorForSuperTypes(baseType: CangJieTypeMarker): TypeSubstitutorMarker?

    /**
     * 计算若干类型的交叉是否为空集
     * @return 空交叉的详细信息，若非空则返回 null
     */
    fun computeEmptyIntersectionTypeKind(types: Collection<CangJieTypeMarker>): EmptyIntersectionTypeInfo? =
        EmptyIntersectionTypeChecker.computeEmptyIntersectionEmptiness(types)

    /** 是否允许将类型变量半固定到另一个类型变量（实验性，默认关闭） */
    val allowSemiFixationToOtherTypeVariables: Boolean get() = false

    /** 是否启用字典序类型变量就绪计算（实验性，默认关闭） */
    val lexicographicVariableReadinessCalculation: Boolean get() = false
}

// =====================================================================
// 泛型实参列表（具体实现类）
// =====================================================================

/**
 * 泛型实参列表的具体实现
 * 仓颉中泛型实参是普通类型，无型变注解（in/out）
 */
class ArgumentList(initialSize: Int) : ArrayList<TypeArgumentMarker>(initialSize), TypeArgumentListMarker

// =====================================================================
// 核心类型操作上下文
// =====================================================================

/**
 * 类型系统核心上下文
 * 定义了所有类型操作的抽象接口，使类型检查器与具体类型实现解耦
 *
 * 仓颉类型系统特点：
 * - 区分值类型（struct）和引用类型（class），两者都可实现接口
 * - 无可空类型，用 Option<T> 表示可选值
 * - 无显式型变注解，形变由编译器根据使用位置自动推导
 * - 泛型只有普通形式 class A<T> {}，无星号投影
 * - 所有类型均为刚性类型，无弹性类型
 */
interface TypeSystemContext : TypeSystemOptimizationContext {

    // ------------------------------------------------------------------
    // 类型转换（安全 downcast）
    // ------------------------------------------------------------------

    /**
     * @deprecated 对 RigidTypeMarker 调用此方法是无意义的，请直接使用该值
     */
    @Deprecated(level = DeprecationLevel.ERROR, message = "此调用无实际效果，请直接使用该值")
    fun RigidTypeMarker.asRigidType(): RigidTypeMarker = this

    /** 尝试将任意仓颉类型转为刚性类型，若非刚性类型则返回 null */
    fun CangJieTypeMarker.asRigidType(): RigidTypeMarker?

    // ------------------------------------------------------------------
    // 错误与未推断类型判断
    // ------------------------------------------------------------------

    /** 判断类型是否是错误类型（类型检查失败的占位） */
    fun CangJieTypeMarker.isError(): Boolean

    /** 判断类型构造器是否来自错误类型 */
    fun TypeConstructorMarker.isError(): Boolean

    /** 判断类型是否是尚未推断的参数类型 */
    fun CangJieTypeMarker.isUninferredParameter(): Boolean




    // ------------------------------------------------------------------
    // 泛型实参访问
    // 仓颉泛型实参无型变注解（in/out），实参就是一个具体类型
    // ------------------------------------------------------------------

    /** 获取类型的泛型实参数量 */
    fun CangJieTypeMarker.argumentsCount(): Int

    /** 获取指定位置的泛型实参 */
    fun CangJieTypeMarker.getArgument(index: Int): TypeArgumentMarker

    /** 获取所有泛型实参列表 */
    fun CangJieTypeMarker.getArguments(): List<TypeArgumentMarker>

    /** 安全地获取指定位置的泛型实参（越界返回 null） */
    fun RigidTypeMarker.getArgumentOrNull(index: Int): TypeArgumentMarker? {
        if (index in 0 until argumentsCount()) return getArgument(index)
        return null
    }

    // ------------------------------------------------------------------
    // 存根类型判断
    // ------------------------------------------------------------------

    /** 判断是否是存根类型 */
    fun RigidTypeMarker.isStubType(): Boolean

    /** 判断是否是用于子类型检查的存根类型 */
    fun RigidTypeMarker.isStubTypeForVariableInSubtyping(): Boolean

    /** 判断是否是用于 Builder 推断的存根类型 */
    fun RigidTypeMarker.isStubTypeForBuilderInference(): Boolean

    /** 解包存根类型构造器，还原出底层的类型变量构造器 */
    fun TypeConstructorMarker.unwrapStubTypeVariableConstructor(): TypeConstructorMarker

    // ------------------------------------------------------------------
    // 类型与类型实参互转
    // ------------------------------------------------------------------

    /** 将类型转为泛型实参（用于嵌套泛型场景） */
    fun CangJieTypeMarker.asTypeArgument(): TypeArgumentMarker

    /**
     * 获取泛型实参的类型
     * 仓颉无星号投影，所以此方法始终返回非 null 的具体类型
     */
    fun TypeArgumentMarker.getType(): CangJieTypeMarker?

    /** 替换泛型实参的类型（保持其他属性不变） */
    fun TypeArgumentMarker.replaceType(newType: CangJieTypeMarker): TypeArgumentMarker

    // ------------------------------------------------------------------
    // 类型构造器属性访问
    // ------------------------------------------------------------------

    /** 获取类型构造器的泛型参数数量 */
    fun TypeConstructorMarker.parametersCount(): Int

    /** 获取指定位置的类型参数 */
    fun TypeConstructorMarker.getParameter(index: Int): TypeParameterMarker

    /** 获取所有类型参数列表 */
    fun TypeConstructorMarker.getParameters(): List<TypeParameterMarker>

    /** 获取类型构造器的直接父类型列表 */
    fun TypeConstructorMarker.supertypes(): Collection<CangJieTypeMarker>

    /** 判断是否是交叉类型构造器（A & B） */
    fun TypeConstructorMarker.isIntersection(): Boolean

    /**
     * 判断是否是 class 或 struct 的类型构造器
     * 仓颉区分值类型（struct）和引用类型（class），两者都可实现接口
     */
    fun TypeConstructorMarker.isClassTypeConstructor(): Boolean

    /** 判断是否是 interface 的类型构造器 */
    fun TypeConstructorMarker.isInterface(): Boolean

    /** 判断是否是整型字面量类型构造器（用于字面量推断） */
    fun TypeConstructorMarker.isIntegerLiteralTypeConstructor(): Boolean

    /** 判断是否是整型字面量常量类型构造器 */
    fun TypeConstructorMarker.isIntegerLiteralConstantTypeConstructor(): Boolean

    /** 判断是否是整型常量运算类型构造器 */
    fun TypeConstructorMarker.isIntegerConstantOperatorTypeConstructor(): Boolean

    /** 判断是否是匿名类型 */
    fun TypeConstructorMarker.isAnonymous(): Boolean

    /** 若该类型构造器来自类型参数，返回对应的类型参数标记 */
    fun TypeConstructorMarker.getTypeParameterClassifier(): TypeParameterMarker?

    /** 判断是否是类型参数的类型构造器（对应泛型形参 T） */
    fun TypeConstructorMarker.isTypeParameterTypeConstructor(): Boolean

    /** 获取类型变量构造器关联的类型参数（如有） */
    val TypeVariableTypeConstructorMarker.typeParameter: TypeParameterMarker?

    // ------------------------------------------------------------------
    // 类型参数属性访问
    // 仓颉形变由编译器推导，此处仅保留上界约束相关操作
    // ------------------------------------------------------------------

    /** 获取类型参数的上界数量（如 T <: A & B 则为 2） */
    fun TypeParameterMarker.upperBoundCount(): Int

    /** 获取指定位置的上界类型 */
    fun TypeParameterMarker.getUpperBound(index: Int): CangJieTypeMarker

    /** 获取所有上界类型列表 */
    fun TypeParameterMarker.getUpperBounds(): List<CangJieTypeMarker>

    /** 获取类型参数对应的类型构造器 */
    fun TypeParameterMarker.getTypeConstructor(): TypeConstructorMarker

    /** 判断类型参数是否有递归上界（如 T <: Comparable<T>） */
    fun TypeParameterMarker.hasRecursiveBounds(selfConstructor: TypeConstructorMarker? = null): Boolean

    // ------------------------------------------------------------------
    // 类型构造器相等性
    // ------------------------------------------------------------------

    /** 判断两个类型构造器是否相等（对应同一个 class/struct/interface） */
    fun areEqualTypeConstructors(c1: TypeConstructorMarker, c2: TypeConstructorMarker): Boolean

    /** 判断类型构造器是否是可表示类型（非内部合成类型） */
    fun TypeConstructorMarker.isDenotable(): Boolean

    // ------------------------------------------------------------------
    // 常用类型谓词
    // ------------------------------------------------------------------

    /**
     * 获取类型的类型构造器
     * 仓颉所有类型均为刚性类型，直接转型
     */
    fun CangJieTypeMarker.typeConstructor(): TypeConstructorMarker =
        asRigidType()!!.typeConstructor()

    /** 获取刚性类型的类型构造器 */
    fun RigidTypeMarker.typeConstructor(): TypeConstructorMarker

    /** 判断是否是 Any（所有类型的公共父类型） */
    fun CangJieTypeMarker.isAny() = typeConstructor().isAnyConstructor()

    /** 判断是否是 class/interface 等引用语义类型，不包含 struct/enum 值语义类型。 */
    fun CangJieTypeMarker.isClassLikeType(): Boolean = false

    /** 判断是否是 Nothing（不可达底层类型） */
    fun CangJieTypeMarker.isNothing() = typeConstructor().isNothingConstructor()

    // ------------------------------------------------------------------
    // 内置结构类型
    // 函数类型和元组类型不是普通用户泛型，子类型关系由语言内建规则决定。
    // ------------------------------------------------------------------

    /** 判断该类型是否是函数类型（默认 false，由具体上下文覆盖） */
    fun CangJieTypeMarker.isFunctionType(): Boolean = false

    /** 提取函数类型的参数类型列表（最后一个元素为返回值类型） */
    fun CangJieTypeMarker.extractArgumentsForFunctionType(): List<CangJieTypeMarker> =
        error("Not a function type")

    /**
     * 判断两个函数类型的非参数/返回值类型头是否兼容。
     *
     * 官方 TypeManager::IsFuncParametersSubtype 要求 CFunc 与变长参数属性一致；
     * 具体类型系统在这里暴露对应判断，公共 subtype 算法只负责方差方向。
     */
    fun areEqualFunctionTypeKinds(subType: CangJieTypeMarker, superType: CangJieTypeMarker): Boolean = true

    /** 判断该类型是否是元组类型（默认 false，由具体上下文覆盖） */
    fun CangJieTypeMarker.isTupleType(): Boolean = false

    /** 提取元组类型的元素类型列表 */
    fun CangJieTypeMarker.extractElementsForTupleType(): List<CangJieTypeMarker> =
        error("Not a tuple type")

    /** 判断该类型是否是 VArray 类型（默认 false，由具体上下文覆盖） */
    fun CangJieTypeMarker.isVArrayType(): Boolean = false

    /** 提取 VArray 的元素类型 */
    fun CangJieTypeMarker.extractElementTypeForVArrayType(): CangJieTypeMarker =
        error("Not a VArray type")

    /** 提取 VArray 的编译期固定尺寸 */
    fun CangJieTypeMarker.extractSizeForVArrayType(): Long =
        error("Not a VArray type")

    // ------------------------------------------------------------------
    // Option 自动装箱
    // 对齐官方 C++ TypeManager::IsSubtype(..., allowOptionBox=true)：
    // 目标类型是 core.Option<T> 且目标 Option 嵌套层级更深时，允许源类型按 T 继续做子类型检查。
    // 具体上下文负责识别本类型系统中的 core.Option<T>。
    // ------------------------------------------------------------------

    /** 若当前类型是标准库 `Option<T>`，返回其元素类型；否则返回 null。 */
    fun CangJieTypeMarker.optionBoxedElementType(): CangJieTypeMarker? = null

    /** 统计连续嵌套的 `Option` 层级，对齐官方 `CountOptionNestedLevel`。 */
    fun CangJieTypeMarker.optionNestedLevel(): Int {
        var current = this
        var level = 0
        while (true) {
            current = current.optionBoxedElementType() ?: return level
            level++
        }
    }

    /** 判断刚性类型是否是 class 或 struct 实例化的类型 */
    fun RigidTypeMarker.isClassType(): Boolean = typeConstructor().isClassTypeConstructor()

    /** 快速查找当前类型中满足给定构造器的父类型列表（优化路径，可选实现） */
    fun RigidTypeMarker.fastCorrespondingSupertypes(constructor: TypeConstructorMarker): List<SimpleTypeMarker>? = null

    /** 判断是否是整型字面量类型 */
    fun RigidTypeMarker.isIntegerLiteralType(): Boolean = typeConstructor().isIntegerLiteralTypeConstructor()

    /** 获取整型字面量类型所有可能的具体整型类型 */
    fun RigidTypeMarker.possibleIntegerTypes(): Collection<CangJieTypeMarker>

    /** 判断类型构造器是否对应不可被继承的 final class 或 struct */
    fun TypeConstructorMarker.isCommonFinalClassConstructor(): Boolean


    /** 将刚性类型转为泛型实参列表视图（用于遍历泛型实参） */
    fun RigidTypeMarker.asArgumentList(): TypeArgumentListMarker

    // ------------------------------------------------------------------
    // TypeArgumentListMarker 操作符重载
    // ------------------------------------------------------------------

    /** 按下标访问泛型实参列表中的元素 */
    operator fun TypeArgumentListMarker.get(index: Int): TypeArgumentMarker {
        return when (this) {
            is SimpleTypeMarker -> getArgument(index)
            is ArgumentList -> get(index)
            else -> error("未知的泛型实参列表类型: $this, ${this::class}")
        }
    }

    /** 获取泛型实参列表的长度 */
    fun TypeArgumentListMarker.size(): Int {
        return when (this) {
            is RigidTypeMarker -> argumentsCount()
            is ArgumentList -> size
            else -> error("未知的泛型实参列表类型: $this, ${this::class}")
        }
    }

    /** 为泛型实参列表提供迭代器 */
    operator fun TypeArgumentListMarker.iterator() = object : Iterator<TypeArgumentMarker> {
        private var argumentIndex: Int = 0
        override fun hasNext(): Boolean = argumentIndex < size()
        override fun next(): TypeArgumentMarker {
            val argument = get(argumentIndex)
            argumentIndex += 1
            return argument
        }
    }

    // ------------------------------------------------------------------
    // 内置类型构造器判断
    // ------------------------------------------------------------------

    /** 判断是否是 Any 的类型构造器 */
    fun TypeConstructorMarker.isAnyConstructor(): Boolean

    /** 判断是否是 Nothing 的类型构造器 */
    fun TypeConstructorMarker.isNothingConstructor(): Boolean

    /** 判断是否是 Array 的类型构造器 */
    fun TypeConstructorMarker.isArrayConstructor(): Boolean

    // ------------------------------------------------------------------
    // 值类型/引用类型相关
    // 仓颉区分 class（引用类型）和 struct（值类型），两者都可实现接口
    // ------------------------------------------------------------------

    /**
     * 判断是否是单一类型构造器类型（非交叉类型）
     * 仓颉的 class 和 struct 都满足此条件
     */
    fun RigidTypeMarker.isSingleClassifierType(): Boolean

    /** 判断是否是值类型（struct）的类型构造器 */
    fun TypeConstructorMarker.isValueTypeConstructor(): Boolean

    // ------------------------------------------------------------------
    // 类型操作
    // ------------------------------------------------------------------

    /**
     * 计算类型集合的交叉类型（A & B & C）
     * 仓颉支持交叉类型用于多接口约束表达
     */
    fun intersectTypes(types: Collection<CangJieTypeMarker>): CangJieTypeMarker
    fun intersectTypes(types: Collection<SimpleTypeMarker>): SimpleTypeMarker

    /** 判断是否是刚性类型 */
    fun CangJieTypeMarker.isRigidType(): Boolean = asRigidType() != null

    /** 判断是否是原始值类型（Int、Float、Bool 等内置值类型） */
    fun RigidTypeMarker.isPrimitiveType(): Boolean = (this as? SimpleTypeMarker)?.isPrimitiveType() == true
    fun SimpleTypeMarker.isPrimitiveType(): Boolean

    /** 获取类型携带的注解列表（用于反射，不干预推断） */
    fun CangJieTypeMarker.getAttributes(): List<AnnotationMarker>

    /** 获取替换过程中父类型的遍历策略 */
    fun substitutionSupertypePolicy(type: RigidTypeMarker): TypeCheckerState.SupertypesPolicy

    /** 判断是否是类型变量类型（推断过程中的未知类型） */
    fun CangJieTypeMarker.isTypeVariableType(): Boolean

    // ------------------------------------------------------------------
    // 类型替换器构造
    // ------------------------------------------------------------------

    /**
     * 根据类型构造器到类型的映射表创建替换器
     * 用于泛型实例化（如将 T → Int 的替换）
     */
    fun typeSubstitutorByTypeConstructor(map: Map<TypeConstructorMarker, CangJieTypeMarker>): TypeSubstitutorMarker

    /** 创建空替换器（恒等替换，不修改任何类型） */
    fun createEmptySubstitutor(): TypeSubstitutorMarker

    /**
     * 安全地对给定类型执行替换
     * @return 替换后的类型，若无需替换则返回原类型 [type]
     */
    fun TypeSubstitutorMarker.safeSubstitute(type: CangJieTypeMarker): CangJieTypeMarker
}

// =====================================================================
// 捕获状态枚举
// =====================================================================

/**
 * 捕获类型的产生场景
 */
enum class CaptureStatus {
    /** 用于子类型检查阶段的捕获 */
    FOR_SUBTYPING,

    /** 用于约束合并阶段的捕获 */
    FOR_INCORPORATION,

    /** 用于表达式类型推断阶段的捕获 */
    FROM_EXPRESSION
}

// =====================================================================
// 工具函数
// =====================================================================

/**
 * 判断泛型实参列表中所有元素是否都满足给定谓词
 */
inline fun TypeArgumentListMarker.all(
    context: TypeSystemContext,
    crossinline predicate: (TypeArgumentMarker) -> Boolean
): Boolean = with(context) {
    repeat(size()) { index ->
        if (!predicate(get(index))) return false
    }
    return true
}

/**
 * 带类型描述的 require 断言
 * 当条件不满足时，附带值的类型信息抛出 IllegalArgumentException
 */
@OptIn(ExperimentalContracts::class)
fun requireOrDescribe(condition: Boolean, value: Any?) {
    contract {
        returns() implies condition
    }
    require(condition) {
        val typeInfo = if (value != null) "，类型 = '${value::class}'" else ""
        "意外的值: value = '$value'$typeInfo"
    }
}
