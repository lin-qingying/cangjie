// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

class CA<T> {}

interface I2_1 {
    static func goo(): Unit {
        println("I2-1")
    }
}

interface I2_2 <: I2_1 {
    static func goo(): Unit {
        println("I2-2")
    }
}

interface I3 {
    static func goo(): Unit
}

extend<T> CA<T> <: I3 where T <: I2_1 & I2_2 {
    public static func goo(): Unit {
        T.goo()
        println("here")
    }
}

main() {
    CA<Nothing>.goo()
    0
}

/* SCAN
error: can not instantiate 'Generics-T' by 'Nothing' for it has unimplemented static member
  ==> interface_static_impl_with_generic12.cj:35:8:
   | 
35 |     CA<Nothing>.goo()
   |        ^ 
   | 
   # note: 'Nothing' type has no members

1 error generated, 1 error printed.
*/
