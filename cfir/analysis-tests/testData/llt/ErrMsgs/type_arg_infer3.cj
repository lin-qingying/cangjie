// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt --diagnostic-format json %f -o %output

// ASSERT: scan sema_unable_to_infer_generic_func
// ASSERT: scan mismatched types
// ASSERT: scan of type 'Class-B<Int64>', impossible to match parameter type 'Class-A<Generics-T>'
// ASSERT: scan of return type 'Class-A<Generics-T>', impossible to match expected type 'Class-B<Int64>'

// test err msg for arg/param and ret/expected type mismatch
func f<T>(a: A<T>) {}

func g<T>(a: A<T>): A<T> {
    a
}

class A<T> {}

class B<T> {}

main() {
    f(B<Int>())
    let s: B<Int> = g(A<Int>())
}
