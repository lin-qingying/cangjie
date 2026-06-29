package org.cangnova.cangjie.analysis.api.lightDeclarations

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.psi.CjFile

/**
 * Light declaration 的来源元数据。
 *
 * 用于把 light declaration 反向定位回真实 PSI / 反编译 / 合成产物,
 * 例如在生成文档、跳转定义或导出报告时使用。
 *
 * @property kind 来源类别,详见 [CaLightDeclarationOriginKind]。
 * @property description 来源的人类可读描述,用于日志、调试与报告。
 * @property containingFile 声明所在的仓颉文件,若不存在(例如纯合成)则为 `null`。
 * @property sourceElement 与 light declaration 对齐的 PSI 节点;
 *  对于反编译或合成产物,该字段可能为 `null`。
 */
data class CaLightDeclarationOrigin(
    /** Light declaration 的来源类别。 */
    val kind: CaLightDeclarationOriginKind,

    /** 来源的人类可读描述。 */
    val description: String,

    /** 来源声明所在的仓颉文件，纯合成来源可为 `null`。 */
    val containingFile: CjFile?,

    /** 与 light declaration 对齐的原始 PSI 元素。 */
    val sourceElement: PsiElement?,
)
