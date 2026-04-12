// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: apilevel.cj canIUse.cj device_info.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt apilevel.cj -o apilevel.%middle
// EXEC: %compiler %cmp_opt %compile_lib_opt device_info.cj -o device_info.%middle
// ERRCHECK: %compiler %noColor %cmp_opt_chir2hlir %f device_info.%middle -o %output --cfg=APILevel_level=23

import ohos.labels.*
import ohos.device_info.*

@!APILevel[since: "19"]
public func f19() {}

@!APILevel[since: "20"]
public func f20() {}

@!APILevel[since: "21"]
public func f21() {}

@!APILevel[since: "22"]
public func f22() {}

@!APILevel[since: "23"]
public func f23() {}

main() {
    @IfAvailable(level: 20, {=> 
        @IfAvailable(level: 22, {=> // <= 22
            f19() // ok
            f20() // ok
            f21() // ok
            f22() // ok
            f23() // error
        }, {=> // <= 20
            f19() // ok
            f20() // ok
            f21() // error
            f22() // error
            f23() // error
        }) 
    }, {=>
        @IfAvailable(level: 21, {=> // <= 21
            f19() // ok
            f20() // ok
            f21() // ok
            f22() // error
            f23() // error
        }, {=> // <= 23
            f19() // ok
            f20() // ok
            f21() // ok
            f22() // ok
            f23() // ok
        })
    })
}

/* SCAN
error: cannot reference 'f23'(level: 23) which higher than level of the current scope(level: 22)
  ==> test08.cj:37:13:
   | 
37 |             f23() // error
   |             ^^^^^ 
   | 

error: cannot reference 'f21'(level: 21) which higher than level of the current scope(level: 20)
  ==> test08.cj:41:13:
   | 
41 |             f21() // error
   |             ^^^^^ 
   | 

error: cannot reference 'f22'(level: 22) which higher than level of the current scope(level: 20)
  ==> test08.cj:42:13:
   | 
42 |             f22() // error
   |             ^^^^^ 
   | 

error: cannot reference 'f23'(level: 23) which higher than level of the current scope(level: 20)
  ==> test08.cj:43:13:
   | 
43 |             f23() // error
   |             ^^^^^ 
   | 

error: cannot reference 'f22'(level: 22) which higher than level of the current scope(level: 21)
  ==> test08.cj:50:13:
   | 
50 |             f22() // error
   |             ^^^^^ 
   | 

error: cannot reference 'f23'(level: 23) which higher than level of the current scope(level: 21)
  ==> test08.cj:51:13:
   | 
51 |             f23() // error
   |             ^^^^^ 
   | 

6 errors generated, 6 errors printed.
*/
