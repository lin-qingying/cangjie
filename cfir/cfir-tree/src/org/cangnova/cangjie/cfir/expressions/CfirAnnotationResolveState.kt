package org.cangnova.cangjie.cfir.expressions

/**
 * 单个 annotation call 的语义解析状态。
 *
 * 该状态只描述 annotation call 已完成的工作，不创建独立 lazy resolver；
 * 状态推进由宿主 declaration 的现有 TYPES/STATUS/BODY_RESOLVE processor
 * 负责，失败必须停在 [ERROR] 而不能退回 raw 名称路径。
 */
public enum class CfirAnnotationResolveState {
    /** 仅有 raw annotation surface。 */
    UNRESOLVED,

    /** annotation type reference 已解析。 */
    TYPE_RESOLVED,

    /** 显式实参已按现有 mapping 完成绑定。 */
    ARGUMENTS_RESOLVED,

    /** annotation 的语义 metadata 已发布给消费者。 */
    SEMANTIC_RESOLVED,

    /** 任一 owner 阶段失败；该状态可观察且不可静默降级。 */
    ERROR,
}
