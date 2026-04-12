// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

func f1(x: Rune) {
    match (x) {
        case r'仓' => 0
        case _ => 1
    }
}

func f2(x: Rune) {
    match (x) {
        case r'仓' => 0
        case _ => 1
    }
}

func f3(x: Rune) {
    match (x) {
        case '仓' => 0
        case _ => 1
    }
}

func f4(x: Rune) {
    match (x) {
        case "仓" => 0
        case _ => 1
    }
}

func f5(x: Rune) {
    match (x) {
        case '''仓''' => 0
        case _ => 1
    }
}

func f6(x: Rune) {
    match (x) {
        case """仓""" => 0
        case _ => 1
    }
}

main() {
    f1(r'仓') + f2(r'仓') + f3(r'仓') + f4(r'仓') + f5(r'仓') + f6(r'仓')
}
