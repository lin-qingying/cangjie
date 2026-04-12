// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

class Class {
    static var b: Int64 = 1
}

interface A<T> where T <: Class {
    static prop c: Int64 {
        get() {
            T.b // error
        }
    }
    mut static prop d: Int64 {
        get() {
            T.b // error
        }
        set(_) {
            T.b // error
        }
    }
}

main() {}

/* SCAN
error: static member cannot depend on generic parameter 'Generics-T'
  ==> generic_static_prop03.cj:16:13:
   | 
16 |             T.b // error
   |             ^ 
   | 
note: generic argument declaration here
  ==> generic_static_prop03.cj:13:13:
   | 
13 | interface A<T> where T <: Class {
   |             ^ 
   | 

error: static member cannot depend on generic parameter 'Generics-T'
  ==> generic_static_prop03.cj:21:13:
   | 
21 |             T.b // error
   |             ^ 
   | 
note: generic argument declaration here
  ==> generic_static_prop03.cj:13:13:
   | 
13 | interface A<T> where T <: Class {
   |             ^ 
   | 

error: static member cannot depend on generic parameter 'Generics-T'
  ==> generic_static_prop03.cj:24:13:
   | 
24 |             T.b // error
   |             ^ 
   | 
note: generic argument declaration here
  ==> generic_static_prop03.cj:13:13:
   | 
13 | interface A<T> where T <: Class {
   |             ^ 
   | 

3 errors generated, 3 errors printed.
*/
