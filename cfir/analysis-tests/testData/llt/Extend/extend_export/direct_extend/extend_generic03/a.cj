// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package pkga

public open class A {
    protected open func foo() {
        return 1
    }

    protected static func foo1() {
        return 1
    }
}

public type TA = A

extend TA {
    public func me() {
        foo() + foo1()
    }
}
