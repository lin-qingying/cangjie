/*
 * CangJie语言词法分析器定义
 * =======================
 * 
 * 这是CangJie编程语言的JFlex词法分析器定义文件。
 * 词法分析器负责将源代码分解成一系列标记(tokens)，供解析器进行处理。
 *
 * JFlex文件结构规则：
 * ----------------
 * JFlex文件由三个部分组成，用 % % 分隔：
 * 
 * 1. 用户代码部分（第一部分）
 *    - 包声明和导入语句
 *    - 用户自定义的类和方法
 *    - 在 %{ 和 %} 之间的代码会被直接复制到生成的词法分析器类中
 * 
 * 2. 选项和声明部分（第二部分）
 *    - JFlex选项（如 %unicode, %class 等）
 *    - 状态声明（%state, %xstate）
 *    - 宏定义
 * 
 * 3. 词法规则部分（第三部分）
 *    - 模式和动作对
 *    - 每个规则格式：pattern { java code }
 *
 * JFlex选项说明：
 * -------------
 * %unicode     - 启用完整的Unicode支持
 * %class       - 指定生成的词法分析器类名
 * %implements  - 指定要实现的接口
 * %function    - 指定词法分析的主方法名
 * %type        - 指定返回的标记类型
 * %state       - 声明包容性状态
 * %xstate      - 声明排他性状态
 * %eof{        - 指定文件结束时的操作
 *
 * 状态系统说明：
 * -----------
 * 词法分析器使用状态机来处理复杂的标记：
 * 1. 初始状态（YYINITIAL）- 默认状态
 * 2. 排他性状态（%xstate）：
 *    - STRING_PREFIX: 字符串前缀处理
 *    - STRING_SINGLE: 单引号字符串处理
 *    - STRING_DOUBLE: 双引号字符串处理
 *    - RAW_STRING_SINGLE: 原始单引号字符串
 *    - RAW_STRING_DOUBLE: 原始双引号字符串
 *    - HSAH_STRING_SINGLE: 哈希单引号字符串
 *    - HSAH_STRING_DOUBLE: 哈希双引号字符串
 *    - SHORT_TEMPLATE_ENTRY: 简单字符串插值
 *    - BLOCK_COMMENT: 块注释
 *    - DOC_COMMENT: 文档注释
 * 3. 状态转换：
 *    - pushState(): 压入新状态
 *    - popState(): 恢复之前的状态
 *    - yybegin(): 直接切换到指定状态
 *
 * 宏定义规则：
 * ----------
 * 1. 字符类：
 *    DIGIT               = [0-9]                    数字字符
 *    DIGIT_OR_UNDERSCORE = [0-9_]                  数字或下划线
 *    XID_START          = Unicode 15.0 XID_Start
 *    XID_CONTINUE_EXTRA = Unicode 15.0 XID_Continue 中不属于 XID_Start 的字符
 *    HORIZONTAL_SPACE   = [\ \t\f]                 水平空白
 *    NL                 = \r\n|\n                   独立换行词元
 * 
 * 2. 标识符：
 *    IDENTIFIER_START    = XID_Start 或下划线      标识符起始字符
 *    IDENTIFIER_PART     = XID_Continue             标识符组成部分
 *    PLAIN_IDENTIFIER   = {IDENTIFIER_START}{IDENTIFIER_PART}*  普通标识符
 *    ESCAPED_IDENTIFIER = `[^`\r\n]+`              转义标识符
 *    FIELD_IDENTIFIER   = \${IDENTIFIER}          字段标识符
 * 
 * 3. 数值字面量：
 *    INTEGER_LITERAL    = 支持四种进制（十进制、十六进制、二进制、八进制）
 *    FLOAT_LITERAL     = 支持小数点和科学计数法
 *    
 * 4. 字符串相关：
 *    ESCAPE_SEQUENCE    = \\(u\{HEX_DIGIT{1,8}\}|[^\r\n])  转义序列
 *    STRING_CONTENT    = [^\\\"'\r\n\$]+          字符串内容
 *    INTERPOLATION     = \$+                      字符串插值标记
 *
 * 错误处理策略：
 * -----------
 * 1. 普通表达式状态中的未知字符返回独立词法标记，由解析诊断通道报告
 * 2. 特殊词法状态保留各自的错误标记
 * 3. 状态相关的错误提供详细的错误信息
 * 4. 文件结束检查确保所有状态正确结束
 *
 * 标记类型系统：
 * -----------
 * 1. 关键字标记：各种语言关键字
 * 2. 标识符标记：普通标识符、字段标识符等
 * 3. 字面量标记：数值、字符串、字符等
 * 4. 运算符标记：算术、逻辑、位运算等
 * 5. 分隔符标记：括号、逗号、分号等
 * 6. 注释标记：行注释、块注释、文档注释
 * 7. 特殊标记：空白字符、错误字符等
 */

package org.cangnova.cangjie.lexer;
import com.intellij.psi.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.containers.Stack;
import com.intellij.lexer.FlexLexer;

%%

%unicode
%class _CangJieLexer
%implements FlexLexer

%{
 public _CangJieLexer() {
    this((java.io.Reader)null);
 }
%}

%{
    private static final class State {
        final int lBraceCount;
        final int requiredInterpolationPrefix;
        final int state;
        final String name;

        public State(int state, int lBraceCount, int requiredInterpolationPrefix) {
            this.name = "无";
            this.state = state;
            this.lBraceCount = lBraceCount;
            this.requiredInterpolationPrefix = requiredInterpolationPrefix;
        }

        public State(String name, int state, int lBraceCount, int requiredInterpolationPrefix) {
            this.name = name;
            this.state = state;
            this.lBraceCount = lBraceCount;
            this.requiredInterpolationPrefix = requiredInterpolationPrefix;
        }

        @Override
        public String toString() {
            return "yystate = " + state +
                (lBraceCount == 0 ? "" : ", lBraceCount = " + lBraceCount) +
                (requiredInterpolationPrefix == -1 ? "" : ", requiredInterpolationPrefix = " + requiredInterpolationPrefix);
        }
    }

    private final Stack<State> states = new Stack<State>();
    private int lBraceCount;
    private int requiredInterpolationPrefix;
    private int commentStart;
    private int commentDepth;

    private void pushState(int state) {
        pushState("无",state);
    }

    private void pushState(String name, int state) {
        states.push(new State(name,yystate(), lBraceCount, requiredInterpolationPrefix));
        lBraceCount = 0;
        requiredInterpolationPrefix = -1;
        yybegin(state);
    }

    private void pushInterpolationPrefix(int interpolationPrefix) {
        states.push(new State(yystate(), lBraceCount, requiredInterpolationPrefix));
        lBraceCount = 0;
        requiredInterpolationPrefix = interpolationPrefix;
        yybegin(STRING_PREFIX);
    }

    private void popState() {
        State state = states.pop();
        lBraceCount = state.lBraceCount;
        requiredInterpolationPrefix = state.requiredInterpolationPrefix;
        yybegin(state.state);
    }

    private State getState() {
        return states.isEmpty() ? null : states.peek();
    }

    private IElementType commentStateToTokenType(int state) {
        switch (state) {
            case BLOCK_COMMENT:
                return CjTokens.BLOCK_COMMENT;
            case DOC_COMMENT:
                return CjTokens.DOC_COMMENT;
            default:
                throw new IllegalArgumentException("Unexpected comment state: " + state);
        }
    }
%}

%scanerror CangJieLexerException

%function advance
%type IElementType
%eof{
  return;
%eof}

%xstate STRING_PREFIX  HSAH_STRING_SINGLE  HSAH_STRING_DOUBLE  STRING_SINGLE  STRING_DOUBLE  RAW_STRING_SINGLE RAW_STRING_DOUBLE SHORT_TEMPLATE_ENTRY BLOCK_COMMENT DOC_COMMENT
%state LONG_TEMPLATE_ENTRY UNMATCHED_BACKTICK

/* 数值字面量定义
 * ===========
 * 支持以下类型的数值：
 * 1. 整数字面量：
 *    - 十进制：123, 1_000_000
 *    - 十六进制：0xFF, 0x1A_2B
 *    - 二进制：0b1010, 0b1010_0101
 *    - 八进制：0o777, 0o77_77
 *    - 可选类型后缀：u8, u16, u32, u64, i8, i16, i32, i64
 * 
 * 2. 浮点数字面量：
 *    - 十进制：123.45, .45, 1e10, 1.2e-10
 *    - 十六进制：0x1.8p1, 0x1.0p-1
 *    - 可选类型后缀：f32, f16, f64
 */

/* 数字相关定义
 * ==========
 * 支持以下数字格式：
 * 1. 整数：十进制、十六进制、二进制、八进制
 * 2. 浮点数：小数点表示、科学计数法
 * 3. 数字可以用下划线分隔以提高可读性
 */

/* 空白字符
 * =======
 */
// 官方 lexer 接受 LF/CRLF，孤立 CR 保持非法；保留输入长度以维持原始 source range。
HORIZONTAL_SPACE=[\ \t\f]
LINE_TERMINATOR=\r\n|\n

/* 标识符相关定义
 * ===========
 * 支持以下类型的标识符：
 * 1. 普通标识符：XID_Start 或下划线开头，后跟 XID_Continue
 * 2. 转义标识符：反引号包围的任意字符序列
 * 3. 字段标识符：以$开头的标识符
 */

// 标识符基本组成
// Unicode 15.0 XID 区间取自 cangjie_compiler v1.1.3 src/Utils/Unicode.cpp。
// Copyright (c) Huawei Technologies Co., Ltd. 2025. Apache-2.0 with Runtime Library Exception.
// XID_CONTINUE_EXTRA 只包含 XID_Start 之外的字符；下划线允许作为仓颉标识符首字符。
XID_START=[\u{41}-\u{5a}\u{61}-\u{7a}\u{aa}\u{b5}\u{ba}\u{c0}-\u{d6}\u{d8}-\u{f6}\u{f8}-\u{2c1}\u{2c6}-\u{2d1}\u{2e0}-\u{2e4}\u{2ec}\u{2ee}\u{370}-\u{374}\u{376}-\u{377}\u{37b}-\u{37d}\u{37f}\u{386}\u{388}-\u{38a}\u{38c}\u{38e}-\u{3a1}\u{3a3}-\u{3f5}\u{3f7}-\u{481}\u{48a}-\u{52f}\u{531}-\u{556}\u{559}\u{560}-\u{588}\u{5d0}-\u{5ea}\u{5ef}-\u{5f2}\u{620}-\u{64a}\u{66e}-\u{66f}\u{671}-\u{6d3}\u{6d5}\u{6e5}-\u{6e6}\u{6ee}-\u{6ef}\u{6fa}-\u{6fc}\u{6ff}\u{710}\u{712}-\u{72f}\u{74d}-\u{7a5}\u{7b1}\u{7ca}-\u{7ea}\u{7f4}-\u{7f5}\u{7fa}\u{800}-\u{815}\u{81a}\u{824}\u{828}\u{840}-\u{858}\u{860}-\u{86a}\u{870}-\u{887}\u{889}-\u{88e}\u{8a0}-\u{8c9}\u{904}-\u{939}\u{93d}\u{950}\u{958}-\u{961}\u{971}-\u{980}\u{985}-\u{98c}\u{98f}-\u{990}\u{993}-\u{9a8}\u{9aa}-\u{9b0}\u{9b2}\u{9b6}-\u{9b9}\u{9bd}\u{9ce}\u{9dc}-\u{9dd}\u{9df}-\u{9e1}\u{9f0}-\u{9f1}\u{9fc}\u{a05}-\u{a0a}\u{a0f}-\u{a10}\u{a13}-\u{a28}\u{a2a}-\u{a30}\u{a32}-\u{a33}\u{a35}-\u{a36}\u{a38}-\u{a39}\u{a59}-\u{a5c}\u{a5e}\u{a72}-\u{a74}\u{a85}-\u{a8d}\u{a8f}-\u{a91}\u{a93}-\u{aa8}\u{aaa}-\u{ab0}\u{ab2}-\u{ab3}\u{ab5}-\u{ab9}\u{abd}\u{ad0}\u{ae0}-\u{ae1}\u{af9}\u{b05}-\u{b0c}\u{b0f}-\u{b10}\u{b13}-\u{b28}\u{b2a}-\u{b30}\u{b32}-\u{b33}\u{b35}-\u{b39}\u{b3d}\u{b5c}-\u{b5d}\u{b5f}-\u{b61}\u{b71}\u{b83}\u{b85}-\u{b8a}\u{b8e}-\u{b90}\u{b92}-\u{b95}\u{b99}-\u{b9a}\u{b9c}\u{b9e}-\u{b9f}\u{ba3}-\u{ba4}\u{ba8}-\u{baa}\u{bae}-\u{bb9}\u{bd0}\u{c05}-\u{c0c}\u{c0e}-\u{c10}\u{c12}-\u{c28}\u{c2a}-\u{c39}\u{c3d}\u{c58}-\u{c5a}\u{c5d}\u{c60}-\u{c61}\u{c80}\u{c85}-\u{c8c}\u{c8e}-\u{c90}\u{c92}-\u{ca8}\u{caa}-\u{cb3}\u{cb5}-\u{cb9}\u{cbd}\u{cdd}-\u{cde}\u{ce0}-\u{ce1}\u{cf1}-\u{cf2}\u{d04}-\u{d0c}\u{d0e}-\u{d10}\u{d12}-\u{d3a}\u{d3d}\u{d4e}\u{d54}-\u{d56}\u{d5f}-\u{d61}\u{d7a}-\u{d7f}\u{d85}-\u{d96}\u{d9a}-\u{db1}\u{db3}-\u{dbb}\u{dbd}\u{dc0}-\u{dc6}\u{e01}-\u{e30}\u{e32}\u{e40}-\u{e46}\u{e81}-\u{e82}\u{e84}\u{e86}-\u{e8a}\u{e8c}-\u{ea3}\u{ea5}\u{ea7}-\u{eb0}\u{eb2}\u{ebd}\u{ec0}-\u{ec4}\u{ec6}\u{edc}-\u{edf}\u{f00}\u{f40}-\u{f47}\u{f49}-\u{f6c}\u{f88}-\u{f8c}\u{1000}-\u{102a}\u{103f}\u{1050}-\u{1055}\u{105a}-\u{105d}\u{1061}\u{1065}-\u{1066}\u{106e}-\u{1070}\u{1075}-\u{1081}\u{108e}\u{10a0}-\u{10c5}\u{10c7}\u{10cd}\u{10d0}-\u{10fa}\u{10fc}-\u{1248}\u{124a}-\u{124d}\u{1250}-\u{1256}\u{1258}\u{125a}-\u{125d}\u{1260}-\u{1288}\u{128a}-\u{128d}\u{1290}-\u{12b0}\u{12b2}-\u{12b5}\u{12b8}-\u{12be}\u{12c0}\u{12c2}-\u{12c5}\u{12c8}-\u{12d6}\u{12d8}-\u{1310}\u{1312}-\u{1315}\u{1318}-\u{135a}\u{1380}-\u{138f}\u{13a0}-\u{13f5}\u{13f8}-\u{13fd}\u{1401}-\u{166c}\u{166f}-\u{167f}\u{1681}-\u{169a}\u{16a0}-\u{16ea}\u{16ee}-\u{16f8}\u{1700}-\u{1711}\u{171f}-\u{1731}\u{1740}-\u{1751}\u{1760}-\u{176c}\u{176e}-\u{1770}\u{1780}-\u{17b3}\u{17d7}\u{17dc}\u{1820}-\u{1878}\u{1880}-\u{18a8}\u{18aa}\u{18b0}-\u{18f5}\u{1900}-\u{191e}\u{1950}-\u{196d}\u{1970}-\u{1974}\u{1980}-\u{19ab}\u{19b0}-\u{19c9}\u{1a00}-\u{1a16}\u{1a20}-\u{1a54}\u{1aa7}\u{1b05}-\u{1b33}\u{1b45}-\u{1b4c}\u{1b83}-\u{1ba0}\u{1bae}-\u{1baf}\u{1bba}-\u{1be5}\u{1c00}-\u{1c23}\u{1c4d}-\u{1c4f}\u{1c5a}-\u{1c7d}\u{1c80}-\u{1c88}\u{1c90}-\u{1cba}\u{1cbd}-\u{1cbf}\u{1ce9}-\u{1cec}\u{1cee}-\u{1cf3}\u{1cf5}-\u{1cf6}\u{1cfa}\u{1d00}-\u{1dbf}\u{1e00}-\u{1f15}\u{1f18}-\u{1f1d}\u{1f20}-\u{1f45}\u{1f48}-\u{1f4d}\u{1f50}-\u{1f57}\u{1f59}\u{1f5b}\u{1f5d}\u{1f5f}-\u{1f7d}\u{1f80}-\u{1fb4}\u{1fb6}-\u{1fbc}\u{1fbe}\u{1fc2}-\u{1fc4}\u{1fc6}-\u{1fcc}\u{1fd0}-\u{1fd3}\u{1fd6}-\u{1fdb}\u{1fe0}-\u{1fec}\u{1ff2}-\u{1ff4}\u{1ff6}-\u{1ffc}\u{2071}\u{207f}\u{2090}-\u{209c}\u{2102}\u{2107}\u{210a}-\u{2113}\u{2115}\u{2118}-\u{211d}\u{2124}\u{2126}\u{2128}\u{212a}-\u{2139}\u{213c}-\u{213f}\u{2145}-\u{2149}\u{214e}\u{2160}-\u{2188}\u{2c00}-\u{2ce4}\u{2ceb}-\u{2cee}\u{2cf2}-\u{2cf3}\u{2d00}-\u{2d25}\u{2d27}\u{2d2d}\u{2d30}-\u{2d67}\u{2d6f}\u{2d80}-\u{2d96}\u{2da0}-\u{2da6}\u{2da8}-\u{2dae}\u{2db0}-\u{2db6}\u{2db8}-\u{2dbe}\u{2dc0}-\u{2dc6}\u{2dc8}-\u{2dce}\u{2dd0}-\u{2dd6}\u{2dd8}-\u{2dde}\u{3005}-\u{3007}\u{3021}-\u{3029}\u{3031}-\u{3035}\u{3038}-\u{303c}\u{3041}-\u{3096}\u{309d}-\u{309f}\u{30a1}-\u{30fa}\u{30fc}-\u{30ff}\u{3105}-\u{312f}\u{3131}-\u{318e}\u{31a0}-\u{31bf}\u{31f0}-\u{31ff}\u{3400}-\u{4dbf}\u{4e00}-\u{a48c}\u{a4d0}-\u{a4fd}\u{a500}-\u{a60c}\u{a610}-\u{a61f}\u{a62a}-\u{a62b}\u{a640}-\u{a66e}\u{a67f}-\u{a69d}\u{a6a0}-\u{a6ef}\u{a717}-\u{a71f}\u{a722}-\u{a788}\u{a78b}-\u{a7ca}\u{a7d0}-\u{a7d1}\u{a7d3}\u{a7d5}-\u{a7d9}\u{a7f2}-\u{a801}\u{a803}-\u{a805}\u{a807}-\u{a80a}\u{a80c}-\u{a822}\u{a840}-\u{a873}\u{a882}-\u{a8b3}\u{a8f2}-\u{a8f7}\u{a8fb}\u{a8fd}-\u{a8fe}\u{a90a}-\u{a925}\u{a930}-\u{a946}\u{a960}-\u{a97c}\u{a984}-\u{a9b2}\u{a9cf}\u{a9e0}-\u{a9e4}\u{a9e6}-\u{a9ef}\u{a9fa}-\u{a9fe}\u{aa00}-\u{aa28}\u{aa40}-\u{aa42}\u{aa44}-\u{aa4b}\u{aa60}-\u{aa76}\u{aa7a}\u{aa7e}-\u{aaaf}\u{aab1}\u{aab5}-\u{aab6}\u{aab9}-\u{aabd}\u{aac0}\u{aac2}\u{aadb}-\u{aadd}\u{aae0}-\u{aaea}\u{aaf2}-\u{aaf4}\u{ab01}-\u{ab06}\u{ab09}-\u{ab0e}\u{ab11}-\u{ab16}\u{ab20}-\u{ab26}\u{ab28}-\u{ab2e}\u{ab30}-\u{ab5a}\u{ab5c}-\u{ab69}\u{ab70}-\u{abe2}\u{ac00}-\u{d7a3}\u{d7b0}-\u{d7c6}\u{d7cb}-\u{d7fb}\u{f900}-\u{fa6d}\u{fa70}-\u{fad9}\u{fb00}-\u{fb06}\u{fb13}-\u{fb17}\u{fb1d}\u{fb1f}-\u{fb28}\u{fb2a}-\u{fb36}\u{fb38}-\u{fb3c}\u{fb3e}\u{fb40}-\u{fb41}\u{fb43}-\u{fb44}\u{fb46}-\u{fbb1}\u{fbd3}-\u{fc5d}\u{fc64}-\u{fd3d}\u{fd50}-\u{fd8f}\u{fd92}-\u{fdc7}\u{fdf0}-\u{fdf9}\u{fe71}\u{fe73}\u{fe77}\u{fe79}\u{fe7b}\u{fe7d}\u{fe7f}-\u{fefc}\u{ff21}-\u{ff3a}\u{ff41}-\u{ff5a}\u{ff66}-\u{ff9d}\u{ffa0}-\u{ffbe}\u{ffc2}-\u{ffc7}\u{ffca}-\u{ffcf}\u{ffd2}-\u{ffd7}\u{ffda}-\u{ffdc}\u{10000}-\u{1000b}\u{1000d}-\u{10026}\u{10028}-\u{1003a}\u{1003c}-\u{1003d}\u{1003f}-\u{1004d}\u{10050}-\u{1005d}\u{10080}-\u{100fa}\u{10140}-\u{10174}\u{10280}-\u{1029c}\u{102a0}-\u{102d0}\u{10300}-\u{1031f}\u{1032d}-\u{1034a}\u{10350}-\u{10375}\u{10380}-\u{1039d}\u{103a0}-\u{103c3}\u{103c8}-\u{103cf}\u{103d1}-\u{103d5}\u{10400}-\u{1049d}\u{104b0}-\u{104d3}\u{104d8}-\u{104fb}\u{10500}-\u{10527}\u{10530}-\u{10563}\u{10570}-\u{1057a}\u{1057c}-\u{1058a}\u{1058c}-\u{10592}\u{10594}-\u{10595}\u{10597}-\u{105a1}\u{105a3}-\u{105b1}\u{105b3}-\u{105b9}\u{105bb}-\u{105bc}\u{10600}-\u{10736}\u{10740}-\u{10755}\u{10760}-\u{10767}\u{10780}-\u{10785}\u{10787}-\u{107b0}\u{107b2}-\u{107ba}\u{10800}-\u{10805}\u{10808}\u{1080a}-\u{10835}\u{10837}-\u{10838}\u{1083c}\u{1083f}-\u{10855}\u{10860}-\u{10876}\u{10880}-\u{1089e}\u{108e0}-\u{108f2}\u{108f4}-\u{108f5}\u{10900}-\u{10915}\u{10920}-\u{10939}\u{10980}-\u{109b7}\u{109be}-\u{109bf}\u{10a00}\u{10a10}-\u{10a13}\u{10a15}-\u{10a17}\u{10a19}-\u{10a35}\u{10a60}-\u{10a7c}\u{10a80}-\u{10a9c}\u{10ac0}-\u{10ac7}\u{10ac9}-\u{10ae4}\u{10b00}-\u{10b35}\u{10b40}-\u{10b55}\u{10b60}-\u{10b72}\u{10b80}-\u{10b91}\u{10c00}-\u{10c48}\u{10c80}-\u{10cb2}\u{10cc0}-\u{10cf2}\u{10d00}-\u{10d23}\u{10e80}-\u{10ea9}\u{10eb0}-\u{10eb1}\u{10f00}-\u{10f1c}\u{10f27}\u{10f30}-\u{10f45}\u{10f70}-\u{10f81}\u{10fb0}-\u{10fc4}\u{10fe0}-\u{10ff6}\u{11003}-\u{11037}\u{11071}-\u{11072}\u{11075}\u{11083}-\u{110af}\u{110d0}-\u{110e8}\u{11103}-\u{11126}\u{11144}\u{11147}\u{11150}-\u{11172}\u{11176}\u{11183}-\u{111b2}\u{111c1}-\u{111c4}\u{111da}\u{111dc}\u{11200}-\u{11211}\u{11213}-\u{1122b}\u{1123f}-\u{11240}\u{11280}-\u{11286}\u{11288}\u{1128a}-\u{1128d}\u{1128f}-\u{1129d}\u{1129f}-\u{112a8}\u{112b0}-\u{112de}\u{11305}-\u{1130c}\u{1130f}-\u{11310}\u{11313}-\u{11328}\u{1132a}-\u{11330}\u{11332}-\u{11333}\u{11335}-\u{11339}\u{1133d}\u{11350}\u{1135d}-\u{11361}\u{11400}-\u{11434}\u{11447}-\u{1144a}\u{1145f}-\u{11461}\u{11480}-\u{114af}\u{114c4}-\u{114c5}\u{114c7}\u{11580}-\u{115ae}\u{115d8}-\u{115db}\u{11600}-\u{1162f}\u{11644}\u{11680}-\u{116aa}\u{116b8}\u{11700}-\u{1171a}\u{11740}-\u{11746}\u{11800}-\u{1182b}\u{118a0}-\u{118df}\u{118ff}-\u{11906}\u{11909}\u{1190c}-\u{11913}\u{11915}-\u{11916}\u{11918}-\u{1192f}\u{1193f}\u{11941}\u{119a0}-\u{119a7}\u{119aa}-\u{119d0}\u{119e1}\u{119e3}\u{11a00}\u{11a0b}-\u{11a32}\u{11a3a}\u{11a50}\u{11a5c}-\u{11a89}\u{11a9d}\u{11ab0}-\u{11af8}\u{11c00}-\u{11c08}\u{11c0a}-\u{11c2e}\u{11c40}\u{11c72}-\u{11c8f}\u{11d00}-\u{11d06}\u{11d08}-\u{11d09}\u{11d0b}-\u{11d30}\u{11d46}\u{11d60}-\u{11d65}\u{11d67}-\u{11d68}\u{11d6a}-\u{11d89}\u{11d98}\u{11ee0}-\u{11ef2}\u{11f02}\u{11f04}-\u{11f10}\u{11f12}-\u{11f33}\u{11fb0}\u{12000}-\u{12399}\u{12400}-\u{1246e}\u{12480}-\u{12543}\u{12f90}-\u{12ff0}\u{13000}-\u{1342f}\u{13441}-\u{13446}\u{14400}-\u{14646}\u{16800}-\u{16a38}\u{16a40}-\u{16a5e}\u{16a70}-\u{16abe}\u{16ad0}-\u{16aed}\u{16b00}-\u{16b2f}\u{16b40}-\u{16b43}\u{16b63}-\u{16b77}\u{16b7d}-\u{16b8f}\u{16e40}-\u{16e7f}\u{16f00}-\u{16f4a}\u{16f50}\u{16f93}-\u{16f9f}\u{16fe0}-\u{16fe1}\u{16fe3}\u{17000}-\u{187f7}\u{18800}-\u{18cd5}\u{18d00}-\u{18d08}\u{1aff0}-\u{1aff3}\u{1aff5}-\u{1affb}\u{1affd}-\u{1affe}\u{1b000}-\u{1b122}\u{1b132}\u{1b150}-\u{1b152}\u{1b155}\u{1b164}-\u{1b167}\u{1b170}-\u{1b2fb}\u{1bc00}-\u{1bc6a}\u{1bc70}-\u{1bc7c}\u{1bc80}-\u{1bc88}\u{1bc90}-\u{1bc99}\u{1d400}-\u{1d454}\u{1d456}-\u{1d49c}\u{1d49e}-\u{1d49f}\u{1d4a2}\u{1d4a5}-\u{1d4a6}\u{1d4a9}-\u{1d4ac}\u{1d4ae}-\u{1d4b9}\u{1d4bb}\u{1d4bd}-\u{1d4c3}\u{1d4c5}-\u{1d505}\u{1d507}-\u{1d50a}\u{1d50d}-\u{1d514}\u{1d516}-\u{1d51c}\u{1d51e}-\u{1d539}\u{1d53b}-\u{1d53e}\u{1d540}-\u{1d544}\u{1d546}\u{1d54a}-\u{1d550}\u{1d552}-\u{1d6a5}\u{1d6a8}-\u{1d6c0}\u{1d6c2}-\u{1d6da}\u{1d6dc}-\u{1d6fa}\u{1d6fc}-\u{1d714}\u{1d716}-\u{1d734}\u{1d736}-\u{1d74e}\u{1d750}-\u{1d76e}\u{1d770}-\u{1d788}\u{1d78a}-\u{1d7a8}\u{1d7aa}-\u{1d7c2}\u{1d7c4}-\u{1d7cb}\u{1df00}-\u{1df1e}\u{1df25}-\u{1df2a}\u{1e030}-\u{1e06d}\u{1e100}-\u{1e12c}\u{1e137}-\u{1e13d}\u{1e14e}\u{1e290}-\u{1e2ad}\u{1e2c0}-\u{1e2eb}\u{1e4d0}-\u{1e4eb}\u{1e7e0}-\u{1e7e6}\u{1e7e8}-\u{1e7eb}\u{1e7ed}-\u{1e7ee}\u{1e7f0}-\u{1e7fe}\u{1e800}-\u{1e8c4}\u{1e900}-\u{1e943}\u{1e94b}\u{1ee00}-\u{1ee03}\u{1ee05}-\u{1ee1f}\u{1ee21}-\u{1ee22}\u{1ee24}\u{1ee27}\u{1ee29}-\u{1ee32}\u{1ee34}-\u{1ee37}\u{1ee39}\u{1ee3b}\u{1ee42}\u{1ee47}\u{1ee49}\u{1ee4b}\u{1ee4d}-\u{1ee4f}\u{1ee51}-\u{1ee52}\u{1ee54}\u{1ee57}\u{1ee59}\u{1ee5b}\u{1ee5d}\u{1ee5f}\u{1ee61}-\u{1ee62}\u{1ee64}\u{1ee67}-\u{1ee6a}\u{1ee6c}-\u{1ee72}\u{1ee74}-\u{1ee77}\u{1ee79}-\u{1ee7c}\u{1ee7e}\u{1ee80}-\u{1ee89}\u{1ee8b}-\u{1ee9b}\u{1eea1}-\u{1eea3}\u{1eea5}-\u{1eea9}\u{1eeab}-\u{1eebb}\u{20000}-\u{2a6df}\u{2a700}-\u{2b739}\u{2b740}-\u{2b81d}\u{2b820}-\u{2cea1}\u{2ceb0}-\u{2ebe0}\u{2f800}-\u{2fa1d}\u{30000}-\u{3134a}\u{31350}-\u{323af}]
XID_CONTINUE_EXTRA=[\u{30}-\u{39}\u{5f}\u{b7}\u{300}-\u{36f}\u{387}\u{483}-\u{487}\u{591}-\u{5bd}\u{5bf}\u{5c1}-\u{5c2}\u{5c4}-\u{5c5}\u{5c7}\u{610}-\u{61a}\u{64b}-\u{669}\u{670}\u{6d6}-\u{6dc}\u{6df}-\u{6e4}\u{6e7}-\u{6e8}\u{6ea}-\u{6ed}\u{6f0}-\u{6f9}\u{711}\u{730}-\u{74a}\u{7a6}-\u{7b0}\u{7c0}-\u{7c9}\u{7eb}-\u{7f3}\u{7fd}\u{816}-\u{819}\u{81b}-\u{823}\u{825}-\u{827}\u{829}-\u{82d}\u{859}-\u{85b}\u{898}-\u{89f}\u{8ca}-\u{8e1}\u{8e3}-\u{903}\u{93a}-\u{93c}\u{93e}-\u{94f}\u{951}-\u{957}\u{962}-\u{963}\u{966}-\u{96f}\u{981}-\u{983}\u{9bc}\u{9be}-\u{9c4}\u{9c7}-\u{9c8}\u{9cb}-\u{9cd}\u{9d7}\u{9e2}-\u{9e3}\u{9e6}-\u{9ef}\u{9fe}\u{a01}-\u{a03}\u{a3c}\u{a3e}-\u{a42}\u{a47}-\u{a48}\u{a4b}-\u{a4d}\u{a51}\u{a66}-\u{a71}\u{a75}\u{a81}-\u{a83}\u{abc}\u{abe}-\u{ac5}\u{ac7}-\u{ac9}\u{acb}-\u{acd}\u{ae2}-\u{ae3}\u{ae6}-\u{aef}\u{afa}-\u{aff}\u{b01}-\u{b03}\u{b3c}\u{b3e}-\u{b44}\u{b47}-\u{b48}\u{b4b}-\u{b4d}\u{b55}-\u{b57}\u{b62}-\u{b63}\u{b66}-\u{b6f}\u{b82}\u{bbe}-\u{bc2}\u{bc6}-\u{bc8}\u{bca}-\u{bcd}\u{bd7}\u{be6}-\u{bef}\u{c00}-\u{c04}\u{c3c}\u{c3e}-\u{c44}\u{c46}-\u{c48}\u{c4a}-\u{c4d}\u{c55}-\u{c56}\u{c62}-\u{c63}\u{c66}-\u{c6f}\u{c81}-\u{c83}\u{cbc}\u{cbe}-\u{cc4}\u{cc6}-\u{cc8}\u{cca}-\u{ccd}\u{cd5}-\u{cd6}\u{ce2}-\u{ce3}\u{ce6}-\u{cef}\u{cf3}\u{d00}-\u{d03}\u{d3b}-\u{d3c}\u{d3e}-\u{d44}\u{d46}-\u{d48}\u{d4a}-\u{d4d}\u{d57}\u{d62}-\u{d63}\u{d66}-\u{d6f}\u{d81}-\u{d83}\u{dca}\u{dcf}-\u{dd4}\u{dd6}\u{dd8}-\u{ddf}\u{de6}-\u{def}\u{df2}-\u{df3}\u{e31}\u{e33}-\u{e3a}\u{e47}-\u{e4e}\u{e50}-\u{e59}\u{eb1}\u{eb3}-\u{ebc}\u{ec8}-\u{ece}\u{ed0}-\u{ed9}\u{f18}-\u{f19}\u{f20}-\u{f29}\u{f35}\u{f37}\u{f39}\u{f3e}-\u{f3f}\u{f71}-\u{f84}\u{f86}-\u{f87}\u{f8d}-\u{f97}\u{f99}-\u{fbc}\u{fc6}\u{102b}-\u{103e}\u{1040}-\u{1049}\u{1056}-\u{1059}\u{105e}-\u{1060}\u{1062}-\u{1064}\u{1067}-\u{106d}\u{1071}-\u{1074}\u{1082}-\u{108d}\u{108f}-\u{109d}\u{135d}-\u{135f}\u{1369}-\u{1371}\u{1712}-\u{1715}\u{1732}-\u{1734}\u{1752}-\u{1753}\u{1772}-\u{1773}\u{17b4}-\u{17d3}\u{17dd}\u{17e0}-\u{17e9}\u{180b}-\u{180d}\u{180f}-\u{1819}\u{18a9}\u{1920}-\u{192b}\u{1930}-\u{193b}\u{1946}-\u{194f}\u{19d0}-\u{19da}\u{1a17}-\u{1a1b}\u{1a55}-\u{1a5e}\u{1a60}-\u{1a7c}\u{1a7f}-\u{1a89}\u{1a90}-\u{1a99}\u{1ab0}-\u{1abd}\u{1abf}-\u{1ace}\u{1b00}-\u{1b04}\u{1b34}-\u{1b44}\u{1b50}-\u{1b59}\u{1b6b}-\u{1b73}\u{1b80}-\u{1b82}\u{1ba1}-\u{1bad}\u{1bb0}-\u{1bb9}\u{1be6}-\u{1bf3}\u{1c24}-\u{1c37}\u{1c40}-\u{1c49}\u{1c50}-\u{1c59}\u{1cd0}-\u{1cd2}\u{1cd4}-\u{1ce8}\u{1ced}\u{1cf4}\u{1cf7}-\u{1cf9}\u{1dc0}-\u{1dff}\u{203f}-\u{2040}\u{2054}\u{20d0}-\u{20dc}\u{20e1}\u{20e5}-\u{20f0}\u{2cef}-\u{2cf1}\u{2d7f}\u{2de0}-\u{2dff}\u{302a}-\u{302f}\u{3099}-\u{309a}\u{a620}-\u{a629}\u{a66f}\u{a674}-\u{a67d}\u{a69e}-\u{a69f}\u{a6f0}-\u{a6f1}\u{a802}\u{a806}\u{a80b}\u{a823}-\u{a827}\u{a82c}\u{a880}-\u{a881}\u{a8b4}-\u{a8c5}\u{a8d0}-\u{a8d9}\u{a8e0}-\u{a8f1}\u{a8ff}-\u{a909}\u{a926}-\u{a92d}\u{a947}-\u{a953}\u{a980}-\u{a983}\u{a9b3}-\u{a9c0}\u{a9d0}-\u{a9d9}\u{a9e5}\u{a9f0}-\u{a9f9}\u{aa29}-\u{aa36}\u{aa43}\u{aa4c}-\u{aa4d}\u{aa50}-\u{aa59}\u{aa7b}-\u{aa7d}\u{aab0}\u{aab2}-\u{aab4}\u{aab7}-\u{aab8}\u{aabe}-\u{aabf}\u{aac1}\u{aaeb}-\u{aaef}\u{aaf5}-\u{aaf6}\u{abe3}-\u{abea}\u{abec}-\u{abed}\u{abf0}-\u{abf9}\u{fb1e}\u{fe00}-\u{fe0f}\u{fe20}-\u{fe2f}\u{fe33}-\u{fe34}\u{fe4d}-\u{fe4f}\u{ff10}-\u{ff19}\u{ff3f}\u{ff9e}-\u{ff9f}\u{101fd}\u{102e0}\u{10376}-\u{1037a}\u{104a0}-\u{104a9}\u{10a01}-\u{10a03}\u{10a05}-\u{10a06}\u{10a0c}-\u{10a0f}\u{10a38}-\u{10a3a}\u{10a3f}\u{10ae5}-\u{10ae6}\u{10d24}-\u{10d27}\u{10d30}-\u{10d39}\u{10eab}-\u{10eac}\u{10efd}-\u{10eff}\u{10f46}-\u{10f50}\u{10f82}-\u{10f85}\u{11000}-\u{11002}\u{11038}-\u{11046}\u{11066}-\u{11070}\u{11073}-\u{11074}\u{1107f}-\u{11082}\u{110b0}-\u{110ba}\u{110c2}\u{110f0}-\u{110f9}\u{11100}-\u{11102}\u{11127}-\u{11134}\u{11136}-\u{1113f}\u{11145}-\u{11146}\u{11173}\u{11180}-\u{11182}\u{111b3}-\u{111c0}\u{111c9}-\u{111cc}\u{111ce}-\u{111d9}\u{1122c}-\u{11237}\u{1123e}\u{11241}\u{112df}-\u{112ea}\u{112f0}-\u{112f9}\u{11300}-\u{11303}\u{1133b}-\u{1133c}\u{1133e}-\u{11344}\u{11347}-\u{11348}\u{1134b}-\u{1134d}\u{11357}\u{11362}-\u{11363}\u{11366}-\u{1136c}\u{11370}-\u{11374}\u{11435}-\u{11446}\u{11450}-\u{11459}\u{1145e}\u{114b0}-\u{114c3}\u{114d0}-\u{114d9}\u{115af}-\u{115b5}\u{115b8}-\u{115c0}\u{115dc}-\u{115dd}\u{11630}-\u{11640}\u{11650}-\u{11659}\u{116ab}-\u{116b7}\u{116c0}-\u{116c9}\u{1171d}-\u{1172b}\u{11730}-\u{11739}\u{1182c}-\u{1183a}\u{118e0}-\u{118e9}\u{11930}-\u{11935}\u{11937}-\u{11938}\u{1193b}-\u{1193e}\u{11940}\u{11942}-\u{11943}\u{11950}-\u{11959}\u{119d1}-\u{119d7}\u{119da}-\u{119e0}\u{119e4}\u{11a01}-\u{11a0a}\u{11a33}-\u{11a39}\u{11a3b}-\u{11a3e}\u{11a47}\u{11a51}-\u{11a5b}\u{11a8a}-\u{11a99}\u{11c2f}-\u{11c36}\u{11c38}-\u{11c3f}\u{11c50}-\u{11c59}\u{11c92}-\u{11ca7}\u{11ca9}-\u{11cb6}\u{11d31}-\u{11d36}\u{11d3a}\u{11d3c}-\u{11d3d}\u{11d3f}-\u{11d45}\u{11d47}\u{11d50}-\u{11d59}\u{11d8a}-\u{11d8e}\u{11d90}-\u{11d91}\u{11d93}-\u{11d97}\u{11da0}-\u{11da9}\u{11ef3}-\u{11ef6}\u{11f00}-\u{11f01}\u{11f03}\u{11f34}-\u{11f3a}\u{11f3e}-\u{11f42}\u{11f50}-\u{11f59}\u{13440}\u{13447}-\u{13455}\u{16a60}-\u{16a69}\u{16ac0}-\u{16ac9}\u{16af0}-\u{16af4}\u{16b30}-\u{16b36}\u{16b50}-\u{16b59}\u{16f4f}\u{16f51}-\u{16f87}\u{16f8f}-\u{16f92}\u{16fe4}\u{16ff0}-\u{16ff1}\u{1bc9d}-\u{1bc9e}\u{1cf00}-\u{1cf2d}\u{1cf30}-\u{1cf46}\u{1d165}-\u{1d169}\u{1d16d}-\u{1d172}\u{1d17b}-\u{1d182}\u{1d185}-\u{1d18b}\u{1d1aa}-\u{1d1ad}\u{1d242}-\u{1d244}\u{1d7ce}-\u{1d7ff}\u{1da00}-\u{1da36}\u{1da3b}-\u{1da6c}\u{1da75}\u{1da84}\u{1da9b}-\u{1da9f}\u{1daa1}-\u{1daaf}\u{1e000}-\u{1e006}\u{1e008}-\u{1e018}\u{1e01b}-\u{1e021}\u{1e023}-\u{1e024}\u{1e026}-\u{1e02a}\u{1e08f}\u{1e130}-\u{1e136}\u{1e140}-\u{1e149}\u{1e2ae}\u{1e2ec}-\u{1e2f9}\u{1e4ec}-\u{1e4f9}\u{1e8d0}-\u{1e8d6}\u{1e944}-\u{1e94a}\u{1e950}-\u{1e959}\u{1fbf0}-\u{1fbf9}\u{e0100}-\u{e01ef}]
IDENTIFIER_START={XID_START}|_
IDENTIFIER_PART={XID_START}|{XID_CONTINUE_EXTRA}

// 标识符类型

PLAIN_IDENTIFIER={IDENTIFIER_START}{IDENTIFIER_PART}*  // 普通标识符
BOOLEAN_LITERAL= true | false         // 布尔字面量
ESCAPED_IDENTIFIER = `[^`\r\n]+`      // 反引号包围的标识符（不含回车）
IDENTIFIER={ESCAPED_IDENTIFIER}|{PLAIN_IDENTIFIER}  // 完整标识符定义
FIELD_IDENTIFIER = \${IDENTIFIER}     // 字段标识符（以$开头）

/* 注释
 * ====
 */
EOL_COMMENT="/""/"[^\r\n]*           // 单行注释（到行尾，不含回车）

/* 字符和字符串相关定义
 * ================
 * 支持以下特性：
 * 1. 转义序列
 * 2. Unicode转义
 * 3. 原始字符串
 * 4. 字符串插值
 */

// 转义序列
ESCAPE_SEQUENCE=\\(u\{ {HEX_DIGIT}{1,8} \} | [^\r\n])  // 支持Unicode转义和普通转义

// 字节字符字面量
CHARACTER_BYTE_LITERAL = {CHARACTER_BYTE_SINGLE_LITERAL}  // 字节字符（b前缀）
CHARACTER_BYTE_SINGLE_LITERAL = b \'  ({SINGLE_CHAR}  | {ESCAPE_SEQUENCE})*  \'

// 基本字符定义
SINGLE_CHAR=[^'\\\r\n]               // 单个字符（非引号、反斜杠、回车换行）

// 转义序列组合
ESCAPE_SEQ  = ({ESCAPE_SEQUENCE} | {ESCAPED_IDENTIFIER})  // 转义序列或转义标识符

// 字符字面量
RUNE_LITERAL = {RUNE_SINGLE_LITERAL} | {RUNE_DOUBLE_LITERAL}  // 字符字面量（支持单双引号）
RUNE_SINGLE_LITERAL = r \'  ({SINGLE_CHAR} | {ESCAPE_SEQ})*  \'  // 单引号字符
RUNE_DOUBLE_LITERAL = r \" ({SINGLE_CHAR} | {ESCAPE_SEQ})* \"   // 双引号字符

/* 字符串引号定义
 * ===========
 * 支持多种字符串界定符：
 * 1. 三引号字符串（原始字符串）
 * 2. 哈希字符串（可变长度分隔符）
 * 3. 普通字符串（单引号或双引号）
 */

// 三引号字符串
THREE_QUO_SINGLE =   (\'\'\')        // 三个单引号
THREE_OR_SINGLE_MORE_QUO = ({THREE_QUO_SINGLE}\'*)  // 三个或更多单引号
THREE_QUO_DOUBLE =   (\"\"\")        // 三个双引号
THREE_OR_DOUBLE_MORE_QUO = ({THREE_QUO_DOUBLE}\"*)  // 三个或更多双引号

// 哈希字符串
HASH_QUO_SINGLE = (#+\')             // 哈希加单引号（开始）
HSAH_OR_SINGLE_MORE_QUO = (\'#+)     // 单引号加哈希（结束）
HASH_QUO_DOUBLE = (#+\")             // 哈希加双引号（开始）
HSAH_OR_DOUBLE_MORE_QUO = (\"#+)     // 双引号加哈希（结束）

// 基本引号
SINGLE_QUO = \'                      // 单引号
DOUBLE_QUO = \"                      // 双引号

/* 字符串内容和插值
 * =============
 */

// 字符串内容
REGULAR_STRING_PART_DOUBLE=[^\\\"\r\n\$]+  // CR 独立成段，保证完整 CRLF 使用最长换行匹配
REGULAR_STRING_PART_SINGLE=[^\\\'\r\n\$]+  // CR 独立成段，保证完整 CRLF 使用最长换行匹配
INTERPOLATION = \$+                      // 字符串插值标记

// 字符串模板
SHORT_TEMPLATE_ENTRY={INTERPOLATION}{IDENTIFIER}  // 简单模板插值（$标识符）

// 特殊标记
LONELY_DOLLAR=\$                     // 单独的美元符号
LONG_TEMPLATE_ENTRY_START=\$\{       // 长模板插值开始标记
LONELY_BACKTICK=`                    // 单独的反引号

/* 数字相关定义
 * ==========
 * 支持以下数字格式：
 * 1. 整数：十进制、十六进制、二进制、八进制
 * 2. 浮点数：小数点表示、科学计数法
 * 3. 数字可以用下划线分隔以提高可读性
 */

// 基本数字字符

DIGIT=[0-9]                           // 单个数字字符
DIGIT_OR_UNDERSCORE=[0-9_]           // 数字或下划线（用于分隔数字）

DIGITS_WITH_TRAILING_UNDERSCORE={DIGIT}{DIGIT_OR_UNDERSCORE}*  // 数字序列，允许尾随下划线

// 不同进制的数字
HEX_DIGIT=[0-9A-Fa-f]                // 十六进制数字（0-9和A-F）
HEX_DIGIT_OR_UNDERSCORE=[0-9A-Fa-f_] // 十六进制数字或下划线
HEX_DIGITS_WITH_TRAILING_UNDERSCORE = {HEX_DIGIT}{HEX_DIGIT_OR_UNDERSCORE}*
BIN_DIGIT=[0-1]                      // 二进制数字（0和1）
BIN_DIGIT_OR_UNDERSCORE=[01_]        // 二进制数字或下划线
BIN_DIGITS_WITH_TRAILING_UNDERSCORE = {BIN_DIGIT}{BIN_DIGIT_OR_UNDERSCORE}*

OCT_DIGIT=[0-7]                      // 八进制数字（0-7）
OCT_DIGIT_OR_UNDERSCORE=[0-7_]       // 八进制数字或下划线
OCT_DIGITS_WITH_TRAILING_UNDERSCORE = {OCT_DIGIT}{OCT_DIGIT_OR_UNDERSCORE}*

// 整数类型后缀
INTEGER_SUFFIX=(u8|u16|u32|u64|i8|i16|i32|i64)  // 整数类型后缀

// 整数字面量（支持所有进制）
INTEGER_LITERAL=({DECIMAL_INTEGER_LITERAL}|{HEX_INTEGER_LITERAL}|{BIN_INTEGER_LITERAL}|{OCT_INTEGER_LITERAL}){INTEGER_SUFFIX}?

// 各种进制的整数表示
DECIMAL_INTEGER_LITERAL=0(_*)?|[1-9]{DIGIT_OR_UNDERSCORE}*  // 十进制整数
HEX_INTEGER_LITERAL=0[Xx]{HEX_DIGITS_WITH_TRAILING_UNDERSCORE}  // 十六进制整数
BIN_INTEGER_LITERAL=0[Bb]{BIN_DIGITS_WITH_TRAILING_UNDERSCORE} // 二进制整数
OCT_INTEGER_LITERAL=0[Oo]{OCT_DIGITS_WITH_TRAILING_UNDERSCORE} // 八进制整数

// 浮点数类型后缀
FLOAT_SUFFIX=(f32|f16|f64)           // 浮点数类型后缀

// 浮点数字面量（支持十进制和十六进制）

FLOAT_LITERAL={ FLOAT_LITERAL_POINT} |{HEX_FLOATING_POINT_LITERAL}

// 十进制浮点数
FLOAT_LITERAL_POINT = ({FLOATING_POINT_LITERAL1}|{FLOATING_POINT_LITERAL2}|{FLOATING_POINT_LITERAL3}){FLOAT_SUFFIX}?
FLOATING_POINT_LITERAL1=({DIGITS_WITH_TRAILING_UNDERSCORE})"."({DIGITS_WITH_TRAILING_UNDERSCORE})({EXPONENT_PART})? // 123__.45__, 8__.1__
FLOATING_POINT_LITERAL2="."({DIGITS_WITH_TRAILING_UNDERSCORE})({EXPONENT_PART})?           // .45__, .45__e10__
FLOATING_POINT_LITERAL3=({DIGITS_WITH_TRAILING_UNDERSCORE})({EXPONENT_PART})               // 123__e10__
// 十六进制浮点数
HEX_FLOATING_POINT_LITERAL= {HEX_FLOATING_POINT_LITERAL1} |{HEX_FLOATING_POINT_LITERAL2} |{HEX_FLOATING_POINT_LITERAL3}
HEX_FLOATING_POINT_LITERAL1= 0[xX]{HEX_DIGITS_WITH_TRAILING_UNDERSCORE}"."({HEX_DIGITS_WITH_TRAILING_UNDERSCORE})[pP][-]?{DIGITS_WITH_TRAILING_UNDERSCORE}
HEX_FLOATING_POINT_LITERAL2= 0[xX]"."({HEX_DIGITS_WITH_TRAILING_UNDERSCORE})[pP][-]?{DIGITS_WITH_TRAILING_UNDERSCORE}
HEX_FLOATING_POINT_LITERAL3= 0[xX]{HEX_DIGITS_WITH_TRAILING_UNDERSCORE}[pP][-]?{DIGITS_WITH_TRAILING_UNDERSCORE}

// 指数部分
EXPONENT_PART=[Ee][-]?{DIGITS_WITH_TRAILING_UNDERSCORE}      // 科学计数法的指数部分，如e10__, e+10__, e-10__

%%

/* 词法规则部分
 * ==========
 * 本部分定义了词法分析器的所有规则，按以下类别组织：
 * 1. 字符串处理规则：包括各种字符串字面量和字符串插值
 * 2. 注释处理规则：包括单行注释、块注释和文档注释
 * 3. 基本词法单元：包括空白字符、标识符、关键字等
 * 4. 错误处理规则：处理未匹配字符和特殊状态错误
 */

/* 字符串处理规则
 * ============
 * 支持以下类型的字符串：
 * 1. 哈希字符串：#"..."# 或 #'...'#，支持可变数量的#作为分隔符
 * 2. 原始字符串："""...""" 或 '''...'''，不处理转义序列
 * 3. 普通字符串："..." 或 '...'，支持转义序列
 * 4. 字符串插值：${...}
 */

// 哈希字符串（单引号）处理
{HASH_QUO_SINGLE} {
    lBraceCount = yytext().length() - 1;  // 记录哈希字符数量
    pushState("多行字符串字面量",HSAH_STRING_SINGLE);
    return CjTokens.OPEN_QUOTE;
}

// 哈希字符串内容处理
<HSAH_STRING_SINGLE> {
    {LINE_TERMINATOR}     { return CjTokens.REGULAR_STRING_PART; }  // 换行符处理
    \" | \'          { return CjTokens.REGULAR_STRING_PART; }  // 引号处理
    \\               { return CjTokens.REGULAR_STRING_PART; }  // 反斜杠处理
    {LONELY_DOLLAR}  { return CjTokens.REGULAR_STRING_PART; }
    {HSAH_OR_SINGLE_MORE_QUO} {  // 结束引号处理
        int lenght = yytext().length() - 1;
        int lBraceCount1 = getState() != null ? getState().lBraceCount : lBraceCount;
        if(lBraceCount1 == lenght){  // 哈希数量匹配
            popState();
            return CjTokens.CLOSING_QUOTE;
        }
        else if(lBraceCount1 < lenght){  // 哈希数量过多
            popState();
            yypushback(lenght - lBraceCount1);
            return CjTokens.CLOSING_QUOTE;
        }else {  // 哈希数量不足
            return CjTokens.REGULAR_STRING_PART;
        }
    }
}

// 哈希字符串（双引号）处理
{HASH_QUO_DOUBLE} {
    lBraceCount = yytext().length() - 1;  // 记录哈希字符数量
    pushState("HSAH_STRING_DOUBLE",HSAH_STRING_DOUBLE);
    return CjTokens.OPEN_QUOTE;
}

// 哈希字符串（双引号）内容处理
<HSAH_STRING_DOUBLE> {
    {LINE_TERMINATOR}     { return CjTokens.REGULAR_STRING_PART; }  // 换行符处理
    \" | \'          { return CjTokens.REGULAR_STRING_PART; }  // 引号处理
    \\               { return CjTokens.REGULAR_STRING_PART; }  // 反斜杠处理
    {LONELY_DOLLAR}  { return CjTokens.REGULAR_STRING_PART; }

    {HSAH_OR_DOUBLE_MORE_QUO} {  // 结束引号处理
        int lenght = yytext().length() - 1;
        int lBraceCount1 = getState() != null ? getState().lBraceCount : lBraceCount;
        if(lBraceCount1 == lenght){  // 哈希数量匹配
            popState();
            return CjTokens.CLOSING_QUOTE;
        }
        else if(lBraceCount1 < lenght){  // 哈希数量过多
            popState();
            yypushback(lenght - lBraceCount1);
            return CjTokens.CLOSING_QUOTE;
        }else {  // 哈希数量不足
            return CjTokens.REGULAR_STRING_PART;
        }
    }
}

/* 原始字符串处理
 * ============
 * 原始字符串不处理转义序列，所有字符都按原样处理
 * 支持单引号和双引号两种形式：
 * 1. 单引号：'''...'''
 * 2. 双引号："""..."""
 */

// 原始字符串（单引号）处理
{THREE_QUO_SINGLE} {
    pushState("RAW_STRING_SINGLE",RAW_STRING_SINGLE);
    return CjTokens.OPEN_QUOTE;
}

// 原始字符串（单引号）内容处理
<RAW_STRING_SINGLE> {
    {LINE_TERMINATOR}     { return CjTokens.REGULAR_STRING_PART; }  // 换行符处理
    \" | \'          { return CjTokens.REGULAR_STRING_PART; }  // 引号处理
    \\               { return CjTokens.REGULAR_STRING_PART; }  // 反斜杠处理
    {THREE_OR_SINGLE_MORE_QUO} {  // 结束引号处理
        int length = yytext().length();
        if (length <= 3) {  // 正好三个引号
            popState();
            return CjTokens.CLOSING_QUOTE;
        } else {  // 超过三个引号，回退多余部分
            yypushback(3);
            return CjTokens.REGULAR_STRING_PART;
        }
    }
}

// 原始字符串（双引号）处理
{THREE_QUO_DOUBLE} {
    pushState("RAW_STRING_DOUBLE",RAW_STRING_DOUBLE);
    return CjTokens.OPEN_QUOTE;
}

// 原始字符串（双引号）内容处理
<RAW_STRING_DOUBLE> {
    {LINE_TERMINATOR}     { return CjTokens.REGULAR_STRING_PART; }  // 换行符处理
    \" | \'          { return CjTokens.REGULAR_STRING_PART; }  // 引号处理
    \\               { return CjTokens.REGULAR_STRING_PART; }  // 反斜杠处理
    {THREE_OR_DOUBLE_MORE_QUO} {  // 结束引号处理
        int length = yytext().length();
        if (length <= 3) {  // 正好三个引号
            popState();
            return CjTokens.CLOSING_QUOTE;
        } else {  // 超过三个引号，回退多余部分
            yypushback(3);
            return CjTokens.REGULAR_STRING_PART;
        }
    }
}

//b\" {
//    pushState(STRING);
//    return CjTokens.OPEN_QUOTE;
//}


 {SINGLE_QUO} {
    pushState("STRING_SINGLE",STRING_SINGLE);
    return CjTokens.OPEN_QUOTE;
}
<STRING_SINGLE> {LINE_TERMINATOR} {
    popState();
    yypushback(yylength());
    return CjTokens.DANGLING_NEWLINE;
}
<STRING_SINGLE>     {SINGLE_QUO} {
    popState();
    return CjTokens.CLOSING_QUOTE;
}
<STRING_SINGLE> {ESCAPE_SEQUENCE} {
    return CjTokens.ESCAPE_SEQUENCE;
}

{DOUBLE_QUO} {
    pushState("STRING_DOUBLE",STRING_DOUBLE);
    return CjTokens.OPEN_QUOTE;
}
<STRING_DOUBLE> {LINE_TERMINATOR} {
    popState();
    yypushback(yylength());
    return CjTokens.DANGLING_NEWLINE;
}
<STRING_DOUBLE>     {DOUBLE_QUO} {
    popState();
    return CjTokens.CLOSING_QUOTE;
}
<STRING_DOUBLE> {ESCAPE_SEQUENCE} {
    return CjTokens.ESCAPE_SEQUENCE;
}

// 孤立 CR 是内容；CRLF 的两字符换行规则优先于该单字符匹配。
<STRING_DOUBLE,STRING_SINGLE,RAW_STRING_DOUBLE,RAW_STRING_SINGLE,HSAH_STRING_DOUBLE,HSAH_STRING_SINGLE> \r {
    return CjTokens.REGULAR_STRING_PART;
}


<STRING_DOUBLE,STRING_SINGLE> {ESCAPE_SEQUENCE} {
    return CjTokens.ESCAPE_SEQUENCE;
}

<STRING_SINGLE, RAW_STRING_SINGLE ,HSAH_STRING_SINGLE> {REGULAR_STRING_PART_SINGLE} {
    return CjTokens.REGULAR_STRING_PART;
}
<STRING_DOUBLE, RAW_STRING_DOUBLE ,HSAH_STRING_DOUBLE> {REGULAR_STRING_PART_DOUBLE} {
    return CjTokens.REGULAR_STRING_PART;
}

<STRING_SINGLE,STRING_DOUBLE, RAW_STRING_DOUBLE,RAW_STRING_SINGLE> {SHORT_TEMPLATE_ENTRY} {
    int interpolationPrefix = 0;
    for (int i = 0; i < yylength(); i++) {
        if (yycharat(i) == '$') { interpolationPrefix++; }
        else { break; }
    }
    int rest = yylength() - interpolationPrefix;
    if (interpolationPrefix == requiredInterpolationPrefix) {
        pushState("SHORT_TEMPLATE_ENTRY",SHORT_TEMPLATE_ENTRY);
        yypushback(rest);
        return CjTokens.SHORT_TEMPLATE_ENTRY_START;
    } else if (interpolationPrefix < requiredInterpolationPrefix) {
        yypushback(rest);
        return CjTokens.REGULAR_STRING_PART;
    } else {
        yypushback(requiredInterpolationPrefix + rest);
        return CjTokens.REGULAR_STRING_PART;
    }
}


// Only *this* keyword is itself an expression valid in this position
// *null*, *true* and *false* are also keywords and expression, but it does not make sense to put them
// in a string template for it'd be easier to just type them in without a dollar
<SHORT_TEMPLATE_ENTRY> "this" {
    popState();
    return CjTokens.THIS_KEYWORD;
}

<SHORT_TEMPLATE_ENTRY> {IDENTIFIER} {
    popState();
    return CjTokens.IDENTIFIER;
}

<STRING_DOUBLE,STRING_SINGLE, RAW_STRING_DOUBLE,RAW_STRING_SINGLE > {LONELY_DOLLAR} {
    return CjTokens.REGULAR_STRING_PART;
}
<STRING_DOUBLE,STRING_SINGLE, RAW_STRING_DOUBLE,RAW_STRING_SINGLE> {LONG_TEMPLATE_ENTRY_START} {
    pushState( "字符串模板", LONG_TEMPLATE_ENTRY);
    return CjTokens.LONG_TEMPLATE_ENTRY_START;
}

<LONG_TEMPLATE_ENTRY> "{" {
    lBraceCount++;
    return CjTokens.LBRACE;
}
<LONG_TEMPLATE_ENTRY> "}" {
    if (lBraceCount == 0) {
        popState();
        return CjTokens.LONG_TEMPLATE_ENTRY_END;
    }
    lBraceCount--;
    return CjTokens.RBRACE;
}



/* 注释处理规则
 * ==========
 * 支持三种类型的注释：
 * 1. 空块注释：/**/
 * 2. 文档注释：/** ... */
 * 3. 块注释：/* ... */
 */

// 空块注释
"/**/" {
    return CjTokens.BLOCK_COMMENT;
}

// 文档注释
"/**" {
    pushState("DOC_COMMENT",DOC_COMMENT);
    commentDepth = 0;
    commentStart = getTokenStart();
}

// 块注释
"/*" {
    pushState("BLOCK_COMMENT",BLOCK_COMMENT);
    commentDepth = 0;
    commentStart = getTokenStart();
}

// 注释内容处理
<BLOCK_COMMENT, DOC_COMMENT> {
    "/*"     { commentDepth++; }  // 嵌套注释处理
    "*/"     {  // 注释结束处理
        if (commentDepth > 0) {
            commentDepth--;
        }
        else {
            int state = yystate();
            popState();
            zzStartRead = commentStart;
            return commentStateToTokenType(state);
        }
    }
    <<EOF>>  {  // 文件结束处理
        int state = yystate();
        popState();
        zzStartRead = commentStart;
        return commentStateToTokenType(state);
    }
    [\s\S]   {}  // 其他字符处理
}

/* 基本词法单元
 * ==========
 */

// 水平空白属于常规空白；换行作为独立 NL 词元交给语义 builder 决定是否跳过。
({HORIZONTAL_SPACE})+ {
    return CjTokens.WHITE_SPACE;
}

<YYINITIAL, LONG_TEMPLATE_ENTRY> {LINE_TERMINATOR} {
    return CjTokens.NL;
}

// 行注释
{EOL_COMMENT} {
    return CjTokens.EOL_COMMENT;
}

// 数值字面量
{FLOAT_LITERAL} {
    return CjTokens.FLOAT_LITERAL;
}

{INTEGER_LITERAL} {
    return CjTokens.INTEGER_LITERAL;
}

// 字符字面量
{RUNE_LITERAL} {
    return CjTokens.RUNE_LITERAL;
}

{CHARACTER_BYTE_LITERAL} {
    return CjTokens.CHARACTER_BYTE_LITERAL;
}

/* 关键字
 * ======
 * 按功能分类：
 * 1. 包和模块相关
 * 2. 类型声明相关
 * 3. 控制流相关
 * 4. 修饰符
 * 5. 基本类型
 * 6. 变量声明
 * 7. 特殊关键字
 */

// 包和模块相关关键字
"package"    { return CjTokens.PACKAGE_KEYWORD; }
"import"     { return CjTokens.IMPORT_KEYWORD; }
"foreign"    { return CjTokens.FOREIGN_KEYWORD; }
"features"   { return CjTokens.FEATURES_KEYWORD; }

// 类型声明相关关键字
"interface"  { return CjTokens.INTERFACE_KEYWORD; }
"class"      { return CjTokens.CLASS_KEYWORD; }
"enum"       { return CjTokens.ENUM_KEYWORD; }
"struct"     { return CjTokens.STRUCT_KEYWORD; }
"type"       { return CjTokens.TYPE_KEYWORD; }
"extend"     { return CjTokens.EXTEND_KEYWORD; }

// 控制流相关关键字
"if"         { return CjTokens.IF_KEYWORD; }
"else"       { return CjTokens.ELSE_KEYWORD; }
"for"        { return CjTokens.FOR_KEYWORD; }
"while"      { return CjTokens.WHILE_KEYWORD; }
"do"         { return CjTokens.DO_KEYWORD; }
"break"      { return CjTokens.BREAK_KEYWORD; }
"continue"   { return CjTokens.CONTINUE_KEYWORD; }
"return"     { return CjTokens.RETURN_KEYWORD; }
"throw"      { return CjTokens.THROW_KEYWORD; }
"perform"    { return CjTokens.PERFORM_KEYWORD; }
"resume"     { return CjTokens.RESUME_KEYWORD; }
"throwing"   { return CjTokens.THROWING_KEYWORD; }
"try"        { return CjTokens.TRY_KEYWORD; }
"catch"      { return CjTokens.CATCH_KEYWORD; }
"handle"     { return CjTokens.HANDLE_KEYWORD; }
"finally"    { return CjTokens.FINALLY_KEYWORD; }
"match"      { return CjTokens.MATCH_KEYWORD; }
"case"       { return CjTokens.CASE_KEYWORD; }

// 修饰符关键字
"public"     { return CjTokens.PUBLIC_KEYWORD; }
"private"    { return CjTokens.PRIVATE_KEYWORD; }
"protected"  { return CjTokens.PROTECTED_KEYWORD; }
"internal"   { return CjTokens.INTERNAL_KEYWORD; }
"static"     { return CjTokens.STATIC_KEYWORD; }
"abstract"   { return CjTokens.ABSTRACT_KEYWORD; }
"override"   { return CjTokens.OVERRIDE_KEYWORD; }
"redef"      { return CjTokens.REDEF_KEYWORD; }
"operator"   { return CjTokens.OPERATOR_KEYWORD; }
"synchronized" { return CjTokens.SYNCHRONIZED_KEYWORD; }
"unsafe"     { return CjTokens.UNSAFE_KEYWORD; }
"mut"        { return CjTokens.MUT_KEYWORD; }
"common"     { return CjTokens.COMMON_KEYWORD; }
"specific"   { return CjTokens.SPECIFIC_KEYWORD; }

// 基本类型关键字
"Int8"       { return CjTokens.INT8_KEYWORD; }
"Int16"      { return CjTokens.INT16_KEYWORD; }
"Int32"      { return CjTokens.INT32_KEYWORD; }
"Int64"      { return CjTokens.INT64_KEYWORD; }
"UInt8"      { return CjTokens.UINT8_KEYWORD; }
"UInt16"     { return CjTokens.UINT16_KEYWORD; }
"UInt32"     { return CjTokens.UINT32_KEYWORD; }
"UInt64"     { return CjTokens.UINT64_KEYWORD; }
"Float16"    { return CjTokens.FLOAT16_KEYWORD; }
"Float32"    { return CjTokens.FLOAT32_KEYWORD; }
"Float64"    { return CjTokens.FLOAT64_KEYWORD; }
"IntNative"  { return CjTokens.INTNATIVE_KEYWORD; }
"UIntNative" { return CjTokens.UINTNATIVE_KEYWORD; }
"Bool"       { return CjTokens.BOOL_KEYWORD; }
"Unit"       { return CjTokens.UNIT_KEYWORD; }
"Rune"       { return CjTokens.RUNE_KEYWORD; }
"Nothing"    { return CjTokens.NOTHING_KEYWORD; }

// 变量声明关键字
"let"        { return CjTokens.LET_KEYWORD; }
"var"        { return CjTokens.VAR_KEYWORD; }
"const"      { return CjTokens.CONST_KEYWORD; }
"prop"       { return CjTokens.PROP_KEYWORD; }

// 特殊关键字
"func"       { return CjTokens.FUNC_KEYWORD; }
"init"       { return CjTokens.INIT_KEYWORD; }
"main"       { return CjTokens.MAIN_KEYWORD; }
"this"       { return CjTokens.THIS_KEYWORD; }
"This"       { return CjTokens.THIS_KEYWORD_UPPER; }
"super"      { return CjTokens.SUPER_KEYWORD; }
"true"       { return CjTokens.TRUE_KEYWORD; }
"false"      { return CjTokens.FALSE_KEYWORD; }
"is"         { return CjTokens.IS_KEYWORD; }
"in"         { return CjTokens.IN_KEYWORD; }
"as"         { return CjTokens.AS_KEYWORD; }
"where"      { return CjTokens.WHERE_KEYWORD; }
"spawn"      { return CjTokens.SPAWN_KEYWORD; }
"inout"      { return CjTokens.INOUT_KEYWORD; }
"quote"      { return CjTokens.QUOTE_KEYWORD; }
"macro"      { return CjTokens.MACRO_KEYWORD; }
"VArray"     { return CjTokens.VARRAY_KEYWORD; }
"_"          { return CjTokens.UNDERLINE; }

// 标识符
{FIELD_IDENTIFIER} { return CjTokens.FIELD_IDENTIFIER; }
{IDENTIFIER}       { return CjTokens.IDENTIFIER; }

// 特殊标识符处理
\!in{IDENTIFIER_PART}  { yypushback(yylength() - 1); return CjTokens.EXCL; }
\!is{IDENTIFIER_PART}  { yypushback(yylength() - 1); return CjTokens.EXCL; }

/* 运算符和分隔符
 * ============
 * 按类型分类：
 * 1. 算术运算符
 * 2. 比较运算符
 * 3. 逻辑运算符
 * 4. 位运算符
 * 5. 赋值运算符
 * 6. 特殊运算符
 * 7. 分隔符
 */

// 算术运算符
"++"         { return CjTokens.PLUSPLUS; }
"--"         { return CjTokens.MINUSMINUS; }
"+"          { return CjTokens.PLUS; }
"-"          { return CjTokens.MINUS; }
"*"          { return CjTokens.MUL; }
"**"         { return CjTokens.MULMUL; }
"/"          { return CjTokens.DIV; }
"%"          { return CjTokens.PERC; }

// 比较运算符
"<="         { return CjTokens.LTEQ; }
"=="         { return CjTokens.EQEQ; }
"!="         { return CjTokens.EXCLEQ; }
"<"          { return CjTokens.LT; }
">"          { return CjTokens.GT; }
"<:"         { return CjTokens.LTCOLON; }

// 逻辑运算符
"&&"         { return CjTokens.ANDAND; }
"||"         { return CjTokens.OROR; }
"!"          { return CjTokens.EXCL; }

// 位运算符
"&"          { return CjTokens.AND; }
"|"          { return CjTokens.OR; }
"^"          { return CjTokens.XOR; }
"~"          { return CjTokens.TILDE; }
"<<"         { return CjTokens.LTLT; }

// 赋值运算符
"="          { return CjTokens.EQ; }
"+="         { return CjTokens.PLUSEQ; }
"-="         { return CjTokens.MINUSEQ; }
"*="         { return CjTokens.MULTEQ; }
"**="        { return CjTokens.MULMULEQ; }
"/="         { return CjTokens.DIVEQ; }
"%="         { return CjTokens.PERCEQ; }
"&="         { return CjTokens.ANDEQ; }
"|="         { return CjTokens.OREQ; }
"^="         { return CjTokens.XOREQ; }
"&&="        { return CjTokens.ANDANDEQ; }
"||="        { return CjTokens.OROREQ; }
"<<="        { return CjTokens.LTLTEQ; }

// 特殊运算符
"->"         { return CjTokens.ARROW; }
"=>"         { return CjTokens.DOUBLE_ARROW; }
"<-"         { return CjTokens.LEFT_ARROW; }
"~>"         { return CjTokens.COMPOSITION; }
"|>"         { return CjTokens.PIPELINE; }
".."         { return CjTokens.RANGE; }
"..="        { return CjTokens.RANGEEQ; }
"?"          { return CjTokens.QUEST; }
"..."        { return CjTokens.ELLIPSIS; }
// 分隔符
"\\("        { return CjTokens.ESCAPE_LPAR; }
"\\)"        { return CjTokens.ESCAPE_RPAR; }
"\\["        { return CjTokens.ESCAPE_LBRACKET; }
"\\]"        { return CjTokens.ESCAPE_RBRACKET; }
"\\@"        { return CjTokens.ESCAPE_AT; }
"["          { return CjTokens.LBRACKET; }
"]"          { return CjTokens.RBRACKET; }
"{"          { return CjTokens.LBRACE; }
"}"          { return CjTokens.RBRACE; }
"("          { return CjTokens.LPAR; }
")"          { return CjTokens.RPAR; }
"."          { return CjTokens.DOT; }
"::"         { return CjTokens.DOUBLE_COLON; }
":"          { return CjTokens.COLON; }
";"          { return CjTokens.SEMICOLON; }
","          { return CjTokens.COMMA; }
"@"          { return CjTokens.AT; }
"@!"          { return CjTokens.ATEXCL; }
"$"          { return CjTokens.DOLLAR; }

/* 错误处理规则
 * ==========
 */

// 未匹配的反引号
{LONELY_BACKTICK} {
    pushState(UNMATCHED_BACKTICK);
    return TokenType.BAD_CHARACTER;
}

// 原生 GetToken 的 IsMacroEscape 仅看紧邻原字符：合法候选保留 ILLEGAL 前缀供 scanner 消费。
// JFlex trailing context 不消费目标字符；其余反斜杠与普通未知字符一同产生 LEX 报告。
<YYINITIAL, LONG_TEMPLATE_ENTRY> \\ / [\$@()\[\]] {
    return TokenType.BAD_CHARACTER;
}

// 普通代码和字符串插值表达式中的未知字符有独立词法种类，后续诊断不依赖 parser 消费顺序。
<YYINITIAL, LONG_TEMPLATE_ENTRY> [\s\S] {
    return CjTokens.UNKNOWN_TOKEN_START;
//    String errorMessage = String.format("Unexpected character '%s' at position %d", yytext(), getTokenStart());
//    throw new CangJieLexerException(errorMessage, getTokenStart());
}

// 未闭合反引号状态仍需消费任意后续字符，避免词法扫描器对非法输入抛出无匹配异常。
<UNMATCHED_BACKTICK> [\s\S] {
    return TokenType.BAD_CHARACTER;
}

// 特定状态的错误处理
<STRING_DOUBLE,STRING_SINGLE,RAW_STRING_DOUBLE,RAW_STRING_SINGLE,SHORT_TEMPLATE_ENTRY,BLOCK_COMMENT,DOC_COMMENT,HSAH_STRING_DOUBLE,HSAH_STRING_SINGLE> . {
//    String state = yystate() == STRING_DOUBLE ? "string" :
//                  yystate() == STRING_SINGLE ? "string" :
//                  yystate() == RAW_STRING_DOUBLE ? "raw string" :
//                  yystate() == RAW_STRING_SINGLE ? "raw string" :
//                  yystate() == SHORT_TEMPLATE_ENTRY ? "string template" :
//                  yystate() == BLOCK_COMMENT ? "block comment" :
//                  yystate() == DOC_COMMENT ? "doc comment" :
//                  yystate() == HSAH_STRING_DOUBLE ? "hash string" :
//                  yystate() == HSAH_STRING_SINGLE ? "hash string" : "unknown";
//    String errorMessage = String.format("Unexpected character '%s' in %s at position %d", yytext(), state, getTokenStart());
//    throw new CangJieLexerException(errorMessage, getTokenStart());
    return TokenType.BAD_CHARACTER;
}
