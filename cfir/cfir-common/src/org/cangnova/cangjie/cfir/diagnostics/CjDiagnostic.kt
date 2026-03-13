package org.cangjie.cfir.diagnostics

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.cangjie.cfir.source.AbstractCjSourceElement
import org.cangjie.cfir.source.CjLightSourceElement
import org.cangjie.cfir.source.CjPsiSourceElement
import org.cangjie.cli.common.messages.CompilerMessageSourceLocation

// ------------------------------ diagnostics ------------------------------

sealed class CjDiagnostic {
    abstract val severity: Severity
    abstract val factory: AbstractCjDiagnosticFactory
    abstract val isValid: Boolean
    abstract val firstRange: TextRange
    abstract val context: DiagnosticBaseContext

    val factoryName: String
        get() = factory.name

    fun renderMessage(): String {
        return factory.ktRenderer.render(this)
    }
}

class CjDiagnosticWithoutSource(
    val message: String,
    val location: CompilerMessageSourceLocation?,
    override val severity: Severity,
    override val factory: CjSourcelessDiagnosticFactory,
    override val context: DiagnosticBaseContext,
) : CjDiagnostic() {
    override val isValid: Boolean
        get() = true

    override val firstRange: TextRange
        get() = TextRange.EMPTY_RANGE
}

sealed class CjDiagnosticWithSource : CjDiagnostic(), DiagnosticMarker {
    abstract val element: AbstractCjSourceElement
    abstract override val factory: CjDiagnosticFactoryN
    abstract val positioningStrategy: AbstractSourceElementPositioningStrategy
    abstract override val severity: Severity

    final override val textRanges: List<TextRange>
        get() = positioningStrategy.markDiagnostic(this)

    final override val isValid: Boolean
        get() = positioningStrategy.isValid(element)

    final override val firstRange: TextRange
        get() = DiagnosticRangeUtils.firstRange(textRanges)
}

sealed class CjSimpleDiagnostic : CjDiagnosticWithSource() {
    abstract override val factory: CjDiagnosticFactory0
}

sealed class CjDiagnosticWithParameters1<A> : CjDiagnosticWithSource(), DiagnosticWithParameters1Marker<A> {
    abstract override val a: A
    abstract override val factory: CjDiagnosticFactory1<A>
}

sealed class CjDiagnosticWithParameters2<A, B> : CjDiagnosticWithSource(), DiagnosticWithParameters2Marker<A, B> {
    abstract override val a: A
    abstract override val b: B
    abstract override val factory: CjDiagnosticFactory2<A, B>
}

sealed class CjDiagnosticWithParameters3<A, B, C> : CjDiagnosticWithSource(), DiagnosticWithParameters3Marker<A, B, C> {
    abstract override val a: A
    abstract override val b: B
    abstract override val c: C
    abstract override val factory: CjDiagnosticFactory3<A, B, C>
}

sealed class CjDiagnosticWithParameters4<A, B, C, D> : CjDiagnosticWithSource(),
    DiagnosticWithParameters4Marker<A, B, C, D> {
    abstract override val a: A
    abstract override val b: B
    abstract override val c: C
    abstract override val d: D
    abstract override val factory: CjDiagnosticFactory4<A, B, C, D>
}

// ------------------------------ psi diagnostics ------------------------------

interface CjPsiDiagnostic : DiagnosticMarker {
    val factory: CjDiagnosticFactoryN
    val element: CjPsiSourceElement
    override val textRanges: List<TextRange>
    override val severity: Severity

    override val psiElement: PsiElement
        get() = element.psi

    val psiFile: PsiFile
        get() = psiElement.containingFile
}

private const val CHECK_PSI_CONSISTENCY_IN_DIAGNOSTICS = true

private fun CjPsiDiagnostic.checkPsiTypeConsistency() {
    if (CHECK_PSI_CONSISTENCY_IN_DIAGNOSTICS) {
        require(factory.psiType.isInstance(element.psi)) {
            "${element.psi::class} is not a subtype of ${factory.psiType} for factory $factory"
        }
    }
}

data class CjPsiSimpleDiagnostic(
    override val element: CjPsiSourceElement,
    override val severity: Severity,
    override val factory: CjDiagnosticFactory0,
    override val positioningStrategy: AbstractSourceElementPositioningStrategy,
    override val context: DiagnosticBaseContext,
) : CjSimpleDiagnostic(), CjPsiDiagnostic {
    init {
        checkPsiTypeConsistency()
    }
}

data class CjPsiDiagnosticWithParameters1<A>(
    override val element: CjPsiSourceElement,
    override val a: A,
    override val severity: Severity,
    override val factory: CjDiagnosticFactory1<A>,
    override val positioningStrategy: AbstractSourceElementPositioningStrategy,
    override val context: DiagnosticBaseContext,
) : CjDiagnosticWithParameters1<A>(), CjPsiDiagnostic {
    init {
        checkPsiTypeConsistency()
    }
}


data class CjPsiDiagnosticWithParameters2<A, B>(
    override val element: CjPsiSourceElement,
    override val a: A,
    override val b: B,
    override val severity: Severity,
    override val factory: CjDiagnosticFactory2<A, B>,
    override val positioningStrategy: AbstractSourceElementPositioningStrategy,
    override val context: DiagnosticBaseContext,
) : CjDiagnosticWithParameters2<A, B>(), CjPsiDiagnostic {
    init {
        checkPsiTypeConsistency()
    }
}

data class CjPsiDiagnosticWithParameters3<A, B, C>(
    override val element: CjPsiSourceElement,
    override val a: A,
    override val b: B,
    override val c: C,
    override val severity: Severity,
    override val factory: CjDiagnosticFactory3<A, B, C>,
    override val positioningStrategy: AbstractSourceElementPositioningStrategy,
    override val context: DiagnosticBaseContext,
) : CjDiagnosticWithParameters3<A, B, C>(), CjPsiDiagnostic {
    init {
        checkPsiTypeConsistency()
    }
}

data class CjPsiDiagnosticWithParameters4<A, B, C, D>(
    override val element: CjPsiSourceElement,
    override val a: A,
    override val b: B,
    override val c: C,
    override val d: D,
    override val severity: Severity,
    override val factory: CjDiagnosticFactory4<A, B, C, D>,
    override val positioningStrategy: AbstractSourceElementPositioningStrategy,
    override val context: DiagnosticBaseContext,
) : CjDiagnosticWithParameters4<A, B, C, D>(), CjPsiDiagnostic {
    init {
        checkPsiTypeConsistency()
    }
}

// ------------------------------ light tree diagnostics ------------------------------

interface CjLightDiagnostic : DiagnosticMarker {
    val element: CjLightSourceElement

    @Deprecated("Should not be called", level = DeprecationLevel.HIDDEN)
    override val psiElement: PsiElement
        get() = error("psiElement should not be called on CjLightDiagnostic")
}

data class CjLightSimpleDiagnostic(
    override val element: CjLightSourceElement,
    override val severity: Severity,
    override val factory: CjDiagnosticFactory0,
    override val positioningStrategy: AbstractSourceElementPositioningStrategy,
    override val context: DiagnosticBaseContext,
) : CjSimpleDiagnostic(), CjLightDiagnostic

data class CjLightDiagnosticWithParameters1<A>(
    override val element: CjLightSourceElement,
    override val a: A,
    override val severity: Severity,
    override val factory: CjDiagnosticFactory1<A>,
    override val positioningStrategy: AbstractSourceElementPositioningStrategy,
    override val context: DiagnosticBaseContext,
) : CjDiagnosticWithParameters1<A>(), CjLightDiagnostic

data class CjLightDiagnosticWithParameters2<A, B>(
    override val element: CjLightSourceElement,
    override val a: A,
    override val b: B,
    override val severity: Severity,
    override val factory: CjDiagnosticFactory2<A, B>,
    override val positioningStrategy: AbstractSourceElementPositioningStrategy,
    override val context: DiagnosticBaseContext,
) : CjDiagnosticWithParameters2<A, B>(), CjLightDiagnostic

data class CjLightDiagnosticWithParameters3<A, B, C>(
    override val element: CjLightSourceElement,
    override val a: A,
    override val b: B,
    override val c: C,
    override val severity: Severity,
    override val factory: CjDiagnosticFactory3<A, B, C>,
    override val positioningStrategy: AbstractSourceElementPositioningStrategy,
    override val context: DiagnosticBaseContext,
) : CjDiagnosticWithParameters3<A, B, C>(), CjLightDiagnostic

data class CjLightDiagnosticWithParameters4<A, B, C, D>(
    override val element: CjLightSourceElement,
    override val a: A,
    override val b: B,
    override val c: C,
    override val d: D,
    override val severity: Severity,
    override val factory: CjDiagnosticFactory4<A, B, C, D>,
    override val positioningStrategy: AbstractSourceElementPositioningStrategy,
    override val context: DiagnosticBaseContext,
) : CjDiagnosticWithParameters4<A, B, C, D>(), CjLightDiagnostic

// ------------------------------ offset-based diagnostics ------------------------------

interface CjOffsetsOnlyDiagnostic : DiagnosticMarker {
    val element: AbstractCjSourceElement

    @Deprecated("Should not be called", level = DeprecationLevel.HIDDEN)
    override val psiElement: PsiElement
        get() = error("psiElement should not be called on CjOffsetsOnlyDiagnostic")
}

data class CjOffsetsOnlySimpleDiagnostic(
    override val element: AbstractCjSourceElement,
    override val severity: Severity,
    override val factory: CjDiagnosticFactory0,
    override val positioningStrategy: AbstractSourceElementPositioningStrategy,
    override val context: DiagnosticBaseContext,
) : CjSimpleDiagnostic(), CjOffsetsOnlyDiagnostic

data class CjOffsetsOnlyDiagnosticWithParameters1<A>(
    override val element: AbstractCjSourceElement,
    override val a: A,
    override val severity: Severity,
    override val factory: CjDiagnosticFactory1<A>,
    override val positioningStrategy: AbstractSourceElementPositioningStrategy,
    override val context: DiagnosticBaseContext,
) : CjDiagnosticWithParameters1<A>(), CjOffsetsOnlyDiagnostic

data class CjOffsetsOnlyDiagnosticWithParameters2<A, B>(
    override val element: AbstractCjSourceElement,
    override val a: A,
    override val b: B,
    override val severity: Severity,
    override val factory: CjDiagnosticFactory2<A, B>,
    override val positioningStrategy: AbstractSourceElementPositioningStrategy,
    override val context: DiagnosticBaseContext,
) : CjDiagnosticWithParameters2<A, B>(), CjOffsetsOnlyDiagnostic

data class CjOffsetsOnlyDiagnosticWithParameters3<A, B, C>(
    override val element: AbstractCjSourceElement,
    override val a: A,
    override val b: B,
    override val c: C,
    override val severity: Severity,
    override val factory: CjDiagnosticFactory3<A, B, C>,
    override val positioningStrategy: AbstractSourceElementPositioningStrategy,
    override val context: DiagnosticBaseContext,
) : CjDiagnosticWithParameters3<A, B, C>(), CjOffsetsOnlyDiagnostic

data class CjOffsetsOnlyDiagnosticWithParameters4<A, B, C, D>(
    override val element: AbstractCjSourceElement,
    override val a: A,
    override val b: B,
    override val c: C,
    override val d: D,
    override val severity: Severity,
    override val factory: CjDiagnosticFactory4<A, B, C, D>,
    override val positioningStrategy: AbstractSourceElementPositioningStrategy,
    override val context: DiagnosticBaseContext,
) : CjDiagnosticWithParameters4<A, B, C, D>(), CjOffsetsOnlyDiagnostic



