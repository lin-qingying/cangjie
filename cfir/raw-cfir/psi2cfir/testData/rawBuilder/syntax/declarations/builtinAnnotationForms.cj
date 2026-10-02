// RUN_PIPELINE_TILL: FRONTEND
// 剩余可写的内置注解形态与内建编译标记形态：
//   1. @Attribute[...]：identifier / 字符串字面量 / 上下文关键字属性值，逗号分隔，空参数 [] 与不带方括号的裸写法；
//      宿主覆盖类、interface、struct、enum、顶层函数、顶层变量、成员变量、成员函数、prop、函数形参、init 形参、extend 成员函数。
//   2. @EnsurePreparedToMock：类 / 顶层函数 / 成员函数位置。
//   3. @Deprecated：空参数 []、具名 message、裸写法（位置参数 + since + strict 的组合见 annotationForms.cj，不重复）。
//   4. @Overflow*：方括号形式，方括号内的 identifier 独立决定溢出策略，与注解名解耦。
//   5. @When：cjc_version / backend / debug / test 内置条件变量与 !debug、括号组合的多条件写法（os / arch 见 whenConditions.cj，不重复）。
//   6. @Annotation：自定义注解类型声明（必须 const init），以及 @X 裸写法与 @X[] 空参数写法两种使用形式。
//   7. @sourcePackage() / @sourceFile() / @sourceLine()：作为表达式、顶层 let 初值、算术运算操作数、具名形参默认值、字符串插值。
//   8. 多个内建注解在同一声明上的叠加。
// 官方依据：
//   include/cangjie/Parse/Parser.h:52 NAME_TO_ANNO_KIND（ATTRIBUTE / ENSURE_PREPARED_TO_MOCK / DEPRECATED / ANNOTATION / WHEN /
//     NUMERIC_OVERFLOW 共 15 个内建注解名）；src/Parse/ParseUtils.cpp:22 GetContextualKeyword。
//   src/Parse/ParseAnnotations.cpp:19 ParserImpl::ParseAttributeAnnotation（:28 只收 IDENTIFIER、STRING_LITERAL 与上下文关键字，
//     逗号分隔；未见 '[' 时直接返回，故 @Attribute 裸写法合法）；:41 ParserImpl::ParseOverflowAnnotation（:46 见 '[' 时方括号内
//     单个 IDENTIFIER 经 Utils::StringToOverflowStrategy 决定策略，:53 未见 '[' 时由注解名后缀决定）；:63
//     ParserImpl::ParseWhenAnnotation（[] 内为表达式，只接受一元 / 二元 / 引用表达式）；:110
//     ParserImpl::ParseAnnotationArguments（通用 key: value 列表，逗号后可跟 ']'）；:266
//     ParserImpl::CheckDeprecatedAnnotation（message / since / strict 三种实参）；:296 ParserImpl::ParseAnnotation 按 kind 分派。
//   src/Parse/ParseDecl.cpp:1877 ParserImpl::ParseParamInParamList（:1888 先 ParseAnnotations 再 ParseModifiers，形参可带注解）。
//   src/Parse/ParseMacro.cpp:144 IsBuiltinMacro（sourcePackage / sourceFile / sourceLine 三个内建宏）。
//   文档：《内置编译标记》之「源码位置」《@Attribute》《@Deprecated》与「@Deprecated 参数」；《注解》之「测试框架注解」「自定义注解」
//     与 AnnotationKind 目标位；《条件编译》之「内置编译条件变量」（os / backend / arch / cjc_version / debug / test）与「多条件编译」。
// 取舍说明（均为 cjc 1.0.5 / 1.1.3 实测，故不写入本文件）：
//   @EnsurePreparedToMock 文档限定只能用于 lambda 且需要 --test / --mock；lambda 形态在无 --test 时报 sema_mock_not_in_test_mode，
//     而上述声明位置的裸形态两版均零诊断，故按实测记录声明位置形态。
//   @When 的版本条件变量是 cjc_version 而非 version；@When[version >= "1.0.0"] 两版均报
//     conditional_compilation_not_support_this_condition。
//   @ConstSafe 仅 std 包可见（ParseAnnotations.cpp:147 STD_ONLY_ANNO），非 std 包报 sema_undeclared_identifier。
// cjc 1.0.5 / 1.1.3 零诊断。

// —— @Attribute：属性值形态（identifier / 字符串 / 上下文关键字，逗号分隔）——
@Attribute[a, b, "s"]
class BuiltinAnnoClass {}

@Attribute[State]
var builtinAnnoVarA: Int64 = 0

@Attribute["Binding"]
var builtinAnnoVarB: Int64 = 0

@Attribute[public, open]
class BuiltinAnnoCtxKeyword {}

// —— @Attribute：空参数 [] 与不带方括号的裸写法 ——
@Attribute[]
class BuiltinAnnoEmpty {}

@Attribute
class BuiltinAnnoBare {}

// —— @Attribute：声明宿主 ——
@Attribute[State]
interface BuiltinAnnoIface {
    func f(): Int64
}

@Attribute[State]
struct BuiltinAnnoStruct {
    var v: Int64 = 0
}

@Attribute[State]
enum BuiltinAnnoEnum {
    | A
    | B
}

@Attribute[State]
func builtinAnnoFunc(x: Int64): Int64 {
    return x
}

// 形参上的注解：ParseParamInParamList 先解析注解再解析修饰符与形参本体。
@Attribute[In]
func builtinAnnoParam(@Attribute[State] x: Int64): Int64 {
    return x
}

class BuiltinAnnoMember {
    @Attribute[State]
    var v: Int64 = 0

    @Attribute["Binding", other]
    func m(@Attribute[In] x: Int64): Int64 {
        return v + x
    }

    @Attribute["Binding"]
    prop q: Int64 {
        get() {
            return v
        }
    }

    init(@Attribute[In] y: Int64) {
        this.v = y
    }
}

extend Int64 {
    @Attribute[State]
    func builtinAnnoExtendMember(): Int64 {
        return this
    }
}

// —— @EnsurePreparedToMock：声明位置形态 ——
@EnsurePreparedToMock
class BuiltinAnnoMockClass {}

@EnsurePreparedToMock
func builtinAnnoMockFunc(): Int64 {
    return 1
}

class BuiltinAnnoMockMember {
    @EnsurePreparedToMock
    func f(): Int64 {
        return 1
    }
}

// —— @Deprecated：空参数 / 具名 message / 裸写法 ——
@Deprecated[]
class BuiltinAnnoDepEmpty {}

@Deprecated[message: "msg"]
class BuiltinAnnoDepNamed {}

@Deprecated
class BuiltinAnnoDepBare {}

// —— @Overflow*：方括号形式，方括号内的 identifier 独立决定策略 ——
@OverflowWrapping[wrapping]
func builtinAnnoOverflowWrapping(x: UInt8): UInt8 {
    return x + 1u8
}

@OverflowThrowing[saturating]
func builtinAnnoOverflowSaturating(x: UInt8): UInt8 {
    return x + 1u8
}

// —— @When：内置条件变量 cjc_version / backend / debug / test 与多条件组合 ——
@When[cjc_version >= "1.0.0"]
class BuiltinAnnoWhenVersion {}

@When[backend == "cjnative"]
func builtinAnnoWhenBackend(): Int64 {
    return 1
}

@When[debug]
func builtinAnnoWhenDebug(): Int64 {
    return 1
}

@When[!debug]
func builtinAnnoWhenNotDebug(): Int64 {
    return 2
}

@When[test]
class BuiltinAnnoWhenTest {}

@When[!test]
class BuiltinAnnoWhenNotTest {}

@When[(os == "Windows" || os == "Linux") && !debug]
func builtinAnnoWhenCombo(): Int64 {
    return 3
}

// —— @Annotation：自定义注解类型声明与两种使用写法 ——
@Annotation
public class BuiltinAnnoMarked {
    const init() {}
}

@BuiltinAnnoMarked
class BuiltinAnnoMarkedBare {}

@BuiltinAnnoMarked[]
class BuiltinAnnoMarkedEmpty {}

// —— 内建编译标记：表达式宿主 ——
let builtinAnnoSrcFile: String = @sourceFile()
let builtinAnnoSrcPackage: String = @sourcePackage()

func builtinAnnoSrcExpr(line: Int64): Int64 {
    return line + @sourceLine()
}

func builtinAnnoSrcDefault(line!: Int64 = @sourceLine()): Int64 {
    return line
}

func builtinAnnoSrcInterp(): String {
    return "${@sourceFile()}:${@sourceLine()}"
}

// —— 多个内建注解在同一声明上的叠加 ——
@When[!debug]
@Attribute[State]
@Deprecated["gone", since: "1.0.0"]
@EnsurePreparedToMock
class BuiltinAnnoStacked {}