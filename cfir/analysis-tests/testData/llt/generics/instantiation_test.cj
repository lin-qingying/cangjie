// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

interface a<b, c> where b <: Int8, c <: d<UInt8, Range<UInt16>> {
    static func e<b, c>(f!: Float64) where b <: UInt64, c <: Int64 {
        {=>} is d<UInt8, Range<UInt16>>
    }
}

interface d<b, g> {}

interface h<b, c> <: a<Int8, d<UInt8, Range<UInt16>>> {}

class i <: h<UInt16, UInt16> {
    static var m = i()
    var j: Int32 = 4
    static func k<b>(): Int32 {
        if (e(f: 52.0)) {
            1
        } else {
            m.j
        }
    }
}

func l() {
    i.k<Range<Int64>>
}

main() {
    l()
    0
}

/* SCAN
error: the upper bound 'Int8' of generic parameter 'Generics-b' must be class or interface
 ==> instantiation_test.cj:9:13:
  | 
9 | interface a<b, c> where b <: Int8, c <: d<UInt8, Range<UInt16>> {
  |             ^ 
  | 

error: the upper bound 'UInt64' of generic parameter 'Generics-b' must be class or interface
  ==> instantiation_test.cj:10:19:
   | 
10 |     static func e<b, c>(f!: Float64) where b <: UInt64, c <: Int64 {
   |                   ^ 
   | 

error: the upper bound 'Int64' of generic parameter 'Generics-c' must be class or interface
  ==> instantiation_test.cj:10:22:
   | 
10 |     static func e<b, c>(f!: Float64) where b <: UInt64, c <: Int64 {
   |                      ^ 
   | 

3 errors generated, 3 errors printed.
*/
