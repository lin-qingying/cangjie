// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package dep1

import ohos.labels.APILevel

public func foo(): Int64 {
    0
}

public func foo(a: Array<Int64>): Int64 {
    1
}

public func foo(a: Array<String>): Int64 {
    2
}

public class A {
    public init() {}
    public init(a: Int64) {}
    public init(a: String) {}

    public func foo(): Int64 {
        0
    }
    public func foo(a: Array<Int64>): Int64 {
        1
    }
    public func foo(a: Array<String>): Int64 {
        2
    }

    public var a = 0
    public prop b: Int64 {
        get() {
            1
        }
    }
}

public struct B {
    public init() {}
    public init(a: Int64) {}
    public init(a: String) {}
    public func foo(): Int64 {
        0
    }
    public func foo(a: Array<Int64>): Int64 {
        1
    }
    public func foo(a: Array<String>): Int64 {
        2
    }

    public var a = 0
    public prop b: Int64 {
        get() {
            1
        }
    }
}

public enum C {
    A | B(Int64)

    public func foo(): Int64 {
        0
    }
    public func foo(a: Array<Int64>): Int64 {
        1
    }
    public func foo(a: Array<String>): Int64 {
        2
    }
}

public interface D {
    func foo(): Int64 {
        0
    }
    func foo(a: Array<Int64>): Int64 {
        1
    }
    func foo(a: Array<String>): Int64 {
        2
    }
}
