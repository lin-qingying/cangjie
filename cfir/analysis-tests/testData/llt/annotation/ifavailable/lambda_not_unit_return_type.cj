// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: level_syscap_check/canIUse.cj level_syscap_check/device_info.cj

import ohos.device_info.DeviceInfo

main() {
    @IfAvailable(level:19, { => println(3) }, {=>println(1)})
    0
}
