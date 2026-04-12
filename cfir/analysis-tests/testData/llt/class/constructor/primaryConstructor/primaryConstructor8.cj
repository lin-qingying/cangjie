// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f

class Data {
    var b: Int32
    public let p0: Int32
    public Data(x: Int32, var p0: Int32) {
        this.p0 = 3
    }
}

func run(): Int32 {
    let a: Int32 = 5
    var data: Data = Data(a, a)
    return data.p0 - 5
}

main(): Int64 {
    return Int64(run())
}

/* SCAN
error: redefinition of declaration 'p0'
  ==> primaryConstructor8.cj:12:31:
   | 
12 |     public Data(x: Int32, var p0: Int32) {
   |                               ^^ 
   | 
note: 'p0' is previously declared here
  ==> primaryConstructor8.cj:11:16:
   | 
11 |     public let p0: Int32
   |                ^^ 
   | 

error: cannot assign to immutable value
  ==> primaryConstructor8.cj:13:9:
   | 
13 |         this.p0 = 3
   |         ^^^^^^^^^^^ 
   | 
note: variable 'p0' is immutable
  ==> primaryConstructor8.cj:11:16:
   | 
11 |     public let p0: Int32
   |                ^^ 
   | 

error: the uninitialized member variable 'b' is not initialized in the constructor of class or struct
  ==> primaryConstructor8.cj:12:5:
   | 
12 |     public Data(x: Int32, var p0: Int32) {
   |     ^ 
   | 

3 errors generated, 3 errors printed.
*/
