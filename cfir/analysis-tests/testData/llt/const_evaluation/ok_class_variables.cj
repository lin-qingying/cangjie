// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC-1: %run %run_opt %output %run_args

class A {
    let x = 0
    let y = 1

    const func f() {
        x + y
    }

    const init() {}
}

main() {
    const a = A()
    a.f()
}
