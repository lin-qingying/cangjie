// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

struct B {
    let c: Int64 = 1
}

func foo(b: Option<Int64>) {
    match (b) {
        case Some(_) => 0
        case None => 1
    }
}

main() {
    let a = Some(foo)
    let b = Some(B())
    let x = a?(b?.c)
    match (x) {
        case Some(t) => t
        case None => 1
    }
}
