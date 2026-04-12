// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output

let res = 2

open class A {
    public func test(): Int64 {
        return res
    }
}

class B <: A {}

interface I1 {
    func test(): Int64
}

extend B <: I1 {}

main() {
    let b: I1 = B()
    if (b.test() != res) {
        return 1
    }
    return 0
}
