// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix 2>&1 | compare %f
// EXEC: %run %run_opt %n.%suffix

main(): Int64 {
    let a = B()
    a.foo()
    if (a.a == 6 && a.b == 4) {
        return 0
    }
    return 1
}

class B {
    public var a: Int32 = 5
    public var b: Int32 = 5
    public var array = [1, 2, 3]
    public func foo() {
        this.a++
        this.b--
        array[0] = array[0] + 1
    }
}

// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace
