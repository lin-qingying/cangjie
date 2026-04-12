// DEPENDENCE: ../level_syscap_check/apilevel.cj device_info.cj dep.cj dep1.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt apilevel.cj -o apilevel.%middle
// EXEC: %compiler %cmp_opt %compile_lib_opt device_info.cj -o device_info.%middle
// EXEC: %compiler %cmp_opt %compile_shared_lib_opt dep.cj device_info.%middle apilevel.%middle -o libdep.%dylib_suffix
// EXEC: %move dep.cjo dep.cjo.bak
// EXEC: %rmfile libdep.%dylib_suffix
// EXEC: %compiler %cmp_opt %compile_shared_lib_opt dep1.cj device_info.%middle apilevel.%middle -o libdep.%dylib_suffix
// EXEC: %rmfile dep.cjo
// EXEC: %move dep.cjo.bak dep.cjo
// (CJNATIVE) EXEC: %compiler -ldep -L. %f device_info.%middle apilevel.%middle --cfg=APILevel_level=18 -o %output
// (CJNATIVE) EXEC: %run %run_opt %output %run_args | compare %f

import dep.*
import ohos.device_info.DeviceInfo

main() {
    @IfAvailable(level: 19, {=> A().a; func1() }, {=> func2()})
    0
}

// ASSERT: scan 18


// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.
