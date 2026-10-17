package com.quanthour.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import java.util.UUID

val Indigo = Color(0xFF2B2F77); val Amber = Color(0xFFF5A623)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repo = Repo(applicationContext)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Indigo, secondary = Amber, tertiary = Amber)) {
                Surface(Modifier.fillMaxSize().systemBarsPadding()) { App(repo) }
            }
        }
    }
}

@Composable
fun App(repo: Repo) {
    var user by remember { mutableStateOf<User?>(null) }
    var role by remember { mutableStateOf<String?>(null) }
    var open by remember { mutableStateOf<Content?>(null) }
    var quiz by remember { mutableStateOf(false) }
    var adding by remember { mutableStateOf(false) }
    val u = user; val c = open; val r = role
    BackHandler(enabled = quiz || c != null || adding || (u == null && r != null)) {
        when { quiz -> quiz = false; c != null -> open = null; adding -> adding = false; else -> role = null }
    }
    when {
        u == null && r == null -> Welcome { role = it }
        u == null && r != null -> Auth(repo, r, { role = null }) { user = it }
        u != null && u.role == "creator" ->
            if (adding) AddContent(repo, u) { adding = false } else CreatorHome(repo, u, { adding = true }) { user = null; role = null }
        u != null && c != null && quiz -> QuizScreen(repo, u, c) { quiz = false }
        u != null && c != null -> Detail(repo, u, c, { quiz = true }) { open = null }
        u != null -> LearnerHome(repo, u, { open = it }) { user = null; role = null }
    }
}

@Composable
fun Welcome(pick: (String) -> Unit) {
    Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Filled.Timer, null, Modifier.size(72.dp), tint = Amber)
        Text("Quant Hour", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = Indigo)
        Text("Master GAT, SAT & GRE quantitative prep", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(40.dp))
        Button({ pick("learner") }, Modifier.fillMaxWidth().height(56.dp)) { Icon(Icons.Filled.School, null); Spacer(Modifier.width(8.dp)); Text("I'm a Learner") }
        Spacer(Modifier.height(14.dp))
        OutlinedButton({ pick("creator") }, Modifier.fillMaxWidth().height(56.dp)) { Icon(Icons.Filled.Edit, null); Spacer(Modifier.width(8.dp)); Text("I'm a Creator (Teacher)") }
    }
}

@Composable
fun Auth(repo: Repo, role: String, onBack: () -> Unit, onDone: (User) -> Unit) {
    var reg by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }; var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }; var err by remember { mutableStateOf<String?>(null) }
    val label = if (role == "creator") "Creator" else "Learner"
    Column(Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.Center) {
        TextButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null); Text(" Back") }
        Text("$label ${if (reg) "Sign up" else "Login"}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Indigo)
        Spacer(Modifier.height(16.dp))
        if (reg) { OutlinedTextField(name, { name = it }, label = { Text("Full name") }, modifier = Modifier.fillMaxWidth(), singleLine = true); Spacer(Modifier.height(8.dp)) }
        OutlinedTextField(email, { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(pass, { pass = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth(), singleLine = true, visualTransformation = PasswordVisualTransformation())
        err?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
        Spacer(Modifier.height(16.dp))
        Button({
            val (u, e) = if (reg) repo.register(name, email, pass, role) else repo.login(email, pass, role)
            if (u != null) onDone(u) else err = e
        }, Modifier.fillMaxWidth().height(50.dp)) { Text(if (reg) "Create account" else "Login") }
        TextButton({ reg = !reg; err = null }) { Text(if (reg) "Already have an account? Login" else "New here? Sign up as $label") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(title: String, onLogout: (() -> Unit)? = null, onBack: (() -> Unit)? = null) {
    TopAppBar(
        title = { Text(title, fontWeight = FontWeight.Bold) },
        navigationIcon = { onBack?.let { IconButton(it) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } } },
        actions = { onLogout?.let { IconButton(it) { Icon(Icons.AutoMirrored.Filled.Logout, "Logout") } } },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Indigo, titleContentColor = Color.White, actionIconContentColor = Color.White, navigationIconContentColor = Color.White)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearnerHome(repo: Repo, u: User, onOpen: (Content) -> Unit, onLogout: () -> Unit) {
    var exam by remember { mutableStateOf("All") }
    val list = repo.contents.filter { exam == "All" || it.exam == exam }
    val done = repo.contents.count { repo.progress.containsKey("${u.email}|${it.id}") }
    Scaffold(topBar = { TopBar("Hi, ${u.name.substringBefore(' ')}", onLogout) }) { pad ->
        Column(Modifier.padding(pad).padding(horizontal = 16.dp)) {
            Text("$done of ${repo.contents.size} lessons completed", Modifier.padding(vertical = 10.dp), color = Indigo)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("All") .plus(EXAMS).forEach { e -> FilterChip(exam == e, { exam = e }, label = { Text(e) }) }
            }
            Spacer(Modifier.height(8.dp))
            if (list.isEmpty()) Text("No content yet for this exam.", Modifier.padding(24.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(list, key = { it.id }) { c ->
                    val p = repo.progress["${u.email}|${c.id}"]
                    Card(Modifier.fillMaxWidth(), onClick = { onOpen(c) }) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                AssistChip({}, label = { Text(c.exam) })
                                Text(c.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text("by ${c.creatorName}  •  ${if (c.qs.isEmpty()) "Lesson" else "${c.qs.size} questions"}", style = MaterialTheme.typography.bodySmall)
                            }
                            if (p != null) Text("$p%", color = Indigo, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Detail(repo: Repo, u: User, c: Content, onQuiz: () -> Unit, onBack: () -> Unit) {
    Scaffold(topBar = { TopBar(c.exam, onBack = onBack) }) { pad ->
        Column(Modifier.padding(pad).padding(20.dp).verticalScroll(rememberScrollState())) {
            Text(c.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Indigo)
            Text("by ${c.creatorName}", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(16.dp))
            Text(c.body, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(24.dp))
            if (c.qs.isNotEmpty()) Button(onQuiz, Modifier.fillMaxWidth()) { Text("Take quiz (${c.qs.size})") }
            else Button({ repo.record(u.email, c.id, 100); onBack() }, Modifier.fillMaxWidth()) { Text("Mark as complete") }
        }
    }
}

@Composable
fun QuizScreen(repo: Repo, u: User, c: Content, onBack: () -> Unit) {
    val ans = remember { mutableStateMapOf<Int, Int>() }
    var score by remember { mutableStateOf<Int?>(null) }
    Scaffold(topBar = { TopBar("Quiz: ${c.title}", onBack = onBack) }) { pad ->
        Column(Modifier.padding(pad).padding(16.dp).verticalScroll(rememberScrollState())) {
            c.qs.forEachIndexed { i, q ->
                Text("${i + 1}. ${q.text}", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 12.dp))
                q.opts.forEachIndexed { j, o ->
                    val s = score
                    val tint = when { s == null -> Color.Unspecified; j == q.answer -> Color(0xFF2E7D32); ans[i] == j -> MaterialTheme.colorScheme.error; else -> Color.Unspecified }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(ans[i] == j, { if (s == null) ans[i] = j })
                        Text(o, color = tint)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            val s = score
            if (s == null) Button({
                val pct = c.qs.indices.count { ans[it] == c.qs[it].answer } * 100 / c.qs.size
                score = pct; repo.record(u.email, c.id, pct)
            }, Modifier.fillMaxWidth(), enabled = ans.size == c.qs.size) { Text("Submit") }
            else { Text("Your score: $s%", style = MaterialTheme.typography.headlineSmall, color = Indigo, fontWeight = FontWeight.Bold)
                Button(onBack, Modifier.fillMaxWidth()) { Text("Back to lesson") } }
        }
    }
}

@Composable
fun CreatorHome(repo: Repo, u: User, onAdd: () -> Unit, onLogout: () -> Unit) {
    val mine = repo.contents.filter { it.creator == u.email }
    Scaffold(topBar = { TopBar("Creator: ${u.name.substringBefore(' ')}", onLogout) },
        floatingActionButton = { ExtendedFloatingActionButton(onAdd, containerColor = Amber) { Icon(Icons.Filled.Add, null); Text(" New content") } }) { pad ->
        Column(Modifier.padding(pad).padding(16.dp)) {
            Text("${mine.size} published  •  ${repo.progress.keys.count { k -> mine.any { k.endsWith("|${it.id}") } }} learner completions", color = Indigo)
            if (mine.isEmpty()) Text("Nothing yet. Tap “New content” to publish your first lesson.", Modifier.padding(top = 24.dp))
            LazyColumn(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(mine, key = { it.id }) { c ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("${c.exam} • ${c.title}", fontWeight = FontWeight.SemiBold)
                                val n = repo.progress.keys.count { it.endsWith("|${c.id}") }
                                Text("${c.qs.size} questions • $n learners done", style = MaterialTheme.typography.bodySmall)
                            }
                            IconButton({ repo.delete(c.id) }) { Icon(Icons.Filled.Delete, "Delete") }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddContent(repo: Repo, u: User, onDone: () -> Unit) {
    var exam by remember { mutableStateOf("SAT") }
    var title by remember { mutableStateOf("") }; var body by remember { mutableStateOf("") }
    val qs = remember { mutableStateListOf<Q>() }
    var qt by remember { mutableStateOf("") }; val opts = remember { mutableStateListOf("", "", "", "") }
    var correct by remember { mutableStateOf(0) }
    Scaffold(topBar = { TopBar("New content", onBack = onDone) }) { pad ->
        Column(Modifier.padding(pad).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { EXAMS.forEach { e -> FilterChip(exam == e, { exam = e }, label = { Text(e) }) } }
            OutlinedTextField(title, { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(body, { body = it }, label = { Text("Lesson content") }, modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp))
            HorizontalDivider()
            Text("Quiz questions (optional) — added: ${qs.size}", fontWeight = FontWeight.SemiBold)
            OutlinedTextField(qt, { qt = it }, label = { Text("Question") }, modifier = Modifier.fillMaxWidth())
            opts.forEachIndexed { i, o ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(correct == i, { correct = i })
                    OutlinedTextField(o, { opts[i] = it }, label = { Text("Option ${i + 1}${if (correct == i) " (correct)" else ""}") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                }
            }
            OutlinedButton({
                qs += Q(qt.trim(), opts.map { it.trim() }, correct); qt = ""; for (i in 0..3) opts[i] = ""; correct = 0
            }, enabled = qt.isNotBlank() && opts.all { it.isNotBlank() }) { Text("Add question") }
            Button({
                repo.publish(Content(UUID.randomUUID().toString(), u.email, u.name, exam, title.trim(), body.trim(), qs.toList())); onDone()
            }, Modifier.fillMaxWidth().height(50.dp), enabled = title.isNotBlank() && body.isNotBlank()) { Text("Publish") }
        }
    }
}
