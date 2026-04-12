// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

interface I1 {
    // with default implementation
    func f(a: Int64) {
        a
    }
    static func g(a: Int64) {
        a
    }
}

interface I2 <: I1 {
    func f(a: Int32) {
        a + 1
    }
    static func g(a: Int64) {
        a + 1
    }
}

interface I3 <: I1 {
    override func f(a: Int32) {
        a + 1
    }
    static redef func g(a: Int32) {
        a
    }
}

interface I4 <: I1 {
    override func f(a: Int64) {
        a + 1
    }
}

main() {}

/* SCAN
error: 'override' function 'f' does not have an overridden function in its supertype
  ==> optionalModifiers02_2.cj:29:5:
   | 
29 |     override func f(a: Int32) {
   |     ^ 
   | 

error: 'redef' function 'g' does not have a redefined 'static' function in its supertype
  ==> optionalModifiers02_2.cj:32:5:
   | 
32 |     static redef func g(a: Int32) {
   |     ^ 
   | 

2 errors generated, 2 errors printed.
*/
