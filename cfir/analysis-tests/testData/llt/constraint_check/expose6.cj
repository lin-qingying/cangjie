// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


interface I0 {}

interface I1 <: I0 {}

interface I2 <: I0 {}

open class C1 <: I1 & I2 {
    func h(a: I1): Unit {}
}

class C2 <: C1 & I2 {
    func h(a: I2): Unit {}
}

func f<T>(a: T): Unit where T <: C1 & C2 {
    var lam1 = <!sema_ambiguous_use!>a.h<!>
    var lam2: (I0) -> Unit = <!sema_no_match_function_declaration_for_ref!>a<!>.h
}

main() {}
