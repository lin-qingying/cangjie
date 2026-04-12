// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

interface I1 {
    prop a: Int64
}

interface I2 {
    prop a: Int64
}

class A <: I1 & I2 {
    public prop a: Int64 {
        get() {
            3
        }
    }
}

func test<T>(i: T) where T <: I1 & I2 {
    let x = i.a
    return x
}

main() {
    let ins = A()
    let v = test(ins)
    if (v != ins.a) {
        return 1
    }
    return 0
}
