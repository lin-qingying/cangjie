package org.cangnova.cangjie.cfir.render

import org.cangnova.cangjie.cfir.types.ConeAnyType
import org.cangnova.cangjie.cfir.types.AbbreviatedTypeAttribute
import org.cangnova.cangjie.cfir.types.ConeCStringType
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeClassLikeErrorLookupTag
import org.cangnova.cangjie.cfir.types.ConeClassifierLookupTag
import org.cangnova.cangjie.cfir.types.ConeClassLikeLookupTag
import org.cangnova.cangjie.cfir.types.ConeClassifierType
import org.cangnova.cangjie.cfir.types.ConeDiagnostic
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeFunctionType
import org.cangnova.cangjie.cfir.types.ConeIdealFloatConstantType
import org.cangnova.cangjie.cfir.types.ConeIdealFloatLiteralType
import org.cangnova.cangjie.cfir.types.ConeIdealIntConstantType
import org.cangnova.cangjie.cfir.types.ConeIdealIntLiteralType
import org.cangnova.cangjie.cfir.types.ConeIdealLiteralType
import org.cangnova.cangjie.cfir.types.ConeIntersectionType
import org.cangnova.cangjie.cfir.types.ConePointerType
import org.cangnova.cangjie.cfir.types.ConePlaceholderType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeQuestType
import org.cangnova.cangjie.cfir.types.ConeSimpleCangJieType
import org.cangnova.cangjie.cfir.types.ConeStructType
import org.cangnova.cangjie.cfir.types.ConeStubType
import org.cangnova.cangjie.cfir.types.ConeStubTypeConstructor
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.ConeTypeAliasType
import org.cangnova.cangjie.cfir.types.ConeTypeProjection
import org.cangnova.cangjie.cfir.types.ConeTypeVariableType
import org.cangnova.cangjie.cfir.types.ConeTypeVariableTypeConstructor
import org.cangnova.cangjie.cfir.types.ConeUnionType
import org.cangnova.cangjie.cfir.types.ConeVArrayType
import org.cangnova.cangjie.cfir.types.getConstructor
import org.cangnova.cangjie.type.model.TypeConstructorMarker

/**
 * 仓颉 Cone 类型渲染器。
 *
 * 设计目标：
 * 1. 以仓颉语义为中心渲染（class/interface、struct、enum、func、tuple、VArray 等）。
 * 2. 不引入 Kotlin 特有概念（如 flexible/nullability 星投影语义）。
 * 3. 对推断期内部类型（captured/stub/type-variable）给出明确的调试可读输出。
 */
open class ConeTypeRenderer(
    /** 类型属性渲染策略。 */
    private val attributeRenderer: ConeAttributeRenderer = ConeAttributeRenderer.ToString,
    /** 是否渲染 captured 类型的内部细节。 */
    private var renderCapturedDetails: Boolean = false,
) {
    /**
     * 当前渲染输出缓冲区。
     */
    lateinit var builder: StringBuilder

    /**
     * 当前类型渲染器使用的 ID 渲染策略。
     */
    lateinit var idRenderer: ConeIdRenderer

    /**
     * 渲染入口：将 [type] 直接写入 [builder]。
     */
    fun render(type: ConeCangJieType) {
        ensureState()
        renderType(type)
    }

    /**
     * 单独渲染类型构造器，主要用于错误诊断场景。
     */
    fun renderConstructor(constructor: TypeConstructorMarker) {
        ensureState()
        when (constructor) {
            is ConeTypeVariableTypeConstructor -> {
                builder.append("TypeVariable(")
                builder.append(constructor.debugName)
                builder.append(")")
            }

            is ConeClassLikeErrorLookupTag -> {
                builder.append(renderDiagnostic(constructor.diagnostic, prefix = "ERROR CLASS: "))
            }

            is ConeClassLikeLookupTag -> idRenderer.renderClassId(constructor.classId)
            is ConeClassifierLookupTag -> builder.append(constructor.name.asString())
            is ConeStubTypeConstructor -> {
                builder.append("Stub(")
                builder.append(constructor.variable.typeConstructor.debugName)
                builder.append(")")
            }

            is ConePlaceholderType -> {
                builder.append("Placeholder(")
                builder.append(constructor.debugName)
                builder.append(")")
            }

            else -> builder.append(constructor.toString())
        }
    }

    /**
     * 渲染诊断文本，子类可覆盖调整前后缀格式。
     */
    open fun renderDiagnostic(diagnostic: ConeDiagnostic, prefix: String = "", suffix: String = ""): String {
        return "$prefix${diagnostic.reason}$suffix"
    }

    /**
     * 渲染类型属性。
     *
     * 默认直接按当前 attribute renderer 输出。
     */
    protected open fun ConeCangJieType.renderAttributes() {
        if (!attributes.any()) return
        builder.append(attributeRenderer.render(attributes))
    }

    /**
     * 调试可读场景下保留的属性渲染入口。
     *
     * 对齐 Kotlin FIR：这里不能再回调 [renderAttributes]，否则调试渲染器覆写
     * `renderAttributes()` 后会递归。当前仓库还没有“编译器内部属性”白名单，
     * 因此暂时直接复用全部 attributes 的 renderer 输出。
     */
    protected fun ConeCangJieType.renderNonCompilerAttributes() {
        val nonCompilerAttributes = attributes.filter { it.key != AbbreviatedTypeAttribute::class }
        if (nonCompilerAttributes.isEmpty()) return
        builder.append(attributeRenderer.render(nonCompilerAttributes))
    }

    /**
     * 具体类型渲染分派。
     */
    protected open fun renderType(type: ConeCangJieType) {
        type.renderAttributes()
        when (type) {
            is ConePrimitiveType -> builder.append(type.kind.typeName)
            is ConeIdealLiteralType -> renderIdealLiteralType(type)
            is ConeClassLikeType -> renderClassLikeType(type)
            is ConeStructType -> renderStructType(type)
            is ConeEnumType -> renderEnumType(type)
            is ConeTypeAliasType -> renderTypeAliasType(type)
            is ConeFunctionType -> renderFunctionType(type)
            is ConeTupleType -> renderTupleType(type)
            is ConeVArrayType -> renderVArrayType(type)
            is ConePointerType -> renderPointerType(type)
            is ConeCStringType -> builder.append("CString")
            is ConeIntersectionType -> renderIntersectionType(type)
            is ConeUnionType -> renderUnionType(type)
            is ConeErrorType -> builder.append(renderDiagnostic(type.diagnostic, prefix = "ERROR TYPE: "))
            is ConeStubType -> {
                builder.append("Stub(")
                builder.append(type.constructor.debugName)
                builder.append(":")
                builder.append(type.kind.name)
                builder.append(")")
            }

            is ConeTypeVariableType -> {
                builder.append("TypeVariable(")
                builder.append(type.typeConstructor.debugName)
                builder.append(")")
            }

            is ConeQuestType -> builder.append("?")
            ConeAnyType -> builder.append("Any")
            is ConePlaceholderType -> {
                builder.append("Placeholder(")
                builder.append(type.debugName)
                builder.append(")")
            }

            is ConeSimpleCangJieType ->
                renderSimpleType(type)


        }
    }

    /**
     * 渲染分类器类型自身携带的类型实参。
     */
    private fun ConeClassifierType.renderTypeArguments() {
        if (typeArguments.isEmpty()) return
        builder.append("<")
        for ((index, typeArgument) in typeArguments.withIndex()) {
            if (index > 0) {
                builder.append(", ")
            }
            typeArgument.render()
        }
        builder.append(">")
    }

    /**
     * 渲染普通 simple type。
     *
     * 默认实现先渲染类型构造器，再为 class-like 类型补充类型实参。
     */
    protected open fun renderSimpleType(type: ConeSimpleCangJieType) {
        val hasTypeArguments = type is ConeClassLikeType && type.typeArguments.isNotEmpty()
        renderConstructor(type.getConstructor())
        if (hasTypeArguments) {
            type.renderTypeArguments()
        }


    }

    /**
     * 理想字面量类型渲染。
     *
     * 常量类型带值：`IdealInt(42)`, `IdealFloat(3.14)`；
     * 运算类型无值：`IdealInt`, `IdealFloat`。
     */
    protected open fun renderIdealLiteralType(type: ConeIdealLiteralType) {
        when (type) {
            is ConeIdealIntConstantType -> {
                builder.append("IdealInt(")
                builder.append(type.value)
                builder.append(")")
            }

            is ConeIdealFloatConstantType -> {
                builder.append("IdealFloat(")
                builder.append(type.value)
                builder.append(")")
            }

            is ConeIdealIntLiteralType -> builder.append("IdealInt")
            is ConeIdealFloatLiteralType -> builder.append("IdealFloat")
        }
    }

    /**
     * class/interface 实例类型渲染。
     *
     * 语法上 class 与 interface 在类型位置都以“名义类型 + 实参”形式出现，
     * 因此这里保持统一输出；仅在 this-type 追加标记。
     */
    protected open fun renderClassLikeType(type: ConeClassLikeType) {
        idRenderer.renderClassId(type.classId)
        renderTypeArguments(type.typeArguments)
        if (type.isThisType) {
            builder.append(".This")
        }
    }

    /**
     * struct 值类型渲染。
     *
     * 仓颉语义下 struct 是名义值类型。为避免与 class 同名时调试歧义，这里保留 `struct` 前缀。
     */
    protected open fun renderStructType(type: ConeStructType) {
        builder.append("struct ")
        idRenderer.renderClassId(type.classId)
        renderTypeArguments(type.typeArguments)
    }

    /**
     * enum 类型渲染。
     *
     * `ref enum` 会保留 `ref` 前缀，便于区分值语义/引用语义枚举形态。
     */
    protected open fun renderEnumType(type: ConeEnumType) {
        if (type.isRefEnum) {
            builder.append("ref ")
        }
        builder.append("enum ")
        idRenderer.renderClassId(type.classId)
        renderTypeArguments(type.typeArguments)
    }

    /**
     * typealias 类型渲染。
     *
     * 默认仅渲染别名身份和实参，不主动展开 `expandedType`，避免掩盖别名本身语义。
     */
    protected open fun renderTypeAliasType(type: ConeTypeAliasType) {
        builder.append("typealias ")
        idRenderer.renderClassId(type.classId)
        renderTypeArguments(type.typeArguments)
    }

    /**
     * 函数类型渲染：`(P1, P2, ...) -> R`。
     *
     * C 互操作/闭包/可变参是函数语义标签，体现在前缀与参数尾部标记。
     */
    protected open fun renderFunctionType(type: ConeFunctionType) {
        if (type.isCFunc) builder.append("cfunc ")
        if (type.isClosureType) builder.append("closure ")

        builder.append("(")
        type.parameterTypes.forEachIndexed { index, parameterType ->
            if (index > 0) builder.append(", ")
            renderType(parameterType)
        }
        if (type.hasVariableLenArg) {
            if (type.parameterTypes.isNotEmpty()) builder.append(", ")
            builder.append("...")
        }
        builder.append(") -> ")
        renderType(type.returnType)
    }

    /**
     * 元组类型渲染：`(T1, T2, ...)`。
     */
    protected open fun renderTupleType(type: ConeTupleType) {
        builder.append("(")
        type.elementTypes.forEachIndexed { index, elementType ->
            if (index > 0) builder.append(", ")
            renderType(elementType)
        }
        builder.append(")")
    }

    /**
     * VArray 类型渲染：`VArray<T, N>`。
     *
     * 注意：这里是编译器特殊建模的定长数组；
     * 标准库 `Array<T>` 走名义 `struct` 类型路径，不在此分支。
     */
    protected open fun renderVArrayType(type: ConeVArrayType) {
        builder.append("VArray<")
        renderType(type.elementType)
        builder.append(", ")
        builder.append(type.size)
        builder.append(">")
    }

    /**
     * C 指针类型渲染：`CPointer<T>`。
     */
    protected open fun renderPointerType(type: ConePointerType) {
        builder.append("CPointer<")
        renderType(type.pointeeType)
        builder.append(">")
    }

    /**
     * 交叉类型渲染：`(A & B & ...)`。
     */
    protected open fun renderIntersectionType(type: ConeIntersectionType) {
        builder.append("(")
        type.intersectedTypes.forEachIndexed { index, intersected ->
            if (index > 0) builder.append(" & ")
            renderType(intersected)
        }
        builder.append(")")
    }

    /**
     * 联合类型渲染：`(A | B | ...)`。
     */
    protected open fun renderUnionType(type: ConeUnionType) {
        builder.append("(")
        type.unionTypes.forEachIndexed { index, unionType ->
            if (index > 0) builder.append(" | ")
            renderType(unionType)
        }
        builder.append(")")
    }

    /**
     * 渲染类型实参列表。
     */
    private fun renderTypeArguments(typeArguments: List<ConeTypeProjection>) {
        if (typeArguments.isEmpty()) return
        builder.append("<")
        typeArguments.forEachIndexed { index, projection ->
            if (index > 0) builder.append(", ")
            projection.render()
        }
        builder.append(">")
    }

    /**
     * 在当前 renderer 上渲染单个类型投影。
     */
    protected fun ConeTypeProjection.render() {
        when (this) {
            is ConeCangJieType -> {
                render(this)
            }
        }
    }

    /**
     * 确保 renderer 拥有可用的输出缓冲区和 ID 渲染器。
     */
    private fun ensureState() {
        if (!::builder.isInitialized) {
            builder = StringBuilder()
        }
        if (!::idRenderer.isInitialized) {
            idRenderer = ConeShortIdRenderer()
        }
        idRenderer.builder = builder
    }
}
