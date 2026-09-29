# CJMP 官方版本取证（2026-09-29）

本记录由 CangjieSemanticsAuthority 读取官方 Git 对象得到，用于修正
`docs/cjmp-framework-review-and-fix-plan-20260928.md` 附录 A。
本次未修改 external，未运行 Gradle；本文通过 apply_patch 写入。版本差异来自源码取证；随后新增的 12 组 P1 cjc 1.1.3 实测记录位于 `p1-cjc-20260929/README.md`，精确命令和原始 JSON 位于同目录。

## 1. 取证范围与版本身份

- 仓库：`D:/code/intellij/cangjie/external/cangjie_compiler`。
- 工作树 HEAD：`b776b44c26611eac105b9f17e69e6097f68a4b68`，标签 **v1.0.0**。
- 远端追踪引用 `origin/main`：`799e9f6545cc8a83355c5d77e777f8f571215815`，2026-09-22T12:48:23+08:00，提交标题 `!2120 merge enhance-cjmp-perf into main`。
- **本地共 46 个 tag，不是 47 个**；清单和对象日期见 §3。此次没有 fetch，不声称覆盖远端当前可能新增的 tag。
- 读取形式是 `git show <tag>:<path>`、`git grep <tag> -- <path>`；不能把 external 工作树文件当作 main。
- 本记录的“最早 tag”指本地现存 tag 中源码已体现该语义的首个相关发行序列节点。Git 作者日期、提交日期、tag 指向提交日期分别列出，不把作者日期冒充发布日。
- `git tag --contains` 是祖先关系证据，不等于语义存在性：例如 v1.1.3 包含 specific 语义，但不包含关键词改名原提交 acd6457；release 分支合入/重放使二者不同。§3 的源码矩阵优先于祖先推断。

## 2. 主计划附录必须更正的事实

| 原条目 | 核实结果 | 准确边界 |
|---|---|---|
| A.2 main 列宣称回退单 common、没有 features/options 门、旧注解表 | 错误。origin/main 保留多 common、features/options 门及扩展 unsupported/ObjC 表 | v1.2.0-alpha.20 开始，直到本地最新 main |
| A.2 #2–7 只写“1.2 alpha” | 精度不足。alpha.06/.07/.08/.16/.17/.18/.19 都没有这些新增项 | 最早 v1.2.0-alpha.20；共同引入提交 7a9258ae8e69f49d234f971ca8e7dd117d1c3470 |
| A.2 #5 把 Specific 判据统一描述为 CJO→CHIR | 混淆前端和驱动两种事实。1.1.3 parser/matcher 用 commonPartCjo；驱动早已用 inputChirFiles | 1.2-alpha.20 前端变 commonPartCjos，驱动变 commonPartChirs，不能只用单一“SpecificModeByChirInputs”门解释 |
| A.2 #9 声称 module_version_not_identical 全版本死诊断 | 错误。v1.3.0-alpha.05/main 已在普通和 common CJO 加载中调用该诊断 | 最早 v1.3.0-alpha.05；提交 12883f2bd87561ccebea71fa5b01f7ea2a7ddde6 |
| A.2 #7 把 JAVA_MIRROR 一直当属性位注解表项 | 1.3-alpha.02 起删除该表项；属于 Java 属性模型迁移，不能据此推导 JavaMirror 可任意不匹配 | 最早 v1.3.0-alpha.02；提交 1b67893f23c3ed54c60cf2fa36a4d5893fc3232d |
| A.2 #1 把全部 1.1 预发布形态统称 common/specific | 早期 tag 使用 common/platform；泛型规则、默认参数规则、后置返回检查也经历变化 | 关键词 specific 最早本地 tag v1.1.0-beta.20；较早 alpha.34–.70 使用 platform |
| 附录 F5 的 Box 泛型数量诊断引用 CheckCJMP.cpp:984 | 报错结论正确，nominal 的直接 owner 引用不准 | CheckCommonSpecificGenericMatch→CheckGenericTypeBoundsMapped；详见 §7 |
| A.2 未登记同一文件的 common/specific 规则改变 | 1.2-alpha.20 起两种修饰符都只检查是否处于任一 CJMP 编译模式，不再拒绝同文件混合 | ParseCJMPDecl.cpp:107–130，提交 7a9258a |

上表只列本次已逐行核实的版本差异，不声称这些列穷尽全部官方 CJMP 历史。早期预发布若纳入实现目标，不能用一个 CANGJIE_1_1_0 或 CANGJIE_1_2_0 常量假装分辨所有 alpha。

## 3. 全部 46 个 tag 的对象、日期与源码分组

分组含义：

- A：无 CJMP 文件。
- B：common/platform；解析期直接禁止 CJMP 泛型；没有 CheckMatchedFunctionReturnTypes。
- C：common/platform；不再直接禁止 CJMP 泛型；尚无 CheckMatchedFunctionReturnTypes（alpha.66）。
- D：common/platform；有返回类型后检查；旧单 common 模型（alpha.68/.69/.70）。
- E：common/specific；返回类型后检查；单 common；没有新增 features/options/CJO 格式版本门。
- F：common/specific；多 common、features/options 门、扩展 unsupported 和 ObjC 表；仍含 JAVA_MIRROR 位表项；没有 CJO 格式版本门。
- G：与 F 相同，但移除 JAVA_MIRROR 位表项；仍无 CJO 格式版本门。
- H：与 G 相同，新增 CJO 格式版本门。

所有 A 组以外的分组均有 CJMP；分组按源码行为，不按标签字符串字典序。下列日期来自 `git log -1 <tag> --format='%H|%aI|%cI'`，是 tag 指向的提交日期，不是 tag 创建时间。

| tag | 指向提交 | 作者日期 | 提交日期 | 组 |
|---|---|---|---|---|
| v1.0.0 | `b776b44c26611eac105b9f17e69e6097f68a4b68` | 2025-08-08T17:20:56+08:00 | 2025-08-08T17:20:56+08:00 | A |
| v1.0.2 | `1769691cdbfbfbcaa0204f85e5b425d3310de43f` | 2025-09-25T12:41:26+08:00 | 2025-09-25T13:04:36+08:00 | A |
| v1.0.3-beta | `18b9e3ab06953abdb6f8a5c64a40f9fe5139c79c` | 2025-10-11T10:54:07+08:00 | 2025-10-13T21:07:48+08:00 | A |
| v1.0.5 | `0e5b7acb07558a9084048ceff9faef164157dcc7` | 2025-12-05T15:35:13+08:00 | 2025-12-05T15:35:13+08:00 | A |
| v1.1.0 | `74452c8ee3387e4406f005a72ddd14926b52306f` | 2026-03-28T19:26:00+08:00 | 2026-03-28T19:26:00+08:00 | E |
| v1.1.0-alpha.34 | `5aafb7254ec30166664a89b2f1475d6da1acd491` | 2025-11-28T17:46:39+08:00 | 2025-11-28T17:46:39+08:00 | B |
| v1.1.0-alpha.35 | `d90759546fd021d610b606528c361bc1e594ff29` | 2025-12-02T20:20:02+08:00 | 2025-12-02T20:20:02+08:00 | B |
| v1.1.0-alpha.36 | `cc703c93aaeadc7175fa1722d810862fbd31f383` | 2025-12-05T12:19:24+08:00 | 2025-12-05T12:19:24+08:00 | B |
| v1.1.0-alpha.37 | `1078ae4896483e75f45be574e991d113fdc7bfde` | 2025-12-05T21:54:55+08:00 | 2025-12-05T21:54:55+08:00 | B |
| v1.1.0-alpha.44 | `ece6fe64273887610609ec2d583a588d746b36ca` | 2025-12-12T14:09:22+08:00 | 2025-12-12T14:09:22+08:00 | B |
| v1.1.0-alpha.55 | `fec5dda0420756187b871431971a1f29310aebc5` | 2025-12-26T10:43:58+08:00 | 2025-12-26T10:43:58+08:00 | B |
| v1.1.0-alpha.65 | `1ef724bfebc8860a102a9001cb210cbebccc5e3b` | 2026-01-14T12:09:31+08:00 | 2026-01-14T12:09:31+08:00 | B |
| v1.1.0-alpha.66 | `db9df81692a066643526c0f2d8581a88ceb66401` | 2026-01-21T15:39:02+08:00 | 2026-01-21T15:39:02+08:00 | C |
| v1.1.0-alpha.68 | `01e46e768814d0e7bd6913831dd873577f60db7b` | 2026-01-31T16:12:19+08:00 | 2026-01-31T16:12:19+08:00 | D |
| v1.1.0-alpha.69 | `fc766524a601455d5a3db656fb51bb68aebee58a` | 2026-02-05T15:41:51+08:00 | 2026-02-05T15:41:51+08:00 | D |
| v1.1.0-alpha.70 | `0482d3bbe618ead31e5e970d198400ca3bbbfca6` | 2026-02-11T10:35:52+08:00 | 2026-02-11T10:35:52+08:00 | D |
| v1.1.0-beta.1 | `3400d4818bc22c670b02e14ffd9a7ca8e42af692` | 2025-11-21T09:37:55+08:00 | 2025-11-21T09:37:55+08:00 | B |
| v1.1.0-beta.20 | `19062335b0f44d5454bf529dc2e05cf51e7f4360` | 2026-02-27T14:42:52+08:00 | 2026-02-27T14:42:52+08:00 | E |
| v1.1.0-beta.21 | `640d8c46c3d480b5674aa43c5341880d0123c99c` | 2026-03-03T17:59:17+08:00 | 2026-03-03T17:59:17+08:00 | E |
| v1.1.0-beta.23 | `0d7538c6e92c2de0e0281be499a111e7538b4271` | 2026-03-09T10:23:10+08:00 | 2026-03-09T10:23:10+08:00 | E |
| v1.1.0-beta.24 | `539a1ae95525c7143ec362ba4f2ab2712d4d7665` | 2026-03-16T12:04:35+08:00 | 2026-03-16T12:04:35+08:00 | E |
| v1.1.0-beta.25 | `5c2a51451fe3c53624c14a26a8a22227f41e64a4` | 2026-03-23T16:06:11+08:00 | 2026-03-23T16:06:11+08:00 | E |
| v1.1.0.beta.0 | `b376a161c4a89720e7c125cbbd882ec74f272caa` | 2025-11-10T09:33:39+08:00 | 2025-11-10T09:33:39+08:00 | B |
| v1.1.1 | `dafb3074984558f568c8804b74a5161e327243a2` | 2026-04-10T11:38:35+08:00 | 2026-04-10T11:38:35+08:00 | E |
| v1.1.2 | `64134bfb07bccc956b10f8ece3f7ac799ab3da22` | 2026-04-28T15:19:09+08:00 | 2026-04-28T15:19:09+08:00 | E |
| v1.1.3 | `14276740ffec1262b81b6b5236b1d840af270b37` | 2026-05-14T19:05:03+08:00 | 2026-05-14T19:05:03+08:00 | E |
| v1.2.0-alpha.06 | `90a739d52c56b7787acedd5af2c7c7d9d08d9c42` | 2026-04-02T11:25:58+08:00 | 2026-04-02T11:25:58+08:00 | E |
| v1.2.0-alpha.07 | `eebbb5a44d4bb16cc1146668438eb5c3facfe41d` | 2026-04-16T11:46:04+08:00 | 2026-04-16T11:46:04+08:00 | E |
| v1.2.0-alpha.08 | `38863da441711c0f4701a5932c980c560b97f94f` | 2026-04-28T16:08:02+08:00 | 2026-04-28T16:08:02+08:00 | E |
| v1.2.0-alpha.16 | `9c9ef2997e1f55f6dda62585253f41a85f6656db` | 2026-04-30T11:25:06+08:00 | 2026-04-30T11:25:06+08:00 | E |
| v1.2.0-alpha.17 | `2d86b1405e18b41dc36db0a3dbd71b96c0cb1be1` | 2026-05-07T14:56:29+08:00 | 2026-05-07T14:56:29+08:00 | E |
| v1.2.0-alpha.18 | `8177dde9ee5055d1149842f68e342e7698736d69` | 2026-05-14T14:57:58+08:00 | 2026-05-14T14:57:58+08:00 | E |
| v1.2.0-alpha.19 | `8154d39188b1ce285557a8e9af244782ad266838` | 2026-05-21T11:42:28+08:00 | 2026-05-21T11:42:28+08:00 | E |
| v1.2.0-alpha.20 | `4a3970486e6ff08ab35b4bf06e1b2e77e248f571` | 2026-06-05T12:25:05+08:00 | 2026-06-05T12:25:05+08:00 | F |
| v1.2.0-alpha.21 | `21fb7125daab9447c90c02b4811b76e85291b5e4` | 2026-06-08T17:52:44+08:00 | 2026-06-08T17:52:44+08:00 | F |
| v1.2.0-alpha.22 | `39c993de63a5802c5d572a3341ab6ae82e7def88` | 2026-06-12T11:41:30+08:00 | 2026-06-12T11:41:30+08:00 | F |
| v1.2.0-beta.01 | `0126ae596f99e60aa04234807cb77f9ac006ad64` | 2026-06-18T15:46:01+08:00 | 2026-06-18T15:46:01+08:00 | F |
| v1.2.0-beta.02 | `2a7fba413e2176d79c5707ea4d4e05cc90d39e1f` | 2026-06-25T16:57:53+08:00 | 2026-06-25T16:57:53+08:00 | F |
| v1.2.0-beta.03 | `da369996140e49c504093fd30ea940b91d15d0f3` | 2026-07-03T17:27:43+08:00 | 2026-07-03T17:27:43+08:00 | F |
| v1.2.0-beta.rc | `4acb71771eae43b7f8c1bf542e9fdac05a21b963` | 2026-07-10T17:44:02+08:00 | 2026-07-10T17:44:02+08:00 | F |
| v1.2.0-beta.rc1 | `9c719681c66201ecd46acf7f5358c6acf573f35f` | 2026-07-17T15:19:56+08:00 | 2026-07-17T15:19:56+08:00 | F |
| v1.2.0-beta.rc2 | `c1e7b009f26e7bed73958ee7c49607ff67fe2fe3` | 2026-07-22T17:59:09+08:00 | 2026-07-22T17:59:09+08:00 | F |
| v1.3.0-alpha.02 | `59680833b32ad309694993406d728716d8c65c53` | 2026-08-20T11:26:35+08:00 | 2026-08-20T11:26:35+08:00 | G |
| v1.3.0-alpha.03 | `29f8d9531e88a2d4187014700411cf02468ba759` | 2026-08-28T11:57:40+08:00 | 2026-08-28T11:57:40+08:00 | G |
| v1.3.0-alpha.04 | `34be950e69a5d7f89e49af48efa068d3d85275ad` | 2026-09-10T11:49:07+08:00 | 2026-09-10T11:49:07+08:00 | G |
| v1.3.0-alpha.05 | `3bc845c6c759fb610d0d7ffd434a61d1cab8bc8c` | 2026-09-17T13:16:03+08:00 | 2026-09-17T13:16:03+08:00 | H |

## 4. 引入提交与完整 tag-contains 证据

### `fc4b011fd673aaab7c5c25944b4b02fb42d276a9`

- 提交：feat: add some new features
- 作者日期：2025-10-09T15:26:17+08:00
- 提交日期：2025-10-24T01:38:36+08:00
- `git tag --contains` 返回 41 个 tag：

```text
v1.1.0
v1.1.0-alpha.34
v1.1.0-alpha.35
v1.1.0-alpha.36
v1.1.0-alpha.37
v1.1.0-alpha.44
v1.1.0-alpha.55
v1.1.0-alpha.65
v1.1.0-alpha.66
v1.1.0-alpha.68
v1.1.0-alpha.69
v1.1.0-alpha.70
v1.1.0-beta.1
v1.1.0-beta.20
v1.1.0-beta.21
v1.1.0-beta.23
v1.1.0-beta.24
v1.1.0-beta.25
v1.1.0.beta.0
v1.1.1
v1.1.2
v1.2.0-alpha.06
v1.2.0-alpha.07
v1.2.0-alpha.08
v1.2.0-alpha.16
v1.2.0-alpha.17
v1.2.0-alpha.18
v1.2.0-alpha.19
v1.2.0-alpha.20
v1.2.0-alpha.21
v1.2.0-alpha.22
v1.2.0-beta.01
v1.2.0-beta.02
v1.2.0-beta.03
v1.2.0-beta.rc
v1.2.0-beta.rc1
v1.2.0-beta.rc2
v1.3.0-alpha.02
v1.3.0-alpha.03
v1.3.0-alpha.04
v1.3.0-alpha.05
```

### `acd64576b0653987272f458111475d1321e0fcc0`

- 提交：feat(cjmp): replace the keyword 'platform' with 'specific' for cjmp
- 作者日期：2026-02-19T13:20:13+03:00
- 提交日期：2026-02-19T13:20:13+03:00
- `git tag --contains` 返回 28 个 tag：

```text
v1.1.0
v1.1.0-beta.20
v1.1.0-beta.21
v1.1.0-beta.23
v1.1.0-beta.24
v1.1.0-beta.25
v1.1.1
v1.1.2
v1.2.0-alpha.06
v1.2.0-alpha.07
v1.2.0-alpha.08
v1.2.0-alpha.16
v1.2.0-alpha.17
v1.2.0-alpha.18
v1.2.0-alpha.19
v1.2.0-alpha.20
v1.2.0-alpha.21
v1.2.0-alpha.22
v1.2.0-beta.01
v1.2.0-beta.02
v1.2.0-beta.03
v1.2.0-beta.rc
v1.2.0-beta.rc1
v1.2.0-beta.rc2
v1.3.0-alpha.02
v1.3.0-alpha.03
v1.3.0-alpha.04
v1.3.0-alpha.05
```

### `1320e542afadce1a54db9f402f39820e7e822515`

- 提交：fix: move common/specific matching to an earlier sema stage
- 作者日期：2025-12-19T16:47:11+03:00
- 提交日期：2026-01-20T20:25:32+03:00
- `git tag --contains` 返回 31 个 tag：

```text
v1.1.0
v1.1.0-alpha.68
v1.1.0-alpha.69
v1.1.0-alpha.70
v1.1.0-beta.20
v1.1.0-beta.21
v1.1.0-beta.23
v1.1.0-beta.24
v1.1.0-beta.25
v1.1.1
v1.1.2
v1.2.0-alpha.06
v1.2.0-alpha.07
v1.2.0-alpha.08
v1.2.0-alpha.16
v1.2.0-alpha.17
v1.2.0-alpha.18
v1.2.0-alpha.19
v1.2.0-alpha.20
v1.2.0-alpha.21
v1.2.0-alpha.22
v1.2.0-beta.01
v1.2.0-beta.02
v1.2.0-beta.03
v1.2.0-beta.rc
v1.2.0-beta.rc1
v1.2.0-beta.rc2
v1.3.0-alpha.02
v1.3.0-alpha.03
v1.3.0-alpha.04
v1.3.0-alpha.05
```

### `7a9258ae8e69f49d234f971ca8e7dd117d1c3470`

- 提交：feat: sync dev to main
- 作者日期：2026-05-25T09:16:56+08:00
- 提交日期：2026-05-25T09:16:56+08:00
- `git tag --contains` 返回 13 个 tag：

```text
v1.2.0-alpha.20
v1.2.0-alpha.21
v1.2.0-alpha.22
v1.2.0-beta.01
v1.2.0-beta.02
v1.2.0-beta.03
v1.2.0-beta.rc
v1.2.0-beta.rc1
v1.2.0-beta.rc2
v1.3.0-alpha.02
v1.3.0-alpha.03
v1.3.0-alpha.04
v1.3.0-alpha.05
```

### `1b67893f23c3ed54c60cf2fa36a4d5893fc3232d`

- 提交：fix: java mirror and java impl attributes inference with caching fields in ast decl
- 作者日期：2026-07-27T16:54:28+03:00
- 提交日期：2026-07-28T17:34:55+03:00
- `git tag --contains` 返回 4 个 tag：

```text
v1.3.0-alpha.02
v1.3.0-alpha.03
v1.3.0-alpha.04
v1.3.0-alpha.05
```

### `12883f2bd87561ccebea71fa5b01f7ea2a7ddde6`

- 提交：feat: support cjo version checker
- 作者日期：2026-09-03T12:45:59Z
- 提交日期：2026-09-16T14:45:36Z
- `git tag --contains` 返回 1 个 tag：

```text
v1.3.0-alpha.05
```

## 5. 当前版本门的准确语义与源码

以下无前缀的路径均相对于官方仓库；行号必须与所列 ref 一起使用。

### 5.1 多 common、features、options、模式来源

v1.1.3：

- `include/cangjie/Option/Option.h:473`：单个 `optional<string> commonPartCjo`。
- `src/Parse/ParseCJMPDecl.cpp:102–105`：parser 的 compileCommon = outputMode CHIR；compileSpecific = commonPartCjo != nullopt。
- `src/Sema/CJMP/CheckCJMP.cpp:39–42`：matcher 的模式使用同样的 CJO 判据。
- `include/cangjie/Option/Option.h:1145–1152`：驱动 `IsCompilingCJMPSpecific()` 已经使用 `inputChirFiles.size() > 0`。
- `src/Modules/ASTSerialization/ASTLoader.cpp:298–331`：common 加载包名/文件逻辑，不存在 ASTLoaderCJMP.cpp 中的新 features/options 门。

v1.2.0-alpha.20：

- `include/cangjie/Option/Option.h:482–484`：commonPartCjos/commonPartChirs 两个 vector；`IsCompilingCJMPSpecific` 返回 commonPartChirs.size() > 0。
- `src/Parse/ParseCJMPDecl.cpp:298–313`：CompileCommon 看 CHIR 输出；CompilePlatform 看 commonPartCjos.size() > 0。
- `src/Parse/ParseCJMPDecl.cpp:107–130`：COMMON/SPECIFIC 都要求 CompilePlatform() || CompileCommon()，旧的“同文件不能同时 common/specific”检查已移除。
- `src/Sema/CJMP/CheckCJMP.cpp:37–41`：compilePlatform 看 CJO 数量，severalParents 看数量 > 1。
- `src/Sema/CJMP/CheckCJMP.cpp:1078–1098`：severalParents 时不会在第一个 matched 后 break。**这里是遍历全部候选的规则，本函数没有按 parent 分组并“每个 parent first-fit 一次”的结构。** 不能把表格简写解释为官方存在该额外分组。
- `src/Modules/ASTSerialization/ASTLoaderCJMP.cpp:61–97`：child/specific features 来自第一份源码文件，parent features 必须是 child features 子集，否则报 feature_is_not_subset_of_child_set，附 common 差集和 child feature note。
- 同文件 `:158–182`：没有 options 时发 module_common_cjo_no_options 后返回 true；debug/opt 不同发对应诊断并返回 false。
- 同文件 `:185–237`：VerifyForData→包名检查→features 验证/加载文件→ValidateOptions→后续 imports。此版没有 CheckCjoVersion。
- 相同源结构延续到 beta.rc2、1.3 alpha 和 origin/main；main 的版本门另见下节。

### 5.2 CJO 格式版本门：1.3-alpha.05 起

`origin/main:src/Modules/ASTSerialization/ASTLoader.cpp:52–66,352–387`：

1. 缺少 cjoVersion：不兼容。
2. producer CJO major 必须与当前 CJO_MAJOR_VERSION 相等。
3. 同 major 下，producer minor 必须 <= 当前 CJO_MINOR_VERSION。
4. patch 不参与兼容性判断。
5. 不兼容诊断为 module_version_not_identical，展示 producer/current 的编译器版本供用户理解；**判断依据不是 CANGJIE_VERSION 字符串相等**。
6. `ASTLoaderCJMP.cpp:185–195` common 加载在读 package 后立即调用 CheckCjoVersion；普通 ASTLoader 的依赖/包加载同样调用。

v1.3.0-alpha.04 没有此门，v1.3.0-alpha.05 有；不是“全版本都是死诊断”，也不是“1.2 引入版本门”。

### 5.3 注解表

- v1.1.3 `src/Sema/CJMP/CheckCJMPAnnotations.cpp`：属性位表为 C、JAVA_MIRROR、JAVA_HAS_DEFAULT、OBJ_C_MIRROR；unsupported 为 JAVA、CALLING_CONV、CONSTSAFE、ENSURE_PREPARED_TO_MOCK、UNKNOWN。
- v1.2.0-alpha.20 起：属性位表增加 OBJ_C_INIT、OBJ_C_OPTIONAL；unsupported 增加 FOREIGN_GETTER_NAME、FOREIGN_SETTER_NAME、NON_PRODUCT。
- v1.3.0-alpha.02 起：属性位表删除 JAVA_MIRROR，其他上述新项仍在；原提交涉及 JavaMirror/JavaImpl 属性推导模型。
- `origin/main:src/Sema/CJMP/CheckCJMPAnnotations.cpp:42–53` 给出当前完整表；`:171–195` 属性不一致双向报告；`:268–286` 普通注解双向匹配。

### 5.4 关键词与早期预发布

- 按 tag 指向提交日期，最早现存 CJMP tag 是名称特殊的 v1.1.0.beta.0（2025-11-10），之后有 v1.1.0-beta.1（2025-11-21）；alpha.34 指向 2025-11-28，不能把标签字典序当时间顺序。源码的 CJMP 文件可通过 follow 追溯到 fc4b011（作者 2025-10-09，提交 2025-10-24）。
- alpha.34 的 `ParseCJMPDecl.cpp:103–139,174–187,226–237` 使用 PLATFORM，禁止 CJMP 泛型并禁止 platform 函数参数默认值。
- alpha.66 已去除 parse_cjmp_generic_decl 的直接拒绝，并已有“双方默认值不得同时存在”的 matcher 诊断；alpha.68 起有 CheckMatchedFunctionReturnTypes。
- 1320e542 的标题明确是“move common/specific matching to an earlier sema stage”；作者日期早于真正合入日期，不能用作者日期判断哪个 tag 已包含。
- acd6457 将 platform 改名 specific；最早源码含 specific 的本地 tag 是 v1.1.0-beta.20。
- v1.1.0 正式版到 v1.1.3 的相关 P1 返回处理路径一致，但这不意味着较早 1.1 alpha 的语义相同。

## 6. P1：双方隐式返回、配对时机与后置检查

### 6.1 common 可以省略返回类型的条件

v1.1.3 `src/Parse/ParseDecl.cpp:2037–2044`：

```cpp
bool hasNoBody = !ret->funcBody->body;
bool hasNoReturnType = !ret->funcBody->retType;
if (ret->TestAttr(Attribute::COMMON) && hasNoReturnType && hasNoBody) {
    ParseDiagnoseRefactor(DiagKindRefactor::parse_common_function_must_have_return_type, *ret);
}
if (ret->TestAttr(Attribute::SPECIFIC) && hasNoReturnType && hasNoBody) {
    ParseDiagnoseRefactor(DiagKindRefactor::parse_specific_function_must_have_return_type, *ret);
}
```

因此带函数体的 common 与 specific 都可以省略返回类型；无函数体者必须显式声明返回类型。
`ParseCJMPDecl.cpp:24–35,57–65` 的 HasDefault 因 body 为 true，给 common 加 COMMON_WITH_DEFAULT。
origin/main `ParseDecl.cpp:1973–1979` 仍是上述条件。

### 6.2 配对前的类型表示与边界

- v1.1.3 `src/Sema/PreCheck.cpp:1774–1802`：返回默认设 Quest；有显式 retType 就取其类型；有空 body 则预先设 Unit；构造器另取所在类型或 Unit。这一逻辑不区分 common/specific。
- `src/Sema/TypeChecker.cpp:2027–2047`：PrepareTypeCheck→PreCheck→CollectDeclsWithMember→MatchSpecificWithCommon；主体 TypeCheck 在之后。
- `CheckCJMP.cpp:915–933`：generic 映射后走 IsFuncTySubType，或 IsFuncDeclSubType。
- `TypeManager.cpp:1962–1964`：参数类型 identical，返回做 IsSubtype。
- `TypeManager.cpp:996–1003`：先排无效类型；**任一侧真实 Quest 返回 true**。这不是“所有未取得类型/错误类型一律放行”。
- 因而 LL 为避免在配对锁内推进 common IMPLICIT，可以让双方真实尚未推断的返回延后兼容判断；最终统一后检查。无需仅为 matcher 强行解析 common 的函数体。
- 空函数体的省略返回已预先为 Unit，与一般 Quest 不同，不能因“无显式返回”而永久跳过具体类型复核。

### 6.3 后置检查及变量

- `TypeChecker.cpp:2064–2067`：PostTypeCheck 调 CheckReturnAndVariableTypes。
- `CheckCJMP.cpp:535–569`：已配对函数要求双方返回类型已 resolved；specific <: common；必要时经泛型映射重新匹配；不相容报 sema_return_type_incompatible。
- `CheckCJMP.cpp:604–619`：扫描所有有 specificImplementation 的 common FuncDecl，enum constructor 除外；没有“只检查 specific 原来省略类型”的限制，因此 common 隐式返回同样进入复核。
- `CheckCJMP.cpp:1050–1087`：specific 变量 initial 类型先延迟不等诊断但仍建立映射；`:571–591,622–639` 后验变量类型（等价，不是协变）。
- `CheckCJMP.cpp:948–963`：把 common 默认参数 assignment/desugarDecl 传给 specific，置 HAS_INITIAL。
- `TypeCheckCall.cpp:2300–2301`：调用候选经 RemoveCommonCandidatesIfHasSpecific 过滤。

docs MCP：`manual_source_zh_cn_define_functions`《定义函数》确认省略返回类型由函数体类型及全部 return 联合推断；空函数体为 Unit。CJMP 文档查询未命中，CJMP 特有规则以上述官方源码为依据。

A1 use3/use4 在旧目录中原先没有已运行日志。2026-09-29 已补 `p1-cjc-20260929` 的 12 组精确 cjc 1.1.3 探针：use3/use4、双侧隐式 Int64、双侧空体 Unit 均成功；推断后返回不兼容与配对前具体返回不兼容产生不同诊断，见该目录 README 和原始 JSON。两段式 common 已完成推断；任侧 Quest 的 LL 锁内行为仍以本节源码证据为准。

## 7. P2 候选与附录 F5 的 owner

### 7.1 COMMON 与 FROM_COMMON_PART 独立

均为 v1.1.3：

- `src/Modules/ASTSerialization/ASTWriter.cpp:1574–1577`：serializingCommon 给产出的每个声明加 FROM_COMMON_PART，不推导 COMMON。
- `ASTLoader.cpp:604–607`：CopyAttrs 原样恢复声明属性；FROM_COMMON_PART/deserializingCommon 参与是否加 IMPORTED。
- `ASTLoader.cpp:319–322`：common 加载把文件置 COMMON、FROM_COMMON_PART、isCommon；不能据此把每个声明置 COMMON。
- `src/Parse/ParseCJMPDecl.cpp:57–65`：仅已带 COMMON 的声明才补 COMMON_WITH_DEFAULT。
- `src/Sema/CJMP/CheckCJMP.cpp:668–693`：CollectDecl 只看 COMMON/SPECIFIC，跳过 ASTKind PRIMARY_CTOR_DECL。

### 7.2 不得把所有 primary constructor 排除

- `src/Sema/Utils.cpp:223–243`、`TypeChecker.cpp:2091–2093`：合并前先 desugar primary constructor。
- `src/Sema/Desugar/DesugarInTypeCheck.cpp:242–280`：生成 **FuncDecl init**，CloneAttrs(fd)，加 CONSTRUCTOR 和 PRIMARY_CONSTRUCTOR，放入类/结构体成员。
- 原始 PRIMARY_CTOR_DECL 不参与候选；携带 PRIMARY_CONSTRUCTOR 位的反糖 FuncDecl 则可以合法参与候选。二者不是同一个判据。
- `CheckCJMP.cpp:968–978` 还明确对 primary/init 配对做语义检查，证实 primary 函数不是应全部过滤的形态。
- `src/Parse/ParseDecl.cpp:1483–1488`：enum 的 COMMON/SPECIFIC 传播到载荷 FuncDecl 和无载荷 VarDecl 构造器。旧探针 p_b1 已验证 CJO 中两形态都有 COMMON+FROM_COMMON_PART，无 COMMON_WITH_DEFAULT。

### 7.3 F5：泛型数量不等

- `CheckCJMP.cpp:320–327`：合并时只跳过 generic/non-generic 不一致；两个都是泛型不会因数量不同在此跳过。
- nominal 检查调用链：`MatchCommonNominalDeclWithSpecific:802–858`→`CheckCommonSpecificGenericMatch:792–798`→`src/Sema/InheritanceChecker/StructInheritanceChecker.cpp:1219–1225 CheckGenericTypeBoundsMapped`。
- 后者直接比较 GenericsCount，数量不等发 sema_generic_member_type_argument_different。
- `CheckCJMP.cpp:982–985` 是函数匹配里的另一个同名诊断，不是 p_f5 的 Box nominal 根因。
- p_f5 实测错误结论保留；实现时应由声明级泛型约束检查 owner 报告，不应改写成 nominal 配对失败。

## 8. 复核命令形状

所有命令为只读，PowerShell 7；文本文件读取明确 UTF-8。

```powershell
git -C external/cangjie_compiler tag --list
git -C external/cangjie_compiler log -1 origin/main --format='%H %aI %cI %s'
git -C external/cangjie_compiler show v1.1.3:src/Sema/TypeManager.cpp
git -C external/cangjie_compiler show v1.2.0-alpha.20:src/Modules/ASTSerialization/ASTLoaderCJMP.cpp
git -C external/cangjie_compiler show origin/main:src/Modules/ASTSerialization/ASTLoader.cpp
git -C external/cangjie_compiler tag --contains 7a9258ae8e69f49d234f971ca8e7dd117d1c3470
git -C external/cangjie_compiler log --follow -S 'TokenKind::SPECIFIC' origin/main -- src/Parse/ParseCJMPDecl.cpp
```

## 9. 后续补充：早期 generic/default/后检查的精确提交

此处是 Git 源码取证，未运行早期 alpha 编译器。未知的其他预发布语义仍属 P0 待细化项。

| 变化 | 最早体现变化的本地 tag | 精确提交 | 作者日期 | 提交日期 |
|---|---|---|---|---|
| 去掉 common/platform 泛型声明一律报 parse_cjmp_generic_decl 的解析期禁令 | v1.1.0-alpha.66（alpha.65 尚有禁令） | `fcf6399c551bc4af82ce06ec6a243619957f1baa` | 2025-10-19T13:31:22+03:00 | 2026-01-20T08:40:18Z |
| 去掉 platform 参数一律禁止默认值；添加双方同时默认值错误 sema_cjmp_parameter_default_value_both_sides | v1.1.0-alpha.66（alpha.65 尚为旧规则） | `936e451ba3b2fd1836b5c7d8bf95eeed0ad9b3d6` | 2025-12-04T08:35:22Z | 2026-01-20T08:40:19Z |
| 配对前移并增加 CheckMatchedFunctionReturnTypes 等后置复核 | v1.1.0-alpha.68（alpha.66 尚无该后检查） | `1320e542afadce1a54db9f402f39820e7e822515` | 2025-12-19T16:47:11+03:00 | 2026-01-20T20:25:32+03:00 |

精确 diff 依据：

- `git show fcf6399 -- src/Parse/ParseCJMPDecl.cpp` 删除 CheckCJMPModifiersOf 中的 GENERIC→parse_cjmp_generic_decl 分支；这只证明旧禁令删除，不概括所有新泛型合法性规则。
- `git show 936e451 -- src/Parse/ParseCJMPDecl.cpp src/Sema/CJMP/CheckCJMP.cpp` 删除 CheckCJMPFuncParams 的 platform default 一律报错，给 MatchCJMPFunction 加入 commonHasDefault && platformHasDefault 检查；同提交还增加 nominal COMMON_WITH_DEFAULT 聚合和其他增强，未逐项声称本项目已对齐。
- `git log --follow -S 'CheckMatchedFunctionReturnTypes' v1.1.0-alpha.68 -- src/Sema/CJMP/CheckCJMP.cpp` 定位到 1320e542；该提交完整 contains 列表已在 §4。
- 这些提交的作者日期显著早于提交日期；alpha.65 仍是旧规则，不能按 2025-10/12 的作者日期误判版本边界。

### 新补充提交 `fcf6399c551bc4af82ce06ec6a243619957f1baa` 的完整 tag-contains

- feat: sync add features of CJMP/FFI-Java/FFI-ObjC
- 作者日期：2025-10-19T13:31:22+03:00；提交日期：2026-01-20T08:40:18Z。
- `git tag --contains` 返回 32 个 tag：

```text
v1.1.0
v1.1.0-alpha.66
v1.1.0-alpha.68
v1.1.0-alpha.69
v1.1.0-alpha.70
v1.1.0-beta.20
v1.1.0-beta.21
v1.1.0-beta.23
v1.1.0-beta.24
v1.1.0-beta.25
v1.1.1
v1.1.2
v1.2.0-alpha.06
v1.2.0-alpha.07
v1.2.0-alpha.08
v1.2.0-alpha.16
v1.2.0-alpha.17
v1.2.0-alpha.18
v1.2.0-alpha.19
v1.2.0-alpha.20
v1.2.0-alpha.21
v1.2.0-alpha.22
v1.2.0-beta.01
v1.2.0-beta.02
v1.2.0-beta.03
v1.2.0-beta.rc
v1.2.0-beta.rc1
v1.2.0-beta.rc2
v1.3.0-alpha.02
v1.3.0-alpha.03
v1.3.0-alpha.04
v1.3.0-alpha.05
```

### 新补充提交 `936e451ba3b2fd1836b5c7d8bf95eeed0ad9b3d6` 的完整 tag-contains

- feat: cjmp cs enhancement
- 作者日期：2025-12-04T08:35:22Z；提交日期：2026-01-20T08:40:19Z。
- `git tag --contains` 返回 32 个 tag：

```text
v1.1.0
v1.1.0-alpha.66
v1.1.0-alpha.68
v1.1.0-alpha.69
v1.1.0-alpha.70
v1.1.0-beta.20
v1.1.0-beta.21
v1.1.0-beta.23
v1.1.0-beta.24
v1.1.0-beta.25
v1.1.1
v1.1.2
v1.2.0-alpha.06
v1.2.0-alpha.07
v1.2.0-alpha.08
v1.2.0-alpha.16
v1.2.0-alpha.17
v1.2.0-alpha.18
v1.2.0-alpha.19
v1.2.0-alpha.20
v1.2.0-alpha.21
v1.2.0-alpha.22
v1.2.0-beta.01
v1.2.0-beta.02
v1.2.0-beta.03
v1.2.0-beta.rc
v1.2.0-beta.rc1
v1.2.0-beta.rc2
v1.3.0-alpha.02
v1.3.0-alpha.03
v1.3.0-alpha.04
v1.3.0-alpha.05
```

## 10. 后续补充：first-fit 与早期 classifier 查名

- 新增 cjc 1.1.3 实测见 `p1-first-fit-20260929/README.md`：11 组尝试全部保留，
  包含 common 编译失败的多候选构造和合法 common CJO 下的非法 specific 占用/缺体对照。
- 没有构造出合法“多个完全兼容 common + 两个 specific”的单 common-part 程序；
  不伪造 CJO、不用多 common 输入冒充 1.1.3。源码明确支持占用失败后继续后续候选，
  实測明确占用诊断优先于缺体。
- 早期 nominal 类型名选择独立于配对成功：
  v1.1.3 `src/Sema/PreCheck.cpp:390–450 GetTyFromASTType(RefType&)` 在 LookupTopLevel 后
  过滤非类型声明，先按 scopeLevel 排序，同一 scopeLevel 偏好 SPECIFIC；
  `:432–441` 首项有 SPECIFIC 即选它，**不读取 specificImplementation**。
- 同时 `CheckCJMP.cpp:173–180` 的 merge 可因 kind 不同失败，`:320–323` 可因
  generic/non-generic 不同跳过。上述早期查名偏好并不以 merge 或配对成功为条件。
- 这一证据只授权“同 scope 类型候选的 specific 优先级”；不能扩张成所有 provider
  中跨 scope 全局删除 common，也不能改成需等待 CJMP_MATCHING 属性的 getter，
  否则可能引入阶段依赖循环。


