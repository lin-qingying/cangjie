package org.cangnova.cangjie.analysis.api.renderer.declarations.bodies

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaEnumConstructorSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaDeclarationContainerSymbol
import org.cangnova.cangjie.analysis.api.symbols.name

/**
 * 类主体成员排序策略。
 *
 * 在 provider 提供的成员基础上, 决定它们在输出中的顺序。
 *
 * 对齐 Kotlin Analysis API 的 `KaRendererBodyMemberScopeSorter`。
 */
fun interface CaRendererBodyMemberScopeSorter {
    /** 对 [members] 进行排序并返回新列表。 */
    fun sortMembers(
        analysisSession: CaSession,
        members: List<CaDeclarationSymbol>,
        container: CaDeclarationContainerSymbol,
    ): List<CaDeclarationSymbol>

    companion object {


        /**
         * 预设: 将 enum 构造子(`case`)放在最前, 其余成员保持相对顺序。
         *
         * 贴近仓颉 enum 源码风格: 构造子列在前, 然后是普通成员。
         */
        val ENUM_CONSTRUCTORS_AT_BEGINNING: CaRendererBodyMemberScopeSorter = CaRendererBodyMemberScopeSorter {
                _: CaSession,
                members: List<CaDeclarationSymbol>,
                _: CaDeclarationContainerSymbol,
            -> members.sortedBy { it !is CaEnumConstructorSymbol }
        }
    }
}
