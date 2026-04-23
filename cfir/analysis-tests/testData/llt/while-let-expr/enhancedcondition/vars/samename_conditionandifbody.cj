// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

main() {
    let a = Some(1)
    while (let Some(b) <- a) {
        let <!REDECLARATION!>b<!> = <!UNRESOLVED_REFERENCE!>b<!>
        break
    }
}
