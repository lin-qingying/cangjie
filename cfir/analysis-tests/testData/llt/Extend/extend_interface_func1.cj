// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix | compare %f
// ASSERT: scan 123A

interface ToStringI {
    func ToString(): String
}

extend Int64 <: ToStringI {
    public func ToString(): String {
        return this.toString()
    }
}

class A {}

extend A <: ToStringI {
    public func ToString(): String {
        return "A"
    }
}

main(): Int64 {
    print(123.ToString())
    print(A().ToString())
    return 0
}
