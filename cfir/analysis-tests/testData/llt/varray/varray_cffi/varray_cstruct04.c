// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

#include <stdio.h>
#include <stdint.h>

struct CStruct {
    int8_t c; // 1 byte
    int g[3];   // 12 byte
};

void f(struct CStruct s)
{
    printf("%d %d %d\n", s.g[0], s.g[1], s.g[2]);
}

unsigned long size_of_CStruct()
{
    printf("CStruct size is %lu\n", sizeof(struct CStruct));
    return sizeof(struct CStruct);
}
