package org.cangnova.cangjie.ide.completion.impl.cfir.recovery

import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.analyze
import org.cangnova.cangjie.analysis.api.completion.CaCompletionCandidateStatus
import org.cangnova.cangjie.analysis.api.components.CaScopeKind
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaNamedSymbol
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionIdentity
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionIdentityKind
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionImportPlan
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionImportTargetData
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionRecoveryService
import org.cangnova.cangjie.ide.completion.api.lookup.ImportResolution
import org.cangnova.cangjie.ide.completion.api.lookup.RecoveredTarget
import org.cangnova.cangjie.ide.completion.impl.cfir.sections.callableIdentityKey
import org.cangnova.cangjie.ide.completion.impl.cfir.sections.classifierIdentityKey
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjFile

/**
 * 候选语义复查实现。
 *
 * 复查的输入只有身份（不含符号、类型或会话），因此恢复路径是：
 * **重新进入分析 → 在使用点的作用域里按身份重新定位 → 交给统一候选判定器复算可达性与导入**。
 *
 * 定位按**身份键**而不是短名：仓颉允许同名不同参数的声明共存，
 * 只按短名匹配会把用户选中的 `foo(Int64)` 恢复成 `foo(String)`。
 */
@OptIn(CaExperimentalApi::class)
class CfirCompletionRecoveryService : CompletionRecoveryService {
    override fun recover(useSite: CjElement, identity: CompletionIdentity): RecoveredTarget? {
        // 关键字与文档标签没有可失效的声明身份：有效性只取决于语言版本。
        //
        // 声明生成建议同理，但理由不同：它**不引用已有声明**，而是「这里可以生成一个新成员」。
        // 它的有效性取决于「该类型此刻是否仍缺这个成员」，那是生成器的判断，
        // 不是按身份恢复符号能回答的。把它交给符号恢复会让每个声明建议都被判失效——
        // 而失效的表现是用户选什么都插不进去，且不报错。
        if (identity.kind == CompletionIdentityKind.KEYWORD ||
            identity.kind == CompletionIdentityKind.DOCUMENTATION ||
            identity.kind == CompletionIdentityKind.DECLARATION_SUGGESTION
        ) {
            return RecoveredTarget(identity, identity.qualifiedName, CompletionImportPlan.NoImport, visible = true)
        }
        val file = useSite.containingCjFile ?: return null
        return analyze(file) {
            val symbol = locateSymbol(file, identity) ?: return@analyze null
            val decision = symbol.checkCompletionCandidate(useSite)
            if (!decision.status.isDeterminedUsable) return@analyze null
            val importPlan = when (decision.status) {
                CaCompletionCandidateStatus.DIRECT -> CompletionImportPlan.NoImport
                CaCompletionCandidateStatus.REQUIRES_IMPORT -> CompletionImportPlan.AddImport(
                    CompletionImportTargetData(
                        importPath = checkNotNull(decision.requiredImport) {
                            "REQUIRES_IMPORT 候选必须提供 requiredImport: ${decision.symbol}"
                        },
                        expressionText = identity.name,
                    ),
                )
                CaCompletionCandidateStatus.HIDDEN,
                CaCompletionCandidateStatus.NOT_APPLICABLE,
                CaCompletionCandidateStatus.UNKNOWN,
                -> return@analyze null
            }
            RecoveredTarget(
                identity = identity,
                qualifiedName = identity.qualifiedName.ifEmpty { identity.name },
                importPlan = importPlan,
                visible = true,
            )
        }
    }

    /**
     * 在使用点按稳定身份定位符号。
     *
     * 两条路径：
     * 1. **导入作用域**——已可直接引用的声明走这里；
     * 2. **包作用域回退**——分析期结论为 `AddImport` 的候选按定义就不在导入作用域里，
     *    只查导入作用域会把它们一律判成失效，导致「需要导入的候选永远插不进去」。
     *
     * 局部声明两条路径都不查：它们绑定在具体的函数体上，而弹窗展示期间函数体
     * 可能已经改变。恢复时把局部候选判为已失效，让调用方退回「重新触发一次补全」，
     * 比插入到一个作用域已变的同名变量上安全。
     */
    private fun CaSession.locateSymbol(
        file: CjFile,
        identity: CompletionIdentity,
    ): CaDeclarationSymbol? {
        for (entry in file.importingScopeContext.scopes) {
            if (entry.kind == CaScopeKind.LOCAL_SCOPE) continue
            when (identity.kind) {
                CompletionIdentityKind.TYPE -> {
                    for (classifier in entry.scope.classifiers { true }) {
                        if (classifierIdentityKey(classifier) == identity.handle) {
                            return classifier
                        }
                    }
                }
                CompletionIdentityKind.SYMBOL -> {
                    for (callable in entry.scope.callables { true }) {
                        if (matchesIdentity(callable, identity)) {
                            return callable
                        }
                    }
                }
                else -> Unit
            }
        }
        return locateInPackageScope(identity)
    }

    /**
     * 包作用域回退：按身份的限定名找到所属包，再在其中按身份键定位。
     *
     * 包不存在时返回 `null`：那正是「声明确实已不存在」的信号。
     */
    private fun CaSession.locateInPackageScope(identity: CompletionIdentity): CaDeclarationSymbol? {
        val packagePath = identity.qualifiedName.substringBeforeLast('.', "")
        if (packagePath.isEmpty()) return null
        val packageScope = getPackageScope(FqName.fromSegments(packagePath.split('.'))) ?: return null
        when (identity.kind) {
            CompletionIdentityKind.TYPE ->
                return packageScope.classifiers { true }
                    .firstOrNull { classifierIdentityKey(it) == identity.handle }
            CompletionIdentityKind.SYMBOL ->
                return packageScope.callables { true }.firstOrNull { matchesIdentity(it, identity) }
            else -> Unit
        }
        return null
    }

    /**
     * 符号身份与候选身份是否同一。
     *
     * 用 [callableIdentityKey] 而不是短名：它把签名并入键，因此同名重载不会被
     * 互相认领，恢复总是落回用户实际选中的那个重载。
     */
    private fun CaSession.matchesIdentity(
        symbol: CaCallableSymbol,
        identity: CompletionIdentity,
    ): Boolean {
        val shortName = (symbol as? CaNamedSymbol)?.name?.asString() ?: return false
        if (shortName != identity.name) return false
        return callableIdentityKey(symbol) == identity.deduplicationKey
    }

}

/**
 * 把复查结论包装成导入解析结果，供插入阶段与 [ImportResolution] 一起消费。
 */
fun RecoveredTarget.asImportResolution(): ImportResolution = ImportResolution(importPlan)
