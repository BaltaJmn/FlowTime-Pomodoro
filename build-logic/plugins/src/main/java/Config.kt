object Config {
    const val baseApplicationId = "com.baltajmn.flowtime"
    const val versionCode = 21
    const val versionName = "1.0.21"

    object Feature {
        const val Screens = "$baseApplicationId.features.screens"
    }

    object Core {
        const val Persistence = "$baseApplicationId.core.persistence"
        const val Navigation = "$baseApplicationId.core.navigation"
        const val Common = "$baseApplicationId.core.common"
        const val Design = "$baseApplicationId.core.design"
    }
}