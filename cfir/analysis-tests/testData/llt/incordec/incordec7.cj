// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix 2>&1 | compare %f

main(): Int64 {
    let a = B()
    return 0
}

open class A {
    public var p: Int64 = 8
}

class B <: A {
    public var a: Unit = ()
    public func foo() {
        this.p++
        p++
        super.p++
        this.a++
        a++
        a
    }
}

/* SCAN
error: mismatched types
  ==> incordec7.cj:24:9:
   | 
24 |         this.a++
   |         ^^^^^^ expected 'integer type', found 'Unit'
   | 
   # note: the base of increment or decrement expressions should be of integer type

error: mismatched types
  ==> incordec7.cj:25:9:
   | 
25 |         a++
   |         ^ expected 'integer type', found 'Unit'
   | 
   # note: the base of increment or decrement expressions should be of integer type

2 errors generated, 2 errors printed.
*/
