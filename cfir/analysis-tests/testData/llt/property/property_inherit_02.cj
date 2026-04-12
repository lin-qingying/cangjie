// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

abstract class A {
    public prop p: Int64
}

class B <: A {
    public prop p: Int64 {
        get() {
            111
        }
    }
}

class C {
    private let aa: A
    init(a: A) {
        aa = a
    }

    prop p: Int64 {
        get() {
            aa.p
        }
    }
}

main(): Int64 {
    let b = B()
    let val = C(b)
    if (val.p != 111) {
        return 1
    }
    return 0
}
