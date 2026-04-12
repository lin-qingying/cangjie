// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

interface I1 {}

interface I2 <: I1 {}

interface I3 {
    func foo() {
        println("foo3")
    }
}

interface I4 {
    func foo() {
        println("foo4")
    }
}

class C {}

extend C <: I2 & I4 {}

extend C <: I1 & I3 {}

main() {
    C().foo()
}

/* SCAN
error: ambiguous match for function call 'foo'
  ==> multi_default_impl_04.cj:32:9:
   | 
32 |     C().foo()
   |         ^ 
   | 
note: found candidate
  ==> multi_default_impl_04.cj:14:10:
   | 
14 |     func foo() {
   |          ^ 
   | 
note: found candidate
  ==> multi_default_impl_04.cj:20:10:
   | 
20 |     func foo() {
   |          ^ 
   | 

1 error generated, 1 error printed.
*/
