// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

open class I1<F, K> {
    static func f3(a: F, b: K) {
        println("123")
    }
}

class I2<K> <: I1<K, K> {}

main(): Unit {
    I2.f3(Int64(1), Int32(1))
}

/* SCAN
error: generic types substitutions are inconsistent for 'Class-I2<Generics-K>'
  ==> class_static_member_access_invalid03.cj:18:5:
   | 
18 |     I2.f3(Int64(1), Int32(1))
   |     ^^ 
   | 
   # note: 'Generics-K' to 'Int32', 'Int64'

1 error generated, 1 error printed.
*/
