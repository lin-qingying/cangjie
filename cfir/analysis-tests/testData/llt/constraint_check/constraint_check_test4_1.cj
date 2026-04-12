// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %n.%suffix

func foo<T>() where T <: Int32 {
    return 0
}

main(): Int64 {
    foo<Int32>()
}

/* SCAN
error: the upper bound 'Int32' of generic parameter 'Generics-T' must be class or interface
 ==> constraint_check_test4_1.cj:9:10:
  | 
9 | func foo<T>() where T <: Int32 {
  |          ^ 
  | 

1 error generated, 1 error printed.
*/
