// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: apilevel.cj canIUse.cj device_info.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt apilevel.cj -o apilevel.%middle
// EXEC: %compiler %cmp_opt %compile_lib_opt device_info.cj -o device_info.%middle
// ERRCHECK: %compiler %noColor %cmp_opt_chir2hlir %f device_info.%middle -o %output --cfg=APILevel_level=10

import ohos.device_info.*
import ohos.labels.*

@APILevel[since: "19"]
public func foo() {}

@APILevel[since: "18"]
public func goo() {
    let a = @IfAvailable(level: 19, {=>
        foo()
    }, {=>
        foo()
    })
}

main() {}

/* SCAN
error: cannot reference 'foo'(level: 19) which higher than level of the current scope(level: 18)
  ==> test09.cj:23:9:
   | 
23 |         foo()
   |         ^^^^^ 
   | 

1 error generated, 1 error printed.
*/
