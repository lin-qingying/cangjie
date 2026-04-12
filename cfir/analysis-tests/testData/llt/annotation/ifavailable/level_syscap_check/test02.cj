// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: canIUse.cj device_info.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt canIUse.cj -o canIUse.%middle
// EXEC: %compiler %cmp_opt %compile_lib_opt device_info.cj -o device_info.%middle
// EXEC: %compiler %cmp_opt %f canIUse.%middle device_info.%middle

import ohos.device_info.*
import ohos.base.*

main() {
    @IfAvailable(level: 19, {=>}, {=>})
    @IfAvailable(syscap: "xxx", {=>}, {=>})
}
