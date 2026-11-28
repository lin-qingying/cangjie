// RUN_PIPELINE_TILL: FRONTEND
// 证据：官方 cjc 1.0.5 实测（两段编译：pa 先产出 .cjo，pb 用 --import-path 引用）：
//   `abstract sealed class Sealed` + 另一包 `class Sub <: Sealed`
//   → error: cannot inherit imported 'sealed' class 'Sealed'（锚子类名 `Sub`，附注 sealed 声明所在包）
//   同包内跨文件继承零诊断。
//   `public abstract sealed class Sealed` 另给 parse_redundant_modifier warning（'sealed' implies 'public'，锚 `public`）。
// 官方实现 TypeChecker::CheckSealedInheritance：父声明带 sealed 且是 imported、子声明非 imported 才报错。
// 锚点：主锚是子类名 `Sub`（整段），OtherHint 指向父类型引用 `Sealed`。
// 本项目：CFIR CANNOT_INHERIT_SEALED 按 CjTypeReference 定位到父类型引用，与官方主锚不同（实现缺口）。
// FILE: sealed.cj
package pa

<!REDUNDANT_MODIFIER!>public<!> abstract sealed class Sealed {
    public init() {}
}

// FILE: derived.cj
package pb

import pa.Sealed

public class Sub <: <!CANNOT_INHERIT_SEALED!>Sealed<!> {
    public init() {
        super()
    }
}
