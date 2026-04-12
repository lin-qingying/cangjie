// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: level_syscap_check/canIUse.cj level_syscap_check/device_info.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt device_info.cj -o device_info.%middle
// EXEC: %compiler %cmp_opt %compile_lib_opt canIUse.cj -o canIUse.%middle
// ERRCHECK: %compiler %cmp_opt %diag_json %f device_info.%middle canIUse.%middle
// ASSERT: scan-3 sema_ifavailable_level_limit

import ohos.base.canIUse
import ohos.device_info.DeviceInfo

main() {
    @IfAvailable(level: 18, { => }, { => })
    @IfAvailable(level: 19, { => }, { => })
    @IfAvailable(syscap: "xxx", 
        { => @IfAvailable(level: 18, { => }, { => }) },
        { => @IfAvailable(level: 18, { => }, { => }) }
    )
    0
}