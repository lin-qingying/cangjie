// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix
// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace

public class A {
    public init() {}

    public static var var131: Int32 = 0;
    public var lambda = {=> var131}
    public static var lambda1 = {=> var131}
    public var v = var131
    public init(a: Int8) {
        this()
        var lambda1 = {
            =>
            var131 = var131 + 1
            return var131
        }
        lambda1()
    }
    public func f() {
        return 2
    }
}

main(): Int64 {
    var rec = A(2)
    var rec1 = A(2).lambda()
    if (rec.lambda() != 2 && A.lambda1() != 2 && rec1 != 2) {
        return 1
    }
    var b = {
        =>
        if (true) {}
        else {}
        A();
    }().f()
    return 0
}
