// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f
// EXEC: %run %run_opt %n.%suffix
// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace

struct A<T> {
    public var a = {=> 5}
    public func f(x: T) {
        func g() {
            this.f1(x)
        }
        var d = {=> this.f1(x)}
        return this.f1(x)
    }
    public func g() {
        this
    }
    public func f1(t: T): T {
        return t
    }
}

main(): Int64 {
    var a = A<Int64>().f(2)
    if (a == 2 && A<Int64>().g().g().a() == 5) {
        print("${a}")
        return 0
    }
    return 1
}
