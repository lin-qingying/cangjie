// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

struct B {
    let b: Int64
    let c: Int64

    prop d: Int64 {
        get() {
            b + c - 3
        }
    }

    B(b: Int64, c: Int64) {
        this.b = b
        this.c = c
    }
}

func foo(b: Int64, c: Int64) {
    Some(B(b, c))
}

main() {
    let a = Some(foo)
    let b = 1
    let c = 2
    let x = a?(b, c)?.d
    match (x) {
        case Some(t) => t
        case None => 1
    }
}
