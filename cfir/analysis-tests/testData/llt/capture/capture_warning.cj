// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


let x = 4

<!chir_dce_unused_function!>private func f() {
    <!chir_dce_unused_function!>func f1() {
        <!CAPTURE_HAS_SHADOW_VARIABLE!>x<!>
    }<!>
    let <!chir_dce_unused_variable!>a<!> = {
        => <!CAPTURE_HAS_SHADOW_VARIABLE!>x<!>
    }
    let <!chir_dce_unused_variable!>x<!> = 2
}<!>

main() {
    0
}
