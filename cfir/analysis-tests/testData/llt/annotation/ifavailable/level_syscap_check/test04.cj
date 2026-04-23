// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: apilevel.cj canIUse.cj device_info.cj

import ohos.labels.*
import ohos.device_info.*

@!APILevel[since: "21"]
public func f21() {}

main() {
    @IfAvailable(level: 22, {=> 
        f21() // ok
    }, {=>
        <!APILEVEL_REF_HIGHER!>f21()<!> // error
    })
}
