// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: pa pb
// EXEC: %compiler %cmp_opt %compile_lib_opt -p pa -o libpa.a
// ERRCHECK: %compiler %cmp_opt --diagnostic-format=json libpa.a -p pb
// ASSERT: regex sema_mismatched_types
