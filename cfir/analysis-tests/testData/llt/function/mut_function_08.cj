// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC-2: %run %run_opt %output %run_args

class C {
    var c = 0
}

struct A {
    let x = [0, 1]
    var c = C()

    func f() {
        x[0] += 1
        c.c += 1
    }
}

main() {
    let a = A()
    a.f()
    a.x[0] + a.c.c
}
