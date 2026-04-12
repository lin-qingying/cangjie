// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: ../../apilevel.cj dep4.cj dep.cj.d
// EXEC: %compiler %cmp_opt %compile_lib_opt apilevel.cj -o apilevel.%middle
// EXEC: %compiler %cmp_opt %compile_lib_opt apilevel.%middle dep4.cj -o dep.%middle
// EXEC: %compiler %cmp_opt %f dep.%middle apilevel.%middle --cfg=APILevel_level=11

import dep.*

main() {
    A()
    foo()
    0
}
