// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: pkg1 pkg2 pkg3
// EXEC: %compiler %cmp_opt %compile_lib_opt pkg1/a.cj -o pkg1.%middle
// EXEC: %compiler %cmp_opt %compile_lib_opt pkg2/b.cj pkg1.%middle -o pkg2.%middle
// EXEC: %compiler %cmp_opt %compile_lib_opt pkg3/c.cj pkg2.%middle  pkg1.%middle -o pkg3.%middle
// (not CJNATIVE) EXEC: %compiler %cmp_opt %f pkg1.%middle pkg2.%middle pkg3.%middle -o %output
// (not not CJNATIVE) EXEC: %compiler %cmp_opt %f pkg2.%middle pkg1.%middle pkg3.%middle -o %output
// RUN-EXEC: %run %run_opt %output %run_args

internal import pkg1.*
internal import pkg2.*
internal import pkg3.*

main() {
    call<MyInterface<() -> Int64>>()
}
