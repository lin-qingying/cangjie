// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

struct R1 {
    public var i = 0
}

extend R1 {
    public mut func foo(): Unit {
        i += 1
    }

    public func goo1() {
        return <!IMMUTABLE_FUNCTION_CANNOT_ACCESS_MUTABLE_FUNCTION!>foo<!>
    }

    public func goo2() {
        return <!IMMUTABLE_FUNCTION_CANNOT_ACCESS_MUTABLE_FUNCTION!>foo<!>()
    }
}

interface I1 {
    mut func foo(): Unit {
    }

    func goo1() {
        return <!IMMUTABLE_FUNCTION_CANNOT_ACCESS_MUTABLE_FUNCTION!>foo<!>
    }

    func goo2() {
        return <!IMMUTABLE_FUNCTION_CANNOT_ACCESS_MUTABLE_FUNCTION!>foo<!>()
    }
}

main() {
    return 1
}
