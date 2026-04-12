// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

struct A {
    public let a: Int32
    public A(x: Int32) {
    }
}

struct Data {
    public Data() {}
    public var b = Data()
}

struct Data2 {
    public var b: Data2 = Data2()
}

struct GenRec {
    public var a: Int64
    public var b: Int32
    public init(x: Int64, y: Int32, str: String) {
        this(x)
        this.b = y
    }
    private GenRec(x: Int64) {
        this.a = x
    }
}

main(): Int64 {
    var data2 = GenRec(3, 4, "1")
    if (data2.a == 3 && data2.b == 4) {
        return 0
    }
    return 1
}

/* SCAN
error: recursive constructor calling detected
  ==> primaryConstructor8.cj:17:20:
   | 
17 |     public var b = Data()
   |                    ^^^^ 
   | 
note: depends on
  ==> primaryConstructor8.cj:16:12:
   | 
16 |     public Data() {}
   |            ^^^^ 
   | 
note: depends on
  ==> primaryConstructor8.cj:17:20:
   | 
17 |     public var b = Data()
   |                    ^^^^ 
   | 

error: recursive constructor calling detected
  ==> primaryConstructor8.cj:21:27:
   | 
21 |     public var b: Data2 = Data2()
   |                           ^^^^^ 
   | 
note: depends on
  ==> primaryConstructor8.cj:20:8:
   | 
20 | struct Data2 {
   |        ^^^^^ 
   | 
note: depends on
  ==> primaryConstructor8.cj:21:27:
   | 
21 |     public var b: Data2 = Data2()
   |                           ^^^^^ 
   | 

error: value type recursive detected: 'Data->Data'
  ==> primaryConstructor8.cj:15:1:
   | 
15 | struct Data {
   | ^ 
   | 

error: value type recursive detected: 'Data2->Data2'
  ==> primaryConstructor8.cj:20:1:
   | 
20 | struct Data2 {
   | ^ 
   | 

error: the uninitialized member variable 'a' is not initialized in the constructor of class or struct
  ==> primaryConstructor8.cj:11:5:
   | 
11 |     public A(x: Int32) {
   |     ^ 
   | 

error: the uninitialized member variable 'b' is not initialized in the constructor of class or struct
  ==> primaryConstructor8.cj:31:5:
   | 
31 |     private GenRec(x: Int64) {
   |     ^ 
   | 

6 errors generated, 6 errors printed.
*/
