// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

func foo<T>(a: T) {
    bar<Int64>(1)
    a
}

func bar<T>(a: T) {
    baz(foo<Int64>(1)) // Use unknown as function call
    a
}

func baz<T>(a: T): T {
    a
}

main(): Unit {
    bar(1)
}

/* SCAN
error: unable to infer return type, please add type annotation
 ==> nest_function_use_01.cj:9:6:
  | 
9 | func foo<T>(a: T) {
  |      ^^^ 
  | 
note: with recursive usage from
  ==> nest_function_use_01.cj:15:9:
   | 
15 |     baz(foo<Int64>(1)) // Use unknown as function call
   |         ^^^^^^^^^^ 
   | 

1 error generated, 1 error printed.
*/
