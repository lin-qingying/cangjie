// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: hide_err.cj hide_use_err.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt hide_err.cj -o hide_err.%middle
// EXEC: %compiler %cmp_opt %compile_lib_opt hide_use_err.cj -o hide_use_err.%middle
// EXEC: %compiler %cmp_opt hide_use_err.%middle hide_err.%middle %n.cj -o %n.%suffix -Woff all

import ohos.dep.*

main() {
    var a = A()
    0
}
