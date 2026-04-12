// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: level_syscap_check/canIUse.cj level_syscap_check/device_info.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt device_info.cj -o device_info.%middle
// EXEC: %compiler %f device_info.%middle -o %output --cfg=APILevel_level=0
// EXEC: %run %run_opt %output %run_args | compare %f
import ohos.device_info.DeviceInfo

main() {
    @IfAvailable(level:19, { => println(0) }, {=>println(3)})
}

/* SCAN
0
*/
