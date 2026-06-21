


interface MyFactorCandidatesPlugin {}

extend<T> ?T <: MyFactorCandidatesPlugin {}

class MydataFactor<T> where T <: MyFactorCandidatesPlugin {}

let deviceTypeFactor = MydataFactor<Option<String>>()

main() {}
