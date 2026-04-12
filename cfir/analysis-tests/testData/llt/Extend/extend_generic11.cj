// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// (CJNATIVE) EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

main(): Int64 {
    A<Int64>.f
    if (A<Int64>.a != 1) {
        return 1
    }
    C<Int64>.a.a1
    C<Int64>.a.f1
    return 0
}

class A<T> where T <: Int64 {}

extend<T> A<T> where T <: String {
    public static prop a: Int64 {
        get() {
            return 1
        }
    }
    public static func f() {
        2
    }

    public prop a1: Int64 {
        get() {
            return 1
        }
    }
    public func f1() {
        2
    }
}

class C<T> where T <: Int64 {
    public static var a: A<T> = A<T>()
}

/* SCAN
error: the upper bound 'Int64' of generic parameter 'Generics-T' must be class or interface
  ==> extend_generic11.cj:19:9:
   | 
19 | class A<T> where T <: Int64 {}
   |         ^ 
   | 

error: the upper bound 'Int64' of generic parameter 'Generics-T' must be class or interface
  ==> extend_generic11.cj:21:8:
   | 
21 | extend<T> A<T> where T <: String {
   |        ^ 
   | 

error: the upper bound 'Int64' of generic parameter 'Generics-T' must be class or interface
  ==> extend_generic11.cj:41:9:
   | 
41 | class C<T> where T <: Int64 {
   |         ^ 
   | 

error: 'f' is not a member of class 'A<Int64>'
  ==> extend_generic11.cj:10:14:
   | 
10 |     A<Int64>.f
   |              ^ 
   | 

error: 'a' is not a member of class 'A<Int64>'
  ==> extend_generic11.cj:11:18:
   | 
11 |     if (A<Int64>.a != 1) {
   |                  ^ 
   | 

error: 'a1' is not a member of class 'A<Int64>'
  ==> extend_generic11.cj:14:16:
   | 
14 |     C<Int64>.a.a1
   |                ^^ 
   | 

error: 'f1' is not a member of class 'A<Int64>'
  ==> extend_generic11.cj:15:16:
   | 
15 |     C<Int64>.a.f1
   |                ^^ 
   | 

error: static member cannot depend on generic parameter 'Generics-T'
  ==> extend_generic11.cj:42:26:
   | 
42 |     public static var a: A<T> = A<T>()
   |                          ^^^^ 
   | 
note: generic argument declaration here
  ==> extend_generic11.cj:41:9:
   | 
41 | class C<T> where T <: Int64 {
   |         ^ 
   | 

error: static member cannot depend on generic parameter 'Generics-T'
  ==> extend_generic11.cj:42:33:
   | 
42 |     public static var a: A<T> = A<T>()
   |                                 ^^^^ 
   | 
note: generic argument declaration here
  ==> extend_generic11.cj:41:9:
   | 
41 | class C<T> where T <: Int64 {
   |         ^ 
   | 

9 errors generated, 9 errors printed.
*/
