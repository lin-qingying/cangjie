// DEPENDENCE: testa.cj
// EXEC: %compiler %noColor %compile_lib_opt %f testa.cj -o %output -j1

package testa

private class A {}

public class C {
    private func f(a: A) {}
}

// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.
