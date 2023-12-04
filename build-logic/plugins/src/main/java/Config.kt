object Config {
    const val baseApplicationId = "com.baltajmn.flowtime"

    object Feature {
        const val Profile = "$baseApplicationId.features.profile"
        const val Screens = "$baseApplicationId.features.screens"
    }

    object Core {
        const val Persistence = "$baseApplicationId.core.persistence"
        const val Navigation = "$baseApplicationId.core.navigation"
        const val Common = "$baseApplicationId.core.common"
        const val Design = "$baseApplicationId.core.design"
    }
}