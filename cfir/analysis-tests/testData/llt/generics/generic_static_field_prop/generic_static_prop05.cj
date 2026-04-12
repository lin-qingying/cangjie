// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

class Class {
    static var b: Int64 = 1
}

struct A<T> {}

extend<T> A<T> where T <: Class {
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
  ==> generic_static_prop05.cj:18:13:
   | 
18 |             T.b // error
   |             ^ 
   | 
note: generic argument declaration here
  ==> generic_static_prop05.cj:15:8:
   | 
15 | extend<T> A<T> where T <: Class {
   |        ^ 
   | 

error: static member cannot depend on generic parameter 'Generics-T'
  ==> generic_static_prop05.cj:23:13:
   | 
23 |             T.b // error
   |             ^ 
   | 
note: generic argument declaration here
  ==> generic_static_prop05.cj:15:8:
   | 
15 | extend<T> A<T> where T <: Class {
   |        ^ 
   | 

error: static member cannot depend on generic parameter 'Generics-T'
  ==> generic_static_prop05.cj:26:13:
   | 
26 |             T.b // error
   |             ^ 
   | 
note: generic argument declaration here
  ==> generic_static_prop05.cj:15:8:
   | 
15 | extend<T> A<T> where T <: Class {
   |        ^ 
   | 

3 errors generated, 3 errors printed.
*/
