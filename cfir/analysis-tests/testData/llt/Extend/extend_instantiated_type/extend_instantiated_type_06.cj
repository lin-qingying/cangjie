// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output

class A<T, K> where K <: ToString {}

extend<T> A<Int64, T> <: ToString {
    public func toString() {
        return "A<Int64, T>"
    }
}

main() {
    let a1 = A<Int64, Rune>()
    let t: ToString = a1
    if (t.toString() != "A<Int64, T>") {
        return 1
    }
    return 0
}
