package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CheckerSink

/**
 * collection literal 约束系统需要回写的外层候选上下文。
 *
 * 在重载解析阶段它通常指向直接外层调用；在 completion 阶段也可以指向更外层的任意调用。
 */
class CollectionLiteralOuterCandidateContext(
    /**
     * 需要被 collection literal 约束系统扩展的候选。
     */
    val containingCandidate: Candidate,
    /**
     * 外层候选的 checker sink。
     *
     * 只有 collection literal 作为某个外层调用重载解析的一部分被展开时才非 null。
     */
    val checkerSink: CheckerSink? = null,
)
