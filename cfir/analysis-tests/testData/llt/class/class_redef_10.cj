// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-0: %compiler %cmp_opt %f -o %output
// EXEC-0: %run %run_opt %output %run_args

interface I1 {
    static func foo(): Int64
    func f1(): Int64
}

open class C1 <: I1 {
    public static redef func foo(): Int64 {
        return 3
    }
    public override func f1(): Int64 {
        return 4
    }
}

main(): Int64 {
    return C1.foo() + C1().f1() - 7
}
