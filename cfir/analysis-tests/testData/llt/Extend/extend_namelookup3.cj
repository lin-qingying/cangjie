// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

// functions within different extensions of the same type can call each other.
class A {}

extend A {
    func goo() {
        foo()
    }
}

extend A {
    public func foo() {}
}

main() {
    A().goo()
    return 0
}
