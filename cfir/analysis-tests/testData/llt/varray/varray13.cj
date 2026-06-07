// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.
// DIAGNOSTICS: -warnings


func foo() {
    var i = 0
    while (i < 5) {
        i++
        <!chir_dce_unreachable_expression_hint!>var <!chir_dce_unused_variable!>arr1<!>: VArray<Nothing, $5> = VArray<Nothing, $5>(repeat: continue)<!>
    }
    return 0
}

main() {
    return foo()
}
