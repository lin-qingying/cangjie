// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

#include <stdint.h>
#include <stdlib.h>

int64_t* ArrayGen() {
    int64_t* ret = (int64_t*) malloc(sizeof(int64_t) * 10);
    for (int i = 0; i < 10; i++) {
        ret[i] = i;
    }
    return ret;
}

int64_t* ArrayAdd(int64_t* ret) {
    for (int i = 0; i < 10; i++) {
        ret[i]++;
    }
    return ret;
}