// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

interface InterfaceA {
    func foo(): Unit
    static func bar(): Unit {
        println("bar")
    }
}

interface InterfaceB <: InterfaceA {}

class ClassA <: InterfaceA {
    public func foo(): Unit {
        println("foo")
    }
}

extend ClassA <: InterfaceB {}

main() {
    ClassA().foo()
    ClassA.bar()
}
