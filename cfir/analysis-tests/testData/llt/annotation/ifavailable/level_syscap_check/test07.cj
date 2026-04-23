// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: apilevel.cj canIUse.cj device_info.cj

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
            <!APILEVEL_REF_HIGHER!>f23()<!> // error
        }, {=> // <= 20
            f19() // ok
            f20() // ok
            <!APILEVEL_REF_HIGHER!>f21()<!> // error
            <!APILEVEL_REF_HIGHER!>f22()<!> // error
            <!APILEVEL_REF_HIGHER!>f23()<!> // error
        }) 
    }, {=>
        @IfAvailable(level: 21, {=> // <= 21
            f19() // ok
            f20() // ok
            f21() // ok
            <!APILEVEL_REF_HIGHER!>f22()<!> // error
            <!APILEVEL_REF_HIGHER!>f23()<!> // error
        }, {=> // <= 20
            f19() // ok
            f20() // ok
            <!APILEVEL_REF_HIGHER!>f21()<!> // error
            <!APILEVEL_REF_HIGHER!>f22()<!> // error
            <!APILEVEL_REF_HIGHER!>f23()<!> // error
        })
    })
}
