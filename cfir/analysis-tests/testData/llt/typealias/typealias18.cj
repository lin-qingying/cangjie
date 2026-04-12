// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

type A<K> = structA<K>

type B<T> = A<Rune>

type C = B<Int64>

type D<T> = C

struct structA<K> {
    public var b: K
    public structA(var y: K) {
        this.b = y
    }

    public static prop e: Bool {
        get() {
            true
        }
    }
}

main(): Int64 {
    A<Bool>(A<Bool>.e)
    D<structA<Int64>>(r'a')
    D<String>.e
    return 0
}
