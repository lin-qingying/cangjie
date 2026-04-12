// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: %n.pkg.cj
// EXEC: %compiler %n.pkg.cj %compile_lib_opt -o lib%n.%lib_extension
// EXEC: %compiler %f lib%n.%lib_extension

import a.*

@F
extend Int8 {}

@F
func foo() {}

@F
public var a = 3

main() {}
