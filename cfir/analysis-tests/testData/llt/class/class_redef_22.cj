// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

public open class A {
    public static func foo<T>(a: Array<T>): Unit {}
}

public open class B <: A {
    public static redef func foo<T>(a: Array<T>): Unit {}
}

main() {
    return 0
}
