// RUN_PIPELINE_TILL: FRONTEND
// 证据：官方 cjc 1.0.5（准）与 1.1.3（codegen 阶段本机可能崩溃，只取解析/语义诊断）两版实测，
// 静态库模式 `-o out.a --output-type=staticlib --diagnostic-format json`。
// 官方实现：
//   - src/Sema/InheritanceChecker/StructInheritanceChecker.cpp:1141 CheckPropertyInheritance
//     （类继承调用点 655；接口继承调用点 814）
//   - src/Sema/DeclAttributeChecker.cpp:332 CheckPropDeclAttributes
//   - src/Parse/ParseDecl.cpp:874 SetDefaultFunc（只给 INTERFACE_BODY 的 prop 打 DEFAULT 标记）
//   - 文案 include/cangjie/Basic/DiagnosticSema.def:298-301、DiagRefactor/DiagnosticSema.def:161-162
// 范围规则（cjc 1.0.5 / 1.1.3 实测）：`sema_property_have_same_declaration_in_inherit_mut` /
//   `_immut` 与 `sema_property_must_implement_both` 在 1.0.5 只锚声明首字符（1 个字符），在 1.1.3 锚整条 prop 声明；
//   `sema_property_override_implement_type_diff` 两版都锚声明首字符；`sema_property_must_have_accessors`
//   （refactor kind）两版都锚整条 prop 声明。本文件按默认语言版本 1.1.3 的范围写标记，1.0.0 的首字符形态见
//   `propertyInheritanceRulesLangVer100.cj`（`LANGUAGE_VERSION: 1.0.0`）。
// 同一批 `// FILE:` 段的顶层名必须互不相同：这些段属同一包，官方重名报 `sema_redefinition`，
//   CFIR 报 `CLASSIFIER_REDECLARATION` 并级联出 `AMBIGUOUS_USE` / `NOTHING_TO_OVERRIDE` 噪声。
//
// ── 1. override prop 类型与父类不同（类继承）
// 形态：open class 的 `open prop v: Int64`（只读合法）被子类 `override prop v: String` 覆写。
// kind：sema_property_override_implement_type_diff "The type of the override/implement property must be the same"
// 锚点：子 prop 声明首字符（`public` 的 `p`）。1.0.5 与 1.1.3 一致 Range (10,5)..(10,6)。
// 去掉父 prop 的 open 会先报 sema_cannot_override（kind: cannot override property 'v'，
// 锚子 prop 标识符，附注 "the parent property is not modified by 'open'"），故父 prop 必带 open。
// CFIR: CfirOverrideChecker.kt:350 取 propertyNameDiagnosticSource()（属性名 `v`），
//   CfirInheritanceDeepChecker.kt:350 取 nameSource ?: candidate.diagnosticSource → 锚点差异（缺口）。
// FILE: classOverrideTypeDiff.cj
open class OverrideTypeDiffBase {
    public open prop v: Int64 {
        get() {
            return 1
        }
    }
}

class OverrideTypeDiffSub <: OverrideTypeDiffBase {
    <!PROPERTY_OVERRIDE_IMPLEMENT_TYPE_DIFF!>p<!>ublic override prop v: String {
        get() {
            return "x"
        }
    }
}

// FILE: classOverrideTypeDiffOk.cj
// 合法对照：子类 prop 与父类 prop 类型一致，零诊断（1.0.5/1.1.3 各 0 条）。
open class OverrideTypeDiffOkBase {
    public open prop v: Int64 {
        get() {
            return 1
        }
    }
}

class OverrideTypeDiffOkSub <: OverrideTypeDiffOkBase {
    public override prop v: Int64 {
        get() {
            return 2
        }
    }
}

// FILE: classMutParentImmutChild.cj
// 形态：父类 `open mut prop`（get+set）被子类只读 `override prop`（get）覆写。
// kind：sema_property_have_same_declaration_in_inherit_mut "property '%s' should have 'mut' modifier"
// 锚点：子 prop 声明。1.0.5 单字符（声明首列 `public`），1.1.3 整条 prop 声明。
// isVar 不一致时官方走 if 分支，不再检查 must_implement_both，故本形态只有这一条。
// CFIR: CfirGeneralSemanticsChecker.kt:1138 → PROPERTY_HAVE_SAME_DECLARATION_IN_INHERIT_MUT。
open class MutParentImmutChildBase {
    private var s: Int64 = 0

    public open mut prop v: Int64 {
        get() {
            return s
        }
        set(v) {
            s = v
        }
    }
}

class MutParentImmutChildSub <: MutParentImmutChildBase {
    <!PROPERTY_HAVE_SAME_DECLARATION_IN_INHERIT_MUT!>public override prop v: Int64 {
        get() {
            return 1
        }
    }<!>
}

// FILE: classImmutParentMutChild.cj
// 形态：父类 `open prop`（只读）被子类 `override mut prop`（get+set）覆写。
// kind：sema_property_have_same_declaration_in_inherit_immut "property '%s' should be immutable"
// 锚点：子 prop 声明。1.0.5 单字符（声明首列 `public`），1.1.3 整条 prop 声明。
// CFIR: CfirGeneralSemanticsChecker.kt:1147 → PROPERTY_HAVE_SAME_DECLARATION_IN_INHERIT_IMMUT。
open class ImmutParentMutChildBase {
    public open prop v: Int64 {
        get() {
            return 1
        }
    }
}

class ImmutParentMutChildSub <: ImmutParentMutChildBase {
    private var t: Int64 = 0

    <!PROPERTY_HAVE_SAME_DECLARATION_IN_INHERIT_IMMUT!>public override mut prop v: Int64 {
        get() {
            return t
        }
        set(v) {
            t = v
        }
    }<!>
}

// FILE: classMutOverrideOk.cj
// 合法对照：父 mut、子 mut 且都 get+set，零诊断（1.0.5/1.1.3 各 0 条）。
open class MutOverrideOkBase {
    private var s: Int64 = 0

    public open mut prop v: Int64 {
        get() {
            return s
        }
        set(v) {
            s = v
        }
    }
}

class MutOverrideOkSub <: MutOverrideOkBase {
    private var t: Int64 = 0

    public override mut prop v: Int64 {
        get() {
            return t
        }
        set(v) {
            t = v
        }
    }
}

// FILE: classImmutOverrideOk.cj
// 合法对照：父 immut、子 immut 且都只读，零诊断（1.0.5/1.1.3 各 0 条）。
// 说明：非 mut 的 prop 写 setter 属 sema_immutable_property_with_setter（已由 propertyAccessorRules.cj 覆盖），
// 因此"只读子类 prop 覆写 mut 父 prop"（少 setter、mut 判定又不一致）在官方先落 _MUT，单独成段。
open class ImmutOverrideOkBase {
    private var s: Int64 = 0

    public open prop v: Int64 {
        get() {
            return s
        }
    }
}

class ImmutOverrideOkSub <: ImmutOverrideOkBase {
    public override prop v: Int64 {
        get() {
            return 1
        }
    }
}

// FILE: classParentMutGetterOnly.cj
// 形态：父类 `open mut prop` 只写 get（自身就违反 CheckPropDeclAttributes），
// 子类 `override mut prop` 写全 get+set；mut 判定一致，故官方只报父 prop 缺 setter。
// kind：sema_property_must_have_accessors "property must have accessors"（DiagnoseRefactor 锚整条 prop 声明）
// 锚点：父 prop 声明。1.0.5 / 1.1.3 一致（整条 prop 声明，含 `get()` 体，不含子类成员）。
// CFIR: CfirGeneralSemanticsChecker.kt:1049 → PROPERTY_MUST_HAVE_ACCESSORS。
open class ParentMutGetterOnlyBase {
    private var s: Int64 = 0

    <!PROPERTY_MUST_HAVE_ACCESSORS!>public open mut prop v: Int64 {
        get() {
            return s
        }
    }<!>
}

class ParentMutGetterOnlySub <: ParentMutGetterOnlyBase {
    private var t: Int64 = 0

    public override mut prop v: Int64 {
        get() {
            return t
        }
        set(v) {
            t = v
        }
    }
}

// FILE: interfaceDefaultBothChildGetterOnly.cj
// 形态：接口给出带访问器的默认 `mut prop`（SetDefaultFunc 打 DEFAULT），实现类同名 prop 只实现 get。
// kind：sema_property_must_implement_both "property must implement both getter/setter of interface property '%s'"
// 锚点：实现类 prop 声明。1.0.5 单字符（声明首列 `public`），1.1.3 整条 prop 声明。
// CFIR: CfirGeneralSemanticsChecker.kt:1099 → PROPERTY_MUST_IMPLEMENT_BOTH。
interface I1 {
    mut prop v: Int64 {
        get() {
            return 1
        }
        set(v) {}
    }
}

class C1 <: I1 {
    <!PROPERTY_MUST_IMPLEMENT_BOTH!>public override mut prop v: Int64 {
        get() {
            return 1
        }
    }<!>
}

// FILE: interfaceNoAccessorChildGetterOnlyOk.cj
// 合法对照：接口 prop 无访问器（SetDefaultFunc 仍打 DEFAULT，但官方实现类只写 get 时不报），
// 零诊断（1.0.5/1.1.3 各 0 条）。
interface I3 {
    prop v: Int64
}

class C3 <: I3 {
    public override prop v: Int64 {
        get() {
            return 1
        }
    }
}

// FILE: interfaceNoAccessorChildMutOk.cj
// 形态：接口 prop 无访问器，实现类写 mut prop（get+set，访问器齐备，自身合法），仅 mut 判定不一致。
// kind：sema_property_have_same_declaration_in_inherit_immut。
// 锚点：实现类 prop 声明。1.0.5 单字符（声明首列 `public`），1.1.3 整条 prop 声明。
interface I4 {
    prop v: Int64
}

class C4 <: I4 {
    private var t: Int64 = 0

    <!PROPERTY_HAVE_SAME_DECLARATION_IN_INHERIT_IMMUT!>public override mut prop v: Int64 {
        get() {
            return t
        }
        set(v) {
            t = v
        }
    }<!>
}

// FILE: interfaceDefaultBothChildBothOk.cj
// 合法对照：接口默认 `mut prop` 带 get+set，实现类同名 prop 也 get+set，零诊断
// （1.1.3 零诊断；1.0.5 仅 1 条 codegen 阶段 chir_dce_unused_variable warning，不属语义诊断）。
interface I5 {
    mut prop v: Int64 {
        get() {
            return 1
        }
        set(v) {}
    }
}

class C5 <: I5 {
    private var t: Int64 = 0

    public override mut prop v: Int64 {
        get() {
            return t
        }
        set(v) {
            t = v
        }
    }
}

// FILE: interfaceNoAccessorChildMissing.cj
// 形态：接口 `mut prop` 无访问器（SetDefaultFunc 仍打 DEFAULT），实现类同名 prop 只写 set（缺 getter）。
// kind：sema_property_must_implement_both（子 prop 是 mut 且 getters 为空，父 prop 带 DEFAULT）。
// 锚点：实现类 prop 声明。1.0.5 单字符（声明首列 `public`），1.1.3 整条 prop 声明。
// 同段的 sema_immutable_property_with_setter 属 CheckPropDeclAttributes，已由 propertyAccessorRules.cj 覆盖，此处不重复标记。
interface I6 {
    mut prop v: Int64
}

class C6 <: I6 {
    private var t: Int64 = 0

    <!PROPERTY_MUST_IMPLEMENT_BOTH!>public override mut prop v: Int64 {
        set(v) {
            t = v
        }
    }<!>
}

// FILE: staticPropGetterOnlyOk.cj
// 合法对照：static prop 只写 get 合法，零诊断（1.0.5/1.1.3 各 0 条）。
// 官方条件里 static 只在 redef 时才豁免访问器要求；`public static mut prop` 只写 get 仍因
// setters.empty() 命中第一个分支，但 static+非 redef 使整个 && 的前半段为假，故不报。
class D2 {
    public static prop v: Int64 {
        get() {
            return 1
        }
    }
}

// FILE: staticMutPropBothOk.cj
// 合法对照：static mut prop 写全 get+set，零诊断（1.0.5/1.1.3 各 0 条）。
class D3 {
    private static var t: Int64 = 0

    public static mut prop v: Int64 {
        get() {
            return t
        }
        set(v) {
            t = v
        }
    }
}

// FILE: overridePropGetterOnlyInOpenClass.cj
// 形态：open class 的 `open mut prop`（get+set）被子类只读 `override prop`（get）覆写
// —— 类继承的少访问器形态，官方只报 must_have_accessors：父 prop 无 DEFAULT/ABSTRACT 标记，
// 所以 must_implement_both 不触发；且 isVar 不一致走 _MUT 分支。
// kind：sema_property_have_same_declaration_in_inherit_mut。锚点：子 prop 声明。
// 1.0.5 单字符（声明首列 `public`），1.1.3 整条 prop 声明。
open class D4 {
    private var t: Int64 = 0

    public open mut prop v: Int64 {
        get() {
            return t
        }
        set(v) {
            t = v
        }
    }
}

class E4 <: D4 {
    <!PROPERTY_HAVE_SAME_DECLARATION_IN_INHERIT_MUT!>public override prop v: Int64 {
        get() {
            return 1
        }
    }<!>
}

// FILE: overrideMutPropBothOk.cj
// 合法对照：open class 的 `open mut prop` 被子类 `override mut prop`（get+set）完整覆写，零诊断
// （1.0.5/1.1.3 各 0 条）。
open class D5 {
    private var t: Int64 = 0

    public open mut prop v: Int64 {
        get() {
            return t
        }
        set(v) {
            t = v
        }
    }
}

class E5 <: D5 {
    private var s: Int64 = 0

    public override mut prop v: Int64 {
        get() {
            return s
        }
        set(v) {
            s = v
        }
    }
}

// FILE: openClassMutPropGetterOnly.cj
// 形态：普通（非 open）类里 mut prop 只写 get，缺 setter。
// kind：sema_property_must_have_accessors。锚点：整条 prop 声明。
// 1.0.5/1.1.3 一致 Range (4,5)..(8,6)。
class D8 {
    private var t: Int64 = 0

    <!PROPERTY_MUST_HAVE_ACCESSORS!>public mut prop v: Int64 {
        get() {
            return t
        }
    }<!>
}

// FILE: interfaceMutPropNoAccessorOk.cj
// 合法对照：接口里 `mut prop` 不写任何访问器合法，零诊断（1.0.5/1.1.3 各 0 条）。
interface I9 {
    mut prop v: Int64
}

// 已删除的形态（前置错误 / 分支不可达，取证 kind 与 Range 见下；除注明外 1.0.5 与 1.1.3 两版一致）：
//   1. 实现类 prop 完全不写访问器（带 override）
//      → 先报 sema_missing_func_body "property 'v' can not be abstract"（锚 prop 首列，Range (n,5)..(n,6)），
//        1.1.3 在类继承形态另报 sema_property_have_same_declaration_in_inherit_mut（锚 (n,5)..(n,34)），
//        1.0.5 只报 missing_func_body；must_implement_both 与 must_have_accessors 都被屏蔽。
//        受影响形态：接口默认 prop(get+set) + 实现类无访问器 prop（目标 must_implement_both）、
//        同一形态再叠加类型不一致（目标 type_diff）、open 类 prop 被无访问器 prop 覆写（目标 must_have_accessors）。
//   2. static prop 不写任何访问器
//      → sema_missing_func_body（锚 (2,5)..(2,6)）+ sema_class_need_abstract_modifier_or_func_need_impl
//        （锚类名 (1,1)..(1,2)），must_have_accessors 被 CheckPropDeclAttributes 开头的 ABSTRACT 早退屏蔽。
//        "static prop 缺访问器"这一形态在官方不可达，只能用非 static 类形态（classParentMutGetterOnly /
//        openClassMutPropGetterOnly）覆盖 PROPERTY_MUST_HAVE_ACCESSORS。
//   3. 显式 `abstract prop`（open class 或 abstract class）
//      → parse_illegal_modifier_in_scope "unexpected modifier 'abstract' on property declaration in class"
//        （1.1.3 另加 parse_explicitly_abstract_only_for_cjmp_abstract_class）。显式 abstract prop 不是合法语法。
//   4. 接口默认 prop 只写 get，实现类同名 prop 写 get+set（目标 must_implement_both）
//      → 不可达：非 mut 的子 prop 带 setter 先命中 sema_immutable_property_with_setter（锚整条 prop 声明），
//        而 must_implement_both 的 isVar 分支要求父 mut 子 mut（官方只对 mut 子 prop 报该诊断）——
//        把子 prop 改成 mut 又落到 _IMMUT 分支。该形态的 setter 侧已由 propertyAccessorRules.cj 覆盖。
//   5. 非 mut 的 prop 覆写 mut 父 prop 并写 get+set（目标 _IMMUT）
//      → 同时命中 sema_immutable_property_with_setter（1.0.5 还多报 _MUT），非 mut/immut 判定目标不干净；
//        官方只有 1.1.3 报 _IMMUT 这一条，_IMMUT 形态已改用 interfaceNoAccessorChildMutOk.cj 覆盖。
//   6. mut 子 prop 相对 mut 父 prop 少 setter（父 get+set、子只有 get）
//      → 官方合法（1.0.5/1.1.3 零诊断）：isVar 一致，且父 prop 是类成员、无 DEFAULT/ABSTRACT 标记，
//        must_implement_both 不触发；CheckPropDeclAttributes 的 override 豁免让 must_have_accessors 也不触发。
//        因此"子类只读 prop 覆盖 mut 父 prop"（少 getter 且 isVar 不一致）是唯一可达的类继承形态（见 classMutParentImmutChild）。
