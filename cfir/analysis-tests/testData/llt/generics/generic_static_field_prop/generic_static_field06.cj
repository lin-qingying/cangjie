// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

struct Soo<T> {
    public static func foo<V>(): Option<V> {
        return Option<V>.None
    }
    public static var a: Option<String> = foo<String>()
}

class Coo<T> {
    public static func foo<V>(): Option<V> {
        return Option<V>.None
    }
    public static var a: Option<String> = foo<String>()
}

main() {
    0
}

/* SCAN
error: static member cannot depend on generic parameter 'Generics-T'
  ==> generic_static_field06.cj:13:43:
   | 
13 |     public static var a: Option<String> = foo<String>()
   |                                           ^^^^^^^^^^^ 
   | 
note: generic argument declaration here
 ==> generic_static_field06.cj:9:12:
  | 
9 | struct Soo<T> {
  |            ^ 
  | 

error: static member cannot depend on generic parameter 'Generics-T'
  ==> generic_static_field06.cj:20:43:
   | 
20 |     public static var a: Option<String> = foo<String>()
   |                                           ^^^^^^^^^^^ 
   | 
note: generic argument declaration here
  ==> generic_static_field06.cj:16:11:
   | 
16 | class Coo<T> {
   |           ^ 
   | 

2 errors generated, 2 errors printed.
*/
