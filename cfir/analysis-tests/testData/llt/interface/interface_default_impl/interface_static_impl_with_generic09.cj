// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC-PIPE: %run %run_opt %output %run_args 

interface I1<T1> {
    static func test(a: T1): T1 {
        return a
    }
}

interface I2<T2> <: I1<Bool> & I1<Int64> {
    static func test(a: T2): T2 {
        return a
    }
}

func test<K>(): Int64 where K <: I2<String> {
    K.test(0)
}

main(): Int64 {
    test<I2<String>>()
}
