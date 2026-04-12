// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: hide.cj ../level/apilevel.cj mix_macro_dep.cj mix_macro_dep1.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt hide.cj apilevel.cj -o hide.%middle
// EXEC: %compiler %cmp_opt %cmp_macro mix_macro_dep.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt mix_macro_dep1.cj -o mix_macro_dep1.%middle
// EXEC: %compiler %cmp_opt %compile_lib_opt hide.%middle %n.cj -o %n.%suffix -Woff all

package ohos.dep

import ohos.labels.*
import m.*
import anno.*

@M
@!Hide[isChecked: true]
public open class A {
}

@CustomAnno
@!Hide[isChecked: true]
public open class B {
}

@!APILevel[since: "21"]
@!Hide[isChecked: true]
public open class C {
}

@!Hide[isChecked: true]
@!APILevel[since: "21"]
public open class D {
}

@!Hide[isChecked: true]
@C
public struct E {
}
