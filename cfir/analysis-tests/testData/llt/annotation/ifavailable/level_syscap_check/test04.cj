// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: apilevel.cj canIUse.cj device_info.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt apilevel.cj -o apilevel.%middle
// EXEC: %compiler %cmp_opt %compile_lib_opt device_info.cj -o device_info.%middle
// ERRCHECK: %compiler %noColor %cmp_opt_chir2hlir %f device_info.%middle -o %output --cfg=APILevel_level=20

import ohos.labels.*
import ohos.device_info.*

@!APILevel[since: "21"]
public func f21() {}

main() {
    @IfAvailable(level: 22, {=> 
        f21() // ok
    }, {=>
        f21() // error
    })
}

/* SCAN
error: cannot reference 'f21'(level: 21) which higher than level of the current scope(level: 20)
  ==> test04.cj:22:9:
   | 
22 |         f21() // error
   |         ^^^^^ 
   | 

1 error generated, 1 error printed.
*/
