package app.beaesthetic.domain.workout

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlanFormTest {

    private val lateral = Exercise(1, "نشر جانب", ExerciseKind.REPS)
    private val press = Exercise(2, "پرس سرشانه", ExerciseKind.REPS)
    private val wallSit = Exercise(3, "اسکات دیوار", ExerciseKind.SECONDS)
    private val all = listOf(lateral, press, wallSit)

    @Test
    fun `a plan row round-trips through the form`() {
        val item = PlanItem(press, sets = 4, rangeMin = 10, rangeMax = 15, kg = 8.5, perSide = false, targetEffort = 9, restSeconds = 120, note = "بدون قوس کمر")
        assertEquals(item, PlanForms.toItem(PlanForms.from(item), press))
        val max = PlanItem(wallSit, sets = 2, perSide = true)
        assertEquals(max, PlanForms.toItem(PlanForms.from(max), wallSit))
    }

    @Test
    fun `names match across spaces, case and Arabic letters`() {
        assertEquals(press, PlanForms.match("  پرس  سرشانه ", all))
        assertEquals(press, PlanForms.match("پرس سرشانه".replace('ی', 'ي'), all))
        assertEquals(Exercise(9, "Pull-up", ExerciseKind.REPS), PlanForms.match("pull-UP", listOf(Exercise(9, "Pull-up", ExerciseKind.REPS))))
        assertNull(PlanForms.match("", all))
    }

    @Test
    fun `a name is needed, and a move can be in a day only once`() {
        val day = listOf(PlanItem(lateral, 4), PlanItem(press, 4))
        assertEquals(setOf(PlanError.NO_NAME), PlanForms.errors(PlanItemForm(name = " "), day, null))
        assertEquals(setOf(PlanError.DUPLICATE), PlanForms.errors(PlanItemForm(name = "نشر جانب"), day, null))
        // Editing the row itself is fine.
        assertTrue(PlanForms.errors(PlanItemForm(name = "نشر جانب"), day, day[0]).isEmpty())
    }

    @Test
    fun `suggestions leave out the day's moves and filter by the typed text`() {
        val day = listOf(PlanItem(lateral, 4))
        assertEquals(listOf(wallSit, press), PlanForms.suggestions("", all, day))
        assertEquals(listOf(press), PlanForms.suggestions("پرس", all, day))
        // The exact name is already typed: nothing to suggest.
        assertEquals(emptyList(), PlanForms.suggestions("پرس سرشانه", all, day))
    }

    @Test
    fun `an existing name shares the move, a new name renames a move used nowhere else`() {
        val editing = PlanItem(lateral, 4)
        assertEquals(ExerciseChoice.Existing(press.id), PlanForms.choose(PlanItemForm(name = "پرس سرشانه"), editing, all, emptySet()))
        assertEquals(ExerciseChoice.Rename(lateral.id), PlanForms.choose(PlanItemForm(name = "نشر جانب خم"), editing, all, emptySet()))
        // Saturday's lateral raise is also on Friday: Friday's new name is a new move, Saturday keeps its own.
        assertEquals(ExerciseChoice.New, PlanForms.choose(PlanItemForm(name = "نشر جانب خم"), editing, all, setOf(lateral.id)))
        assertEquals(ExerciseChoice.New, PlanForms.choose(PlanItemForm(name = "بارفیکس"), null, all, emptySet()))
    }

    @Test
    fun `the range never turns around`() {
        val form = PlanItemForm(from = 8, to = 12)
        assertEquals(PlanItemForm(from = 14, to = 14), PlanForms.withFrom(form, 14))
        assertEquals(PlanItemForm(from = 6, to = 6), PlanForms.withTo(form, 6))
        assertEquals(1, PlanForms.withFrom(form, 0).from)
    }

    @Test
    fun `switching to seconds starts a seconds range`() {
        val seconds = PlanForms.withKind(PlanItemForm(), ExerciseKind.SECONDS)
        assertEquals(30 to 45, seconds.from to seconds.to)
        assertEquals(seconds, PlanForms.withKind(seconds, ExerciseKind.SECONDS))
    }

    @Test
    fun `empty optional fields are left out`() {
        val item = PlanForms.toItem(PlanItemForm(name = "x", effort = 0, rest = 0, note = "  ", hasKg = false), press)
        assertNull(item.targetEffort)
        assertNull(item.restSeconds)
        assertNull(item.note)
        assertNull(item.kg)
    }

    @Test
    fun `the harder move can't be the move itself or an easier one`() {
        val knee = Exercise(10, "شنا زانو", ExerciseKind.REPS)
        val pushUp = Exercise(11, "شنا", ExerciseKind.REPS)
        val moves = listOf(knee, pushUp)
        val links = mapOf(knee.id to pushUp.id)
        val editing = PlanItem(pushUp, 3)
        assertEquals(setOf(PlanError.HARDER_LOOP), PlanForms.errors(PlanItemForm(name = "شنا", harder = "شنا زانو"), emptyList(), editing, moves, links))
        assertEquals(setOf(PlanError.HARDER_LOOP), PlanForms.errors(PlanItemForm(name = "شنا", harder = " شنا "), emptyList(), editing, moves, links))
        assertTrue(PlanForms.errors(PlanItemForm(name = "شنا", harder = "شنا الماسی"), emptyList(), editing, moves, links).isEmpty())
        // Suggestions leave out the move and its easier variants.
        assertEquals(emptyList(), PlanForms.harderSuggestions("", "شنا", moves, links))
        assertEquals(listOf(pushUp), PlanForms.harderSuggestions("", "شنا زانو", moves, links))
    }

    @Test
    fun `the harder name round-trips through the form`() {
        assertEquals("شنا الماسی", PlanForms.from(PlanItem(press, 3), harder = "شنا الماسی").harder)
    }
}
