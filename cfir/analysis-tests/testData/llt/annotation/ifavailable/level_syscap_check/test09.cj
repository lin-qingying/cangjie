// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: apilevel.cj canIUse.cj device_info.cj

import ohos.device_info.*
import ohos.labels.*

@APILevel[since: "19"]
public func foo() {}

@APILevel[since: "18"]
public func goo() {
    let a = @IfAvailable(level: 19, {=>
        foo()
    }, {=>
        <!APILEVEL_REF_HIGHER!>foo()<!>
    })
}

main() {}
