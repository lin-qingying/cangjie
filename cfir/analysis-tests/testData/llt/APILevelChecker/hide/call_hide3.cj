// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: hide.cj hide_use3.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt hide.cj -o hide.%middle
// EXEC: %compiler %cmp_opt %compile_lib_opt hide_use3.cj -o hide_use3.%middle
// EXEC: %compiler %cmp_opt hide_use3.%middle hide.%middle %n.cj -o %n.%suffix -Woff all

import ohos.dep.*
import ohos.dep

class D<T> <: A/*ok*/ where T <: A/*ok*/ {
    func xxx<R>(d: A/*ok*/): A/*ok*/ where R <: A/*ok*/ {
        var a: A/*ok*/ = A()/*ok*/
        B/*ok*/.AA
        B/*ok*/.BB(1)
        E.AA/*ok*/
        E.BB(1)/*ok*/
        goo(1)/*ok*/
        var b: D<A/*ok*/>
        var c: dep.A/*ok*/
        a is A/*ok*/
        a as A/*ok*/
        aaa/*ok*/ // hiden global var
        a
    }
}

main() {}
