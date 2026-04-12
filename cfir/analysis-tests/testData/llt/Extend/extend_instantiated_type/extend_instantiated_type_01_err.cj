// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

class X {}

class Y {}

class A<T, K> where T <: X, K <: Y {}

extend<T> A<T, T> {}

main() {
}

/* SCAN
error: generic parameter 'T' cannot have two or more class upper bounds 'Class-X, Class-Y' without subtype relation
  ==> extend_instantiated_type_01_err.cj:15:8:
   | 
15 | extend<T> A<T, T> {}
   |        ^ 
   | 

1 error generated, 1 error printed.
*/
