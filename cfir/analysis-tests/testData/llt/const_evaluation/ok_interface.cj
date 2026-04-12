// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC-2: %run %run_opt %output %run_args

interface I {
    const func f(): Int64
    func f1(): Int64
    static const func f2(): Int64
}

const func g<T>(i: T) where T <: I {
    i.f() + T.f2()
}

class A {
    const init() {}
}

extend A <: I {
    public const func f() {
        0
    }
    public const func f1() {
        1
    }
    public static const func f2() {
        2
    }
}

main() {
    const a = A()
    g(a)
}
