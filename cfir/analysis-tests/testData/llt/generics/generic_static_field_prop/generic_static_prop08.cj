// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

struct Soo<T> {
    public static prop foo: Int64 {
        get() {
            1
        }
    }
    public static var a = foo
}

class Coo<T> {
    public static prop foo: Int64 {
        get() {
            1
        }
    }
    public static var a = foo
}

main() {
    0
}

/* SCAN
error: static member cannot depend on generic parameter 'Generics-T'
  ==> generic_static_prop08.cj:15:27:
   | 
15 |     public static var a = foo
   |                           ^^^ 
   | 
note: generic argument declaration here
 ==> generic_static_prop08.cj:9:12:
  | 
9 | struct Soo<T> {
  |            ^ 
  | 

error: static member cannot depend on generic parameter 'Generics-T'
  ==> generic_static_prop08.cj:24:27:
   | 
24 |     public static var a = foo
   |                           ^^^ 
   | 
note: generic argument declaration here
  ==> generic_static_prop08.cj:18:11:
   | 
18 | class Coo<T> {
   |           ^ 
   | 

2 errors generated, 2 errors printed.
*/
