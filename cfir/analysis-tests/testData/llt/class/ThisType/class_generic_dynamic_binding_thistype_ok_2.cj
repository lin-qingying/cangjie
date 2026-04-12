// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %output
// EXEC: %run %run_opt %output %run_args
// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace

var x: Int64 = 0

open class C1<T> {
    public func foo1(): This {
        x = 1
        return this
    }
}

class C2<T> <: C1<T> {
    public func foo2(): This {
        x = 2
        return this
    }
}

main() {
    let obj = C2<Int64>()
    obj.foo1().foo2()
    if (x == 2) {
        return 0
    }
    return 1
}
