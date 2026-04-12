// DEPENDENCE: testa.cj
// ERRCHECK: %compiler %noColor %compile_lib_opt %f testa.cj -o %output

package testa

private struct A {}

public class C<T> {
    private let a: A = A()
}

/* SCAN
error: currently, it is not possible to export two private declarations with the same name
 ==> testa.cj:3:15:
  | 
3 | private class A {}
  |               ^ 
  | 
note: same with private declaration
 ==> testb.cj:6:16:
  | 
6 | private struct A {}
  |                ^ 
  | 

1 error generated, 1 error printed.
*/


// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.
