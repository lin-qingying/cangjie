// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: level_syscap_check/canIUse.cj level_syscap_check/device_info.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt device_info.cj -o device_info.%middle
// EXEC: %compiler %cmp_opt %compile_lib_opt canIUse.cj -o canIUse.%middle
// EXEC: %compiler %f device_info.%middle canIUse.%middle -o %output
// EXEC: %run %run_opt %output %run_args | compare %f

import ohos.base.canIUse
import ohos.device_info.DeviceInfo

func foo(a!: Unit = @IfAvailable(syscap:"camera", {=>println(0)}, {=>println(1)})) {
}

func bar() {
    foo(a: @IfAvailable(syscap:"camera", {=>println(0)}, {=>println(1)}))
}

main() {
    let v = @IfAvailable(syscap:"camera", {=>println(0)}, {=>println(1)})
    @IfAvailable(level:19, { => @IfAvailable(syscap:"camera", {=>println(0)}, {=>println(1)}); }, {=>println(3)})
    foo()
    bar()
    ()
}

// ASSERT: scan-4 0
