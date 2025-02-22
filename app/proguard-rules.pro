-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn org.slf4j.**
-dontwarn org.slf4j.impl.StaticLoggerBinder
-dontwarn com.baltajmn.flowtime.core.common.dispatchers.DispatchersModuleKt
-dontwarn com.baltajmn.flowtime.core.database.di.DatabaseModuleKt
-dontwarn com.baltajmn.flowtime.core.design.components.BottomNavBarItem
-dontwarn com.baltajmn.flowtime.core.design.components.BottomNavBarKt
-dontwarn com.baltajmn.flowtime.core.design.components.LazyListHelpersKt
-dontwarn com.baltajmn.flowtime.core.design.components.TimerAlertDialogKt
-dontwarn com.baltajmn.flowtime.core.design.components.TopNavBarKt
-dontwarn com.baltajmn.flowtime.core.design.module.DesignModuleKt
-dontwarn com.baltajmn.flowtime.core.design.service.SoundViewModel
-dontwarn com.baltajmn.flowtime.core.design.theme.AppTheme
-dontwarn com.baltajmn.flowtime.core.design.theme.ThemeKt
-dontwarn com.baltajmn.flowtime.core.navigation.GRAPH
-dontwarn com.baltajmn.flowtime.core.navigation.MainGraph
-dontwarn com.baltajmn.flowtime.core.navigation.PreMainGraph
-dontwarn com.baltajmn.flowtime.core.navigation.extensions.NavigatePoppingUpToStartDestinationKt
-dontwarn com.baltajmn.flowtime.core.persistence.di.PersistenceModuleKt
-dontwarn com.baltajmn.flowtime.core.persistence.sharedpreferences.DataProvider$DefaultImpls
-dontwarn com.baltajmn.flowtime.core.persistence.sharedpreferences.DataProvider
-dontwarn com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem
-dontwarn com.baltajmn.flowtime.data.di.DataModuleKt
-dontwarn com.baltajmn.flowtime.features.screens.di.ScreensModuleKt
-dontwarn com.baltajmn.flowtime.features.screens.flowtime.FlowTimeScreenKt
-dontwarn com.baltajmn.flowtime.features.screens.flowtime.FlowTimeViewModel
-dontwarn com.baltajmn.flowtime.features.screens.history.HistoryScreenKt
-dontwarn com.baltajmn.flowtime.features.screens.history.HistoryViewModel
-dontwarn com.baltajmn.flowtime.features.screens.onboard.OnBoardScreenKt
-dontwarn com.baltajmn.flowtime.features.screens.onboard.OnBoardViewModel
-dontwarn com.baltajmn.flowtime.features.screens.percentage.PercentageScreenKt
-dontwarn com.baltajmn.flowtime.features.screens.percentage.PercentageViewModel
-dontwarn com.baltajmn.flowtime.features.screens.pomodoro.PomodoroScreenKt
-dontwarn com.baltajmn.flowtime.features.screens.pomodoro.PomodoroViewModel
-dontwarn com.baltajmn.flowtime.features.screens.settings.SettingsScreenKt
-dontwarn com.baltajmn.flowtime.features.screens.settings.SettingsViewModel
-dontwarn com.baltajmn.flowtime.features.screens.splash.SplashScreenKt
-dontwarn com.baltajmn.flowtime.features.screens.splash.SplashViewModel
-dontwarn com.baltajmn.flowtime.features.screens.todoList.TodoListScreenKt
-dontwarn com.baltajmn.flowtime.features.screens.todoList.TodoListViewModel

-keep class com.google.gson.reflect.TypeToken
-keep class * extends com.google.gson.reflect.TypeToken
-keep public class * implements java.lang.reflect.Type

-keepclassmembers class io.ktor.http.** { *; }
### your config ....

# Keep `Companion` object fields of serializable classes.
# This avoids serializer lookup through `getDeclaredClasses` as done for named companion objects.
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
   static <1>$Companion Companion;
}

# Keep `serializer()` on companion objects (both default and named) of serializable classes.
-if @kotlinx.serialization.Serializable class ** {
   static **$* *;
}
-keepclassmembers class <2>$<3> {
   kotlinx.serialization.KSerializer serializer(...);
}



# @Serializable and @Polymorphic are used at runtime for polymorphic serialization.
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault