// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f

interface I1<T> {
    func foo1(x: T) {
        return x
    }
}

struct R1<T> <: I1<T> {
    public mut func foo1(x: T) {
        return x
    }
}

interface I2<T> {
    mut func foo2(x: T) {
        return x
    }
}

struct R2<T> <: I2<T> {
    public func foo2(x: T) {
        return x
    }
}

interface I3<T> {
    func foo3(x: T): Unit
}

struct R3<T> <: I3<T> {
    public mut func foo3(x: T): Unit {
    }
}

interface I4<T> {
    mut func foo4(x: T): Unit
}

struct R4<T> <: I4<T> {
    public func foo4(x: T): Unit {
    }
}

main() {
    return 1
}

/* SCAN
error: 'mut' modifier of 'foo1' is incompatible with that in interface 'I1'
  ==> record_mut_invalid_7.cj:16:5:
   | 
16 |     public mut func foo1(x: T) {
   |     ^ 
   | 

error: 'mut' modifier of 'foo2' is incompatible with that in interface 'I2'
  ==> record_mut_invalid_7.cj:28:5:
   | 
28 |     public func foo2(x: T) {
   |     ^ 
   | 

error: 'mut' modifier of 'foo3' is incompatible with that in interface 'I3'
  ==> record_mut_invalid_7.cj:38:5:
   | 
38 |     public mut func foo3(x: T): Unit {
   |     ^ 
   | 

error: 'mut' modifier of 'foo4' is incompatible with that in interface 'I4'
  ==> record_mut_invalid_7.cj:47:5:
   | 
47 |     public func foo4(x: T): Unit {
   |     ^ 
   | 

4 errors generated, 4 errors printed.
*/
