package com.baltajmn.flowtime.features.screens.platform

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.Month
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.number
import platform.Foundation.NSCalendar
import platform.Foundation.NSDate
import platform.Foundation.NSDateComponents
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterMediumStyle
import platform.Foundation.NSDateFormatterNoStyle
import platform.Foundation.NSDateFormatterShortStyle
import platform.Foundation.NSNumber
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterPercentStyle

actual object Formats {

    actual fun mediumDate(date: LocalDate): String = NSDateFormatter().apply {
        dateStyle = NSDateFormatterMediumStyle
        timeStyle = NSDateFormatterNoStyle
    }.stringFromDate(date.toNSDate())

    actual fun monthYear(date: LocalDate): String = template("LLLLyyyy").stringFromDate(date.toNSDate())

    actual fun weekdayDay(date: LocalDate): String = template("EEEEd").stringFromDate(date.toNSDate())

    actual fun shortMonth(month: Month): String = NSDateFormatter().shortStandaloneMonthSymbols.symbol(month.number - 1)

    actual fun narrowMonth(month: Month): String =
        NSDateFormatter().veryShortStandaloneMonthSymbols.symbol(month.number - 1)

    // Los días del sistema empiezan en domingo.
    actual fun shortWeekday(day: DayOfWeek): String =
        NSDateFormatter().shortStandaloneWeekdaySymbols.symbol(day.isoDayNumber % 7)

    actual fun narrowWeekday(day: DayOfWeek): String =
        NSDateFormatter().veryShortStandaloneWeekdaySymbols.symbol(day.isoDayNumber % 7)

    actual fun shortTime(time: LocalTime): String = NSDateFormatter().apply {
        dateStyle = NSDateFormatterNoStyle
        timeStyle = NSDateFormatterShortStyle
    }.stringFromDate(date(2001, 1, 1, time.hour, time.minute))

    actual fun percent(value: Float): String =
        NSNumberFormatter().apply { numberStyle = NSNumberFormatterPercentStyle }
            .stringFromNumber(NSNumber(float = value)).orEmpty()

    /** El sistema cuenta del domingo (1) al sábado (7). */
    actual fun firstDayOfWeek(): DayOfWeek =
        DayOfWeek((NSCalendar.currentCalendar.firstWeekday.toInt() + 5) % 7 + 1)

    private fun template(template: String) = NSDateFormatter().apply { setLocalizedDateFormatFromTemplate(template) }

    private fun List<*>?.symbol(index: Int): String = this?.getOrNull(index) as? String ?: ""

    private fun LocalDate.toNSDate(): NSDate = date(year, month.number, day, 12, 0)

    private fun date(year: Int, month: Int, day: Int, hour: Int, minute: Int): NSDate {
        val parts = NSDateComponents().apply {
            setYear(year.toLong())
            setMonth(month.toLong())
            setDay(day.toLong())
            setHour(hour.toLong())
            setMinute(minute.toLong())
        }
        return NSCalendar.currentCalendar.dateFromComponents(parts) ?: NSDate()
    }
}
