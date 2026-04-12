// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: %n.mac.cj
// EXEC: %compiler %n.mac.cj --compile-macro
// ERRCHECK: %compiler %f %compile_lib_opt %cmp_opt

import mm.*

@b
func build() {
    @IfAvailable(level:19, {=> }, {=> })
}

func bar() {
    @IfAvailable(level:19, {=> }, {=> })
}

/* SCAN
error: import 'ohos.device_info' to use the 'IfAvailable' expression
  ==> index.cj:15:5:
   | 
15 |     @IfAvailable(level:19, {=> }, {=> })
   |     ^ 
   | 
   # note: depend on declaration 'DeviceInfo'
note: the error occurs after the macro is expanded
  ==> index.cj:13:1:
   | 
13 |   @b
   |  _^
14 | | func build() {
   | | ...
16 | | }
   | |_^
   | 
note: the code after the macro is expanded as follows
   | 
   |   /* 13.1 */func build() {
   |  _^
   | | /* 13.2 */    @IfAvailable(level: 19, { => }, { => })
   | | /* 13.3 */}
   | |___________^
   | 
note: which is expanded as follows
   | 
   | /* 13.2 */    @IfAvailable(level: 19, { => }, { => })
   |               ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ 
   | 

error: import 'ohos.device_info' to use the 'IfAvailable' expression
  ==> index.cj:19:5:
   | 
19 |     @IfAvailable(level:19, {=> }, {=> })
   |     ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ 
   | 
   # note: depend on declaration 'DeviceInfo'

2 errors generated, 2 errors printed.
*/
