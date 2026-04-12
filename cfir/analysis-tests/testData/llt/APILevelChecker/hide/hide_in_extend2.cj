// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: hide.cj call_hide_extend_dep.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt hide.cj -o hide.%middle
// ERRCHECK: %compiler %cmp_opt %compile_lib_opt hide.%middle %n.cj -o %n.%suffix -Woff all

package ohos.dep

import ohos.labels.*

@!Hide[isChecked: true]
public class A {}

@!Hide[isChecked: false]
extend A {}

/* SCAN
error: the parameter 'isChecked' of '@!Hide' is false
  ==> hide_in_extend2.cj:19:1:
   | 
19 | extend A {}
   | ^^^^^^^^^^^ 
   | 
note: should be same with
  ==> hide_in_extend2.cj:16:1:
   | 
16 | public class A {}
   | ^^^^^^^^^^^^^^^^^ 
   | 

1 error generated, 1 error printed.
*/
