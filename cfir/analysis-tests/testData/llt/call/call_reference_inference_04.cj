// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output

interface I {}

extend Int8 <: I {}

func test<T>() where T <: I {
    1
}

func test<T>(a: Int64) {
    2
}

func foo(f: (Int64) -> Int64) {
    f(2)
}

main() {
    let v = foo(test<Float64>)
    if (v != 2) {
        return 1
    }
    return 0
}
