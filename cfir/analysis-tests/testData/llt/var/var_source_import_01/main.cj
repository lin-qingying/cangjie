// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: pkg.cj

// Test for correct import static let var EOF from instantiated decl
internal import pkg.*

main() {
    let x = A<Rune>.EOF
}
