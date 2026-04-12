// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: hide.cj ../level/apilevel.cj mix_macro_dep.cj mix_macro_dep1.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt hide.cj apilevel.cj -o hide.%middle
// EXEC: %compiler %cmp_opt %cmp_macro mix_macro_dep.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt mix_macro_dep1.cj -o mix_macro_dep1.%middle
// EXEC: %compiler %cmp_opt %compile_lib_opt hide.%middle %n.cj -o %n.%suffix 2>&1 | compare %f

package ohos.dep

import ohos.labels.*
import m.*
import anno.*

@!APILevel[since: "21"]
@M
public open class A {
}

@!APILevel[since: "21"]
@CustomAnno
public open class B {
}

@!Hide[isChecked: true]
@M
public open class C {
}

@!Hide[isChecked: true]
@CustomAnno
public open class D {
}

/* SCAN
warning: annotation 'Hide' must be placed below all macros and annotations
  ==> mix_macro1.cj:34:1:
   | 
34 | @!Hide[isChecked: true]
   | ^ 
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff apilevel-check`

warning: annotation 'Hide' must be placed below all macros and annotations
  ==> mix_macro1.cj:29:1:
   | 
29 | @!Hide[isChecked: true]
   | ^ 
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff apilevel-check`

warning: annotation 'APILevel' must be placed below all macros and annotations
  ==> mix_macro1.cj:24:1:
   | 
24 | @!APILevel[since: "21"]
   | ^ 
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff apilevel-check`

warning: annotation 'APILevel' must be placed below all macros and annotations
  ==> mix_macro1.cj:19:1:
   | 
19 | @!APILevel[since: "21"]
   | ^ 
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff apilevel-check`

4 warnings generated, 4 warnings printed.
*/
