/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.LanguageFeature
import org.cangnova.cangjie.annotations.CangjiePlatformAnnotationKind
import org.cangnova.cangjie.cfir.analysis.checkers.CjmpGate
import org.cangnova.cangjie.cfir.analysis.checkers.isConstructorSource
import org.cangnova.cangjie.cfir.analysis.checkers.realSourceModifiers
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationStatus
import org.cangnova.cangjie.cfir.declarations.CfirMemberDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.hasImplicitOrInferredReturnType
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.patterns.CfirEnumPattern
import org.cangnova.cangjie.cfir.patterns.CfirTuplePattern
import org.cangnova.cangjie.cfir.patterns.CfirWildcardPattern
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.name.Name

/**
 * CJMP 解析族规则检查器（CommonSpecific 分组的 `parse_*` 命名空间）。
 *
 * 对齐官方：
 * - `src/Parse/ParseCJMPDecl.cpp`（CheckCJMPModifiers / CheckCJMPModifiersOf /
 *   CheckCJMPModifiersBetween / CheckSpecificInterface / CheckCJMPCtorPresence）；
 * - `src/Parse/ParseDecl.cpp:1974-1980`（函数缺返回类型）、`:2085-2096` 与
 *   `src/Parse/Parser.cpp:455-470`（explicitly abstract）。
 *
 * 架构（D13）：parse 族诊断不从 parser 报告——parser 配置无关（D12），且 raw/light-tree
 * 丢弃语法错误节点、无 CfirErrors 通道；统一落为 CFIR 诊断，由本检查器在声明形状上判定。
 * 版本门经 [CjmpGate]：1.0.x 下只报 `UNSUPPORTED_FEATURE` 并整族短路（§8.1 判定序 ①）。
 *
 * cjc 1.1.3 探针实测（`--experimental` + `--common-part-cjo` 全流程）：
 * - `common func NoRet()` → `'common' function return type must be specified`（锚函数名）；
 * - `common class NoCtor {}` / `common struct NoCtorS {}` →
 *   `at least one constructor is required in common class/struct '...'`（锚声明名）；
 * - `common let (a, b) = (1, 2)` → `tuple pattern can not be 'common'`（锚模式）；
 * - `common static init() {}` → `static init can not be 'common'`（锚整条声明，单条）；
 * - `class Plain2 { common func f(): Unit {} }` →
 *   `function f is common, but class is not common`（锚成员声明）；
 * - `specific interface I2 { func m(): Unit }` →
 *   `the member m must have body in 'specific' I2`（锚成员声明）；
 * - `abstract func` 在非 CJMP 抽象类 → `only common/specific or Native FFI mirror abstract
 *   classes can have explicitly abstract function`（锚成员声明）。
 *
 * 无触发点 3 条（官方 DiagnosticParser.def 声明但 1.1.3 实测不可达，登记保留、不激活）：
 * 参数默认值（specific 单侧默认值合法）、`expected_type_with_cjmp_var`（隐式类型合法）、
 * `parse_cjmp_generic_decl`（泛型限制走 COMMON_GENERIC_FROZEN_NOT_SUPPORTED）。
 *
 * 与官方的登记差异（本仓库暂不覆盖，Phase 5 前不改）：
 * - 锚点：官方 1.1.3 把"函数缺返回类型"锚在 `func` 关键字（探针 4:15/5:12），本仓库
 *   定位策略表无对位策略，暂用声明名（ACTUAL_DECLARATION_NAME）；
 * - 官方对无体顶层函数另报 `body of function '...' is missing`（探针 specific 侧实测），
 *   本仓库无该诊断（parse 错误在 raw/light-tree 丢弃），fixture 不期望。
 *
 * 遗留交接（Phase 3）：`CfirCommonSpecificChecker.checkExplicitlyAbstractUsage` 现以 sema
 * 诊断名报告同一形态；官方 sema 条目无触发点（origin/main 全文无引用），Phase 3 收敛时
 * 需与本节 parse 变体合并去重。
 */
object CfirCjmpParseRulesChecker : CfirBasicDeclarationChecker() {
    /**
     * 分发 CJMP 解析族规则。
     *
     * 携带 common/specific 修饰符的声明先过版本门（不过则只报版本门诊断并整族短路）；
     * 不携带修饰符但形态相关的规则（explicitly abstract）仅在版本门开启时激活，
     * 不报版本诊断——1.0.x 下该形态由通用修饰符检查覆盖（实测 `unexpected modifier`）。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirDeclaration) {
        val cjmpModifiers = declaration.source?.realSourceModifiers().orEmpty()
            .filter { it.token == CjTokens.COMMON_KEYWORD || it.token == CjTokens.SPECIFIC_KEYWORD }

        if (cjmpModifiers.isEmpty()) {
            if (!context.languageVersionSettings.supportsFeature(LanguageFeature.CommonSpecificDeclarations)) return
            checkExplicitlyAbstractUsage(declaration)
            return
        }

        // ① 版本门：1.0.x 只报版本门诊断并整族短路
        if (!CjmpGate.requireVersionSupport(declaration.source)) return
        // ② 模式门（cjc 1.1.3 `MPParserImpl::CheckCJMPDecl`/`CheckCJMPModifiers`）：
        // - 既非 common 也非 specific 编译时整族规则不执行；
        // - 修饰符与编译模式错位（common 声明不在 common 编译、specific 声明不在 specific 编译）时
        //   由 CfirCjmpFilePartChecker 报文件级诊断，本声明的其余规则不执行。
        // （1.1.3 无 parse_unexpected_cjmp_decl 触发点，该条目登记保留。）
        if (!CjmpGate.modeAdmits(context, declaration)) return

        checkFunctionReturnType(declaration)
        checkSpecificMemberImplementation(declaration)
        checkOuterDeclarationMissMatch(declaration)
        checkStaticInitializer(declaration)
        checkPatternDeclaration(declaration)
        checkCommonConstructorPresence(declaration)
        checkExplicitlyAbstractUsage(declaration)
    }

    /**
     * common/specific 函数缺返回类型（官方 ParseDecl.cpp:1974-1980：无函数体且无返回类型）。
     *
     * 构造器不参与：其"返回类型"无源码语法（隐式 Unit 由基建合成），官方 GetDiagKind 虽
     * 把 CONSTRUCTOR 归为 constructor 文案，但实测触发面为普通函数。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkFunctionReturnType(declaration: CfirDeclaration) {
        if (declaration !is CfirFunction || declaration is CfirConstructor) return
        val isCommon = declaration.status.isCommon
        val isSpecific = declaration.status.isSpecific
        if (!isCommon && !isSpecific) return
        // 注意：BODY_RESOLVE 会把隐式返回类型替换为推断结果（CfirDeclarationsResolveTransformer
        // 的 transformFunctionWithGivenSignature 尾部 eager 推断），无体函数同样被推断为 Unit；
        // 因此"源码未写返回类型"必须用仓库既有 helper hasImplicitOrInferredReturnType() 判定。
        if (!declaration.hasImplicitOrInferredReturnType()) return
        if (declaration.body != null) return
        reporter.reportOn(
            source = declaration.source,
            factory = if (isCommon) {
                CfirErrors.PARSE_COMMON_FUNCTION_MUST_HAVE_RETURN_TYPE
            } else {
                CfirErrors.PARSE_SPECIFIC_FUNCTION_MUST_HAVE_RETURN_TYPE
            },
        )
    }

    /**
     * specific 接口成员必须有实现（官方 ParseCJMPDecl.cpp:278-290 CheckSpecificInterface）。
     *
     * 官方 HasDefault 判据：函数有体 / property 有访问器 / 变量有初始值；enum 构造器等
     * 无体语法形态豁免。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkSpecificMemberImplementation(declaration: CfirDeclaration) {
        if (declaration !is CfirInterface || !declaration.status.isSpecific) return
        for (member in declaration.declarations) {
            if (hasDefaultImplementation(member)) continue
            val memberName = memberNameOf(member) ?: continue
            reporter.reportOn(
                source = member.source,
                factory = CfirErrors.PARSE_SPECIFIC_MEMBER_MUST_HAVE_IMPLEMENTATION,
                a = memberName,
                b = declaration.name,
            )
        }
    }

    /**
     * 官方 HasDefault 对位：成员是否自带默认实现或初始化。
     */
    private fun hasDefaultImplementation(member: CfirDeclaration): Boolean = when (member) {
        is CfirFunction -> member.body != null
        is CfirProperty -> member.getter != null || member.setter != null
        is CfirVariable -> member.initializer != null
        else -> true
    }

    /**
     * 成员的 common/specific 与外层容器不一致（官方 ParseCJMPDecl.cpp:264-276
     * CheckCJMPModifiersBetween）。
     *
     * 判据（官方逐字对位）：
     * - 成员 common 且外层既非 common 也非 specific → 报；
     * - 成员 specific 且外层非 specific → 报。
     *
     * 例外：static init 的 cjmp 使用由 [checkStaticInitializer] 单独报告（1.1.3 实测单条）；
     * enum 构造器不独立携带 cjmp（C18：enum 构造器随外层豁免）。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkOuterDeclarationMissMatch(declaration: CfirDeclaration) {
        val memberStatus = declaration.modifierStatus ?: return
        val memberIsCommon = memberStatus.isCommon
        val memberIsSpecific = memberStatus.isSpecific
        if (!memberIsCommon && !memberIsSpecific) return
        if (declaration is CfirConstructor && memberStatus.isStatic) return

        val container = nearestTypeContainer(declaration) ?: return
        val containerStatus = container.modifierStatus ?: return
        val containerIsCommon = containerStatus.isCommon
        val containerIsSpecific = containerStatus.isSpecific
        val mismatch = if (memberIsCommon) {
            !containerIsCommon && !containerIsSpecific
        } else {
            !containerIsSpecific
        }
        if (!mismatch) return

        val memberKind = memberKindText(declaration) ?: return
        val containerKind = containerKindText(container) ?: return
        val kindText = if (memberIsCommon) "common" else "specific"
        val memberName = memberNameOf(declaration)
        val memberDescription = if (memberName != null) "$memberKind ${memberName.asString()}" else memberKind
        reporter.reportOn(
            source = declaration.source,
            factory = CfirErrors.PARSE_CJMP_OUTDECL_MISS_MATCH,
            a = memberDescription,
            b = kindText,
            c = containerKind,
            d = kindText,
        )
    }

    /**
     * static init 不能带 common/specific（官方 ParseCJMPDecl.cpp:242-246；静态初始化器
     * 载体 = CfirConstructor + status.isStatic，见 C31）。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkStaticInitializer(declaration: CfirDeclaration) {
        if (declaration !is CfirConstructor || !declaration.status.isStatic) return
        val kind = when {
            declaration.status.isCommon -> "common"
            declaration.status.isSpecific -> "specific"
            else -> return
        }
        reporter.reportOn(
            source = declaration.source,
            factory = CfirErrors.PARSE_CJMP_STATIC_INIT,
            a = kind,
        )
    }

    /**
     * 模式声明不能是 common（官方 ParseCJMPDecl.cpp:235-241）。
     *
     * 官方仅对 tuple / enum / _（wildcard）三种模式报告（KIND_TO_STR 文案面）；
     * 普通 `let x = 1` 是绑定模式（CfirBindingPattern），不在此列。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkPatternDeclaration(declaration: CfirDeclaration) {
        if (declaration !is CfirPatternVariable || !declaration.status.isCommon) return
        val patternKind = when (declaration.pattern) {
            is CfirTuplePattern -> "tuple"
            is CfirEnumPattern -> "enum"
            is CfirWildcardPattern -> "wildcard"
            else -> return
        }
        reporter.reportOn(
            source = declaration.pattern.source,
            factory = CfirErrors.PARSE_CJMP_PATTERN_DECL,
            a = patternKind,
            b = "common",
        )
    }

    /**
     * common 类/结构体至少需要一个显式构造器（官方 ParseCJMPDecl.cpp:201-223
     * CheckCJMPCtorPresence）。
     *
     * 隐式主构造（source 非构造器源码）不算：`common class NoCtor {}` 实测报错，
     * 且抽象类同样要求（`common abstract class CA { abstract func f() }` 实测报错）。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkCommonConstructorPresence(declaration: CfirDeclaration) {
        if (declaration.modifierStatus?.isCommon != true) return
        val declType = when (declaration) {
            is CfirClass -> "class"
            is CfirStruct -> "struct"
            else -> return
        }
        val hasExplicitCtor = declaration.declarations.any { member ->
            member is CfirConstructor && (!member.isPrimary || member.source?.isConstructorSource() == true)
        }
        if (hasExplicitCtor) return
        reporter.reportOn(
            source = declaration.source,
            factory = CfirErrors.PARSE_CJMP_IN_COMMON_CTOR_REQUIRED,
            a = declType,
            b = declaration.name,
        )
    }

    /**
     * explicitly abstract 只能用于 common/specific 抽象类
     * （官方 ParseDecl.cpp:2083-2096 函数、Parser.cpp:453-470 property）。
     *
     * 官方判据：显式 abstract 修饰符 && 成员自身非 common && 外层非「abstract 且 cjmp」
     * && 非 Java/ObjC mirror（Native FFI）。1.0.x 下同形态由通用修饰符检查报
     * `unexpected modifier 'abstract'`（实测），本规则仅在版本门开启时激活。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkExplicitlyAbstractUsage(declaration: CfirDeclaration) {
        val memberStatus = declaration.modifierStatus ?: return
        if (!memberStatus.isAbstractExplicit) return
        val declKind = when (declaration) {
            is CfirNamedFunction -> "function"
            is CfirProperty -> "property"
            else -> return
        }
        if (memberStatus.isCommon) return
        val container = nearestTypeContainer(declaration)
        val containerStatus = container?.modifierStatus
        val inAbstractCjmp = containerStatus != null &&
                containerStatus.isAbstract &&
                (containerStatus.isCommon || containerStatus.isSpecific)
        val javaMirror = container != null &&
                container.hasInteropAnnotation(CangjiePlatformAnnotationKind.JAVA_MIRROR)
        if (inAbstractCjmp || javaMirror) return
        reporter.reportOn(
            source = declaration.source,
            factory = CfirErrors.PARSE_EXPLICITLY_ABSTRACT_ONLY_FOR_CJMP_ABSTRACT_CLASS,
            a = declKind,
        )
    }

    /**
     * 修饰符状态载体：树模型中 `status` 声明于 [CfirMemberDeclaration]（所有可修饰声明）。
     */
    private val CfirDeclaration.modifierStatus: CfirDeclarationStatus?
        get() = (this as? CfirMemberDeclaration)?.status

    /**
     * 最近的外层类型容器（class-like 或 extend），对应官方
     * CheckCJMPModifiersOf 的 GetMemberDeclPtrs 外层。
     */
    context(context: CheckerContext)
    private fun nearestTypeContainer(declaration: CfirDeclaration): CfirDeclaration? =
        context.containingDeclarations
            .asSequence()
            .mapNotNull { it.cfir as? CfirDeclaration }
            .lastOrNull { it !== declaration && (it is CfirClassLikeDeclaration || it is CfirExtend) }

    /**
     * 成员在诊断文案中的种类（官方 GetDiagKind 对位，裁剪到成员面）。
     */
    private fun memberKindText(declaration: CfirDeclaration): String? = when (declaration) {
        is CfirClassLikeDeclaration -> containerKindText(declaration)
        is CfirConstructor -> "constructor"
        is CfirNamedFunction -> "function"
        is CfirProperty -> "property"
        is CfirFieldVariable -> "variable"
        else -> null
    }

    /**
     * 容器在诊断文案中的种类。
     */
    private fun containerKindText(container: CfirDeclaration): String? = when (container) {
        is CfirClass -> "class"
        is CfirStruct -> "struct"
        is CfirInterface -> "interface"
        is CfirEnum -> "enum"
        is CfirExtend -> "extend"
        else -> null
    }

    /**
     * 成员名（官方 CheckCJMPModifiersBetween 的 inner.identifier.Val()）。
     */
    private fun memberNameOf(declaration: CfirDeclaration): Name? = when (declaration) {
        is CfirClassLikeDeclaration -> declaration.name
        is CfirNamedFunction -> declaration.name
        is CfirProperty -> declaration.name
        is CfirFieldVariable -> declaration.name
        else -> null
    }
}
