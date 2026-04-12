// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

open class B {}

class C <: B {}

class A {
    public let b = 1
    public func test() {
        let a = 3
        func g(): Int64 {
            a + b
        }
        return g
    }
}

main(): Int64 {
    var ins = A()
    var val = ins.test()()
    if (val != 4) {
        return 1
    }
    return 0
}
