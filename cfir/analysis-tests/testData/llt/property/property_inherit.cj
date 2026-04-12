// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

interface I<T> {
    prop abc: Int64
}

class B<X> <: I<X> {
    public prop abc: Int64 {
        get() {
            111
        }
    }
}

func test<T>(inst: T) where T <: I<Int64> {
    return inst.abc
}

main(): Int64 {
    let b = B<Int64>()
    if (test(b) != 111) {
        return 1
    }
    return 0
}
