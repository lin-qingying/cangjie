// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix 2>&1 | compare %f
// EXEC: %run %run_opt %n.%suffix

// ASSERT: regex-not core dump
// ASSERT: regex-not stack trace

interface I0 {
    func foo(): Int64 {
        return 0
    }
}

interface I1 {
    func foo1(): Int64 {
        return 1
    }
}

open class C0 {
    public func foo2(): Int64 {
        return 2
    }
}

class C1 <: C0 & I0 & I1 {
    public func test(): Int64 {
        var res: Int64 = foo()
        return res
    }

    public func test1(): Int64 {
        var res: Int64 = foo1()
        return res
    }

    public func test2(): Int64 {
        var res: Int64 = super.foo2()
        return res
    }
}

main(): Int64 {
    var obj = C1()
    var x = obj.test()
    var y = obj.test1()
    var z = obj.test2()
    if (x == 0 && y == 1 && z == 2) {
        return 0
    }
    return 1
}
