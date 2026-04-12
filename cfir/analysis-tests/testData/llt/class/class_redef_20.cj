// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

public class C<T> {}

public open class A<T> {
    public static func foo<T>(a: C<T>) {
        return 1
    }
}

public open class B<T> <: A<T> {
    public static redef func foo<T>(a: C<T>) {
        return 1
    }
}

main() {
    return 0
}
