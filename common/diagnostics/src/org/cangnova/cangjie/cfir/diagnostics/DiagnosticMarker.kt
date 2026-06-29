package org.cangnova.cangjie.cfir.diagnostics

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.cfir.diagnostics.Severity

/**
 * 诊断对象暴露给测试、渲染和兼容层的最小标记接口。
 */
interface DiagnosticMarker {
    /**
     * 诊断锚定的 PSI 元素。
     */
    val psiElement: PsiElement
    /**
     * 诊断工厂名称。
     */
    val factoryName: String
    /**
     * 诊断严重级别。
     */
    val severity: Severity
    /**
     * 诊断高亮文本范围列表。
     */
    val textRanges: List<TextRange>
}

/**
 * 携带一个渲染参数的诊断标记。
 */
interface DiagnosticWithParameters1Marker<A> : DiagnosticMarker {
    /**
     * 第一个诊断参数。
     */
    val a: A
}

/**
 * 携带两个渲染参数的诊断标记。
 */
interface DiagnosticWithParameters2Marker<A, B> : DiagnosticMarker {
    /**
     * 第一个诊断参数。
     */
    val a: A
    /**
     * 第二个诊断参数。
     */
    val b: B
}

/**
 * 携带三个渲染参数的诊断标记。
 */
interface DiagnosticWithParameters3Marker<A, B, C> : DiagnosticMarker {
    /**
     * 第一个诊断参数。
     */
    val a: A
    /**
     * 第二个诊断参数。
     */
    val b: B
    /**
     * 第三个诊断参数。
     */
    val c: C
}

/**
 * 携带四个渲染参数的诊断标记。
 */
interface DiagnosticWithParameters4Marker<A, B, C, D> : DiagnosticMarker {
    /**
     * 第一个诊断参数。
     */
    val a: A
    /**
     * 第二个诊断参数。
     */
    val b: B
    /**
     * 第三个诊断参数。
     */
    val c: C
    /**
     * 第四个诊断参数。
     */
    val d: D
}

