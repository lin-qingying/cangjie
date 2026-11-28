package org.cangnova.cangjie

import java.util.Collections
import org.cangnova.cangjie.parsing.CjmpFileRole

/** 一次检查的稳定身份与采集顺序；顺序来自解析过程，而不是源码坐标。 */
public data class CjmpRoleEventId<T : Any>(val value: T)

/** 保留原始拼写，角色映射由消费方的语言版本决定。 */
public data class CjmpRoleModifiers(val common: Boolean, val specific: Boolean, val platform: Boolean)

/** 未知上下文不能当作跳过检查处理。 */
public sealed interface CjmpRoleEventKind {
    public data class Check(val modifiers: CjmpRoleModifiers) : CjmpRoleEventKind
    /** extend 仅在原始 token 集合恰含一个可用的角色修饰符时检查文件角色。 */
    public data class Extend(val modifiers: CjmpRoleModifiers, val distinctModifierCount: Int) : CjmpRoleEventKind {
        init {
            val roleTokens = listOf(modifiers.common, modifiers.specific, modifiers.platform).count { it }
            require(distinctModifierCount >= roleTokens)
        }
    }
    public data object Skipped : CjmpRoleEventKind
    public data object Unknown : CjmpRoleEventKind
}

public data class CjmpRoleEvent<T : Any>(val id: CjmpRoleEventId<T>, val order: Long, val kind: CjmpRoleEventKind)

/** PREFIX 包含从文件起点开始的连续事件；PARTIAL 不保证任何位置。 */
public enum class CjmpRoleInputCompleteness { COMPLETE, PREFIX, PARTIAL }

/** 采集语法与消费版本是否已证明具有相同声明及修饰符身份。 */
public enum class CjmpRoleSyntaxIdentity { VERIFIED_EQUIVALENT, UNKNOWN }

/** 前置角色未知时仅发布对所有可能角色均成立的检查结果；状态仍不确定则为 null。 */
public data class CjmpRoleEventResult<T : Any>(
    val event: CjmpRoleEvent<T>,
    val before: CjmpFileRole?,
    val after: CjmpFileRole?,
    val outcome: CjmpRoleEventOutcome,
    val error: CjmpFileRoleError?,
)

public enum class CjmpRoleEventOutcome { ACCEPTED, REJECTED, NOT_CHECKED, SKIPPED, UNKNOWN }

public data class CjmpRoleFirstError<T : Any>(val eventId: CjmpRoleEventId<T>, val error: CjmpFileRoleError)

/** 检查上下文与输入缺失分别记录，避免将未执行检查误认为成功。 */
public enum class CjmpRoleReplayContext { ENABLED_COMPILATION, DISABLED_COMPILATION, SYNTAX_ONLY }

/** 只有完整分支包含完整文件结论；其他分支只暴露已观察到的结果。 */
public sealed class CjmpRoleReplayResult<T : Any>(results: List<CjmpRoleEventResult<T>>) {
    public val events: List<CjmpRoleEventResult<T>> = Collections.unmodifiableList(results.toList())

    public class Complete<T : Any> internal constructor(
        results: List<CjmpRoleEventResult<T>>,
        public val finalRole: CjmpFileRole,
        public val firstError: CjmpRoleFirstError<T>?,
    ) : CjmpRoleReplayResult<T>(results)

    public class NonAuthoritative<T : Any> internal constructor(
        results: List<CjmpRoleEventResult<T>>,
        public val observedRole: CjmpFileRole?,
        public val firstKnownError: CjmpRoleFirstError<T>?,
        public val completeness: CjmpRoleInputCompleteness,
        public val context: CjmpRoleReplayContext,
        public val hasUnknownEvents: Boolean,
        public val syntaxIdentity: CjmpRoleSyntaxIdentity,
    ) : CjmpRoleReplayResult<T>(results)
}

/** 按实际检查顺序重放角色状态，并保留被拒绝检查的部分状态写入。 */
public object CjmpFileRoleReplay {
    public fun <T : Any> replay(
        mode: CjmpFileRoleMode,
        initialRole: CjmpFileRole?,
        completeness: CjmpRoleInputCompleteness,
        events: List<CjmpRoleEvent<T>>,
        languageVersionSettings: LanguageVersionSettings,
        syntaxIdentity: CjmpRoleSyntaxIdentity,
    ): CjmpRoleReplayResult<T> {
        require((completeness == CjmpRoleInputCompleteness.PARTIAL) == (initialRole == null)) {
            "Partial input has no known initial state; complete input and prefixes require one."
        }
        val ids = HashSet<CjmpRoleEventId<T>>()
        var previousOrder: Long? = null
        var role: CjmpFileRole? = if (syntaxIdentity == CjmpRoleSyntaxIdentity.VERIFIED_EQUIVALENT) initialRole else null
        // 前缀缺失时保留所有可能角色；只有所有候选产生相同观察才能发布确定结论。
        var possibleRoles: Set<CjmpFileRole> = role?.let(::setOf) ?: CjmpFileRole.entries.toSet()
        var firstError: CjmpRoleFirstError<T>? = null
        var known = syntaxIdentity == CjmpRoleSyntaxIdentity.VERIFIED_EQUIVALENT
        val results = ArrayList<CjmpRoleEventResult<T>>(events.size)
        val checksEnabled = mode is CjmpFileRoleMode.Compilation && mode.enabled
        for (event in events) {
            require(ids.add(event.id)) { "Duplicate role event identity: ${event.id}" }
            require(previousOrder == null || event.order > previousOrder) { "Role events must be in strictly increasing order." }
            previousOrder = event.order
            val before = role
            var error: CjmpFileRoleError? = null
            val outcome = when (val kind = event.kind) {
                CjmpRoleEventKind.Unknown -> {
                    known = false
                    role = null
                    possibleRoles = CjmpFileRole.entries.toSet()
                    CjmpRoleEventOutcome.UNKNOWN
                }
                CjmpRoleEventKind.Skipped -> if (syntaxIdentity == CjmpRoleSyntaxIdentity.VERIFIED_EQUIVALENT)
                    CjmpRoleEventOutcome.SKIPPED else CjmpRoleEventOutcome.UNKNOWN
                is CjmpRoleEventKind.Check, is CjmpRoleEventKind.Extend -> {
                    val modifiers = when (kind) {
                        is CjmpRoleEventKind.Check -> kind.modifiers
                        is CjmpRoleEventKind.Extend -> kind.modifiers
                        else -> error("Unexpected role event")
                    }
                    val declaration = when (mode) {
                        CjmpFileRoleMode.SyntaxOnly -> CjmpDeclarationRole(modifiers.common, modifiers.specific)
                        is CjmpFileRoleMode.Compilation -> CjmpDeclarationRoleResolver.resolve(
                            modifiers.common, modifiers.specific, modifiers.platform, languageVersionSettings,
                        )
                    }
                    val unknownPlatform = mode is CjmpFileRoleMode.SyntaxOnly && modifiers.platform
                    val skipExtend = kind is CjmpRoleEventKind.Extend &&
                        (kind.distinctModifierCount != 1 || (!declaration.isCommon && !declaration.isSpecific) ||
                            (mode is CjmpFileRoleMode.Compilation && (!mode.enabled || (!mode.compileCommon && !mode.compileSpecific))))
                    if (syntaxIdentity == CjmpRoleSyntaxIdentity.UNKNOWN || unknownPlatform) {
                        known = false
                        role = null
                        possibleRoles = CjmpFileRole.entries.toSet()
                        CjmpRoleEventOutcome.UNKNOWN
                    } else if (skipExtend) CjmpRoleEventOutcome.SKIPPED else
                    {
                        val transitions = possibleRoles.map { CjmpFileRoleTransition.check(mode, it, declaration) }
                        possibleRoles = transitions.mapTo(linkedSetOf()) { it.role }
                        role = possibleRoles.singleOrNull()
                        val errors = transitions.map { it.error }.distinct()
                        error = errors.singleOrNull()
                        if (!checksEnabled) CjmpRoleEventOutcome.NOT_CHECKED
                        else if (transitions.all { it.error == null }) CjmpRoleEventOutcome.ACCEPTED
                        else if (transitions.all { it.error != null }) {
                            if (firstError == null && error != null) firstError = CjmpRoleFirstError(event.id, error)
                            CjmpRoleEventOutcome.REJECTED
                        } else {
                            error = null
                            CjmpRoleEventOutcome.UNKNOWN
                        }
                    }
                }
            }
            results.add(CjmpRoleEventResult(event, before, role, outcome, error))
        }
        if (checksEnabled && known && completeness == CjmpRoleInputCompleteness.COMPLETE) {
            return CjmpRoleReplayResult.Complete(results, checkNotNull(role), firstError)
        }
        val context = when (mode) {
            CjmpFileRoleMode.SyntaxOnly -> CjmpRoleReplayContext.SYNTAX_ONLY
            is CjmpFileRoleMode.Compilation -> if (mode.enabled) CjmpRoleReplayContext.ENABLED_COMPILATION
                else CjmpRoleReplayContext.DISABLED_COMPILATION
        }
        return CjmpRoleReplayResult.NonAuthoritative(results, role, firstError, completeness, context, !known, syntaxIdentity)
    }
}
