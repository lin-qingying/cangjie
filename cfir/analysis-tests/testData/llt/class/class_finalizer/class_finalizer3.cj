// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f

// parse diag
// class A {
//      ~init(a:Int32){} 
// }

class B {
    ~init()() {}
}

class C {
    ~init() {
        return 0
    }
}

class D {
    ~init() {}
}

class F<T> where T <: String {
    ~init<T>() where T <: String {}
}

main() {
    0
}

/* SCAN
error: the upper bound 'Struct-String' of generic parameter 'Generics-T' must be class or interface
  ==> class_finalizer3.cj:28:9:
   | 
28 | class F<T> where T <: String {
   |         ^ 
   | 

error: the upper bound 'Struct-String' of generic parameter 'Generics-T' must be class or interface
  ==> class_finalizer3.cj:29:11:
   | 
29 |     ~init<T>() where T <: String {}
   |           ^ 
   | 

error: finalizer cannot have more than one parameter list
  ==> class_finalizer3.cj:15:10:
   | 
15 |     ~init()() {}
   |          ^ 
   | 

error: cannot convert an integer literal to type 'Unit'
  ==> class_finalizer3.cj:20:16:
   | 
20 |         return 0
   |                ^ 
   | 

error: generic finalizer '~init' is not supported
  ==> class_finalizer3.cj:29:5:
   | 
29 |     ~init<T>() where T <: String {}
   |     ^ 
   | 

5 errors generated, 5 errors printed.
*/
