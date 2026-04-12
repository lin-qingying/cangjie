// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

interface I {
    static func f(a!: Int32): Int32 {
        a
    }
}

class A <: I {
    public static func f(a: Int32): Int32 {
        a + 1
    }
}

main() {
    A.f(0)
}

/* SCAN
error: parameter name mismatched
  ==> abstract_named_00.cj:16:24:
   | 
16 |     public static func f(a: Int32): Int32 {
   |                        ^ ~ this positional parameter conflicts with the corresponding named parameter in the supertype
   | 
note: the corresponding function of the supertype is
  ==> abstract_named_00.cj:10:17:
   | 
10 |     static func f(a!: Int32): Int32 {
   |                 ^ 
   | 

1 error generated, 1 error printed.
*/
