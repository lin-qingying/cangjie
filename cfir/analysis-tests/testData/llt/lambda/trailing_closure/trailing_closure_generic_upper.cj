// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

func test<T>(a: T) where T <: (Int64) -> Int64 {
    a(1)
}

main(): Int64 {
    return test {i => i}
}

/* SCAN
error: the upper bound '(Int64) -> Int64' of generic parameter 'Generics-T' must be class or interface
 ==> trailing_closure_generic_upper.cj:9:11:
  | 
9 | func test<T>(a: T) where T <: (Int64) -> Int64 {
  |           ^ 
  | 

1 error generated, 1 error printed.
*/
