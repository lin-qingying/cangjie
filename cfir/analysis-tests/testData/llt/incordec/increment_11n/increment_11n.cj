// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// (Unix) DEPENDENCE: src expectation_linux/increment_11n.expect.txt
// (Windows) DEPENDENCE: src expectation_win\increment_11n.expect.txt
// EXEC: %compiler %cmp_opt %compile_lib_opt src/pkgb/pkgb.cj -o ./pkgb.%middle
// EXEC-1: %frontendCompiler %cmp_opt %typecheck ./pkgb.%middle src/pkga/pkga.cj -o ./pkga.%middle >%n.txt 2>&1
// EXEC: %diff %n.txt increment_11n.expect.txt
