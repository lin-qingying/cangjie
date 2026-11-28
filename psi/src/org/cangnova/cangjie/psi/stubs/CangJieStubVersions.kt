/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.psi.stubs

/**
 * 提供 `CangJieStubVersions` 单例，集中承载PSI Stub的共享状态、工厂或工具行为。
 */
object CangJieStubVersions {

    /**
     * 210:
     * - common/specific 修饰符 token 进入 MODIFIER_KEYWORDS_ARRAY（尾部追加，存根掩码新增 2 位）
     */
    /**
     * 209:
     * - 枚举声明宏的输入保留 ENUM_CONSTRUCTOR 和 payload 类型列表
     * - macro stub 持久化声明输入与括号 token 输入的边界
     *
     * 208:
     * - annotation stub 持久化 classId、builtin kind 和 `@!` provenance
     *
     * 206:
     * - features directive stub 持久化文件级 feature id，供 compiled/decompiled PSI 恢复
     *
     * 205:
     * - pattern variable stub 持久化 const 声明关键字状态
     *
     * 204:
     * - `.cjo` 顶层 callable stub 持久化 package facade origin
     * - property stub 持久化 origin，供 stub-based library 反序列化恢复 container source
     *
     * 203:
     * - package directive stub 持久化 macro package 标记
     *
     * 202:
     * - extend stub 显式持久化稳定 extendId
     * - decompiled compiled stub 与 source stub 统一使用同一套 extend identity 协议
     */
    /** 212：命名函数持久化 hasEmptyBlockBody，使惰性构建能够区分空体 Unit 与待推断返回。 */
    // 名义成员的 FQName 由真实 owner 生成，旧版本中已写入的 null 名称必须重新构建。
    // 214：参数 hasLetOrVar 排除 const-only；modifier mask 尾部追加 CONST/UNSAFE/INOUT。
    // 215：所有值参数只在存在实际 modifier 时建立 MODIFIER_LIST，源码与 CJO stub 使用同一形状。
    // 216：声明参数使用明确 grammar role；accessor 不再把类型、!、默认值写入参数 stub。
    // 217：普通宏属于表达式原子，默认值和 return 操作数不再截断宏及其后续运算。
    // 218：错误声明持久化独立身份，错名主构造器不再生成构造器/参数 stub。
    // 219：lazy 正文保留源解析模式，annotation-only 函数体不再生成宏形状的 stub。
    // 220：宏 token 输入不再形成嵌套声明 stub；IfAvailable 源形态、NL 与声明诊断结构更新。
    // 221：scoped fragment roots 与原生参数顺序/ExprKind 文法形成新的语法和诊断结构。
    // 222：平台修饰符加入附加修饰符掩码，源码 Stub 身份随之更新。
    // 223：容器 stub 保存解析时的直接成员语法事实。
    // 224：历史 platform 修饰符参与 stub 掩码。
    // 225：文件 stub 持久化有序角色语法快照，未知来源仍保持缺失。
    // 226：文件角色事件与声明 stub 的结构身份一起持久化。
    // 227：文件角色快照保存 UTF-16 源文本指纹。
    // 228：源码标识符在 stub 索引中采用 Unicode NFC 语义名称。
    // 229：角色事件持久化根事务 UUID 与局部序号，顺序独立于身份。
    // 234：继承前缀保存独立来源；重复诊断在 token 捕获时执行，主范围与 hint 分别保持来源。
    // 235：声明返回属性独立发布；文件 stub 以真实结构身份持久化有效属性，codec 升为5。
    // 236：惰性子根保留父事件槽位；文件 stub 在遍历后绑定最终区域快照。
    // 246：目标声明头包含原生变长参数投影，宏调用源码与编译后 PSI 身份分开。
    // 247：全部参数列表、破损列表 owner 与真实构造器变长参数投影进入共享文法。
    // 248：诊断类别与原生点范围、构造器独立缺体、trivia 归属无关的结构签名。
    // 249：错误载体保存 producer 顺序，完整/惰性正文共享诊断区域；持久化 schema 仍为 codec7。
    // 250：LEX 诊断的全文身份与实际 parser 输入位置分开，宏来源映射保留输入归属。
    // 251：参数延迟诊断保持实际 producer 入队顺序，原有 token 错误嵌套保留。
    // 252：根诊断输出与实际输入 seed 分开，惰性子根继续保留父 producer 区域；codec 不变。
    // 253：函数缺名按照官方名称文法保留 lookahead，后续声明进入独立源码 stub。
    // 254：注解缓冲入口复用真实冻结词法输入与正式文件发布事务，codec 不变。
    // 255：实际 AST 的 UNKNOWN 叶保留冻结词法报告，所有原生片段 chameleon 保留完整根结果。
    // 256：真实原生根CharTable交接覆盖零孩子提交，当前来源视图的元数据和并发代次分别验证。
    // 257：自身static-init与父成员检查按官方真实调用顺序发布，相同规范范围保留先报。
    // 258：原生提交采用真实表后撤销SDK旧根key，零孩子/POM重投走同一代次协议。
    // 259：所有错误载体冻结完整提示，primary冲突保存前modifier与实际let/var的独立范围。
    // 260：PSI raw转换归同一Visitor，指定声明复活区分真实enclosing父帧与forced target声明帧。
    // package/accessor 的 binary stub 补齐源码同构前缀，统一失效旧源码与 compiled stub 缓存。
    // 262：枚举构造项复用命名声明的去反引号/NFC身份，旧名称 stub 必须重新构建。
    const val SOURCE_STUB_VERSION = 262

    /**
     * 保存 `BINARY_STUB_VERSION`，供PSI Stub流程读取节点结构或语义信息。
     */
    private const val BINARY_STUB_VERSION = SOURCE_STUB_VERSION

    /**
     * 保存 `CLASSFILE_STUB_VERSION`，供PSI Stub流程读取节点结构或语义信息。
     */
    const val CLASSFILE_STUB_VERSION = BINARY_STUB_VERSION + 0

    /**
     * 保存 `BUILTIN_STUB_VERSION`，供PSI Stub流程读取节点结构或语义信息。
     */
    const val BUILTIN_STUB_VERSION = BINARY_STUB_VERSION + 4


}
