// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: hide.cj ../level/apilevel.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt hide.cj apilevel.cj -o hide.%middle
// ERRCHECK: %compiler %cmp_opt hide.%middle %n.cj -o %n.%suffix -Woff all --cfg=APILevel_level=20

package ohos.dep

import ohos.labels

@!labels.Hide
@!labels.Hide
public class A {}

@!labels.APILevel[since: "21"]
public class B {}

main() {
    B()
    0
}

/* SCAN
error: cannot be annotated with '@!Hide' more than once
  ==> use_qualifiedtype.cj:17:1:
   | 
17 | public class A {}
   | ^^^^^^^^^^^^^^^^^ 
   | 

error: cannot reference 'B'(level: 21) which higher than level of the current scope(level: 20)
  ==> use_qualifiedtype.cj:23:5:
   | 
23 |     B()
   |     ^^^ 
   | 

2 errors generated, 2 errors printed.
*/
