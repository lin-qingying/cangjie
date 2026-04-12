// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

#include <stdio.h>

#define MAX_INDEX 3

void c_f1(int* a)
{
    for (int i = 0; i < MAX_INDEX; ++i) {
        printf("%d ", a[i]);
    }
    printf("\n");
}

void c_f2(int a[MAX_INDEX])
{
    for (int i = 0; i < MAX_INDEX; ++i) {
        printf("%d ", a[i]);
    }
    printf("\n");
}

void c_f3(int (*a)[2])
{
    printf("a[1][1]: %d ", a[1][1]);
    printf("\n");
}

void c_f4(int a[][2])
{
    printf("a[1][1]: %d ", a[1][1]);
    printf("\n");
}