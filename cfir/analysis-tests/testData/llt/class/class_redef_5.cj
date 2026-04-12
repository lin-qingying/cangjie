// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-0: %compiler %cmp_opt %f -o %output
// EXEC-0: %run %run_opt %output %run_args

open class C1 {
    public static func foo(): Int64 {
        return 2
    }
}

class C2 <: C1 {
    public static func foo(): Int64 {
        return 3
    }
}

main(): Int64 {
    return C2.foo() - 3
}
