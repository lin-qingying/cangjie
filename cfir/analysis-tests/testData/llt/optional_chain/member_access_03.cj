// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

struct A {
    let b: B = B()
}

struct B {
    let c: Option<C> = C()
}

struct C {
    let d: Int64 = 0
}

main() {
    let a = Some(A())
    let x = a?.b.c?.d
    match (x) {
        case Some(t) => t
        case None => 1
    }
}
