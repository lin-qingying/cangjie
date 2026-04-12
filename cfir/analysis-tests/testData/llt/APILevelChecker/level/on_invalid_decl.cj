// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: apilevel.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt apilevel.cj -o apilevel.%middle --cfg=APILevel=11
// ERRCHECK: %compiler %cmp_opt apilevel.%middle %n.cj -o %n.%suffix

import ohos.labels.*

func foo(@!APILevel[since: "10", syscap: "xxx"] a: Int64) {}

@!APILevel[since: "10", syscap: "xxx"]
main() {}

/* SCAN
error: unexpected annotation '@APILevel' on main
  ==> on_invalid_decl.cj:15:1:
   | 
15 | @!APILevel[since: "10", syscap: "xxx"]
   | ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ unexpected annotation here
16 | main() {}
   | ~~~~ on main
   | 

1 error generated, 1 error printed.
*/
