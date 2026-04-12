// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args
// ASSERT: scan 11

class C {
    public static var a = 0
    public var b = 0
    public static func foo() {
        a++
        return a
    }
    public func f() {
        b++
        print(b.toString())
        return b
    }
}

main(): Int64 {
    var f = C.foo
    var g = C().f
    func h(h: () -> Int64) {
        print(h().toString())
    }
    g()
    h(C.foo)
    return f() - 2
}
