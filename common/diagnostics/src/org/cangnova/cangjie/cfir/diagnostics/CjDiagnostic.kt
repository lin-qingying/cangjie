package org.cangnova.cangjie.cfir.diagnostics

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.cangnova.cangjie.messages.CompilerMessageSourceLocation
import org.cangnova.cangjie.source.AbstractCjSourceElement
import org.cangnova.cangjie.source.CjLightSourceElement
import org.cangnova.cangjie.source.CjPsiSourceElement


// ------------------------------ diagnostics ------------------------------

/**
 * 仓颉前端诊断的统一基类。
 *
 * 诊断对象承载严重级别、工厂、位置范围和渲染上下文，具体子类负责区分无源码诊断、
 * PSI 源诊断、LightTree 源诊断以及仅 offset 的诊断。
 */
sealed class CjDiagnostic {
    /**
     * 当前诊断在本次语言版本和配置下的严重级别。
     */
    abstract val severity: Severity
    /**
     * 创建当前诊断的工厂，决定诊断名称、默认严重级别和渲染器。
     */
    abstract val factory: AbstractCjDiagnosticFactory
    /**
     * 当前诊断的位置对象是否仍然有效。
     */
    abstract val isValid: Boolean
    /**
     * 当前诊断用于排序和主展示的第一个文本范围。
     */
    abstract val firstRange: TextRange
    /**
     * 渲染和过滤诊断时使用的上下文。
     */
    abstract val context: DiagnosticBaseContext

    /**
     * 诊断工厂的稳定名称，用作测试期望、日志和外部协议标识。
     */
    val factoryName: String
        get() = factory.name

    /**
     * 使用当前诊断工厂的仓颉渲染器生成面向用户的诊断消息。
     */
    fun renderMessage(): String {
        return factory.cjRenderer.render(this)
    }
}

/**
 * 不绑定源码元素的诊断。
 *
 * 主要用于命令行、配置或全局错误，位置可通过 [CompilerMessageSourceLocation] 选择性提供。
 */
class CjDiagnosticWithoutSource(
    /**
     * 已生成的诊断消息文本。
     */
    val message: String,
    /**
     * 可选的编译器消息位置。
     */
    val location: CompilerMessageSourceLocation?,
    /**
     * 无源码诊断的严重级别。
     */
    override val severity: Severity,
    /**
     * 创建该无源码诊断的工厂。
     */
    override val factory: CjSourcelessDiagnosticFactory,
    /**
     * 渲染该诊断时使用的上下文。
     */
    override val context: DiagnosticBaseContext,
) : CjDiagnostic() {
    /**
     * 无源码诊断不依赖 PSI 或轻量树节点，因此始终有效。
     */
    override val isValid: Boolean
        get() = true

    /**
     * 无源码诊断没有源码范围，统一使用空范围。
     */
    override val firstRange: TextRange
        get() = TextRange.EMPTY_RANGE
}

/**
 * 绑定源码元素的诊断基类。
 *
 * 该层统一通过定位策略计算文本范围，并由具体子类补充参数数量和源码元素类型。
 */
sealed class CjDiagnosticWithSource : CjDiagnostic(), DiagnosticMarker {
    /**
     * 产生诊断的抽象源码元素。
     */
    abstract val element: AbstractCjSourceElement
    /**
     * 创建当前有源码诊断的参数化工厂。
     */
    abstract override val factory: CjDiagnosticFactoryN
    /**
     * 将源码元素映射为诊断高亮范围的定位策略。
     */
    abstract val positioningStrategy: AbstractSourceElementPositioningStrategy
    /**
     * 当前有源码诊断的严重级别。
     */
    abstract override val severity: Severity

    /**
     * 根据定位策略计算出的所有高亮范围。
     */
    final override val textRanges: List<TextRange>
        get() = positioningStrategy.markDiagnostic(this)

    /**
     * 当前位置策略是否仍能在源码元素上合法定位。
     */
    final override val isValid: Boolean
        get() = positioningStrategy.isValid(element)

    /**
     * 所有高亮范围中的首个范围。
     */
    final override val firstRange: TextRange
        get() = DiagnosticRangeUtils.firstRange(textRanges)
}

/**
 * 不携带额外参数的有源码诊断。
 */
sealed class CjSimpleDiagnostic : CjDiagnosticWithSource() {
    /**
     * 创建该无参数诊断的工厂。
     */
    abstract override val factory: CjDiagnosticFactory0
}

/**
 * 携带一个渲染参数的有源码诊断。
 */
sealed class CjDiagnosticWithParameters1<A> : CjDiagnosticWithSource(), DiagnosticWithParameters1Marker<A> {
    /**
     * 第一个诊断渲染参数。
     */
    abstract override val a: A
    /**
     * 创建该一参数诊断的工厂。
     */
    abstract override val factory: CjDiagnosticFactory1<A>
}

/**
 * 携带两个渲染参数的有源码诊断。
 */
sealed class CjDiagnosticWithParameters2<A, B> : CjDiagnosticWithSource(), DiagnosticWithParameters2Marker<A, B> {
    /**
     * 第一个诊断渲染参数。
     */
    abstract override val a: A
    /**
     * 第二个诊断渲染参数。
     */
    abstract override val b: B
    /**
     * 创建该二参数诊断的工厂。
     */
    abstract override val factory: CjDiagnosticFactory2<A, B>
}

/**
 * 携带三个渲染参数的有源码诊断。
 */
sealed class CjDiagnosticWithParameters3<A, B, C> : CjDiagnosticWithSource(), DiagnosticWithParameters3Marker<A, B, C> {
    /**
     * 第一个诊断渲染参数。
     */
    abstract override val a: A
    /**
     * 第二个诊断渲染参数。
     */
    abstract override val b: B
    /**
     * 第三个诊断渲染参数。
     */
    abstract override val c: C
    /**
     * 创建该三参数诊断的工厂。
     */
    abstract override val factory: CjDiagnosticFactory3<A, B, C>
}

/**
 * 携带四个渲染参数的有源码诊断。
 */
sealed class CjDiagnosticWithParameters4<A, B, C, D> : CjDiagnosticWithSource(),
    DiagnosticWithParameters4Marker<A, B, C, D> {
    /**
     * 第一个诊断渲染参数。
     */
    abstract override val a: A
    /**
     * 第二个诊断渲染参数。
     */
    abstract override val b: B
    /**
     * 第三个诊断渲染参数。
     */
    abstract override val c: C
    /**
     * 第四个诊断渲染参数。
     */
    abstract override val d: D
    /**
     * 创建该四参数诊断的工厂。
     */
    abstract override val factory: CjDiagnosticFactory4<A, B, C, D>
}

// ------------------------------ psi diagnostics ------------------------------

/**
 * 基于 PSI 源元素的诊断标记。
 *
 * PSI 诊断可以直接提供 [PsiElement] 和 [PsiFile]，适用于 IDE、解析树检查和需要真实 PSI 类型校验的场景。
 */
interface CjPsiDiagnostic : DiagnosticMarker {
    /**
     * 创建当前 PSI 诊断的工厂。
     */
    val factory: CjDiagnosticFactoryN
    /**
     * 产生诊断的 PSI 源元素。
     */
    val element: CjPsiSourceElement
    /**
     * 当前 PSI 诊断的文本范围集合。
     */
    override val textRanges: List<TextRange>
    /**
     * 当前 PSI 诊断的严重级别。
     */
    override val severity: Severity

    /**
     * 当前诊断对应的 IntelliJ PSI 元素。
     */
    override val psiElement: PsiElement
        get() = element.psi

    /**
     * 当前诊断所在的 PSI 文件。
     */
    val psiFile: PsiFile
        get() = psiElement.containingFile
}

/**
 * 是否在创建 PSI 诊断时校验工厂声明的 PSI 类型与实际元素一致。
 */
private const val CHECK_PSI_CONSISTENCY_IN_DIAGNOSTICS = true

/**
 * 校验 PSI 诊断的实际元素类型是否满足工厂声明的 PSI 类型约束。
 */
private fun CjPsiDiagnostic.checkPsiTypeConsistency() {
    if (CHECK_PSI_CONSISTENCY_IN_DIAGNOSTICS) {
        require(factory.psiType.isInstance(element.psi)) {
            "${element.psi::class} is not a subtype of ${factory.psiType} for factory $factory"
        }
    }
}

/**
 * 基于 PSI 源元素的无参数诊断实现。
 */
data class CjPsiSimpleDiagnostic(
    /**
     * 产生诊断的 PSI 源元素。
     */
    override val element: CjPsiSourceElement,
    /**
     * 当前诊断的严重级别。
     */
    override val severity: Severity,
    /**
     * 创建该无参数诊断的工厂。
     */
    override val factory: CjDiagnosticFactory0,
    /**
     * 计算 PSI 诊断高亮范围的定位策略。
     */
    override val positioningStrategy: AbstractSourceElementPositioningStrategy,
    /**
     * 渲染和抑制判断使用的诊断上下文。
     */
    override val context: DiagnosticBaseContext,
) : CjSimpleDiagnostic(), CjPsiDiagnostic {
    init {
        checkPsiTypeConsistency()
    }
}

/**
 * 基于 PSI 源元素的一参数诊断实现。
 */
data class CjPsiDiagnosticWithParameters1<A>(
    /**
     * 产生诊断的 PSI 源元素。
     */
    override val element: CjPsiSourceElement,
    /**
     * 第一个诊断渲染参数。
     */
    override val a: A,
    /**
     * 当前诊断的严重级别。
     */
    override val severity: Severity,
    /**
     * 创建该一参数诊断的工厂。
     */
    override val factory: CjDiagnosticFactory1<A>,
    /**
     * 计算 PSI 诊断高亮范围的定位策略。
     */
    override val positioningStrategy: AbstractSourceElementPositioningStrategy,
    /**
     * 渲染和抑制判断使用的诊断上下文。
     */
    override val context: DiagnosticBaseContext,
) : CjDiagnosticWithParameters1<A>(), CjPsiDiagnostic {
    init {
        checkPsiTypeConsistency()
    }
}


/**
 * 基于 PSI 源元素的二参数诊断实现。
 */
data class CjPsiDiagnosticWithParameters2<A, B>(
    /**
     * 产生诊断的 PSI 源元素。
     */
    override val element: CjPsiSourceElement,
    /**
     * 第一个诊断渲染参数。
     */
    override val a: A,
    /**
     * 第二个诊断渲染参数。
     */
    override val b: B,
    /**
     * 当前诊断的严重级别。
     */
    override val severity: Severity,
    /**
     * 创建该二参数诊断的工厂。
     */
    override val factory: CjDiagnosticFactory2<A, B>,
    /**
     * 计算 PSI 诊断高亮范围的定位策略。
     */
    override val positioningStrategy: AbstractSourceElementPositioningStrategy,
    /**
     * 渲染和抑制判断使用的诊断上下文。
     */
    override val context: DiagnosticBaseContext,
) : CjDiagnosticWithParameters2<A, B>(), CjPsiDiagnostic {
    init {
        checkPsiTypeConsistency()
    }
}

/**
 * 基于 PSI 源元素的三参数诊断实现。
 */
data class CjPsiDiagnosticWithParameters3<A, B, C>(
    /**
     * 产生诊断的 PSI 源元素。
     */
    override val element: CjPsiSourceElement,
    /**
     * 第一个诊断渲染参数。
     */
    override val a: A,
    /**
     * 第二个诊断渲染参数。
     */
    override val b: B,
    /**
     * 第三个诊断渲染参数。
     */
    override val c: C,
    /**
     * 当前诊断的严重级别。
     */
    override val severity: Severity,
    /**
     * 创建该三参数诊断的工厂。
     */
    override val factory: CjDiagnosticFactory3<A, B, C>,
    /**
     * 计算 PSI 诊断高亮范围的定位策略。
     */
    override val positioningStrategy: AbstractSourceElementPositioningStrategy,
    /**
     * 渲染和抑制判断使用的诊断上下文。
     */
    override val context: DiagnosticBaseContext,
) : CjDiagnosticWithParameters3<A, B, C>(), CjPsiDiagnostic {
    init {
        checkPsiTypeConsistency()
    }
}

/**
 * 基于 PSI 源元素的四参数诊断实现。
 */
data class CjPsiDiagnosticWithParameters4<A, B, C, D>(
    /**
     * 产生诊断的 PSI 源元素。
     */
    override val element: CjPsiSourceElement,
    /**
     * 第一个诊断渲染参数。
     */
    override val a: A,
    /**
     * 第二个诊断渲染参数。
     */
    override val b: B,
    /**
     * 第三个诊断渲染参数。
     */
    override val c: C,
    /**
     * 第四个诊断渲染参数。
     */
    override val d: D,
    /**
     * 当前诊断的严重级别。
     */
    override val severity: Severity,
    /**
     * 创建该四参数诊断的工厂。
     */
    override val factory: CjDiagnosticFactory4<A, B, C, D>,
    /**
     * 计算 PSI 诊断高亮范围的定位策略。
     */
    override val positioningStrategy: AbstractSourceElementPositioningStrategy,
    /**
     * 渲染和抑制判断使用的诊断上下文。
     */
    override val context: DiagnosticBaseContext,
) : CjDiagnosticWithParameters4<A, B, C, D>(), CjPsiDiagnostic {
    init {
        checkPsiTypeConsistency()
    }
}

// ------------------------------ light tree diagnostics ------------------------------

/**
 * 基于轻量树源元素的诊断标记。
 *
 * LightTree 诊断服务于无 PSI 前端路径，不允许通过 [psiElement] 回退读取 PSI。
 */
interface CjLightDiagnostic : DiagnosticMarker {
    /**
     * 产生诊断的轻量树源元素。
     */
    val element: CjLightSourceElement

    /**
     * LightTree 诊断没有 PSI 元素，调用该属性属于使用错误。
     */
    @Deprecated("Should not be called", level = DeprecationLevel.HIDDEN)
    override val psiElement: PsiElement
        get() = error("psiElement should not be called on CjLightDiagnostic")
}

/**
 * 基于轻量树源元素的无参数诊断实现。
 */
data class CjLightSimpleDiagnostic(
    /**
     * 产生诊断的轻量树源元素。
     */
    override val element: CjLightSourceElement,
    /**
     * 当前诊断的严重级别。
     */
    override val severity: Severity,
    /**
     * 创建该无参数诊断的工厂。
     */
    override val factory: CjDiagnosticFactory0,
    /**
     * 计算轻量树诊断高亮范围的定位策略。
     */
    override val positioningStrategy: AbstractSourceElementPositioningStrategy,
    /**
     * 渲染和抑制判断使用的诊断上下文。
     */
    override val context: DiagnosticBaseContext,
) : CjSimpleDiagnostic(), CjLightDiagnostic

/**
 * 基于轻量树源元素的一参数诊断实现。
 */
data class CjLightDiagnosticWithParameters1<A>(
    /**
     * 产生诊断的轻量树源元素。
     */
    override val element: CjLightSourceElement,
    /**
     * 第一个诊断渲染参数。
     */
    override val a: A,
    /**
     * 当前诊断的严重级别。
     */
    override val severity: Severity,
    /**
     * 创建该一参数诊断的工厂。
     */
    override val factory: CjDiagnosticFactory1<A>,
    /**
     * 计算轻量树诊断高亮范围的定位策略。
     */
    override val positioningStrategy: AbstractSourceElementPositioningStrategy,
    /**
     * 渲染和抑制判断使用的诊断上下文。
     */
    override val context: DiagnosticBaseContext,
) : CjDiagnosticWithParameters1<A>(), CjLightDiagnostic

/**
 * 基于轻量树源元素的二参数诊断实现。
 */
data class CjLightDiagnosticWithParameters2<A, B>(
    /**
     * 产生诊断的轻量树源元素。
     */
    override val element: CjLightSourceElement,
    /**
     * 第一个诊断渲染参数。
     */
    override val a: A,
    /**
     * 第二个诊断渲染参数。
     */
    override val b: B,
    /**
     * 当前诊断的严重级别。
     */
    override val severity: Severity,
    /**
     * 创建该二参数诊断的工厂。
     */
    override val factory: CjDiagnosticFactory2<A, B>,
    /**
     * 计算轻量树诊断高亮范围的定位策略。
     */
    override val positioningStrategy: AbstractSourceElementPositioningStrategy,
    /**
     * 渲染和抑制判断使用的诊断上下文。
     */
    override val context: DiagnosticBaseContext,
) : CjDiagnosticWithParameters2<A, B>(), CjLightDiagnostic

/**
 * 基于轻量树源元素的三参数诊断实现。
 */
data class CjLightDiagnosticWithParameters3<A, B, C>(
    /**
     * 产生诊断的轻量树源元素。
     */
    override val element: CjLightSourceElement,
    /**
     * 第一个诊断渲染参数。
     */
    override val a: A,
    /**
     * 第二个诊断渲染参数。
     */
    override val b: B,
    /**
     * 第三个诊断渲染参数。
     */
    override val c: C,
    /**
     * 当前诊断的严重级别。
     */
    override val severity: Severity,
    /**
     * 创建该三参数诊断的工厂。
     */
    override val factory: CjDiagnosticFactory3<A, B, C>,
    /**
     * 计算轻量树诊断高亮范围的定位策略。
     */
    override val positioningStrategy: AbstractSourceElementPositioningStrategy,
    /**
     * 渲染和抑制判断使用的诊断上下文。
     */
    override val context: DiagnosticBaseContext,
) : CjDiagnosticWithParameters3<A, B, C>(), CjLightDiagnostic

/**
 * 基于轻量树源元素的四参数诊断实现。
 */
data class CjLightDiagnosticWithParameters4<A, B, C, D>(
    /**
     * 产生诊断的轻量树源元素。
     */
    override val element: CjLightSourceElement,
    /**
     * 第一个诊断渲染参数。
     */
    override val a: A,
    /**
     * 第二个诊断渲染参数。
     */
    override val b: B,
    /**
     * 第三个诊断渲染参数。
     */
    override val c: C,
    /**
     * 第四个诊断渲染参数。
     */
    override val d: D,
    /**
     * 当前诊断的严重级别。
     */
    override val severity: Severity,
    /**
     * 创建该四参数诊断的工厂。
     */
    override val factory: CjDiagnosticFactory4<A, B, C, D>,
    /**
     * 计算轻量树诊断高亮范围的定位策略。
     */
    override val positioningStrategy: AbstractSourceElementPositioningStrategy,
    /**
     * 渲染和抑制判断使用的诊断上下文。
     */
    override val context: DiagnosticBaseContext,
) : CjDiagnosticWithParameters4<A, B, C, D>(), CjLightDiagnostic

// ------------------------------ offset-based diagnostics ------------------------------

/**
 * 仅依赖源码 offset 的诊断标记。
 *
 * 该类诊断可用于既非 PSI 又非 LightTree 的源码元素，不允许读取 [psiElement]。
 */
interface CjOffsetsOnlyDiagnostic : DiagnosticMarker {
    /**
     * 产生诊断的抽象源码元素。
     */
    val element: AbstractCjSourceElement

    /**
     * Offset-only 诊断没有 PSI 元素，调用该属性属于使用错误。
     */
    @Deprecated("Should not be called", level = DeprecationLevel.HIDDEN)
    override val psiElement: PsiElement
        get() = error("psiElement should not be called on CjOffsetsOnlyDiagnostic")
}

/**
 * 仅依赖 offset 的无参数诊断实现。
 */
data class CjOffsetsOnlySimpleDiagnostic(
    /**
     * 产生诊断的抽象源码元素。
     */
    override val element: AbstractCjSourceElement,
    /**
     * 当前诊断的严重级别。
     */
    override val severity: Severity,
    /**
     * 创建该无参数诊断的工厂。
     */
    override val factory: CjDiagnosticFactory0,
    /**
     * 根据 offset 计算高亮范围的定位策略。
     */
    override val positioningStrategy: AbstractSourceElementPositioningStrategy,
    /**
     * 渲染和抑制判断使用的诊断上下文。
     */
    override val context: DiagnosticBaseContext,
) : CjSimpleDiagnostic(), CjOffsetsOnlyDiagnostic

/**
 * 仅依赖 offset 的一参数诊断实现。
 */
data class CjOffsetsOnlyDiagnosticWithParameters1<A>(
    /**
     * 产生诊断的抽象源码元素。
     */
    override val element: AbstractCjSourceElement,
    /**
     * 第一个诊断渲染参数。
     */
    override val a: A,
    /**
     * 当前诊断的严重级别。
     */
    override val severity: Severity,
    /**
     * 创建该一参数诊断的工厂。
     */
    override val factory: CjDiagnosticFactory1<A>,
    /**
     * 根据 offset 计算高亮范围的定位策略。
     */
    override val positioningStrategy: AbstractSourceElementPositioningStrategy,
    /**
     * 渲染和抑制判断使用的诊断上下文。
     */
    override val context: DiagnosticBaseContext,
) : CjDiagnosticWithParameters1<A>(), CjOffsetsOnlyDiagnostic

/**
 * 仅依赖 offset 的二参数诊断实现。
 */
data class CjOffsetsOnlyDiagnosticWithParameters2<A, B>(
    /**
     * 产生诊断的抽象源码元素。
     */
    override val element: AbstractCjSourceElement,
    /**
     * 第一个诊断渲染参数。
     */
    override val a: A,
    /**
     * 第二个诊断渲染参数。
     */
    override val b: B,
    /**
     * 当前诊断的严重级别。
     */
    override val severity: Severity,
    /**
     * 创建该二参数诊断的工厂。
     */
    override val factory: CjDiagnosticFactory2<A, B>,
    /**
     * 根据 offset 计算高亮范围的定位策略。
     */
    override val positioningStrategy: AbstractSourceElementPositioningStrategy,
    /**
     * 渲染和抑制判断使用的诊断上下文。
     */
    override val context: DiagnosticBaseContext,
) : CjDiagnosticWithParameters2<A, B>(), CjOffsetsOnlyDiagnostic

/**
 * 仅依赖 offset 的三参数诊断实现。
 */
data class CjOffsetsOnlyDiagnosticWithParameters3<A, B, C>(
    /**
     * 产生诊断的抽象源码元素。
     */
    override val element: AbstractCjSourceElement,
    /**
     * 第一个诊断渲染参数。
     */
    override val a: A,
    /**
     * 第二个诊断渲染参数。
     */
    override val b: B,
    /**
     * 第三个诊断渲染参数。
     */
    override val c: C,
    /**
     * 当前诊断的严重级别。
     */
    override val severity: Severity,
    /**
     * 创建该三参数诊断的工厂。
     */
    override val factory: CjDiagnosticFactory3<A, B, C>,
    /**
     * 根据 offset 计算高亮范围的定位策略。
     */
    override val positioningStrategy: AbstractSourceElementPositioningStrategy,
    /**
     * 渲染和抑制判断使用的诊断上下文。
     */
    override val context: DiagnosticBaseContext,
) : CjDiagnosticWithParameters3<A, B, C>(), CjOffsetsOnlyDiagnostic

/**
 * 仅依赖 offset 的四参数诊断实现。
 */
data class CjOffsetsOnlyDiagnosticWithParameters4<A, B, C, D>(
    /**
     * 产生诊断的抽象源码元素。
     */
    override val element: AbstractCjSourceElement,
    /**
     * 第一个诊断渲染参数。
     */
    override val a: A,
    /**
     * 第二个诊断渲染参数。
     */
    override val b: B,
    /**
     * 第三个诊断渲染参数。
     */
    override val c: C,
    /**
     * 第四个诊断渲染参数。
     */
    override val d: D,
    /**
     * 当前诊断的严重级别。
     */
    override val severity: Severity,
    /**
     * 创建该四参数诊断的工厂。
     */
    override val factory: CjDiagnosticFactory4<A, B, C, D>,
    /**
     * 根据 offset 计算高亮范围的定位策略。
     */
    override val positioningStrategy: AbstractSourceElementPositioningStrategy,
    /**
     * 渲染和抑制判断使用的诊断上下文。
     */
    override val context: DiagnosticBaseContext,
) : CjDiagnosticWithParameters4<A, B, C, D>(), CjOffsetsOnlyDiagnostic
