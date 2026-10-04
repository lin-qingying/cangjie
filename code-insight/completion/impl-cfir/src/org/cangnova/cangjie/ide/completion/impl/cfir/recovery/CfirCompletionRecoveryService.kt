package org.cangnova.cangjie.ide.completion.impl.cfir.recovery

import org.cangnova.cangjie.ImportPath
import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.analyze
import org.cangnova.cangjie.analysis.api.components.CaScopeKind
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaNamedSymbol
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionIdentity
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionIdentityKind
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionImportPlan
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionImportTargetData
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionRecoveryService
import org.cangnova.cangjie.ide.completion.api.lookup.ImportResolution
import org.cangnova.cangjie.ide.completion.api.lookup.RecoveredTarget
import org.cangnova.cangjie.ide.completion.impl.cfir.sections.callableIdentityKey
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjFile

/**
 * 候选语义复查实现。
 *
 * 复查的输入只有身份（不含符号、类型或会话），因此恢复路径是：
 * **重新进入分析 → 在使用点的作用域里按身份重新定位 → 重算导入需求**。
 *
 * 定位按**身份键**而不是短名：仓颉允许同名不同参数的声明共存，
 * 只按短名匹配会把用户选中的 `foo(Int64)` 恢复成 `foo(String)`。
 */
@OptIn(CaExperimentalApi::class)
class CfirCompletionRecoveryService : CompletionRecoveryService {
    override fun recover(useSite: CjElement, identity: CompletionIdentity): RecoveredTarget? {
        // 关键字与文档标签没有可失效的声明身份：有效性只取决于语言版本。
        if (identity.kind == CompletionIdentityKind.KEYWORD ||
            identity.kind == CompletionIdentityKind.DOCUMENTATION
        ) {
            return RecoveredTarget(identity, identity.qualifiedName, CompletionImportPlan.NoImport, visible = true)
        }
        val file = useSite.containingCjFile ?: return null
        val located = analyze(file) { locateSymbol(file, identity) } ?: return null
        return RecoveredTarget(
            identity = identity,
            qualifiedName = located,
            importPlan = importPlanFor(file, identity, located),
            visible = true,
        )
    }

    /**
     * 在使用点定位符号，返回其限定名。
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
    ): String? {
        for (entry in file.importingScopeContext.scopes) {
            if (entry.kind == CaScopeKind.LOCAL_SCOPE) continue
            when (identity.kind) {
                CompletionIdentityKind.TYPE -> {
                    for (classifier in entry.scope.classifiers { true }) {
                        if ((classifier as? CaNamedSymbol)?.name?.asString() == identity.name) {
                            return identity.qualifiedName.ifEmpty { identity.name }
                        }
                    }
                }
                CompletionIdentityKind.SYMBOL -> {
                    for (callable in entry.scope.callables { true }) {
                        if (matchesIdentity(callable, identity)) {
                            return identity.qualifiedName.ifEmpty { identity.name }
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
    private fun CaSession.locateInPackageScope(identity: CompletionIdentity): String? {
        val packagePath = identity.qualifiedName.substringBeforeLast('.', "")
        if (packagePath.isEmpty()) return null
        val packageScope = getPackageScope(FqName.fromSegments(packagePath.split('.'))) ?: return null
        when (identity.kind) {
            CompletionIdentityKind.TYPE ->
                if (packageScope.classifiers { true }
                        .any { (it as? CaNamedSymbol)?.name?.asString() == identity.name }
                ) {
                    return identity.qualifiedName
                }
            CompletionIdentityKind.SYMBOL ->
                if (packageScope.callables { true }.any { matchesIdentity(it, identity) }) {
                    return identity.qualifiedName
                }
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

    /**
     * 依据文件当前状态重算导入需求。
     *
     * 复查时重新判定而不是复用分析期结论：弹窗展示期间用户可能已经手工加了
     * 或删了 import，沿用旧结论会写出重复行，或在已导入时仍然要求追加。
     *
     * 三条「不需要 import」的判定缺一不可：
     * 1. 限定名与短名相同——目标就在可达范围内，无需写包路径；
     * 2. 目标包就是**本文件自己的包**——同包成员无需导入，
     *    漏掉这一条会为同包声明写出 `import 本包` 这种多余且非法的语句；
     * 3. 文件里已经有等价 import——再写一条会造成二义。
     */
    private fun importPlanFor(
        file: CjFile,
        identity: CompletionIdentity,
        qualifiedName: String,
    ): CompletionImportPlan {
        val packagePath = qualifiedName.substringBeforeLast('.', "")
        if (packagePath.isEmpty() || qualifiedName == identity.name) return CompletionImportPlan.NoImport
        val targetPackage = FqName.fromSegments(packagePath.split('.'))
        if (targetPackage == file.packageFqName) return CompletionImportPlan.NoImport
        val path = ImportPath(targetPackage, false)
        val alreadyImported = file.text.lineSequence().any { line ->
            line.trim().removePrefix("import ").trim() == path.pathStr
        }
        if (alreadyImported) return CompletionImportPlan.NoImport
        return CompletionImportPlan.AddImport(
            CompletionImportTargetData(importPath = path, expressionText = identity.name),
        )
    }
}

/**
 * 把复查结论包装成导入解析结果，供插入阶段与 [ImportResolution] 一起消费。
 */
fun RecoveredTarget.asImportResolution(): ImportResolution = ImportResolution(importPlan)
