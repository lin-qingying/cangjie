package org.cangnova.cangjie.analysis.api.annotations

import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name

/**
 * Analysis API 中公开的注解语义视图。
 *
 * 该抽象用于统一承载“声明上实际出现了哪些注解”这一层稳定信息，
 * 面向上层暴露注解目标类标识、短名以及源码级参数文本。
 *
 * 当前阶段注解模型刻意只表达稳定的结构化信息：
 * 1. [classId] 表示已经能稳定恢复到公开 class-like 标识时的目标类；
 * 2. [shortName] 表示源码层可见的短名；
 * 3. [arguments] 表示注解实参的源码文本快照；
 * 4. [renderedText] 保留原始注解文本，便于文档、渲染和工具层直接消费。
 *
 * 这里不暴露底层 CFIR 注解节点，也不要求上层直接访问 PSI。
 */
interface CaAnnotation : CaLifetimeOwner {
    /**
     * 注解类的公开标识。
     *
     * 当注解目标无法稳定恢复为公开 class-like 标识时返回 `null`。
     * 这是显式语义边界，而不是兜底伪造。
     */
    val classId: ClassId?

    /**
     * 注解在源码中呈现的短名。
     */
    val shortName: Name?

    /**
     * 注解实参的源码文本列表，顺序与源码书写顺序一致。
     */
    val arguments: List<String>

    /**
     * 注解条目的源码渲染文本。
     */
    val renderedText: String
}
