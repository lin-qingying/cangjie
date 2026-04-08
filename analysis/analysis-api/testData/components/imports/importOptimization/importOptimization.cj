// MAIN_FILE_NAME: importOptimization.cj
// FILE: helpers.cj
package sample.helpers

class User {
}

func makeUser(): User {
    return User()
}

func unusedThing(): Int64 {
    return 2
}

// FILE: importOptimization.cj
// EXPECTED_RETAINED_IMPORT: sample.helpers.makeUser
// EXPECTED_DUPLICATE_IMPORT: sample.helpers.makeUser
// EXPECTED_UNUSED_IMPORT: sample.helpers.unusedThing
package sample.imports

import sample.helpers.makeUser
import sample.helpers.makeUser
import sample.helpers.unusedThing

func consume(): sample.helpers.User {
    return makeUser()
}
