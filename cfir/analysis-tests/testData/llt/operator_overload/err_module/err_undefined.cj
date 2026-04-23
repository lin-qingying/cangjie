// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: mod1

internal import mod1.pkg.a

main() {
    let _ = <!UNRESOLVED_REFERENCE!>mod1<!> * a
}
