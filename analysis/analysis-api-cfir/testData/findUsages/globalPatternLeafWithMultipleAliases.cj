// MODULE: main
// MAIN_MODULE
// MAIN_FILE_NAME: globalPatternLeafWithMultipleAliases.cj
// FILE: provider.cj
package sample.patternprovider

public let (<caret_target>globalLeaf, sibling) = (7, true)

public func readLeaf(): Int64 {
    return <caret_providerUse>globalLeaf
}

// FILE: globalPatternLeafWithMultipleAliases.cj
package sample.patternconsumer

import sample.patternprovider.<caret_firstImport>globalLeaf as firstAlias
import sample.patternprovider.<caret_secondImport>globalLeaf as secondAlias
import sample.patternprovider.<caret_originalImport>globalLeaf

func useLeaf(): Int64 {
    return <caret_firstUse>firstAlias + <caret_secondUse>secondAlias + <caret_originalUse>globalLeaf
}

func shadowAlias(): Int64 {
    let firstAlias = 100
    return firstAlias
}
