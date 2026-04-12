// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

interface I1 {
    // no default implementation
    func f(a: Int64): Int32
    static func sf(a: Int64): Int32
}

interface I2 <: I1 {
    override func f(a: Int64) {
        Int32(a + 1)
    }
    static redef func sf(a: Int64): Int32
}

interface I3 <: I1 {
    override func f(a: Int32) {
        a + 1
    }
    static redef func sf(a: Int32): Int32
}

interface I4 <: I1 {
    override func f(a: Int32): Int32
}

main() {}

/* SCAN
error: 'override' function 'f' does not have an overridden function in its supertype
  ==> optionalModifiers02_1.cj:23:5:
   | 
23 |     override func f(a: Int32) {
   |     ^ 
   | 

error: 'redef' function 'sf' does not have a redefined 'static' function in its supertype
  ==> optionalModifiers02_1.cj:26:5:
   | 
26 |     static redef func sf(a: Int32): Int32
   |     ^ 
   | 

error: 'override' function 'f' does not have an overridden function in its supertype
  ==> optionalModifiers02_1.cj:30:5:
   | 
30 |     override func f(a: Int32): Int32
   |     ^ 
   | 

3 errors generated, 3 errors printed.
*/
