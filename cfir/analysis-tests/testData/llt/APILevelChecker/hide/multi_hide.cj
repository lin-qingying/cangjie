// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: hide.cj hide_use1.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt hide.cj -o hide.%middle
// ERRCHECK: %compiler %cmp_opt %compile_lib_opt hide.%middle %n.cj -o %n.%suffix -Woff all

package ohos.dep

import ohos.labels.*

@!Hide
@!Hide[isChecked: true]
public class A {}

/* SCAN
error: cannot be annotated with '@!Hide' more than once
  ==> multi_hide.cj:17:1:
   | 
17 | public class A {}
   | ^^^^^^^^^^^^^^^^^ 
   | 

1 error generated, 1 error printed.
*/
