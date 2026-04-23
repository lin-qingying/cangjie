// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

main(): Int64 {
    return 0
}

open class A<T> {}

extend<T> A<T> {
    public func <!EXTEND_FUNCTION_CANNOT_OVERRIDDEN!>foo<!>() {
        0
    }
}

class B<T> <: A<T> {
    public func foo() {
        1
    }
    public operator func ==(right: B<T>): Bool {
        return true
    }
}
