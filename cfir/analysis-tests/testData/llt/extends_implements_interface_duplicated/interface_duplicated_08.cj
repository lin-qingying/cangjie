// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix 2>&1 | compare %f

interface I {}

interface I0<T> {}

interface I1<K> <: I0<K> & I0<Int32> {}

interface I2<P> <: I1<P> & I {}

interface I3<R1, R2> <: I0<R2> {}

interface I4<Y1, R1, R2> <: I & I3<Y1, R1> & I0<Int64> {}

interface I5<R1, R2> <: I2<Int64> & I0<R2> & I {}

class A<Q> <: I0<Q> & I0<Int64> {
    public func doit(): Int64 {
        return 5
    }
}

main() {
    var a: A<Int64>
    var b = A<Int64>()
    var i2: I2<Int64>
    var i3: I3<Int64, Int64>
    var i4: I4<Int64, Rune, Int32>
    var i5: I5<Rune, Int32>
    return 0
}

/* SCAN
error: class 'A' inherits or implements duplicate interface 'I0'
  ==> interface_duplicated_08.cj:30:12:
   | 
30 |     var a: A<Int64>
   |            ^ 
   | 

error: class 'A' inherits or implements duplicate interface 'I0'
  ==> interface_duplicated_08.cj:31:13:
   | 
31 |     var b = A<Int64>()
   |             ^ 
   | 

2 errors generated, 2 errors printed.
*/
