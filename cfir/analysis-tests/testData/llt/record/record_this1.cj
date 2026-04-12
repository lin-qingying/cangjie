// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f
// EXEC: %run %run_opt %n.%suffix
// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace

struct A {
    public var a = {=> 5}
    public func f() {
        func g() {
            this.f1()
        }
        var x = {=> this.f1()}
        if (this.f1() == 2) {
            return this.f1() + this.a() + g() + x()
        }
        return 0
    }
    public func g() {
        this
    }
    public func f1() {
        return 2
    }
}

main(): Int64 {
    var a = A().f()
    if (a == 11 && A().g().g().a() == 5) {
        print("${a}")
        return 0
    }
    return 1
}
