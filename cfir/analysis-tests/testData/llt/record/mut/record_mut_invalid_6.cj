// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f

interface I1 {
    func foo1() {
        return 0
    }
}

struct R1 <: I1 {
    public mut func foo1() {
        return 0
    }
}

interface I2 {
    mut func foo2() {
        return 0
    }
}

struct R2 <: I2 {
    public func foo2() {
        return 0
    }
}

interface I3 {
    func foo3(): Unit
}

struct R3 <: I3 {
    public mut func foo3(): Unit {
    }
}

interface I4 {
    mut func foo4(): Unit
}

struct R4 <: I4 {
    public func foo4(): Unit {
    }
}

main() {
    return 1
}

/* SCAN
error: 'mut' modifier of 'foo1' is incompatible with that in interface 'I1'
  ==> record_mut_invalid_6.cj:16:5:
   | 
16 |     public mut func foo1() {
   |     ^ 
   | 

error: 'mut' modifier of 'foo2' is incompatible with that in interface 'I2'
  ==> record_mut_invalid_6.cj:28:5:
   | 
28 |     public func foo2() {
   |     ^ 
   | 

error: 'mut' modifier of 'foo3' is incompatible with that in interface 'I3'
  ==> record_mut_invalid_6.cj:38:5:
   | 
38 |     public mut func foo3(): Unit {
   |     ^ 
   | 

error: 'mut' modifier of 'foo4' is incompatible with that in interface 'I4'
  ==> record_mut_invalid_6.cj:47:5:
   | 
47 |     public func foo4(): Unit {
   |     ^ 
   | 

4 errors generated, 4 errors printed.
*/
