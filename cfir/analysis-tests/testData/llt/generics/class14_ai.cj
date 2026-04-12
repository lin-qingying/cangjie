// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

class C<T> {
    public let c: Int64 = 1
}

class B<T> {
    public let b: C<T> = C<T>()
}

class A<T> {
    public let a: B<T> = B<T>()
}

main(): Int64 {
    var d: B<Int64> = B<Int64>()
    print(d.b.c.toString())
    A<Int64>()
    return 0
}
