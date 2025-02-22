object Config {
    const val baseApplicationId = "com.baltajmn.flowtime"
    const val versionCode = 45
    const val versionName = "1.0.45"

    object Feature {
        const val Screens = "$baseApplicationId.features.screens"
    }

    object Core {
        const val Persistence = "$baseApplicationId.core.persistence"
        const val Database = "$baseApplicationId.core.database"
        const val Data = "$baseApplicationId.core.data"
        const val Navigation = "$baseApplicationId.core.navigation"
        const val Common = "$baseApplicationId.core.common"
        const val Design = "$baseApplicationId.core.design"
    }
}