// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: pkg1.cj pkg2.cj

internal import pkg2.y

// Should report "not member of package". NOT "pkg2.x is not imported"
main() {
    println(pkg2.<!UNRESOLVED_REFERENCE!>x<!>)
}
