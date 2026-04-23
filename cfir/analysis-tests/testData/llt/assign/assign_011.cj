// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

public struct S {
    public var x: Int64 = 0
}

public class A {
    private var a: S = S()
    public mut prop s: S {
        get() {
            a
        }
        set(v) {
            a = v
        }
    }
    public func test(): Unit {
        <!CANNOT_ASSIGN_TO_IMMUTABLE!>s.x<!> = 1
    }
}

main() {}
