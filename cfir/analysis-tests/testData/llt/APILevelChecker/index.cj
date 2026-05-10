// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: %n.mac.cj
// EXEC: %compiler %n.mac.cj --compile-macro
// ERRCHECK: %compiler %f %compile_lib_opt %cmp_opt

import mm.*

@b
func build() {
    <!USE_EXPR_WITHOUT_IMPORT!>@IfAvailable(level:19, {=> }, {=> })<!>
}

func bar() {
    <!USE_EXPR_WITHOUT_IMPORT!>@IfAvailable(level:19, {=> }, {=> })<!>
}
