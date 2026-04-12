// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args 2>&1 | compare %f
// ASSERT: scan foo
// ASSERT: scan bar

interface InterfaceA {
    func foo(): Unit
    func bar(): Unit {
        println("bar")
    }
}

interface InterfaceB <: InterfaceA {}

class ClassA {}

extend ClassA <: InterfaceA {
    public func foo(): Unit {
        println("foo")
    }
}

extend ClassA <: InterfaceB {}

main() {
    ClassA().foo()
    ClassA().bar()
}
