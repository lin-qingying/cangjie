// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

public interface I {
    func append(_: String): Any
}

extend StringBuilder <: I {}

main() {}

// ASSERT: scan error: return type of 'append' can only be class/interface type which implements or inherits the interface type 'Interface-Any'
// ASSERT: scan    ==> (package std.core)string_builder.cj
// ASSERT: scan note: cannot override/implement the following function
// ASSERT: scan  ==> invariant_override_returntype_03.cj:10:5:
// ASSERT: scan 1 error generated, 1 error printed.
