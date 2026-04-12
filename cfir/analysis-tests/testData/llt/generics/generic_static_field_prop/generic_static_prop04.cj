// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

class Class {
    static var b: Int64 = 1
}

enum A<T> where T <: Class {
    | XX

    static prop c: Int64 {
        get() {
            T.b // error
        }
    }
}

main() {}

/* SCAN
error: static member cannot depend on generic parameter 'Generics-T'
  ==> generic_static_prop04.cj:18:13:
   | 
18 |             T.b // error
   |             ^ 
   | 
note: generic argument declaration here
  ==> generic_static_prop04.cj:13:8:
   | 
13 | enum A<T> where T <: Class {
   |        ^ 
   | 

1 error generated, 1 error printed.
*/
