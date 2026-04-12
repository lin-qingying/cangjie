// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

let val = 11

interface I<T> {
    func test() {
        return val
    }
}

interface II {}

open class A<T> <: I<T> {}

extend<T> A<T> <: II where T <: II {}

class B<T> <: A<T> {}

main() {
    let b = B<Int64>()
    return b.test() - val
}
