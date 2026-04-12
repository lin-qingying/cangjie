// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

main(): Int64 {
    A<Int64>().f
    if (A<Int64>().a != 1) {
        return 1
    }
    C<Int64>().a.a
    C<Int64>().a.f
    return 0
}

class A<T> where T <: Int64 {}

extend<T> A<T> where T <: String {
    public prop a: Int64 {
        get() {
            return 1
        }
    }
    public func f() {
        2
    }
}

class C<T> where T <: Int64 {
    public var a: A<T> = A<T>()
}

/* SCAN
error: the upper bound 'Int64' of generic parameter 'Generics-T' must be class or interface
  ==> extend_generic12.cj:19:9:
   | 
19 | class A<T> where T <: Int64 {}
   |         ^ 
   | 

error: the upper bound 'Int64' of generic parameter 'Generics-T' must be class or interface
  ==> extend_generic12.cj:21:8:
   | 
21 | extend<T> A<T> where T <: String {
   |        ^ 
   | 

error: the upper bound 'Int64' of generic parameter 'Generics-T' must be class or interface
  ==> extend_generic12.cj:32:9:
   | 
32 | class C<T> where T <: Int64 {
   |         ^ 
   | 

error: 'f' is not a member of class 'A<Int64>'
  ==> extend_generic12.cj:10:16:
   | 
10 |     A<Int64>().f
   |                ^ 
   | 

error: 'a' is not a member of class 'A<Int64>'
  ==> extend_generic12.cj:11:20:
   | 
11 |     if (A<Int64>().a != 1) {
   |                    ^ 
   | 

error: 'a' is not a member of class 'A<Int64>'
  ==> extend_generic12.cj:14:18:
   | 
14 |     C<Int64>().a.a
   |                  ^ 
   | 

error: 'f' is not a member of class 'A<Int64>'
  ==> extend_generic12.cj:15:18:
   | 
15 |     C<Int64>().a.f
   |                  ^ 
   | 

7 errors generated, 7 errors printed.
*/
