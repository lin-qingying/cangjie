// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

type A<T, k> = structA<T, k>

type TI<T, K> = <!sema_not_a_type!>I<!><T, <!sema_undeclared_type_name!>O<!>>

var d = r'#'

extend<<!sema_upper_bound_must_be_class_or_interface!>T<!>, <!sema_upper_bound_must_be_class_or_interface!>K<!>> structA<T, K> <: <!sema_extend_not_interface!>TI<T, K><!> where T <: Rune, K <: String {
    mut static prop i: Rune {
        get() {
            d
        }
        set(v) {
            d = v
        }
    }
}

interface I<T, K> {
    mut static prop i: Rune {
        get() {
            d
        }
        set(v) {
            d = v
        }
    }
}

struct structA<<!sema_upper_bound_must_be_class_or_interface!>T<!>, <!sema_upper_bound_must_be_class_or_interface!>K<!>> where T <: Rune, K <: String {
    var a: T
    var b: K
    structA(x: T, y: K) {
        this.a = x
        this.b = y
    }
}

main(): Int64 {
    func a(a: A<Rune, String>) {
        return a.a.toString() + a.b
    }

    A<Rune, String>.i = r'@'

    if (a(A<Rune, String>(A<Rune, String>.i, "345")) == "@345") {
        return 0
    } else {
        return 1
    }
}
