// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f

interface I<T> where T <: UIntNative {
    prop b1: T {
        get() {
            1
        }
    }
}

extend<T> Option<T> <: I<UIntNative> {}

main() {
    var u1: I<UIntNative> = Some(1)
    return u1.b1 - 1
}

/* SCAN
error: the upper bound 'UIntNative' of generic parameter 'Generics-T' must be class or interface
 ==> generic_default_impl_prop.cj:9:13:
  | 
9 | interface I<T> where T <: UIntNative {
  |             ^ 
  | 

error: cannot convert an integer literal to type 'Generics-T'
  ==> generic_default_impl_prop.cj:12:13:
   | 
12 |             1
   |             ^ 
   | 

2 errors generated, 2 errors printed.
*/
