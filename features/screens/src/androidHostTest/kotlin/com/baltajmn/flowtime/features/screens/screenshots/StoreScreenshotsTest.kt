package com.baltajmn.flowtime.features.screens.screenshots

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import com.baltajmn.flowtime.core.common.extensions.formatMinutesStudying
import com.baltajmn.flowtime.core.design.R
import com.baltajmn.flowtime.core.design.components.SoundSheetBody
import com.baltajmn.flowtime.core.design.sound.PlayerState
import com.baltajmn.flowtime.core.design.sound.PlayerType
import com.baltajmn.flowtime.core.design.sound.SleepTimer
import com.baltajmn.flowtime.core.design.sound.SoundMixes
import com.baltajmn.flowtime.core.design.sound.SoundState
import com.baltajmn.flowtime.data.goal.DayProgress
import com.baltajmn.flowtime.data.goal.Streak
import com.baltajmn.flowtime.data.stats.TaskTime
import com.baltajmn.flowtime.data.tag.Tag
import com.baltajmn.flowtime.data.task.Task
import com.baltajmn.flowtime.data.timer.Phase
import com.baltajmn.flowtime.data.timer.TimerHint
import com.baltajmn.flowtime.data.timer.TimerMode
import com.baltajmn.flowtime.features.screens.fakes.FakeTasks
import com.baltajmn.flowtime.features.screens.focus.FocusContent
import com.baltajmn.flowtime.features.screens.focus.FocusUiState
import com.baltajmn.flowtime.features.screens.pro.ProAccess
import com.baltajmn.flowtime.features.screens.settings.SettingsContent
import com.baltajmn.flowtime.features.screens.stats.StatsContent
import com.baltajmn.flowtime.features.screens.todoList.TodoListContent
import com.baltajmn.flowtime.features.screens.todoList.TodoListViewModel
import java.util.Calendar
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import com.baltajmn.flowtime.data.goal.today as systemToday

/**
 * Las capturas de la ficha de Play (#54): las pantallas con su barra de navegación y datos de ejemplo
 * en el idioma de cada ficha. Salen en build/outputs/roborazzi/store/ y las enmarca, con su titular,
 * tools/store/capturas.py. El widget y la notificación no los pinta Compose: salen del emulador.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
abstract class StoreScreenshots(private val language: String, private val tablet: Boolean) {

    @get:Rule(order = 0)
    val onlyWithRoborazzi = RoborazziOnly()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    private val sample = SAMPLES.getValue(language)
    private val today = systemToday()
    private val tags = sample.tags.mapIndexed { i, name -> Tag(i + 1L, name, i) }
    private val tasks = sample.tasks.mapIndexed { i, (title, description) ->
        Task(
            i + 1L,
            title,
            description.orEmpty(),
            plannedFor = if (i == 2) today.minus(1, DateTimeUnit.DAY) else today,
            position = i
        )
    }

    protected fun shot(
        name: String,
        destination: Int,
        dark: Boolean = false,
        content: @Composable () -> Unit
    ) =
        compose.capture("store/${if (tablet) "tablet" else "phone"}/$language/$name", dark = dark) {
            Navigation(tablet, destination, content)
        }

    /** Pomodoro en marcha, con etiqueta y tarea. */
    protected val working = FocusUiState(
        mode = TimerMode.POMODORO,
        phase = Phase.WORK,
        time = "18:24",
        progress = 0.26f,
        minutesToday = 95L.formatMinutesStudying(),
        goalToday = 120L.formatMinutesStudying(),
        streak = 6,
        tags = tags,
        tagId = 1,
        taskId = 1,
        taskTitle = tasks[0].title,
        pendingTasks = tasks.take(1)
    )

    @Composable
    protected fun Focus(state: FocusUiState, soundPlaying: Boolean = false) = FocusContent(
        state = state,
        showSound = true,
        soundPlaying = soundPlaying,
        onSelectMode = {},
        onAction = {},
        onOpenSounds = {},
        onOpenModeSettings = {}
    )

    @Composable
    protected fun Stats() {
        val base = sampleStats(ProAccess.OPEN)
        StatsContent(
            state = base.copy(
                streak = Streak(current = 6, best = 11),
                today = DayProgress(today, seconds = 95 * 60L, goalMinutes = 120),
                tags = tags.take(2),
                details = base.details.copy(
                    topTasks = listOf(
                        TaskTime(1, tasks[0].title, 3 * 3600L),
                        TaskTime(2, tasks[1].title, 5400L)
                    )
                )
            ),
            onPeriod = {},
            onPrevious = {},
            onNext = {},
            onUnlock = {},
            onDismissProCard = {},
            onCopyHistory = {},
            onPasteHistory = {},
            onExportCsv = {}
        )
    }

    protected fun todo(): TodoListViewModel {
        val done = tasks[1].copy(doneOn = today)
        return TodoListViewModel(FakeTasks(tasks[0], done, tasks[2]), today = { today })
    }

    @Composable
    protected fun Todo(viewModel: TodoListViewModel) {
        val state by viewModel.uiState.collectAsState()
        TodoListContent(
            state = state,
            listState = rememberLazyListState(),
            viewModel = viewModel,
            onSeePro = {}
        )
    }

    protected fun settingsViewModel() = sampleSettings(sample.tags, goalMinutes = 120)
}

/** Las 5 del móvil; el widget y la notificación, del emulador, van en 06 y 07. */
abstract class StorePhoneScreenshots(language: String) : StoreScreenshots(language, tablet = false) {

    @Test
    fun focus() = shot("01_focus", destination = 0) { Focus(working) }

    @Test
    fun dark() = shot("02_dark", destination = 0, dark = true) {
        Focus(
            working.copy(
                mode = TimerMode.FLOW_TIME,
                time = "42:10",
                progress = 0.7f,
                hint = TimerHint.NextStep(atMinutes = 50, breakMinutes = 10),
                tagId = 2
            )
        )
    }

    @Test
    fun stats() = shot("03_stats", destination = 2) { Stats() }

    @Test
    fun tasks() {
        val viewModel = todo()
        shot("04_tasks", destination = 1) { Todo(viewModel) }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun sounds() = shot("05_sounds", destination = 0) {
        Box {
            Focus(working, soundPlaying = true)
            // Como la hoja de verdad: el velo encima de la pantalla y la hoja abajo.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.32f))
            )
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                shape = BottomSheetDefaults.ExpandedShape,
                color = BottomSheetDefaults.ContainerColor,
                tonalElevation = BottomSheetDefaults.Elevation
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    BottomSheetDefaults.DragHandle()
                    SoundSheetBody(
                        sound = SoundState(
                            PlayerType.entries.associateWith { type ->
                                when (type) {
                                    PlayerType.RAIN -> PlayerState(volume = 0.6f, isPlaying = true)
                                    PlayerType.COFFEE_HOUSE -> PlayerState(
                                        volume = 0.35f,
                                        isPlaying = true
                                    )
                                    else -> PlayerState(volume = 0.5f)
                                }
                            }
                        ),
                        sleep = SleepTimer.At(
                            Calendar.getInstance().apply {
                                set(Calendar.HOUR_OF_DAY, 23)
                                set(Calendar.MINUTE, 30)
                            }.timeInMillis
                        ),
                        mixes = SoundMixes.BUILT_IN,
                        onSleep = {},
                        onMix = {},
                        onEditMix = {},
                        onSaveMix = {},
                        onPlay = { _, _ -> },
                        onVolume = { _, _ -> }
                    )
                }
            }
        }
    }
}

/** En tablet: las dos columnas del temporizador y de las estadísticas, y las listas centradas. */
abstract class StoreTabletScreenshots(language: String) : StoreScreenshots(language, tablet = true) {

    @Test
    fun focus() = shot("01_focus", destination = 0) { Focus(working) }

    @Test
    fun stats() = shot("02_stats", destination = 2) { Stats() }

    @Test
    fun tasks() {
        val viewModel = todo()
        shot("03_tasks", destination = 1) { Todo(viewModel) }
    }

    @Test
    fun settings() {
        val viewModel = settingsViewModel()
        shot("04_settings", destination = 3) {
            val state by viewModel.uiState.collectAsState()
            SettingsContent(
                state = state,
                listState = rememberLazyListState(),
                viewModel = viewModel,
                showSound = true,
                onSoundChange = {},
                navigateToIntro = {},
                onOpenPro = {}
            )
        }
    }
}

/** La barra de abajo en el móvil y la lateral en la tablet, como NavigationSuiteScaffold en la app. */
@Composable
private fun Navigation(tablet: Boolean, selected: Int, content: @Composable () -> Unit) {
    val items = listOf(
        R.drawable.ic_timer to R.string.nav_focus,
        R.drawable.ic_list to R.string.nav_todo_list,
        R.drawable.ic_stats to R.string.nav_stats,
        R.drawable.ic_settings to R.string.nav_settings
    )
    if (tablet) {
        Row {
            NavigationRail {
                items.forEachIndexed { i, (icon, label) ->
                    NavigationRailItem(
                        selected = i == selected,
                        onClick = {},
                        icon = { Icon(painterResource(icon), contentDescription = null) },
                        label = { Text(stringResource(label)) }
                    )
                }
            }
            Box(modifier = Modifier.weight(1f)) { content() }
        }
    } else {
        // Sin barra de estado en las capturas: el hueco que dejaría, para que nada quede pegado arriba.
        Column(modifier = Modifier.padding(top = 24.dp)) {
            Box(modifier = Modifier.weight(1f)) { content() }
            NavigationBar {
                items.forEachIndexed { i, (icon, label) ->
                    NavigationBarItem(
                        selected = i == selected,
                        onClick = {},
                        icon = { Icon(painterResource(icon), contentDescription = null) },
                        label = { Text(stringResource(label)) }
                    )
                }
            }
        }
    }
}

private class Sample(val tags: List<String>, val tasks: List<Pair<String, String?>>)

private val SAMPLES = mapOf(
    "es-ES" to Sample(
        listOf("Estudio", "Trabajo", "Lectura", "Idiomas"),
        listOf(
            "Repasar el tema 4" to "Apuntes y ejercicios del final",
            "Leer 20 páginas" to null,
            "Preparar la presentación" to "Diapositivas 5 a 12"
        )
    ),
    "en-US" to Sample(
        listOf("Study", "Work", "Reading", "Languages"),
        listOf(
            "Review chapter 4" to "Notes and the exercises at the end",
            "Read 20 pages" to null,
            "Prepare the presentation" to "Slides 5 to 12"
        )
    ),
    "de-DE" to Sample(
        listOf("Lernen", "Arbeit", "Lesen", "Sprachen"),
        listOf(
            "Kapitel 4 wiederholen" to "Notizen und Übungen am Ende",
            "20 Seiten lesen" to null,
            "Präsentation vorbereiten" to "Folien 5 bis 12"
        )
    ),
    "it-IT" to Sample(
        listOf("Studio", "Lavoro", "Lettura", "Lingue"),
        listOf(
            "Ripassare il capitolo 4" to "Appunti ed esercizi finali",
            "Leggere 20 pagine" to null,
            "Preparare la presentazione" to "Slide da 5 a 12"
        )
    ),
    "ru-RU" to Sample(
        listOf("Учёба", "Работа", "Чтение", "Языки"),
        listOf(
            "Повторить главу 4" to "Конспект и упражнения в конце",
            "Прочитать 20 страниц" to null,
            "Подготовить презентацию" to "Слайды с 5 по 12"
        )
    ),
    "hi-IN" to Sample(
        listOf("पढ़ाई", "काम", "पठन", "भाषाएँ"),
        listOf(
            "अध्याय 4 दोहराना" to "नोट्स और अंत के अभ्यास",
            "20 पन्ने पढ़ना" to null,
            "प्रेज़ेंटेशन तैयार करना" to "स्लाइड 5 से 12"
        )
    ),
    "fr-FR" to Sample(
        listOf("Études", "Travail", "Lecture", "Langues"),
        listOf(
            "Réviser le chapitre 4" to "Notes et exercices de la fin",
            "Lire 20 pages" to null,
            "Préparer la présentation" to "Diapositives 5 à 12"
        )
    ),
    "id" to Sample(
        listOf("Belajar", "Kerja", "Membaca", "Bahasa"),
        listOf(
            "Mengulang bab 4" to "Catatan dan latihan di akhir bab",
            "Membaca 20 halaman" to null,
            "Menyiapkan presentasi" to "Slide 5 sampai 12"
        )
    ),
    "ja-JP" to Sample(
        listOf("勉強", "仕事", "読書", "語学"),
        listOf(
            "第4章を復習する" to "ノートと章末の練習問題",
            "20ページ読む" to null,
            "プレゼンの準備" to "スライド5から12まで"
        )
    ),
    "ko-KR" to Sample(
        listOf("공부", "업무", "독서", "외국어"),
        listOf(
            "4장 복습하기" to "노트와 마지막 연습 문제",
            "20쪽 읽기" to null,
            "발표 준비하기" to "슬라이드 5부터 12까지"
        )
    ),
    "nl-NL" to Sample(
        listOf("Studie", "Werk", "Lezen", "Talen"),
        listOf(
            "Hoofdstuk 4 herhalen" to "Aantekeningen en de oefeningen aan het eind",
            "20 pagina's lezen" to null,
            "De presentatie voorbereiden" to "Dia's 5 tot 12"
        )
    ),
    "pl-PL" to Sample(
        listOf("Nauka", "Praca", "Czytanie", "Języki"),
        listOf(
            "Powtórzyć rozdział 4" to "Notatki i ćwiczenia z końca",
            "Przeczytać 20 stron" to null,
            "Przygotować prezentację" to "Slajdy od 5 do 12"
        )
    ),
    "pt-BR" to Sample(
        listOf("Estudo", "Trabalho", "Leitura", "Idiomas"),
        listOf(
            "Revisar o capítulo 4" to "Anotações e exercícios do final",
            "Ler 20 páginas" to null,
            "Preparar a apresentação" to "Slides 5 a 12"
        )
    ),
    "tr-TR" to Sample(
        listOf("Ders", "İş", "Okuma", "Diller"),
        listOf(
            "4. bölümü tekrar et" to "Notlar ve sondaki alıştırmalar",
            "20 sayfa oku" to null,
            "Sunumu hazırla" to "5 ile 12 arası slaytlar"
        )
    )
)

private const val PHONE = "w411dp-h914dp-port-420dpi"
private const val TABLET = "w1280dp-h800dp-land-hdpi"

@Config(sdk = [34], qualifiers = "es-rES-$PHONE")
class StorePhoneEsTest : StorePhoneScreenshots("es-ES")

@Config(sdk = [34], qualifiers = "en-rUS-$PHONE")
class StorePhoneEnTest : StorePhoneScreenshots("en-US")

@Config(sdk = [34], qualifiers = "de-rDE-$PHONE")
class StorePhoneDeTest : StorePhoneScreenshots("de-DE")

@Config(sdk = [34], qualifiers = "it-rIT-$PHONE")
class StorePhoneItTest : StorePhoneScreenshots("it-IT")

@Config(sdk = [34], qualifiers = "ru-rRU-$PHONE")
class StorePhoneRuTest : StorePhoneScreenshots("ru-RU")

@Config(sdk = [34], qualifiers = "hi-rIN-$PHONE")
class StorePhoneHiTest : StorePhoneScreenshots("hi-IN")

// Los de la App Store que aún no tienen capturas de Play: solo el móvil, para el iPhone.
@Config(sdk = [34], qualifiers = "fr-rFR-$PHONE")
class StorePhoneFrTest : StorePhoneScreenshots("fr-FR")

@Config(sdk = [34], qualifiers = "in-rID-$PHONE")
class StorePhoneIdTest : StorePhoneScreenshots("id")

@Config(sdk = [34], qualifiers = "ja-rJP-$PHONE")
class StorePhoneJaTest : StorePhoneScreenshots("ja-JP")

@Config(sdk = [34], qualifiers = "ko-rKR-$PHONE")
class StorePhoneKoTest : StorePhoneScreenshots("ko-KR")

@Config(sdk = [34], qualifiers = "nl-rNL-$PHONE")
class StorePhoneNlTest : StorePhoneScreenshots("nl-NL")

@Config(sdk = [34], qualifiers = "pl-rPL-$PHONE")
class StorePhonePlTest : StorePhoneScreenshots("pl-PL")

@Config(sdk = [34], qualifiers = "pt-rBR-$PHONE")
class StorePhonePtTest : StorePhoneScreenshots("pt-BR")

@Config(sdk = [34], qualifiers = "tr-rTR-$PHONE")
class StorePhoneTrTest : StorePhoneScreenshots("tr-TR")

@Config(sdk = [34], qualifiers = "es-rES-$TABLET")
class StoreTabletEsTest : StoreTabletScreenshots("es-ES")

@Config(sdk = [34], qualifiers = "en-rUS-$TABLET")
class StoreTabletEnTest : StoreTabletScreenshots("en-US")

@Config(sdk = [34], qualifiers = "de-rDE-$TABLET")
class StoreTabletDeTest : StoreTabletScreenshots("de-DE")

@Config(sdk = [34], qualifiers = "it-rIT-$TABLET")
class StoreTabletItTest : StoreTabletScreenshots("it-IT")

@Config(sdk = [34], qualifiers = "ru-rRU-$TABLET")
class StoreTabletRuTest : StoreTabletScreenshots("ru-RU")

@Config(sdk = [34], qualifiers = "hi-rIN-$TABLET")
class StoreTabletHiTest : StoreTabletScreenshots("hi-IN")
