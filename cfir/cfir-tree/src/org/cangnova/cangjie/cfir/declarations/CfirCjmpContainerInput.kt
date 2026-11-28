package org.cangnova.cangjie.cfir.declarations

import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.resolve.calls.mpp.CjmpContainerKind
import java.util.Collections

/** 原声明的 begin 位置；初始化顺序只比较行列，不以文件编号或合并槽位替代。 */
data class CfirCjmpSourcePosition(val fileName: String, val line: Int, val column: Int)

/** 合并前的真实成员输入；初始化依赖由另一生命周期提供，不冻结在 CJMP 根属性内。 */
data class CfirCjmpMemberInput(
    val symbol: CfirBasedSymbol<*>,
    val name: String,
    val isInstanceVariable: Boolean,
    val isMemberParameter: Boolean,
    val isCommon: Boolean,
    val isSpecific: Boolean,
    val hasCommonDefault: Boolean,
    val originalDoNotExport: Boolean,
    val isFromCommonPart: Boolean,
    val sourcePosition: CfirCjmpSourcePosition?,
)

/** extend 原始类型头；类型等价类在聚合缓存外准备，纯合并只消费其规范键。 */
class CfirCjmpExtendKey(
    val target: ConeCangJieType,
    typeParameters: List<CfirTypeParameterSymbol>,
    interfaces: List<ConeCangJieType>,
    bounds: List<List<ConeCangJieType>>,
) {
    val typeParameters: List<CfirTypeParameterSymbol> = immutableCjmpList(typeParameters)
    val interfaces: List<ConeCangJieType> = immutableCjmpList(interfaces)
    val bounds: List<List<ConeCangJieType>> = immutableCjmpList(bounds.map(::immutableCjmpList))

    init {
        require(typeParameters.size == bounds.size) { "Every extend type parameter must retain its bounds" }
    }
}

/** 只包含本声明原有的物理成员与类型头；合并成员图、占用和最终实现均属于派生结果。 */
class CfirCjmpContainerInput(
    val symbol: CfirBasedSymbol<*>,
    val name: String,
    val kind: CjmpContainerKind,
    val isCommon: Boolean,
    val isSpecific: Boolean,
    val hasGenericNode: Boolean,
    typeParameters: List<CfirTypeParameterSymbol>,
    val isNonExhaustive: Boolean,
    val originalDoNotExport: Boolean,
    members: List<CfirCjmpMemberInput>,
    val sourcePosition: CfirCjmpSourcePosition?,
    val extendKey: CfirCjmpExtendKey?,
) {
    val typeParameters: List<CfirTypeParameterSymbol> = immutableCjmpList(typeParameters)
    val members: List<CfirCjmpMemberInput> = immutableCjmpList(members)
}

internal fun <T> immutableCjmpList(values: Collection<T>): List<T> =
    Collections.unmodifiableList(ArrayList(values))
