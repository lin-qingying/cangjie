// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

// test case: re-enter Unit type if expr generation
class ClassB {
    public var var1: Unit = ();
    public func testa(a!: Int64 = 1): Int64 {
        1
    }
}

var b = {
    =>
    if (true) {}
    else {}
    ClassB();
}().testa()

main(): Int64 {
    return 0
}
