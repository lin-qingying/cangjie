// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

enum TimeUnit2<T> {
    Year2
}

enum TimeUnit {
    Year2(T)
}

main() {
    var time = TimeUnit2<Int32>.Year2
    TimeUnit2<Float64>.Year2
    TimeUnit2<Float64>.Year2
    TimeUnit2<Float64, Int32>.Year2
    TimeUnit2<Int32>.Year2
    TimeUnit2.Year2
}

/* SCAN
error: undeclared type name 'T'
  ==> enum1.cj:14:11:
   | 
14 |     Year2(T)
   |           ^ 
   | 

error: type argument's number does not match type parameter's number
  ==> enum1.cj:21:5:
   | 
21 |     TimeUnit2<Float64, Int32>.Year2
   |     ^^^^^^^^^^^^^^^^^^^^^^^^^ 
   | 

error: generic type should be used with type argument
  ==> enum1.cj:23:5:
   | 
23 |     TimeUnit2.Year2
   |     ^ 
   | 

3 errors generated, 3 errors printed.
*/
