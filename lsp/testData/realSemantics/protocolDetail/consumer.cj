package sample.patternconsumer

import sample.patternprovider.globalLeaf as firstAlias
import sample.patternprovider.globalLeaf as secondAlias
import sample.patternprovider.globalLeaf
import sample.patternprovider.sibling

func useLeaf(): Int64 {
    return firstAlias + secondAlias + globalLeaf
}

func useSibling(): Bool {
    return sibling
}

func shadowAlias(): Int64 {
    let firstAlias = 100
    return firstAlias
}
