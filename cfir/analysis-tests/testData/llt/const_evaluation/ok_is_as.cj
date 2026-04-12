// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

enum E {
    A
}

const func f() {
    let x = A
    let y = x as E
    let z = y is Option<E>
    const func g(x: Option<E>) {
        match (x) {
            case Some(A) => 0
            case None => 1
        }
    }
    if (z) {
        return g(x)
    }
    return 2
}

main() {
    f()
}
