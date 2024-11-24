package com.example.homeaccountingapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.homeaccountingapp.ui.theme.HomeAccountingAppTheme
import kotlinx.coroutines.launch
import androidx.compose.ui.layout.ContentScale
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.LocalContext



class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onResume() {
        super.onResume()
        // Завантаження витрат і доходів при поверненні на активність
        viewModel.loadExpensesFromSharedPreferences(this)
        viewModel.loadIncomesFromSharedPreferences(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.SplashTheme) // Встановлюємо SplashTheme для MainActivity
        super.onCreate(savedInstanceState)

        setContent {
            HomeAccountingAppTheme {
                var showSplash by remember { mutableStateOf(true) }

                if (showSplash) {
                    SplashScreen {
                        showSplash = false // Переходимо на головний екран після таймера
                    }
                } else {
                    MainScreen(
                        onNavigateToIncomes = {
                            val intent = Intent(this@MainActivity, IncomeActivity::class.java)
                            startActivity(intent)
                        },
                        onNavigateToExpenses = {
                            val intent = Intent(this@MainActivity, ExpenseActivity::class.java)
                            startActivity(intent)
                        },
                        onNavigateToAnalytics = {}, // Видалено аналітику
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}


// Функція Splash Screen
@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    // Затримка перед переходом на головний екран
    LaunchedEffect(Unit) {
        delay(2000) // 2 секунди
        onTimeout()
    }

    // Центрування та повноекранне зображення
    Box(
        modifier = Modifier
            .fillMaxSize() // Займає весь екран
            .background(Color.Black), // Колір фону (якщо картинка не покриває весь екран)
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.splash_image), // Ваше зображення
            contentDescription = "Splash Image",
            modifier = Modifier.fillMaxSize(), // Заповнити весь екран
            contentScale = ContentScale.Crop // Розтягнути зображення, щоб заповнило екран
        )
    }
}

class MainViewModel : ViewModel() {
    private val _expenses = MutableLiveData<Map<String, Double>>()
    val expenses: LiveData<Map<String, Double>> = _expenses
    private val _incomes = MutableLiveData<Map<String, Double>>()
    val incomes: LiveData<Map<String, Double>> = _incomes

    fun loadExpensesFromSharedPreferences(context: Context) {
        val sharedPreferences = context.getSharedPreferences("budget_prefs", Context.MODE_PRIVATE)
        val expensesJson = sharedPreferences.getString("expenses", null)
        val expenseMap: Map<String, Double> = if (expensesJson != null) {
            Gson().fromJson(expensesJson, object : TypeToken<Map<String, Double>>() {}.type)
        } else {
            emptyMap()
        }
        _expenses.value = expenseMap
    }

    fun saveExpensesToSharedPreferences(context: Context, expenses: Map<String, Double>) {
        val sharedPreferences = context.getSharedPreferences("budget_prefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val expensesJson = Gson().toJson(expenses)
        editor.putString("expenses", expensesJson)
        editor.apply()
        _expenses.value = expenses // Негайне оновлення LiveData
    }

    fun loadIncomesFromSharedPreferences(context: Context) {
        val sharedPreferences = context.getSharedPreferences("budget_prefs", Context.MODE_PRIVATE)
        val incomesJson = sharedPreferences.getString("incomes", null)
        val incomeMap: Map<String, Double> = if (incomesJson != null) {
            Gson().fromJson(incomesJson, object : TypeToken<Map<String, Double>>() {}.type)
        } else {
            emptyMap()
        }
        _incomes.value = incomeMap
    }

    fun saveIncomesToSharedPreferences(context: Context, incomes: Map<String, Double>) {
        val sharedPreferences = context.getSharedPreferences("budget_prefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val incomesJson = Gson().toJson(incomes)
        editor.putString("incomes", incomesJson)
        editor.apply()
        _incomes.value = incomes // Негайне оновлення LiveData
    }

    fun saveExpenseTransaction(context: Context, transaction: Transaction) {
        val sharedPreferences = context.getSharedPreferences("ExpensePrefs", Context.MODE_PRIVATE)
        val gson = Gson()

        // Завантажуємо поточні транзакції
        val existingTransactionsJson = sharedPreferences.getString("transactions", "[]") ?: "[]"
        val type = object : TypeToken<List<Transaction>>() {}.type
        val existingTransactions: MutableList<Transaction> = gson.fromJson(existingTransactionsJson, type)

        // Додаємо нову транзакцію
        existingTransactions.add(transaction)
        val updatedJson = gson.toJson(existingTransactions)

        // Зберігаємо оновлений список транзакцій
        sharedPreferences.edit().putString("transactions", updatedJson).apply()

        // Форсоване оновлення витрат
        val updatedExpenses = calculateExpenses(existingTransactions)
        _expenses.value = updatedExpenses
        saveExpensesToSharedPreferences(context, updatedExpenses)
    }

    // Допоміжний метод для перерахунку витрат за категоріями
    private fun calculateExpenses(transactions: List<Transaction>): Map<String, Double> {
        return transactions.groupBy { it.category }.mapValues { (_, transactions) ->
            transactions.sumOf { it.amount }
        }
    }


    fun refreshExpenses(context: Context) {
        val sharedPreferences = context.getSharedPreferences("ExpensePrefs", Context.MODE_PRIVATE)
        val gson = Gson()
        val transactionsJson = sharedPreferences.getString("transactions", "[]") ?: "[]"
        val type = object : TypeToken<List<Transaction>>() {}.type
        val transactions: List<Transaction> = gson.fromJson(transactionsJson, type)

        // Перерахунок витрат
        val updatedExpenses = calculateExpenses(transactions)
        _expenses.value = updatedExpenses
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToIncomes: () -> Unit,
    onNavigateToExpenses: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    viewModel: MainViewModel = viewModel()
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showExpenses by remember { mutableStateOf(false) }
    var showIncomes by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showAddTransactionDialog by remember { mutableStateOf(false) }

    // Скидаємо стан кнопок при завантаженні екрана
    LaunchedEffect(Unit) {
        showExpenses = false
        showIncomes = false
    }

    val expenses = viewModel.expenses.observeAsState(initial = emptyMap()).value
    val incomes = viewModel.incomes.observeAsState(initial = emptyMap()).value

    val totalExpenses = expenses.values.sum()
    val totalIncomes = incomes.values.sum()
    val balance = totalIncomes + totalExpenses

    val showWarning = balance < 0  // Змінено умову
    val showSuccess = balance > 0

    var showMessage by remember { mutableStateOf(false) }

    // Таймер для автоматичного приховування повідомлення
    LaunchedEffect(balance) {
        showMessage = showWarning || showSuccess
        delay(5000)
        showMessage = false
    }

    val context = LocalContext.current

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(
                onNavigateToIncomes = { scope.launch { drawerState.close(); onNavigateToIncomes() } },
                onNavigateToExpenses = { scope.launch { drawerState.close(); onNavigateToExpenses() } }
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Домашня бухгалтерія", color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Меню",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121212))
                )
            },
            content = { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .backgroundWithImage(
                            painter = painterResource(id = R.drawable.background_app),
                            contentScale = ContentScale.Crop
                        )
                        .padding(innerPadding)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            ExpandableButtonWithAmount(
                                text = "Загальні доходи",
                                amount = totalIncomes,
                                gradientColors = listOf(
                                    Color(0xFF006400),
                                    Color(0x200032CD32)
                                ),
                                isExpanded = showIncomes,
                                onClick = { showIncomes = !showIncomes }
                            )
                            AnimatedVisibility(visible = showIncomes) {
                                IncomeList(incomes = incomes)
                            }
                            Spacer(modifier = Modifier.height(16.dp))

                            ExpandableButtonWithAmount(
                                text = "Загальні витрати",
                                amount = totalExpenses,
                                gradientColors = listOf(
                                    Color(0xFF8B0000),
                                    Color(0x20B22222)
                                ),
                                isExpanded = showExpenses,
                                onClick = { showExpenses = !showExpenses }
                            )
                            AnimatedVisibility(visible = showExpenses) {
                                ExpensesList(expenses = expenses)
                            }

                            // Діаграми доходів та витрат
                            Spacer(modifier = Modifier.height(32.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Діаграма доходів
                                IncomeExpenseChart(
                                    incomes = incomes,
                                    expenses = expenses,
                                    totalIncomes = totalIncomes,
                                    totalExpenses = totalExpenses
                                )
                            }
                        }

                        // Відображення залишку
                        val formattedBalance = "%,.2f".format(balance).replace(",", " ")
                        Text(
                            text = "Залишок: $formattedBalance грн",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 27.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 16.dp)
                                .align(Alignment.End)
                        )
                    }

                    // Повідомлення
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        AnimatedVisibility(
                            visible = showMessage,
                            enter = slideInVertically(
                                initialOffsetY = { fullHeight -> fullHeight }
                            ),
                            exit = slideOutVertically(
                                targetOffsetY = { fullHeight -> fullHeight }
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 80.dp)
                                    .background(
                                        if (showWarning) Color(0x80B22222) else Color(0x8000B22A),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = if (showWarning) "Вам потрібно менше витрачати" else "Ви на вірному шляху",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }

                    FloatingActionButton(
                        onClick = { showAddTransactionDialog = true },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp),
                        containerColor = Color(0xFFDC143C)
                    ) {
                        Text("+", color = Color.White, style = MaterialTheme.typography.bodyLarge)
                    }

                    if (showAddTransactionDialog) {
                        AddTransactionDialog(
                            categories = expenses.keys.toList(),
                            onDismiss = { showAddTransactionDialog = false },
                            onSave = { transaction ->
                                viewModel.saveExpenseTransaction(context, transaction) // Передаємо поточний Context
                                viewModel.refreshExpenses(context) // Форсоване оновлення витрат
                                showAddTransactionDialog = false
                            }
                        )
                    }


                }
            }
        )
    }
}


@Composable
fun IncomeExpenseChart(
    incomes: Map<String, Double>,
    expenses: Map<String, Double>,
    totalIncomes: Double,
    totalExpenses: Double,
    chartSize: Dp = 150.dp
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // Генерація кольорів для доходів і витрат
        val incomeColors = generateColors(incomes.size.takeIf { it > 0 } ?: 1, isExpense = false)
        val expenseColors = generateColors(expenses.size.takeIf { it > 0 } ?: 1, isExpense = true)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Діаграма доходів
            DonutChart(
                values = incomes.values.toList(),
                maxAmount = totalIncomes,
                chartSize = chartSize,
                colors = incomeColors,
                strokeWidth = 100f,
                chartLabel = "Доходи", // Назва для діаграми доходів
                emptyChartColor = Color(0x8032CD32) // Напівпрозорий зелений для доходів
            )
            Spacer(modifier = Modifier.width(24.dp))
            // Діаграма витрат
            DonutChart(
                values = expenses.values.toList(),
                maxAmount = totalExpenses,
                chartSize = chartSize,
                colors = expenseColors,
                strokeWidth = 100f,
                chartLabel = "Витрати", // Назва для діаграми витрат
                emptyChartColor = Color(0x80B22222) // Напівпрозорий червоний для витрат
            )
        }

        // Легенда для доходів і витрат
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Легенда для доходів
            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier
                    .padding(start = 16.dp)
                    .weight(1f)
            ) {
                incomes.keys.forEachIndexed { index, category ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(color = incomeColors[index], shape = CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = category,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Легенда для витрат
            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier
                    .padding(start = 35.dp, end = 35.dp)
                    .weight(1f)
            ) {
                expenses.keys.forEachIndexed { index, category ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(color = expenseColors[index], shape = CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = category,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DonutChart(
    values: List<Double>,
    maxAmount: Double,
    chartSize: Dp,
    colors: List<Color>,
    strokeWidth: Float,
    chartLabel: String, // Назва діаграми
    emptyChartColor: Color // Колір для пустої діаграми
) {
    Canvas(modifier = Modifier.size(chartSize)) {
        val chartRadius = size.minDimension / 2f
        val innerRadius = chartRadius - strokeWidth * 1.5f // Зменшення розміру внутрішнього кола
        var currentAngle = 0f

        if (values.isEmpty() || maxAmount == 0.0) {
            // Якщо немає категорій або значення порожні, малюємо повний фон діаграми
            drawArc(
                color = emptyChartColor,
                startAngle = 0f,
                sweepAngle = 360f, // Повна діаграма
                useCenter = false,
                topLeft = Offset(0f, 0f),
                size = Size(chartRadius * 2, chartRadius * 2),
                style = Stroke(width = strokeWidth)
            )
        } else if (values.size == 1) {
            // Якщо є тільки одна категорія, заповнюємо діаграму повністю цією категорією
            drawArc(
                color = colors[0],
                startAngle = 0f,
                sweepAngle = 360f, // Повна діаграма
                useCenter = false,
                topLeft = Offset(0f, 0f),
                size = Size(chartRadius * 2, chartRadius * 2),
                style = Stroke(width = strokeWidth)
            )
        } else {
            // Малюємо сегменти діаграми для кількох категорій
            values.forEachIndexed { index, value ->
                val sweepAngle = (value / maxAmount * 360).toFloat() // Перетворюємо на Float
                val color = colors[index % colors.size]

                drawArc(
                    color = color,
                    startAngle = currentAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(0f, 0f),
                    size = Size(chartRadius * 2, chartRadius * 2),
                    style = Stroke(width = strokeWidth) // Ширина кільця
                )
                currentAngle += sweepAngle // Перетворення не потрібне, вже Float
            }
        }

        // Додаємо порожнє коло в центр діаграми
        drawCircle(
            color = Color.Transparent,
            radius = innerRadius, // Використовуємо звужений innerRadius
            style = Stroke(width = 1f) // Порожнє коло
        )

        // Додаємо текст у центр діаграми
        drawContext.canvas.nativeCanvas.apply {
            val textPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE // Білий текст
                textAlign = android.graphics.Paint.Align.CENTER
                textSize = (chartRadius / 4) // Розмір тексту, залежить від розміру діаграми
                isFakeBoldText = true
            }

            drawText(
                chartLabel,
                size.width / 2, // Центр по горизонталі
                size.height / 2 + (textPaint.textSize / 4), // Центр по вертикалі, з урахуванням висоти тексту
                textPaint
            )
        }
    }
}


fun generateColors(size: Int, isExpense: Boolean = false): List<Color> {
    val expenseColors = listOf(
        Color(0xFFD32F2F), // Яскраво-червоний
        Color(0xFFFFC107), // Яскраво-жовтий
        Color(0xFF4CAF50), // Яскраво-зелений
        Color(0xFF2196F3), // Яскраво-синій
        Color(0xFFFF5722), // Яскраво-оранжевий
        Color(0xFF9C27B0), // Яскраво-фіолетовий
        Color(0xFFE91E63), // Яскраво-рожевий
        Color(0xFF00BCD4), // Яскраво-бірюзовий
        Color(0xFF673AB7)  // Насичено-фіолетовий
    )

    val incomeColors = listOf(
        Color(0xFF1E88E5), // Синій
        Color(0xFF43A047), // Зелений
        Color(0xFFF4511E), // Помаранчевий
        Color(0xFFFB8C00), // Жовтогарячий
        Color(0xFF8E24AA), // Фіолетовий
        Color(0xFF26C6DA)  // Бірюзовий
    )
    // Вибираємо кольори залежно від типу
    val baseColors = if (isExpense) expenseColors else incomeColors

    // Генеруємо кольори з урахуванням кількості
    return List(size) { index -> baseColors[index % baseColors.size] }
}
// Функція форматування чисел із пробілами між тисячами
@Composable
fun ExpandableButtonWithAmount(
    text: String,
    amount: Double,
    gradientColors: List<Color>,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    val gradient = Brush.horizontalGradient(
        colors = listOf(
            gradientColors[0],  // Непрозорий колір зліва
            gradientColors[1]   // Прозорий колір справа
        )
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(gradient)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                color = Color.White,
                fontWeight = FontWeight.Bold, // Жирний текст
                fontSize = 18.sp // Розмір шрифту для тексту
            )
            Text(
                text = "₴${"%.2f".format(amount)}",
                color = Color.White,
                fontWeight = FontWeight.Bold, // Жирний текст
                fontSize = 18.sp // Розмір шрифту для суми
            )
        }
    }
}
@Composable
fun IncomeList(incomes: Map<String, Double>) {
    LazyColumn {
        items(incomes.toList()) { (category, amount) ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp, horizontal = 8.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0x99000000), Color(0x66000000)),
                            start = Offset(0f, 0f),
                            end = Offset(1f, 0f) // Горизонтальний градієнт
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = category,
                        style = MaterialTheme.typography.bodyLarge.copy(color = Color.White)
                    )
                    Text(
                        text = "₴${"%.2f".format(amount)}",
                        style = MaterialTheme.typography.bodyLarge.copy(color = Color.White)
                    )
                }
            }
        }
    }
}
@Composable
fun ExpensesList(expenses: Map<String, Double>) {
    LazyColumn {
        items(expenses.toList()) { (category, amount) ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp, horizontal = 8.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0x99000000), Color(0x66000000)),
                            start = Offset(0f, 0f),
                            end = Offset(1f, 0f) // Горизонтальний градієнт
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = category,
                        style = MaterialTheme.typography.bodyLarge.copy(color = Color.White)
                    )
                    Text(
                        text = "₴${"%.2f".format(amount)}",
                        style = MaterialTheme.typography.bodyLarge.copy(color = Color.White)
                    )
                }
            }
        }
    }
}
@Composable
fun CategoryItem(
    text: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    gradientColors: List<Color>
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(
                brush = Brush.horizontalGradient(gradientColors),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            icon() // Іконка зліва
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.White
                )
            )
        }
    }
}
@Composable
fun DrawerContent(
    onNavigateToIncomes: () -> Unit,
    onNavigateToExpenses: () -> Unit,
    // Видалено параметр onNavigateToAnalytics
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(0.8f)
            .background(Color(0xFF1E1E1E).copy(alpha = 0.9f))
            .padding(16.dp)
    ) {
        Text(
            text = "Меню",
            style = MaterialTheme.typography.titleLarge.copy(color = Color.White)
        )
        Spacer(modifier = Modifier.height(40.dp))
        Column(modifier = Modifier.fillMaxWidth()) {
            CategoryItem(
                text = "Доходи",
                icon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_income),
                        contentDescription = "Іконка доходів",
                        tint = Color.White
                    )
                },
                onClick = onNavigateToIncomes,
                gradientColors = listOf(
                    Color(0xFF006400).copy(alpha = 0.5f),
                    Color(0xFF2E8B57).copy(alpha = 0.5f)
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            CategoryItem(
                text = "Витрати",
                icon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_expense),
                        contentDescription = "Іконка витрат",
                        tint = Color.White
                    )
                },
                onClick = onNavigateToExpenses,
                gradientColors = listOf(
                    Color(0xFF8B0000).copy(alpha = 0.5f),
                    Color(0xFFB22222).copy(alpha = 0.5f)
                )
            )
            // Видалено пункт меню для аналітики
        }
    }
}
private fun Modifier.backgroundWithImage(
    painter: androidx.compose.ui.graphics.painter.Painter,
    contentScale: ContentScale
): Modifier {
    return this.then(
        Modifier.paint(
            painter = painter,
            contentScale = contentScale
        )
    )
}
