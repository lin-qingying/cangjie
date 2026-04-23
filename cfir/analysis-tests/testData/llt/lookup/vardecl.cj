// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

let glo = 123

func a() {
    let d = <!UNRESOLVED_REFERENCE!>b<!>
    let glu = glo

    let b = "hello"
}
