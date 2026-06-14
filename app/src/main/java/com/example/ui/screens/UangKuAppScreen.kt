package com.example.ui.screens

import android.os.Build
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.database.BillReminderEntity
import com.example.data.database.BudgetEntity
import com.example.data.database.SavingGoalEntity
import com.example.data.database.TransactionEntity
import com.example.data.security.CryptoHelper
import com.example.ui.viewmodel.FinancialViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UangKuAppScreen(viewModel: FinancialViewModel) {
    val context = LocalContext.current
    
    // Observers
    val transactions by viewModel.transactions.collectAsState()
    val budgets by viewModel.budgets.collectAsState()
    val savingGoals by viewModel.savingGoals.collectAsState()
    val bills by viewModel.billReminders.collectAsState()
    val securityPin by viewModel.securityPin.collectAsState()
    val isLocked by viewModel.isLocked.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val firebaseUrl by viewModel.firebaseUrl.collectAsState()
    val firebaseUserId by viewModel.firebaseUserId.collectAsState()
    val syncMsg by viewModel.syncMessage.collectAsState()
    val syncing by viewModel.syncing.collectAsState()
    val aiAdvice by viewModel.aiRecommendation.collectAsState()
    val aiLoading by viewModel.aiLoading.collectAsState()

    // Screen State
    var currentTab by remember { mutableStateOf("home") } // "home", "analytics", "savings", "ai_bank", "settings"
    var showAddDialog by remember { mutableStateOf(false) }
    var showPremiumPinScreen by remember { mutableStateOf(isLocked) }
    var tempPinInput by remember { mutableStateOf("") }
    var showCalculatorDialog by remember { mutableStateOf(false) }

    // Synchronize locked status
    LaunchedEffect(isLocked) {
        showPremiumPinScreen = isLocked
    }

    // --- LOCK SHIELD SCREEN ---
    if (showPremiumPinScreen) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp)
                .testTag("app_lock_screen"),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Lock Secure",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "UangKu Enkripsi End-to-End",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Masukkan PIN 4-Digit Keamanan Akun Anda:",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    OutlinedTextField(
                        value = tempPinInput,
                        onValueChange = { if (it.length <= 4) tempPinInput = it },
                        label = { Text("PIN Keamanan") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("pin_field"),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (viewModel.unlockApp(tempPinInput)) {
                                showPremiumPinScreen = false
                                tempPinInput = ""
                            } else {
                                Toast.makeText(context, "PIN Salah! Gagal dekripsi berkas lokal", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("unlock_button")
                    ) {
                        Text("Buka Enkripsi Akun")
                    }
                }
            }
        }
    } else {
        // --- BASE MAIN APP HOUSING ---
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    tonalElevation = 0.dp,
                    modifier = Modifier.statusBarsPadding()
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 680.dp)
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Wallet Logo",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "UangKu",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = (-0.5).sp,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(if (syncing) MaterialTheme.colorScheme.tertiary else Color(0xFF10B981))
                                    )
                                    Text(
                                        text = if (syncing) "SINKRONISASI..." else "SYNC AKTIF",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Circular Sync Trigger Button
                            IconButton(
                                onClick = { viewModel.performFullCloudSync() },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                if (syncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Sync Cloud",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // Notification button
                            IconButton(
                                onClick = { },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Notifikasi",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
                }
            },
            bottomBar = {
                Column(
                    modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        thickness = 1.dp
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 680.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        NavigationBar(
                            modifier = Modifier.navigationBarsPadding(),
                            containerColor = Color.Transparent,
                            tonalElevation = 0.dp
                        ) {
                            val items = listOf(
                                Triple("home", "Beranda", Icons.Default.Home),
                                Triple("analytics", "Grafik", Icons.Default.Search),
                                Triple("savings", "Tabungan", Icons.Default.Star),
                                Triple("ai_bank", "Bank & AI", Icons.Default.Info),
                                Triple("settings", "Setelan", Icons.Default.Settings)
                            )
                            items.forEach { (tab, label, icon) ->
                                NavigationBarItem(
                                    selected = currentTab == tab,
                                    onClick = { currentTab = tab },
                                    icon = { 
                                        Icon(
                                            imageVector = icon, 
                                            contentDescription = label,
                                            modifier = Modifier.size(20.dp)
                                        ) 
                                    },
                                    label = { 
                                        Text(
                                            text = label, 
                                            fontSize = 10.sp,
                                            fontWeight = if (currentTab == tab) FontWeight.Bold else FontWeight.Medium
                                        ) 
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                                    )
                                )
                            }
                        }
                    }
                }
            },
            floatingActionButton = {
                Column(horizontalAlignment = Alignment.End) {
                    // Floating button for integrated calculator helper
                    LargeFloatingActionButton(
                        onClick = { showCalculatorDialog = true },
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        modifier = Modifier.padding(bottom = 8.dp).size(52.dp).testTag("fab_calculator")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Kalkulator",
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                    FloatingActionButton(
                        onClick = { showAddDialog = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("fab_add_txn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Tambah Keuangan",
                            tint = Color.White
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.TopCenter
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .widthIn(max = 680.dp)
                ) {
                    // Render Sub-Views based on Navigation state
                    when (currentTab) {
                        "home" -> HomeView(
                            transactions = transactions,
                            securityPin = securityPin,
                            syncMsg = syncMsg,
                            viewModel = viewModel
                        )
                        "analytics" -> AnalyticsView(
                            transactions = transactions,
                            budgets = budgets,
                            viewModel = viewModel
                        )
                        "savings" -> SavingsGoalView(
                            goals = savingGoals,
                            viewModel = viewModel
                        )
                        "ai_bank" -> BankAiIntegrationView(
                            advice = aiAdvice,
                            loading = aiLoading,
                            viewModel = viewModel
                        )
                        "settings" -> SettingsView(
                            pin = securityPin,
                            firebaseUrl = firebaseUrl,
                            firebaseUserId = firebaseUserId,
                            themeMode = themeMode,
                            bills = bills,
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }

    // --- DIALOG: INTERACTIVE FINTECH CALCULATOR ---
    if (showCalculatorDialog) {
        Dialog(onDismissRequest = { showCalculatorDialog = false }) {
            CalculatorModalScreen(
                onDismiss = { showCalculatorDialog = false },
                onCopy = { computedValue ->
                    Toast.makeText(context, "Hasil Rp$computedValue disalin ke clipboard! Tempel saat menambah transaksi.", Toast.LENGTH_LONG).show()
                    showCalculatorDialog = false
                }
            )
        }
    }

    // --- DIALOG: RECORD DATA FORM ---
    if (showAddDialog) {
        Dialog(onDismissRequest = { showAddDialog = false }) {
            AddFinanciaRecordDialog(
                onDismiss = { showAddDialog = false },
                onSave = { amount, type, category, desc, bank ->
                    viewModel.addTransaction(amount, type, category, desc, bank)
                    showAddDialog = false
                }
            )
        }
    }
}

// ==========================================
// VIEW TAB 1: HOMEVIEW (Beranda)
// ==========================================
@Composable
fun HomeView(
    transactions: List<TransactionEntity>,
    securityPin: String,
    syncMsg: String,
    viewModel: FinancialViewModel
) {
    var filterCategory by remember { mutableStateOf("Semua") }

    // Dynamic Math Summary
    val totalIncome = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
    val totalExpense = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    val currentBalance = totalIncome - totalExpense

    val filteredList = if (filterCategory == "Semua") {
        transactions
    } else {
        transactions.filter { it.category == filterCategory }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Balance Header Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        "Total Saldo Tersedia",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        formatRupiah(currentBalance),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        letterSpacing = (-1).sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    
                    if (syncMsg.isNotEmpty()) {
                        Text(
                            syncMsg,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Share, contentDescription = "Masuk", tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Pemasukan", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f))
                            }
                            Text(formatRupiah(totalIncome), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge, color = Color(0xFF10B981))
                        }
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = "Keluar", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Pengeluaran", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f))
                            }
                            Text(formatRupiah(totalExpense), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        // Quick Tools Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Simulator Fintech Instan (SMS)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.simulateBankSmsNotification("BCA", 50000.0, false) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                            modifier = Modifier.weight(1f).height(38.dp)
                        ) {
                            Text("BCA SMS Rp50rb", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { viewModel.simulateBankSmsNotification("GOPAY", 15000.0, false) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                            modifier = Modifier.weight(1f).height(38.dp)
                        ) {
                            Text("Gopay Rp15rb", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Category Fast Filters
        item {
            Text("Aktivitas Finansial", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("Semua", "Makanan", "Transportasi", "Kuliah", "Gaji", "Lainnya").forEach { cat ->
                    val isSelected = filterCategory == cat
                    Card(
                        modifier = Modifier
                            .clickable { filterCategory = cat }
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ) {
                        Text(
                            cat, 
                            fontSize = 11.sp, 
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // Transaction List
        if (filteredList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Info, contentDescription = "Empty", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Belum ada logs transaksi. Klik '+' di kanan bawah atau simulate bank SMS di atas untuk mengaktifkan keuangan!", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    }
                }
            }
        } else {
            items(filteredList) { tx ->
                val descDecrypted = CryptoHelper.decrypt(tx.descriptionEncrypted, securityPin.ifEmpty { null })
                TransactionListItem(tx = tx, desc = descDecrypted) {
                    viewModel.deleteTransaction(tx.id)
                }
            }
        }
    }
}

@Composable
fun TransactionListItem(tx: TransactionEntity, desc: String, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Category Avatar Circle Icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (tx.type == "INCOME") Color(0xFF10B981).copy(alpha = 0.15f) else MaterialTheme.colorScheme.error.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (tx.type == "INCOME") Icons.Default.AddCircle else Icons.Default.Warning,
                        contentDescription = "Cat",
                        tint = if (tx.type == "INCOME") Color(0xFF10B981) else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(desc, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Badge(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
                            Text(tx.category, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 4.dp))
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(tx.bankName, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (tx.isSynced) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.CheckCircle, contentDescription = "Synced Cloud", tint = Color(0xFF10B981), modifier = Modifier.size(10.dp))
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${if (tx.type == "INCOME") "+" else "-"} Rp${formatAmount(tx.amount)}",
                    fontWeight = FontWeight.Black,
                    color = if (tx.type == "INCOME") Color(0xFF10B981) else MaterialTheme.colorScheme.error,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

// ==========================================
// VIEW TAB 2: ANALYTICS & VISUALIZATION (Grafik)
// ==========================================
@Composable
fun AnalyticsView(
    transactions: List<TransactionEntity>,
    budgets: List<BudgetEntity>,
    viewModel: FinancialViewModel
) {
    val context = LocalContext.current
    var budgetCategoryInput by remember { mutableStateOf("Makanan") }
    var budgetLimitInput by remember { mutableStateOf("") }

    // Math summaries for canvas
    val incomeSum = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
    val expenseSum = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }

    // Category calculation for Pie Chart
    val categoryTotals = transactions.filter { it.type == "EXPENSE" }
        .groupBy { it.category }
        .mapValues { entry -> entry.value.sumOf { it.amount } }

    val totalExpenses = categoryTotals.values.sum()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Visualisasi Analitik Keuangan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Pantau pengeluaran Anda dengan grafis real-time Canvas", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // Comparative Bar Chart Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Perbandingan Alur Kas (Masuk vs Keluar)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Simple, Highly Solid comparative Bar Chart drawn on Canvas
                    Box(modifier = Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val maxVal = maxOf(incomeSum, expenseSum, 1.0)
                            val canvasW = size.width
                            val canvasH = size.height
                            
                            val barWidth = canvasW / 5f
                            val margin = canvasW / 10f

                            // Income Bar (Soft Green)
                            val incomeHeight = (incomeSum / maxVal * canvasH).toFloat()
                            drawRect(
                                color = Color(0xFF81C784),
                                topLeft = Offset(margin, canvasH - incomeHeight),
                                size = Size(barWidth, incomeHeight)
                            )

                            // Expense Bar (Soft Red)
                            val expenseHeight = (expenseSum / maxVal * canvasH).toFloat()
                            drawRect(
                                color = Color(0xFFE57373),
                                topLeft = Offset(canvasW - margin - barWidth, canvasH - expenseHeight),
                                size = Size(barWidth, expenseHeight)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFF81C784)))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Total Masuk (Rp${formatAmount(incomeSum)})", fontSize = 10.sp, fontWeight = FontWeight.Medium)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFFE57373)))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Total Keluar (Rp${formatAmount(expenseSum)})", fontSize = 10.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }

        // Pie Chart: Spend Categories Breakdown
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Peta Distribusi Pengeluaran (Pie Chart)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(16.dp))

                    if (categoryTotals.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                            Text("Masukkan data pengeluaran untuk menggambar peta kategori", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        val pieColors = listOf(
                            Color(0xFF6750A4), // Primary Purple
                            Color(0xFFD0BCFF), // Light Lavender
                            Color(0xFF625B71), // Slate Purple
                            Color(0xFFE8DEF8), // Softer Lavender
                            Color(0xFF7D5260), // Mauve Accent
                            Color(0xFFFFD8E4)  // Rose Accent
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Circular Canvas Wedge
                            Box(modifier = Modifier.size(120.dp)) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    var startAngle = 0f
                                    categoryTotals.toList().forEachIndexed { index, pair ->
                                        val angle = (pair.second / totalExpenses * 360f).toFloat()
                                        drawArc(
                                            color = pieColors[index % pieColors.size],
                                            startAngle = startAngle,
                                            sweepAngle = angle,
                                            useCenter = true
                                        )
                                        startAngle += angle
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                categoryTotals.toList().forEachIndexed { index, (cat, amt) ->
                                    val percent = (amt / totalExpenses * 100).toInt()
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(pieColors[index % pieColors.size]))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("$cat $percent% (Rp${formatAmount(amt)})", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Configuration tool: Monthly Limit Budgets (AI benchmark)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Pengatur Anggaran (Mencegah Konsumtif)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        var expanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1f).clickable { expanded = true }) {
                            OutlinedTextField(
                                value = budgetCategoryInput,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Kategori") },
                                modifier = Modifier.fillMaxWidth(),
                                trailingIcon = { Icon(Icons.Default.Add, contentDescription = "Cat") }
                            )
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                listOf("Makanan", "Transportasi", "Kuliah", "Hiburan", "Lainnya").forEach { c ->
                                    DropdownMenuItem(text = { Text(c) }, onClick = {
                                        budgetCategoryInput = c
                                        expanded = false
                                    })
                                }
                            }
                        }

                        OutlinedTextField(
                            value = budgetLimitInput,
                            onValueChange = { budgetLimitInput = it },
                            label = { Text("Limit (Rp)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val limits = budgetLimitInput.toDoubleOrNull() ?: 0.0
                            if (limits > 0) {
                                viewModel.saveBudget(budgetCategoryInput, limits)
                                budgetLimitInput = ""
                                Toast.makeText(context, "Batas anggaran disetel!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Jumlah limit tidak valid", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Simpan Batas Anggaran")
                    }
                }
            }
        }

        // Active Budget Limits tracking list
        item {
            Text("Pemberitahuan Kelompok Anggaran", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }

        if (budgets.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Text(
                        "Gunakan formulir di atas untuk menyetel batas bulanan agar AI memproyeksikan tips penghematan luar biasa!",
                        fontSize = 11.sp, 
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(budgets) { bgt ->
                val spent = transactions.filter { it.type == "EXPENSE" && bgt.category == it.category }.sumOf { it.amount }
                val ratio = minOf(spent / bgt.limitAmount, 1.0).toFloat()
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(bgt.category, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                            IconButton(onClick = { viewModel.deleteBudget(bgt.category) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Del", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                        
                        // Budget meter bar
                        LinearProgressIndicator(
                            progress = { ratio },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = if (ratio >= 0.9f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Terpakai: Rp${formatAmount(spent)}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Limit: Rp${formatAmount(bgt.limitAmount)}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// VIEW TAB 3: TABUNGAN (Savings & Goals)
// ==========================================
@Composable
fun SavingsGoalView(
    goals: List<SavingGoalEntity>,
    viewModel: FinancialViewModel
) {
    val context = LocalContext.current
    var goalNameInput by remember { mutableStateOf("") }
    var goalTargetInput by remember { mutableStateOf("") }

    var contributionGoalSelected by remember { mutableStateOf<SavingGoalEntity?>(null) }
    var contributionAmountInput by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Program Menabung Mahasiswa & Profesional", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Simpan uang receh Anda untuk mewujudkan impian masa depan!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // Add Saving Goal Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Mulai Mimpi Baru (Menabung)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = goalNameInput,
                        onValueChange = { goalNameInput = it },
                        label = { Text("Nama Mimpi (e.g. Laptop Kuliah, Liburan)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = goalTargetInput,
                        onValueChange = { goalTargetInput = it },
                        label = { Text("Target Dana Impian (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val amt = goalTargetInput.toDoubleOrNull() ?: 0.0
                            if (goalNameInput.isNotEmpty() && amt > 0) {
                                viewModel.saveSavingGoal(
                                    name = goalNameInput,
                                    target = amt,
                                    current = 0.0,
                                    deadline = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000) // Default 30 day target duration
                                )
                                goalNameInput = ""
                                goalTargetInput = ""
                                Toast.makeText(context, "Mimpi tabungan didaftarkan!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Detail mimpi tidak lengkap", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Tambahkan Target Tabungan")
                    }
                }
            }
        }

        // Saving Goals Log List
        item {
            Text("Daftar Celengan Aktif", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }

        if (goals.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Text(
                        "Belum ada target menabung didaftarkan. Cari impian Anda dan mulai menyisihkan uang konsumsi Anda!",
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(goals) { goal ->
                val progress = (goal.currentAmount / goal.targetAmount).toFloat()
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(goal.name, fontWeight = FontWeight.Bold)
                                Text("Target Dana: Rp${formatAmount(goal.targetAmount)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Row {
                                IconButton(onClick = { contributionGoalSelected = goal }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.AddCircle, contentDescription = "Contribute", tint = Color(0xFF10B981))
                                }
                                IconButton(onClick = { viewModel.deleteSavingGoal(goal.id) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { progress.coerceAtMost(1f) },
                            modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                            color = Color(0xFF10B981)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Terkumpul: Rp${formatAmount(goal.currentAmount)} (${(progress * 100).toInt()}%)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                            Text("Kekurangan: Rp${formatAmount(maxOf(0.0, goal.targetAmount - goal.currentAmount))}", fontSize = 10.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }

    // Modal dialogue to contribute saving logs!
    if (contributionGoalSelected != null) {
        AlertDialog(
            onDismissRequest = { contributionGoalSelected = null },
            title = { Text("Masukkan Celengan ${contributionGoalSelected?.name}") },
            text = {
                Column {
                    Text("Berapa jumlah uang yang ingin Anda sisihkan dari dompet hari ini?", fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
                    OutlinedTextField(
                        value = contributionAmountInput,
                        onValueChange = { contributionAmountInput = it },
                        label = { Text("Jumlah Sisih (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = contributionAmountInput.toDoubleOrNull() ?: 0.0
                        if (amount > 0 && contributionGoalSelected != null) {
                            viewModel.contributeSaving(contributionGoalSelected!!, amount)
                            contributionGoalSelected = null
                            contributionAmountInput = ""
                            Toast.makeText(context, "Dana celengan ditambahkan!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Jumlah maut tidak valid", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Konfirmasi Celengan")
                }
            },
            dismissButton = {
                TextButton(onClick = { contributionGoalSelected = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

// ==========================================
// VIEW TAB 4: HUBUNG BANK & AI RECOMMENDATIONS (Bank & AI)
// ==========================================
@Composable
fun BankAiIntegrationView(
    advice: String,
    loading: Boolean,
    viewModel: FinancialViewModel
) {
    val context = LocalContext.current
    var pasteStatementText by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Automated Bank Link & AI Strategist", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Sistem penaut bank otomatis dan kecerdasan artifisial pembantu hemat", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // Module A: E-Statement Copy Paste Parser
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = "Stat", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ekstraktor E-Statement Instan", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Text(
                        "Salin teks riwayat transaksi m-banking Anda (BCA KlikBCA, Livin Mandiri, Gopay, OVO) lalu tempel di bawah untuk didata otomatis tanpa input manual satu per satu!",
                        fontSize = 11.sp, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                    OutlinedTextField(
                        value = pasteStatementText,
                        onValueChange = { pasteStatementText = it },
                        label = { Text("Tempel Teks E-Statement di Sini") },
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        placeholder = { Text("m-BCA: Tanggal 13/06/2026 TRSF Rp150.000...") }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            if (pasteStatementText.isNotEmpty()) {
                                viewModel.pasteEStatement(
                                    pasteStatementText,
                                    onSuccess = {
                                        pasteStatementText = ""
                                        Toast.makeText(context, "E-Statement berhasil diimpor otomatis ke UangKu!", Toast.LENGTH_LONG).show()
                                    },
                                    onFailure = {
                                        Toast.makeText(context, "Format teks gagal dikenali. Pastikan ada tulisan nominal Uang RP di dalamnya.", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Impor E-Statement Otomatis")
                    }
                }
            }
        }

        // Module B: AI Budget Advisor
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = "AI", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Strategi Anggaran Bulanan AI Gemini", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Text(
                        "UangKu mengirimkan logs grafik alur kas enkripsi Anda untuk dianalisis oleh AI Gemini. Dapatkan tips hemat konkrit, rasio budget, serta taktik saving rate secara cepat!",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                    Button(
                        onClick = { viewModel.fetchAiBudgetRecommendations() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth().testTag("ai_button")
                    ) {
                        if (loading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                        } else {
                            Text("Model AI: Ambil Analisis Hemat Gemini")
                        }
                    }

                    if (advice.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = advice, 
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.testTag("ai_advice_text")
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// VIEW TAB 5: SETTINGS & BILL ALERTS (Settings)
// ==========================================
@Composable
fun SettingsView(
    pin: String,
    firebaseUrl: String,
    firebaseUserId: String,
    themeMode: String,
    bills: List<BillReminderEntity>,
    viewModel: FinancialViewModel
) {
    val context = LocalContext.current
    var pinText by remember { mutableStateOf(pin) }
    var fbUrlText by remember { mutableStateOf(firebaseUrl) }
    var fbUserText by remember { mutableStateOf(firebaseUserId) }

    // Bill Addition States
    var billTitleInput by remember { mutableStateOf("") }
    var billAmountInput by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Setelan & Pengingat Tagihan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Konfigurasikan perlindungan enkripsi, Firebase CRUD, dan tagihan bulanan", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // Section A: Security & Encrypt Key PIN Spec (End-to-End Cryptography)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Perlindungan Enkripsi PIN (End-to-End)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(
                        "Setel PIN 4-angka untuk mengunci dan mengenkripsi deskripsi log lokal dengan enkripsi AES-128. Tanpa kunci PIN Anda, data di cloud maupun lokal terenkripsi 100%!",
                        fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 4.dp)
                    )
                    OutlinedTextField(
                        value = pinText,
                        onValueChange = { if (it.length <= 4) pinText = it },
                        label = { Text("4-Digit PIN Baru") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("pin_set_field"),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (pinText.length == 4) {
                                    viewModel.setSecurityPin(pinText)
                                    Toast.makeText(context, "PIN Didata! Database Anda terenkripsi.", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "PIN wajib 4 digit angka", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Aktifkan PIN")
                        }
                        Button(
                            onClick = {
                                pinText = ""
                                viewModel.removeSecurity()
                                Toast.makeText(context, "Sistem keamanan PIN dihapus.", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Nonaktifkan PIN")
                        }
                    }
                }
            }
        }

        // Section B: Automatic Mode (Gelap & Terang) Setting
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Preferensi Desain Mode Gelap / Terang", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("AUTO", "LIGHT", "DARK").forEach { mode ->
                            val isSelected = themeMode == mode
                            Button(
                                onClick = { viewModel.changeThemeMode(mode) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.weight(1f).height(36.dp)
                            ) {
                                Text(
                                    text = when (mode) {
                                        "AUTO" -> "Auto Waktu"
                                        "LIGHT" -> "Terang"
                                        else -> "Gelap"
                                    },
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section C: Firebase Cloud CRUD REST API Link
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Pemuat Integrasi Firebase Awan (Cloud CRUD)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = fbUrlText,
                        onValueChange = { fbUrlText = it },
                        label = { Text("URL Firebase Realtime Database") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = fbUserText,
                        onValueChange = { fbUserText = it },
                        label = { Text("Path User / ID Pembeda") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            viewModel.updateFirebaseConfigs(fbUrlText, fbUserText)
                            Toast.makeText(context, "Sistem Firebase diubah! Tekan ikon sinkronisasi di atas untuk memicu CRUD.", Toast.LENGTH_LONG).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Tautkan Struktur Firebase CRUD")
                    }
                }
            }
        }

        // Section D: Manage Bills and Reminders (Smart Alerts)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Daftarkan Pengingat Tagihan (Notifikasi Pintar)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = billTitleInput,
                        onValueChange = { billTitleInput = it },
                        label = { Text("Nama Tagihan (e.g. Uang Kost, Wifi)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = billAmountInput,
                        onValueChange = { billAmountInput = it },
                        label = { Text("Nominal Tagihan (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            val amount = billAmountInput.toDoubleOrNull() ?: 0.0
                            if (billTitleInput.isNotEmpty() && amount > 0) {
                                // Default due date is 2 days from now to immediately mock the smart alerts!
                                val simulatedDueDate = System.currentTimeMillis() + (1L * 24 * 60 * 60 * 1000)
                                viewModel.saveBillReminder(
                                    title = billTitleInput,
                                    amount = amount,
                                    dueDate = simulatedDueDate,
                                    frequency = "MONTHLY"
                                )
                                billTitleInput = ""
                                billAmountInput = ""
                                Toast.makeText(context, "Tagihan terdaftar! Anda akan menerima notifikasi system pintar karena jatuh tempo besok.", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Detail tagihan salah", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Tambahkan Tagihan & Jadwalkan Notifikasi")
                    }
                }
            }
        }

        item {
            Text("Daftar Tagihan Aktif", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }

        if (bills.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Text(
                        "Belum ada agenda tagihan terdaftar.",
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(bills) { bill ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(bill.title, fontWeight = FontWeight.Bold)
                            Text("Biaya: Rp${formatAmount(bill.amount)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "Jatuh Tempo: ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(bill.dueDate))}",
                                fontSize = 10.sp, 
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!bill.isPaid) {
                                Button(
                                    onClick = { viewModel.markBillAsPaid(bill) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                    modifier = Modifier.height(30.dp).testTag("pay_bill_btn")
                                ) {
                                    Text("Bayar Sekarang", fontSize = 9.sp)
                                }
                            } else {
                                Badge(containerColor = Color(0xFF10B981).copy(alpha = 0.2f), contentColor = Color(0xFF10B981)) {
                                    Text("Lunas", fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(onClick = { viewModel.deleteBillReminder(bill.id) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Del", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }

        // Module E: Full System wipe
        item {
            Button(
                onClick = {
                    viewModel.clearAllLocally()
                    Toast.makeText(context, "Log database lokal disapu bersih!", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("RESET DATABASE LOKAL UANGKU")
            }
        }
    }
}

// ==========================================
// MODAL: RECORD DATA FORM
// ==========================================
@Composable
fun AddFinanciaRecordDialog(
    onDismiss: () -> Unit,
    onSave: (amount: Double, type: String, category: String, desc: String, bank: String) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var typeText by remember { mutableStateOf("EXPENSE") } // "EXPENSE" or "INCOME"
    var categoryText by remember { mutableStateOf("Makanan") }
    var descriptionText by remember { mutableStateOf("") }
    var bankText by remember { mutableStateOf("CASH") }

    var catExpanded by remember { mutableStateOf(false) }
    var bankExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 460.dp)
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Header with simple visual badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Icon",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "Tambah Catatan",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "Catat alur kas keuangan masuk/keluar Anda secara instan",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }

            // Customized Segmented Segment Toggle [Pengeluaran vs Pemasukan]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isExpense = typeText == "EXPENSE"
                
                // Expense option
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isExpense) MaterialTheme.colorScheme.error else Color.Transparent)
                        .clickable { typeText = "EXPENSE" },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Pengeluaran",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (isExpense) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Income option
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (!isExpense) Color(0xFF10B981) else Color.Transparent)
                        .clickable { typeText = "INCOME" },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Pemasukan",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (!isExpense) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Amount Input with live-updating formatted Rupiah helper text
            val amountVal = amountText.toDoubleOrNull() ?: 0.0
            val formattedHelperText = if (amountText.isNotEmpty()) {
                "Setara: ${formatRupiah(amountVal)}"
            } else {
                "Contoh input nominal angka: 50000"
            }

            OutlinedTextField(
                value = amountText,
                onValueChange = { input ->
                    if (input.all { it.isDigit() }) {
                        amountText = input
                    }
                },
                label = { Text("Besar Dana Rupiah") },
                placeholder = { Text("cth. Rp 50.000") },
                supportingText = {
                    Text(
                        text = formattedHelperText,
                        fontWeight = FontWeight.SemiBold,
                        color = if (amountText.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_txn_amount"),
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            // Description Input
            OutlinedTextField(
                value = descriptionText,
                onValueChange = { descriptionText = it },
                label = { Text("Keterangan Catatan") },
                placeholder = { Text("cth. Nasi padang, gaji bulanan, tiket bus") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_txn_desc"),
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            // Select category
            Box {
                OutlinedTextField(
                    value = categoryText,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Kategori") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = { Icon(Icons.Default.Add, contentDescription = "Expand") }
                )
                Box(modifier = Modifier.matchParentSize().clickable { catExpanded = true })
                DropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                    listOf("Makanan", "Transportasi", "Kuliah", "Hiburan", "Gaji", "Tabungan", "Lainnya").forEach { cat ->
                        DropdownMenuItem(text = { Text(cat) }, onClick = {
                            categoryText = cat
                            catExpanded = false
                        })
                    }
                }
            }

            // Select Bank Source
            Box {
                OutlinedTextField(
                    value = bankText,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Dompet / Bank") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = { Icon(Icons.Default.Menu, contentDescription = "Expand") }
                )
                Box(modifier = Modifier.matchParentSize().clickable { bankExpanded = true })
                DropdownMenu(expanded = bankExpanded, onDismissRequest = { bankExpanded = false }) {
                    listOf("CASH", "BCA", "MANDIRI", "BNI", "OVO", "GOPAY").forEach { b ->
                        DropdownMenuItem(text = { Text(b) }, onClick = {
                            bankText = b
                            bankExpanded = false
                        })
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            
            // Cancel and Save buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "Batal", 
                        fontWeight = FontWeight.Bold, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                val isValid = amountVal > 0.0 && descriptionText.isNotBlank()
                Button(
                    onClick = {
                        if (isValid) {
                            onSave(amountVal, typeText, categoryText, descriptionText, bankText)
                        }
                    },
                    enabled = isValid,
                    modifier = Modifier
                        .weight(1.5f)
                        .height(46.dp)
                        .testTag("submit_transaction_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (typeText == "EXPENSE") MaterialTheme.colorScheme.error else Color(0xFF10B981),
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "Catat Log", 
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ==========================================
// MODAL: DYNAMIC FLOATING FINTECH CALCULATOR
// ==========================================
@Composable
fun CalculatorModalScreen(
    onDismiss: () -> Unit,
    onCopy: (String) -> Unit
) {
    var displayValue by remember { mutableStateOf("0") }
    var currentOperator by remember { mutableStateOf<Char?>(null) }
    var previousValue by remember { mutableStateOf(0.0) }
    var isNewOperand by remember { mutableStateOf(true) }

    val handleNumPress = { num: String ->
        if (isNewOperand || displayValue == "0") {
            displayValue = num
            isNewOperand = false
        } else {
            displayValue += num
        }
    }

    val handleOpPress = { op: Char ->
        previousValue = displayValue.toDoubleOrNull() ?: 0.0
        currentOperator = op
        isNewOperand = true
    }

    val handleEquals = {
        val currentVal = displayValue.toDoubleOrNull() ?: 0.0
        if (currentOperator != null) {
            val result = when (currentOperator) {
                '+' -> previousValue + currentVal
                '-' -> previousValue - currentVal
                '*' -> previousValue * currentVal
                '/' -> if (currentVal != 0.0) previousValue / currentVal else 0.0
                else -> currentVal
            }
            displayValue = if (result % 1.0 == 0.0) {
                result.toInt().toString()
            } else {
                result.toString()
            }
            currentOperator = null
            isNewOperand = true
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().wrapContentHeight(),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Simbolis Finansial Kalkulator", fontWeight = FontWeight.Bold)
            
            // Screen
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text(
                    text = displayValue,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    maxLines = 1
                )
            }

            // Keyboard grid
            val buttons = listOf(
                listOf("7", "8", "9", "/"),
                listOf("4", "5", "6", "*"),
                listOf("1", "2", "3", "-"),
                listOf("0", "C", "=", "+")
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                buttons.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { char ->
                            Button(
                                onClick = {
                                    when {
                                        char in "0123456789" -> handleNumPress(char)
                                        char == "C" -> {
                                            displayValue = "0"
                                            previousValue = 0.0
                                            currentOperator = null
                                            isNewOperand = true
                                        }
                                        char == "=" -> handleEquals()
                                        else -> handleOpPress(char.first())
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = when (char) {
                                        "C" -> MaterialTheme.colorScheme.errorContainer
                                        "=" -> MaterialTheme.colorScheme.primary
                                        "/", "*", "-", "+" -> MaterialTheme.colorScheme.secondaryContainer
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    contentColor = when (char) {
                                        "C" -> MaterialTheme.colorScheme.onErrorContainer
                                        "=" -> Color.White
                                        "/", "*", "-", "+" -> MaterialTheme.colorScheme.onSecondaryContainer
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                ),
                                modifier = Modifier.weight(1f).height(46.dp)
                            ) {
                                Text(char, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onDismiss) { Text("Tutup") }
                Button(
                    onClick = { onCopy(displayValue) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    Text("Salin Dana Nilai")
                }
            }
        }
    }
}

// ==========================================
// STRING CURRENCY UTILITIES
// ==========================================
fun formatRupiah(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
    return format.format(amount).replace("Rp", "Rp ")
}

fun formatAmount(amount: Double): String {
    val formatter = NumberFormat.getInstance(Locale("in", "ID"))
    return formatter.format(amount)
}

fun formatAmount(amount: Float): String = formatAmount(amount.toDouble())

fun formatAmount(amount: Long): String = formatAmount(amount.toDouble())

fun formatRupiah(amount: Float): String = formatRupiah(amount.toDouble())

fun formatRupiah(amount: Long): String = formatRupiah(amount.toDouble())

fun formatAmount(amount: Int): String = formatAmount(amount.toDouble())
