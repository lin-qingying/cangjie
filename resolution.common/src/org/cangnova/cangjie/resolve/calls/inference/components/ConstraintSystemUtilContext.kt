package org.cangnova.cangjie.resolve.calls.inference.components

import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintPosition
import org.cangnova.cangjie.resolve.calls.inference.model.FixVariableConstraintPosition
import org.cangnova.cangjie.resolve.calls.model.PostponedAtomWithRevisableExpectedType
import org.cangnova.cangjie.type.model.*

/**
 * 约束系统工具方法的上下文接口。
 *
 * 这些方法依赖推断模块内部的具体类型（如 [PostponedAtomWithRevisableExpectedType]），
 * 因此无法下沉到通用的类型系统扩展接口中，由各推断实现单独提供。
 */
interface ConstraintSystemUtilContext {
    /**
     * 根据参数数量获取对应的函数类型构造器。
     *
     * 仓颉中函数类型 `(T1, T2) -> R` 对应一个以参数数量为键的类型构造器，
     * 由类型系统注册表提供。
     */
    fun getBuiltinFunctionTypeConstructor(parametersNumber: Int): TypeConstructorMarker

    /**
     * 从函数类型中提取参数类型列表（不含返回类型）。
     *
     * 仓颉函数类型形如 (T1, T2, ..., Tn) -> R，
     * 类型实参为 [T1, T2, ..., Tn, R]，本方法返回去掉最后一个实参后的列表。
     */
    fun CangJieTypeMarker.extractBuiltinFunctionArgumentTypes(): List<CangJieTypeMarker>

    /**
     * 从被捕获类型中还原原始类型。
     */
    fun CangJieTypeMarker.unCapture(): CangJieTypeMarker

    /**
     * 对类型进行精化（refine），返回更精确的类型表示。
     *
     * 用于在约束注入前对类型做规范化处理，
     * 例如将某些等价形式统一为标准形式，便于后续约束求解。
     */
    fun CangJieTypeMarker.refineType(): CangJieTypeMarker

    /**
     * 判断类型变量是否携带 `OnlyInputTypes` 语义。
     *
     * 这是约束系统与前端声明元数据之间的桥接点：
     * 约束系统只负责在 fix/completion 阶段触发检查，是否需要检查由具体前端实现判定。
     */
    fun TypeVariableMarker.hasOnlyInputTypesAttribute(): Boolean

    // ---- 延迟参数输入类型解析（PostponedArgumentInputTypesResolver） ----

    /**
     * 为延迟参数（如 lambda、可调用引用）创建对应的约束位置标记。
     *
     * 约束位置用于在推断失败时生成准确的错误信息，指明约束来源。
     */
    fun createArgumentConstraintPosition(argument: PostponedAtomWithRevisableExpectedType): ConstraintPosition

    /**
     * 为"固定类型变量"操作创建约束位置标记。
     *
     * 当推断引擎决定将某个类型变量固定为具体类型时，
     * 需要记录触发固定的原子（atom）以便追踪。
     *
     * @param variable 被固定的类型变量。
     * @param atom 触发本次固定的推断原子。
     */
    fun <T> createFixVariableConstraintPosition(variable: TypeVariableMarker, atom: T): FixVariableConstraintPosition<T>

    /**
     * 从延迟参数的显式声明中提取 lambda 形参类型列表。
     *
     * 当用户在 lambda 中显式声明了参数类型（如 `{ x: Int, y: String -> ... }`）时，
     * 可直接从声明中读取，避免为这些参数创建类型变量。
     * 返回 null 表示无法提取（如无显式声明）；列表元素为 null 表示对应位置未声明类型。
     */
    fun extractLambdaParameterTypesFromDeclaration(declaration: PostponedAtomWithRevisableExpectedType): List<CangJieTypeMarker?>?

    /**
     * 判断延迟参数是否为函数表达式（即具名函数字面量，而非 lambda）。
     *
     * 函数表达式与 lambda 在参数类型推断上的处理略有不同：
     * 函数表达式的参数类型必须显式声明，不参与输入类型推断。
     */
    fun PostponedAtomWithRevisableExpectedType.isFunctionExpression(): Boolean

    /**
     * 返回函数表达式的上下文参数数量。
     *
     * 上下文参数（context parameter）是仓颉中类似 Kotlin context receiver 的语言特性，
     * 推断引擎需要知道数量以正确构建对应的函数类型。
     */
    fun PostponedAtomWithRevisableExpectedType.contextParameterCountOfFunctionExpression(): Int

    /**
     * 判断延迟参数是否为 lambda 表达式。
     *
     * lambda 与函数表达式的区别决定了推断引擎是否尝试从期望类型中
     * 反向推导参数类型，并为未知参数创建类型变量。
     */
    fun PostponedAtomWithRevisableExpectedType.isLambda(): Boolean

    // ---- 类型变量创建 ----

    /**
     * 为 lambda 的返回类型创建新的类型变量。
     *
     * 当 lambda 返回类型无法从上下文直接确定时，
     * 推断引擎引入此类型变量，后续通过 lambda 体内的语句收集约束并求解。
     */
    fun createTypeVariableForLambdaReturnType(): TypeVariableMarker

    /**
     * 为 lambda 的第 [index] 个参数类型创建新的类型变量。
     *
     * 仅在该参数没有显式类型声明且无法从期望类型推导时调用。
     *
     * @param argument 包含该参数的延迟推断原子。
     * @param index 参数在参数列表中的位置（从 0 开始）。
     */
    fun createTypeVariableForLambdaParameterType(argument: PostponedAtomWithRevisableExpectedType, index: Int): TypeVariableMarker

    /**
     * 为可调用引用的返回类型创建新的类型变量。
     *
     * 可调用引用（如 `::foo`）的具体签名在解析前未知，
     * 推断引擎为其返回类型引入类型变量，再通过重载解析收窄约束。
     */
    fun createTypeVariableForCallableReferenceReturnType(): TypeVariableMarker

    /**
     * 为可调用引用的第 [index] 个参数类型创建新的类型变量。
     *
     * @param argument 包含该可调用引用的延迟推断原子。
     * @param index 参数在参数列表中的位置（从 0 开始）。
     */
    fun createTypeVariableForCallableReferenceParameterType(
        argument: PostponedAtomWithRevisableExpectedType,
        index: Int
    ): TypeVariableMarker

    /**
     * 是否强制允许推断系统进行"分叉"（forking）。
     *
     * 分叉推断用于处理存在多个候选重载时需要同时探索多条推断路径的场景。
     * 默认关闭，可由具体实现按需开启。
     */
    val isForcedAllowForkingInferenceSystem get() = false
}
