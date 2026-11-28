// LANGUAGE_VERSION: 1.1.0
// RUN_PIPELINE_TILL: FRONTEND
// CJMP 注解兼容性：按身份、字面量 kind/value 双向一一比较，nominal 声明也参与比较。
// common-only 注解锚 specific 声明，specific-only 注解锚 specific 注解，实参差异两侧各报一条。
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package cjmp_annotation_compatibility
import std.core.*

@Annotation
public common class Tag {
    public common let level: Int64 = 0
    public common const init(level_val: Int64)
}

@Annotation
public common class TextTag {
    public common let value: String = ""
    public common const init(value: String)
}

@Annotation
public common class RuneTag {
    public common let value: Rune = r'a'
    public common const init(value: Rune)
}

@!Tag[3]
public common func commonOnlyTag(): Int64

public common func specificOnlyTag(): Int64

@!Tag[1]
public common func argumentTag(): Int64

@!Tag[1 + 0]
public common func constExpressionArgumentTag(): Int64

@!TextTag["same"]
public common func sameStringLiteralTag(): Int64

@!TextTag["s\u{61}me"]
public common func equivalentStringLiteralTag(): Int64

@!RuneTag[r'\u{61}']
public common func equivalentRuneLiteralTag(): Int64

@!Tag[-1]
public common func negativeLiteralExpressionTag(): Int64

@!Tag[1]
public common class NominalTag {
    public common init()
}

@!Tag[1]
public common interface NominalAnnotationInterface {}

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// FILE: specific.cj
package cjmp_annotation_compatibility
import std.core.*

@Annotation
public specific class Tag {
    public specific let level: Int64 = 0
    public specific const init(level_val: Int64) {
    }
}

@Annotation
public specific class TextTag {
    public specific let value: String = ""
    public specific const init(value: String) {
    }
}

@Annotation
public specific class RuneTag {
    public specific let value: Rune = r'a'
    public specific const init(value: Rune) {
    }
}

<!SPECIFIC_HAS_DIFFERENT_ANNOTATION!>public specific func commonOnlyTag(): Int64 {
    1
}<!>

<!SPECIFIC_HAS_DIFFERENT_ANNOTATION!>@!Tag[4]<!>
public specific func specificOnlyTag(): Int64 {
    2
}

<!SPECIFIC_HAS_DIFFERENT_ANNOTATION!><!SPECIFIC_HAS_DIFFERENT_ANNOTATION!>@!Tag[2]<!>
public specific func argumentTag(): Int64 {
    3
}<!>

<!SPECIFIC_HAS_DIFFERENT_ANNOTATION!><!SPECIFIC_HAS_DIFFERENT_ANNOTATION!>@!Tag[1]<!>
public specific func constExpressionArgumentTag(): Int64 {
    6
}<!>

@!TextTag["same"]
public specific func sameStringLiteralTag(): Int64 {
    4
}

@!TextTag["same"]
public specific func equivalentStringLiteralTag(): Int64 {
    9
}

@!RuneTag[r'a']
public specific func equivalentRuneLiteralTag(): Int64 {
    10
}

@!Tag[-1]
public specific func negativeLiteralExpressionTag(): Int64 {
    8
}

<!SPECIFIC_HAS_DIFFERENT_ANNOTATION!>public specific class NominalTag {
    public specific init() {}
}<!>

<!SPECIFIC_HAS_DIFFERENT_ANNOTATION!><!SPECIFIC_HAS_DIFFERENT_ANNOTATION!>@!Tag[2]<!>
public specific interface NominalAnnotationInterface {}<!>
