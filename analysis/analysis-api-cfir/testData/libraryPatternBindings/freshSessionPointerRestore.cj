// MODULE: lib
// MODULE_KIND: LibraryBinaryDecompiled
// FILE: Library.cj
package patternlib

public var (count, enabled) = (1, true)
public let title: String = "library"
public const limit: Int64 = 7

public class Holder {
    public var member: Int64 = 9
    public static var shared: Int64 = 10
}
public enum State {
    Ready
}

// MODULE: main(lib)
// MAIN_MODULE
// MAIN_FILE_NAME: Consumer.cj
// PHYSICAL_SOURCE_FILES
// FILE: Consumer.cj
package authority.libraryconsumer

import patternlib.<caret_firstImport>count as firstAlias
import patternlib.<caret_secondImport>count as secondAlias
import patternlib.enabled
import patternlib.title
import patternlib.limit
import patternlib.Holder
import patternlib.State

func shadowAlias(): Int64 {
    let <caret_shadowDeclaration>firstAlias: Int64 = 100
    return <caret_shadowUse>firstAlias
}

main(): Int64 {
    <caret_firstWrite>firstAlias = <caret_firstRead>firstAlias + 1
    let number: Int64 = <caret_countUse>secondAlias
    let flag: Bool = <caret_enabledUse>enabled
    let text: String = <caret_titleUse>title
    let bound: Int64 = <caret_limitUse>limit
    println(number)
    println(flag)
    println(text)
    println(bound)
    println(Holder().<caret_memberUse>member)
    println(Holder.<caret_sharedUse>shared)
    match (State.<caret_readyUse>Ready) {
        case State.<caret_readyPattern>Ready => println("Ready")
    }
    println(shadowAlias())
    return 0
}
