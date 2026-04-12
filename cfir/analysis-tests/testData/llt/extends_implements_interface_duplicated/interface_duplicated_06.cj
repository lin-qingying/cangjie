// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix 2>&1 | compare %f

interface I {}

interface I0<T> {}

interface I1<K> <: I0<K> & I0<Rune> {}

class A<T> <: I & I1<T> & I {}

class B <: I1<Int64> & I1<Int32> {}

class C <: I1<Rune> & I1<Rune> {}

interface I2 <: I & I {}

interface I4 <: I & I0<Int64> {}

main() {
    return 0
}

/* SCAN
error: class 'A' inherits or implements duplicate interface 'I'
  ==> interface_duplicated_06.cj:15:1:
   | 
15 | class A<T> <: I & I1<T> & I {}
   | ^ 
   | 

error: class 'C' inherits or implements duplicate interface 'I0'
  ==> interface_duplicated_06.cj:19:1:
   | 
19 | class C <: I1<Rune> & I1<Rune> {}
   | ^ 
   | 

error: interface 'I2' inherits or implements duplicate interface 'I'
  ==> interface_duplicated_06.cj:21:1:
   | 
21 | interface I2 <: I & I {}
   | ^ 
   | 

error: interface 'I1' inherits or implements duplicate interface 'I0'
  ==> interface_duplicated_06.cj:19:12:
   | 
19 | class C <: I1<Rune> & I1<Rune> {}
   |            ^ 
   | 

error: interface 'I1' inherits or implements duplicate interface 'I0'
  ==> interface_duplicated_06.cj:19:23:
   | 
19 | class C <: I1<Rune> & I1<Rune> {}
   |                       ^ 
   | 

5 errors generated, 5 errors printed.
*/
