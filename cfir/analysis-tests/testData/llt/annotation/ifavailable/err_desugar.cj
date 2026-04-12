// DEPENDENCE: level_syscap_check/device_info.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt device_info.cj -o device_info.%middle
// ERRCHECK: %compiler %f device_info.%middle %cmp_opt

import ohos.device_info.DeviceInfo


public func foo() {1}
public func goo() {"1"}

main() {
    @IfAvailable(level:19, { => foo() }, { => goo() })
}

/* SCAN
error: types Int64 and Struct-String of the two lambda of this '@IfAvailable' expression mismatch
  ==> err_desugar.cj:12:5:
   | 
12 |     @IfAvailable(level:19, { => foo() }, { => goo() })
   |     ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ 
   | 
   # note: Traces:
The types 'Int64' and 'Struct-String' do not have the smallest common supertype


1 error generated, 1 error printed.
*/

// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.
