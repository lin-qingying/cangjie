// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


interface I {}

struct A <: I {}

func f(<!chir_dce_unused_variable!>x<!>: (Int64, I)): A {
    A()
}

main() {
    let x = (1, f)
    match (x) {
        case <!sema_unreachable_pattern!>_: (Int64, ((Int64, A)) -> I)<!> => 0
        case _ => 1
    }
}
