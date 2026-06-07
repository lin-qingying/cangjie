// FILE: level_syscap_check/canIUse.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package ohos.base

public func canIUse(syscap: String): Bool {
    return syscap == "camera"
}

// FILE: level_syscap_check/device_info.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package ohos.device_info

public class DeviceInfo {
    public static prop sdkApiVersion: Int64 {
        get() {19}
    }
}

// FILE: level_limit.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


import ohos.base.canIUse
import ohos.device_info.DeviceInfo

main() {
    <!IFAVAILABLE_LEVEL_LIMIT!>@IfAvailable(level: 18, { => }, { => })<!>
    @IfAvailable(level: 19, { => }, { => })
    @IfAvailable(syscap: "xxx", 
        { => <!IFAVAILABLE_LEVEL_LIMIT!>@IfAvailable(level: 18, { => }, { => })<!> },
        { => <!IFAVAILABLE_LEVEL_LIMIT!>@IfAvailable(level: 18, { => }, { => })<!> }
    )
    0
}
