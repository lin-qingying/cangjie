// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %enableO2 %f -o %output
// EXEC-0: %run %run_opt %output %run_args

interface IA {
    mut static prop p: Int64 {
        get() {
            1
        }
        set(v) {
            v
        }
    }
}

struct Data<T> <: IA {
    static var _v = 0
    public mut static prop p: Int64 {
        get() {
            _v
        }
        set(v) {
            _v = v
        }
    }
    public mut func foo() {
        0
    }
}

interface I<M> {
    static func sFoo(): Int64 {
        0
    }
}

enum E<M> <: I<M> where M <: IA {
    E1(M)

    public static func sFoo() {
        M.p
    }
}

main() {
    E<Data<Int64>>.sFoo()
}
