// DEPENDENCE: macro.cj
// EXEC: %compiler macro.cj %cmp_macro
// EXEC-PIPE:: %run_macro %compiler %cmp_opt %macro_lib %f -o %output

import macro_definition

@macro_definition.ModifyFunc func testA() {
}
main() {}
/* SCAN
*/

// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.
