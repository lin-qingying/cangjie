// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: apilevel.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt apilevel.cj -o apilevel.%middle
// ERRCHECK: %compiler %cmp_opt apilevel.%middle %n.cj -o %n.%suffix --cfg=APILevel_level=20

import ohos.labels.*

@!APILevel[21]
class A {
    public init() {}
}

main() {
    var a = A()
}

/* SCAN
error: cannot reference 'A'(level: 21) which higher than level of the current scope(level: 20)
  ==> call_construct.cj:19:13:
   | 
19 |     var a = A()
   |             ^^^ 
   | 

1 error generated, 1 error printed.
*/
