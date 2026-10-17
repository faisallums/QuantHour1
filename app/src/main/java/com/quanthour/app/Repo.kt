package com.quanthour.app

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

val EXAMS = listOf("GAT", "SAT", "GRE")

data class User(val email: String, val name: String, val pass: String, val role: String)
data class Q(val text: String, val opts: List<String>, val answer: Int)
data class Content(
    val id: String, val creator: String, val creatorName: String, val exam: String,
    val title: String, val body: String, val qs: List<Q>
)

fun hash(s: String) = MessageDigest.getInstance("SHA-256").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }

/** Local storage (SharedPreferences). Swap for Firebase/your backend for real multi-device use. */
class Repo(ctx: Context) {
    private val sp = ctx.getSharedPreferences("quanthour", Context.MODE_PRIVATE)
    val users = mutableStateListOf<User>()
    val contents = mutableStateListOf<Content>()
    val progress = mutableStateMapOf<String, Int>() // "email|contentId" -> best score %

    init {
        JSONArray(sp.getString("users", "[]")).let { a ->
            for (i in 0 until a.length()) a.getJSONObject(i).let {
                users += User(it.getString("e"), it.getString("n"), it.getString("p"), it.getString("r"))
            }
        }
        JSONArray(sp.getString("contents", "[]")).let { a ->
            for (i in 0 until a.length()) a.getJSONObject(i).let { o ->
                val qa = o.getJSONArray("qs")
                val qs = (0 until qa.length()).map { j ->
                    val q = qa.getJSONObject(j); val oa = q.getJSONArray("o")
                    Q(q.getString("t"), (0 until oa.length()).map { oa.getString(it) }, q.getInt("a"))
                }
                contents += Content(o.getString("id"), o.getString("c"), o.getString("cn"), o.getString("x"), o.getString("t"), o.getString("b"), qs)
            }
        }
        JSONObject(sp.getString("progress", "{}")).let { o -> o.keys().forEach { progress[it] = o.getInt(it) } }
        if (!sp.getBoolean("seeded", false)) { seed(); sp.edit().putBoolean("seeded", true).apply(); save() }
    }

    private fun seed() {
        val t = "team@quanthour.app"; val n = "Quant Hour Team"
        contents += Content("s1", t, n, "SAT", "Linear Equations Basics",
            "To solve a linear equation, isolate the variable.\n\nExample: 2x + 6 = 14\nSubtract 6: 2x = 8\nDivide by 2: x = 4\n\nAlways do the same operation to both sides.",
            listOf(Q("Solve 3x - 5 = 10", listOf("3", "5", "15", "-5"), 1), Q("Solve 2x + 6 = 14", listOf("2", "4", "6", "8"), 1)))
        contents += Content("s2", t, n, "GRE", "Percent Change Shortcuts",
            "Percent change = (new - old) / old x 100.\n\nExample: 80 -> 100 gives 20/80 = 25% increase.\nNote: the base is always the ORIGINAL value.",
            listOf(Q("Price rises from 80 to 100. Percent increase?", listOf("20%", "25%", "30%", "125%"), 1), Q("A 50% increase then 50% decrease results in:", listOf("No change", "25% loss", "25% gain", "50% loss"), 1)))
        contents += Content("s3", t, n, "GAT", "Ratio and Proportion",
            "A ratio a:b splits a total T into parts a/(a+b) x T and b/(a+b) x T.\n\nExample: Divide 60 in the ratio 2:3 -> 24 and 36.",
            listOf(Q("Divide 60 in ratio 2:3. Larger part?", listOf("24", "30", "36", "40"), 2)))
    }

    fun save() {
        sp.edit()
            .putString("users", JSONArray().also { a -> users.forEach { a.put(JSONObject().put("e", it.email).put("n", it.name).put("p", it.pass).put("r", it.role)) } }.toString())
            .putString("contents", JSONArray().also { a -> contents.forEach { c ->
                a.put(JSONObject().put("id", c.id).put("c", c.creator).put("cn", c.creatorName).put("x", c.exam).put("t", c.title).put("b", c.body)
                    .put("qs", JSONArray().also { qa -> c.qs.forEach { q -> qa.put(JSONObject().put("t", q.text).put("o", JSONArray(q.opts)).put("a", q.answer)) } }))
            } }.toString())
            .putString("progress", JSONObject(progress.toMap()).toString())
            .apply()
    }

    fun register(name: String, email: String, pass: String, role: String): Pair<User?, String?> {
        val e = email.trim().lowercase()
        if (name.isBlank() || !e.contains("@") || pass.length < 6) return null to "Enter a name, valid email and a password of 6+ characters."
        if (users.any { it.email == e }) return null to "This email is already registered."
        val u = User(e, name.trim(), hash(pass), role)
        users += u; save(); return u to null
    }

    fun login(email: String, pass: String, role: String): Pair<User?, String?> {
        val u = users.find { it.email == email.trim().lowercase() } ?: return null to "No account found."
        if (u.role != role) return null to "This email is registered as a ${u.role}. Use the ${u.role} login."
        if (u.pass != hash(pass)) return null to "Wrong password."
        return u to null
    }

    fun publish(c: Content) { contents.add(0, c); save() }
    fun delete(id: String) { contents.removeAll { it.id == id }; save() }
    fun record(email: String, id: String, score: Int) {
        val k = "$email|$id"; if (score > (progress[k] ?: -1)) { progress[k] = score; save() }
    }
}
