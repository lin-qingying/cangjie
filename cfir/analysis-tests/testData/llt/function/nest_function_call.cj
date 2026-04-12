// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

func F(a: Int64): Int64 {
    return a
}

main() {
    let a: Int64 = 1
    // 40 F
    var b = F(
        F(
            F(
                F(
                    F(
                        F(
                            F(
                                F(
                                    F(
                                        F(
                                            F(
                                                F(
                                                    F(
                                                        F(
                                                            F(
                                                                F(
                                                                    F(
                                                                        F(
                                                                            F(
                                                                                F(
                                                                                    F(
                                                                                        F(
                                                                                            F(
                                                                                                F(
                                                                                                    F(
                                                                                                        F(
                                                                                                            F(
                                                                                                                F(
                                                                                                                    F(
                                                                                                                        F(
                                                                                                                            F(
                                                                                                                                F(
                                                                                                                                    F(
                                                                                                                                        F(
                                                                                                                                            F(
                                                                                                                                                F(
                                                                                                                                                    F(
                                                                                                                                                        F(
                                                                                                                                                            F(
                                                                                                                                                                F(
                                                                                                                                                                    F(
                                                                                                                                                                        F(
                                                                                                                                                                            a))))))))))))))))))))))))))))))))))))))))))
    println(b)
    0
}
