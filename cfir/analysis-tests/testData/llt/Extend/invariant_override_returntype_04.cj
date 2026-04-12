// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// LEVEL: 0
// ERRCHECK: %compiler %cmp_opt %f -o %output
// (CJNATIVE) ASSERT: scan (package std.core)string.cj
// ASSERT: scan invariant_override_returntype_04.cj

public interface Cloneable {
    func clone(): Cloneable
}

public interface LiteralValue <: Cloneable {
    func clone(): LiteralValue // Compiler error: return type of 'clone' can only be class/interface type which implements or inherits the interface type 'Interface-LiteralValue'
}

extend String <: LiteralValue {}

main() {}
