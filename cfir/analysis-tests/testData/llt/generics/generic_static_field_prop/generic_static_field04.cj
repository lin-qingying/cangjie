// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

class ClassC<T> {}

struct A<T> {
    static var a: ClassC<T> = ClassC<T>()
}

main() {}

/* SCAN
error: static member cannot depend on generic parameter 'Generics-T'
  ==> generic_static_field04.cj:12:19:
   | 
12 |     static var a: ClassC<T> = ClassC<T>()
   |                   ^^^^^^^^^ 
   | 
note: generic argument declaration here
  ==> generic_static_field04.cj:11:10:
   | 
11 | struct A<T> {
   |          ^ 
   | 

error: static member cannot depend on generic parameter 'Generics-T'
  ==> generic_static_field04.cj:12:31:
   | 
12 |     static var a: ClassC<T> = ClassC<T>()
   |                               ^^^^^^^^^ 
   | 
note: generic argument declaration here
  ==> generic_static_field04.cj:11:10:
   | 
11 | struct A<T> {
   |          ^ 
   | 

2 errors generated, 2 errors printed.
*/
