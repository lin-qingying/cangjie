// LANGUAGE_VERSION: 1.0.0
// RUN_PIPELINE_TILL: FRONTEND
// 官方 cjc 1.0.5 的 MainHint 对继承诊断只返回一个字符；这是官方 CLI 范围，不是 CFIR inline 范围。
// 本文件按 CFIR 诊断源规则标记完整的属性声明单元。官方 1.0.0 源码仍用于确认规则本身，
// 不能据其 CLI 的单字符范围裁剪 CFIR 标记。
// 官方实现（v1.0.0 tag，与 1.0.x 报告点一致）：
//   - src/Sema/InheritanceChecker/StructInheritanceChecker.cpp:1141 CheckPropertyInheritance
//     （`diag.Diagnose(*childProp, kind)` / `diag.Diagnose(child, kind)` 对非宏节点退化为 GetBegin 一个字符，
//      include/cangjie/Basic/DiagnosticEngine.h:843-857 的 `Diagnose(const AST::Node&, DiagKind)`）
//   - include/cangjie/Basic/DiagnosticSema.def:299-301
// 所有 inline 标记均覆盖完整属性声明，不对 `public` 等单个 token 做裁剪。

// FILE: langVer100MutParentImmutChild.cj
// 形态：父类 `open mut prop`（get+set）被子类只读 `override prop`（get）覆写。
// kind：sema_property_have_same_declaration_in_inherit_mut "property '%s' should have 'mut' modifier"
// CFIR 锚点：整个子属性声明。
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
    <!PROPERTY_HAVE_SAME_DECLARATION_IN_INHERIT_MUT!>public override prop v: Int64 {
        get() {
            return 1
        }
    }<!>
}

// FILE: langVer100ImmutParentMutChild.cj
// 形态：父类 `open prop`（只读）被子类 `override mut prop`（get+set）覆写。
// kind：sema_property_have_same_declaration_in_inherit_immut "property '%s' should be immutable"
// CFIR 锚点：整个子属性声明。
open class LangVer100ImmutParentBase {
    public open prop v: Int64 {
        get() {
            return 1
        }
    }
}

class LangVer100ImmutParentSub <: LangVer100ImmutParentBase {
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

// FILE: langVer100InterfaceDefaultBothChildGetterOnly.cj
// 形态：接口给出带访问器的默认 `mut prop`（SetDefaultFunc 打 DEFAULT），实现类同名 prop 只实现 get。
// kind：sema_property_must_implement_both "property must implement both getter/setter of interface property '%s'"
// CFIR 锚点：整个实现类属性声明。
interface LangVer100I1 {
    mut prop v: Int64 {
        get() {
            return 1
        }
        set(v) {}
    }
}

class LangVer100C1 <: LangVer100I1 {
    <!PROPERTY_MUST_IMPLEMENT_BOTH!>public override mut prop v: Int64 {
        get() {
            return 1
        }
    }<!>
}

// FILE: langVer100InterfaceNoAccessorChildMutOk.cj
// 形态：接口 prop 无访问器，实现类写 mut prop（get+set，自身合法），仅 mut 判定不一致。
// kind：sema_property_have_same_declaration_in_inherit_immut（接口继承路径，调用点 StructInheritanceChecker.cpp:814）
// CFIR 锚点：整个实现类属性声明。
interface LangVer100I2 {
    prop v: Int64
}

class LangVer100C2 <: LangVer100I2 {
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
