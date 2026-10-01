package com.baltajmn.flowtime.data.timer

/** `@Synchronized` en Android. En iPhone no existe y no hace nada: ver [FocusEngine]. */
@OptIn(ExperimentalMultiplatform::class)
@OptionalExpectation
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.SOURCE)
expect annotation class JvmSynchronized()
