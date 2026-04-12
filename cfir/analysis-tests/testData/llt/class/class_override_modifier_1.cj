// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %output
// EXEC: %run %run_opt %output %run_args

interface I1 {
    func foo(): Int64
}

interface I2 {
    func foo(): Int64 {
        return 1
    }
}

open class C1 {
    public open func foo(): Int64 {
        return 2
    }
}

class C2 <: C1 & I1 & I2 {
    public override func foo(): Int64 {
        return 3
    }
}

main(): Int64 {
    var res = C2().foo()
    if (res == 3) {
        return 0
    }
    return 1
}

// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace
