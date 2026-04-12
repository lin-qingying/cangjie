// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

interface A<U> where U <: A<U> {}

extend<U> A<U> {}

main() {}

/* SCAN
error: extending type 'Interface-A<Generics-U>' is not allowed
  ==> interface5.cj:11:11:
   | 
11 | extend<U> A<U> {}
   |           ^^^^ 
   | 

1 error generated, 1 error printed.
*/
