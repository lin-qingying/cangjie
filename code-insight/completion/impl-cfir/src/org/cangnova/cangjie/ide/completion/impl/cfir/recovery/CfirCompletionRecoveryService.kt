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
import org.cangnova.cangjie.ide.completion.impl.cfir.sections.classifierIdentityKey
import org.cangnova.cangjie.ide.completion.impl.shared.imports.toImportPath
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
                        if (classifierIdentityKey(classifier) == identity.deduplicationKey) {
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
                        .any { classifierIdentityKey(it) == identity.deduplicationKey }
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
        val targetFqName = FqName.fromString(qualifiedName)
        val targetPackage = targetFqName.parent()
        if (targetPackage.isRoot || qualifiedName == identity.name) return CompletionImportPlan.NoImport
        if (targetPackage == file.packageFqName) return CompletionImportPlan.NoImport
        if (file.isReachable(targetFqName, targetPackage)) return CompletionImportPlan.NoImport
        val path = ImportPath(targetPackage, false)
        return CompletionImportPlan.AddImport(
            CompletionImportTargetData(importPath = path, expressionText = identity.name),
        )
    }
}

/**
 * 目标是否已被现有的某条 import 覆盖。
 *
 * 走 PSI 的 import 项而不是逐行文本比较：仓颉里同一个目标有三种等价的导入写法，
 * 只比对整行文本必然漏判，而漏判的后果是**往已经能用的文件里重复写一行 import**：
 *
 * ```cangjie
 * import probe.dep             // 包级导入：包的声明直接可用
 * import probe.dep.target      // 声明级导入
 * import probe.dep.*           // 星号导入
 * ```
 *
 * 三种写法都已用 cjc 1.1.3 实测可编译并可直接引用 `target()`。
 * 因此判定要按**结构**而不是文本：包级与星号导入按包名比对，声明级按完整名比对。
 *
 * 别名不改可达性：`import a.b as c` 只是把短名换成 `c`，目标本身仍然可达，
 * 复查阶段不该因此再要求写一行 import。
 */
private fun CjFile.isReachable(targetFqName: FqName, targetPackage: FqName): Boolean =
    importDirectives.any { directive ->
        directive.importItems.any { item ->
            val imported = item.toImportPath() ?: return@any false
            when {
                // 星号导入按包比对：它引入的是包内全部声明。
                imported.isAllUnder -> imported.fqName == targetPackage
                // 包级导入（本次候选会写出的那种形式）与声明级导入都按完整名比对；
                // 二者在语法上同形，`import a.b` 既可指包 `a.b` 也可指 `a` 中的声明 `b`，
                // 无法从路径本身区分，按「至少覆盖其一」判定即可——多写一行 import 才是要避免的。
                else -> imported.fqName == targetFqName || imported.fqName == targetPackage
            }
        }
    }

/**
 * 把复查结论包装成导入解析结果，供插入阶段与 [ImportResolution] 一起消费。
 */
fun RecoveredTarget.asImportResolution(): ImportResolution = ImportResolution(importPlan)
