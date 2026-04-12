// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

open class A {
    protected open func f(a!: Int32): Int32 {
        a + 1
    }
}

class B <: A {
    protected override func f(a: Int32): Int32 {
        a + 2
    }
}

main() {
    B().f(3)
}

/* SCAN
error: parameter name mismatched
  ==> override_named_02.cj:16:29:
   | 
16 |     protected override func f(a: Int32): Int32 {
   |                             ^ ~ this positional parameter conflicts with the corresponding named parameter in the supertype
   | 
note: the corresponding function of the supertype is
  ==> override_named_02.cj:10:25:
   | 
10 |     protected open func f(a!: Int32): Int32 {
   |                         ^ 
   | 

1 error generated, 1 error printed.
*/
