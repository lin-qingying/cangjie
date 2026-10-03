// LANGUAGE_VERSION: 1.0.0
// RUN_PIPELINE_TILL: FRONTEND
// 证据：官方 cjc 1.0.5 实测（1.1.3 对同一批 kind 锚整条 prop 声明，见 `propertyInheritanceRules.cj`）：
//   `sema_property_have_same_declaration_in_inherit_mut` / `_immut` 与 `sema_property_must_implement_both`
//   在 1.0.5 只锚子（实现）prop 声明的首字符（1 个字符，声明首列的 `public`），1.1.3 起锚整条 prop 声明。
//   1.1.3 对本文件同样的输入实测 Range 均为整条 prop 声明，与本文件的门禁不符（该文件门禁到 1.0.0）。
// 因此这三条按官方 1.0.x 语义写首字符标记，并用 `LANGUAGE_VERSION` 门禁；不受版本影响的两条
// （`PROPERTY_OVERRIDE_IMPLEMENT_TYPE_DIFF` 两版都是首字符、`PROPERTY_MUST_HAVE_ACCESSORS` 两版都是整条声明）
// 留在 `propertyInheritanceRules.cj`。
// 官方实现（v1.0.0 tag，与 1.0.x 报告点一致）：
//   - src/Sema/InheritanceChecker/StructInheritanceChecker.cpp:1141 CheckPropertyInheritance
//     （`diag.Diagnose(*childProp, kind)` / `diag.Diagnose(child, kind)` 对非宏节点退化为 GetBegin 一个字符，
//      include/cangjie/Basic/DiagnosticEngine.h:843-857 的 `Diagnose(const AST::Node&, DiagKind)`）
//   - include/cangjie/Basic/DiagnosticSema.def:299-301
// 本文件里的标记若在 CFIR 侧红，是 CFIR 的锚点没有按语言版本门禁（实现缺口），不是用例写错。

// FILE: langVer100MutParentImmutChild.cj
// 形态：父类 `open mut prop`（get+set）被子类只读 `override prop`（get）覆写。
// kind：sema_property_have_same_declaration_in_inherit_mut "property '%s' should have 'mut' modifier"
// 锚点（1.0.5）：子 prop 声明首字符。
open class LangVer100MutParentBase {
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

class LangVer100MutParentSub <: LangVer100MutParentBase {
    <!PROPERTY_HAVE_SAME_DECLARATION_IN_INHERIT_MUT!>p<!>ublic override prop v: Int64 {
        get() {
            return 1
        }
    }
}

// FILE: langVer100ImmutParentMutChild.cj
// 形态：父类 `open prop`（只读）被子类 `override mut prop`（get+set）覆写。
// kind：sema_property_have_same_declaration_in_inherit_immut "property '%s' should be immutable"
// 锚点（1.0.5）：子 prop 声明首字符。
open class LangVer100ImmutParentBase {
    public open prop v: Int64 {
        get() {
            return 1
        }
    }
}

class LangVer100ImmutParentSub <: LangVer100ImmutParentBase {
    private var t: Int64 = 0

    <!PROPERTY_HAVE_SAME_DECLARATION_IN_INHERIT_IMMUT!>p<!>ublic override mut prop v: Int64 {
        get() {
            return t
        }
        set(v) {
            t = v
        }
    }
}

// FILE: langVer100InterfaceDefaultBothChildGetterOnly.cj
// 形态：接口给出带访问器的默认 `mut prop`（SetDefaultFunc 打 DEFAULT），实现类同名 prop 只实现 get。
// kind：sema_property_must_implement_both "property must implement both getter/setter of interface property '%s'"
// 锚点（1.0.5）：实现类 prop 声明首字符。
interface LangVer100I1 {
    mut prop v: Int64 {
        get() {
            return 1
        }
        set(v) {}
    }
}

class LangVer100C1 <: LangVer100I1 {
    <!PROPERTY_MUST_IMPLEMENT_BOTH!>p<!>ublic override mut prop v: Int64 {
        get() {
            return 1
        }
    }
}

// FILE: langVer100InterfaceNoAccessorChildMutOk.cj
// 形态：接口 prop 无访问器，实现类写 mut prop（get+set，自身合法），仅 mut 判定不一致。
// kind：sema_property_have_same_declaration_in_inherit_immut（接口继承路径，调用点 StructInheritanceChecker.cpp:814）
// 锚点（1.0.5）：实现类 prop 声明首字符。
interface LangVer100I2 {
    prop v: Int64
}

class LangVer100C2 <: LangVer100I2 {
    private var t: Int64 = 0

    <!PROPERTY_HAVE_SAME_DECLARATION_IN_INHERIT_IMMUT!>p<!>ublic override mut prop v: Int64 {
        get() {
            return t
        }
        set(v) {
            t = v
        }
    }
}