package org.cangnova.cangjie.config

import java.util.Collections

/** 一个显式 common parent 输入及其可选的配对 CHIR。 */
public data class CjmpCommonPartInput(
    public val cjoPath: String,
    public val chirPath: String?,
)

/**
 * 一次编译归一化后的 common parent 输入值。
 *
 * 该模型只保存 CLI/configuration 已解析的输入事实；语言版本协议与参数错误由前端配置层负责。
 */
public class CjmpCommonPartInputs private constructor(parents: List<CjmpCommonPartInput>) {
    public val parents: List<CjmpCommonPartInput> = Collections.unmodifiableList(ArrayList(parents))

    public val commonPartCjoPaths: List<String> =
        Collections.unmodifiableList(this.parents.map { it.cjoPath })

    public val commonPartChirPaths: List<String> =
        Collections.unmodifiableList(this.parents.mapNotNull { it.chirPath })

    /** CFIR 声明角色解析使用 CJO parent 是否存在作为 specific 输入判据。 */
    public val isSpecificCompilation: Boolean
        get() = parents.isNotEmpty()

    public companion object {
        public val EMPTY: CjmpCommonPartInputs = CjmpCommonPartInputs(emptyList())

        /** 创建隔离于调用方可变列表的输入快照。 */
        public fun from(parents: List<CjmpCommonPartInput>): CjmpCommonPartInputs =
            if (parents.isEmpty()) EMPTY else CjmpCommonPartInputs(parents)
    }
}
