// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt  %f  -o %output
// EXEC: %run %run_opt %output %run_args

interface X {}

extend String <: X {}

interface Y {}

extend Int64 <: Y {}

main(): Int64 {
    if (B<String, Int64>().m != 1) {
        return 1
    }
    return 0
}

interface I<T> {
    func foo(): Int64 {
        return 1
    }
}

interface J<T, V> <: I<T> & I<V> where T <: X, V <: Y {
    func foo(): Int64 {
        return 1
    }
    prop m: Int64 {
        get() {
            return foo()
        }
    }
}

class B<T, V> <: J<T, V> where T <: X, V <: Y {}
