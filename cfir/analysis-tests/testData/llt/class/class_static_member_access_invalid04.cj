// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

open class A<F, H> {}

extend<F, H> A<F, H> {
    static func f3(a: F, b: H) {
    }
}

class B<K> <: A<K, K> {}

main(): Unit {
    B.f3(Int64(1), Int32(1))
}

/* SCAN
error: generic types substitutions are inconsistent for 'Class-B<Generics-K>'
  ==> class_static_member_access_invalid04.cj:19:5:
   | 
19 |     B.f3(Int64(1), Int32(1))
   |     ^ 
   | 
   # note: 'Generics-K' to 'Int32', 'Int64'

1 error generated, 1 error printed.
*/
