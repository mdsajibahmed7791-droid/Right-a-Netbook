package com.example

import android.content.Context
import android.content.ContextWrapper
import android.content.res.AssetManager
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.NotebookRepository
import com.example.model.AppLanguage
import com.example.model.MainTab
import com.example.ui.auth.AuthScreen
import com.example.ui.components.LottieTaskCompleteOverlay
import com.example.ui.screens.AdminTabScreen
import com.example.ui.screens.DeliveryTabScreen
import com.example.ui.screens.FreeTabScreen
import com.example.ui.screens.ItemsTabScreen
import com.example.ui.screens.PeopleTabScreen
import com.example.ui.screens.ReportTabScreen
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassSurfaceElevated
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.NotebookGradientBackground
import com.example.ui.theme.NotebookTheme
import com.example.ui.theme.TealBright
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.TealTint
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.FormatUtils
import com.example.util.LanguageStore
import com.example.viewmodel.AdminViewModel
import com.example.viewmodel.MainViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        setContent {
            NotebookAppRoot(activity = this@MainActivity)
        }
    }
}

@Composable
private fun NotebookAppRoot(activity: ComponentActivity) {
    val baseContext = LocalContext.current
    val scope = rememberCoroutineScope()
    val languageStore = remember(baseContext) { LanguageStore(baseContext.applicationContext) }
    val currentLanguage by languageStore.languageFlow.collectAsStateWithLifecycle(initialValue = AppLanguage.EN)

    val localizedContext = remember(baseContext, currentLanguage) {
        createLocalizedContext(baseContext, currentLanguage)
    }
    val layoutDirection = if (currentLanguage == AppLanguage.AR) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalConfiguration provides localizedContext.resources.configuration,
        LocalLayoutDirection provides layoutDirection,
        LocalActivityResultRegistryOwner provides activity,
    ) {
        NotebookTheme {
            AppAuthGate(
                currentLanguage = currentLanguage,
                onSelectLanguage = { lang ->
                    scope.launch { languageStore.setLanguage(lang) }
                },
            )
        }
    }
}

private fun createLocalizedContext(baseContext: Context, language: AppLanguage): Context {
    val locale = FormatUtils.localeFor(language.code)
    Locale.setDefault(locale)
    val config = Configuration(baseContext.resources.configuration)
    config.setLocale(locale)
    config.setLayoutDirection(locale)
    val configContext = baseContext.createConfigurationContext(config)
    return object : ContextWrapper(baseContext) {
        override fun getResources(): Resources = configContext.resources
        override fun getAssets(): AssetManager = configContext.assets
    }
}

@Composable
private fun AppAuthGate(
    currentLanguage: AppLanguage,
    onSelectLanguage: (AppLanguage) -> Unit,
) {
    val auth = remember { Firebase.auth }
    var currentUser by remember { mutableStateOf(auth.currentUser) }

    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            currentUser = firebaseAuth.currentUser
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(
            currentLanguage = currentLanguage,
            onSelectLanguage = onSelectLanguage,
            onAuthSuccess = { currentUser = auth.currentUser },
        )
    } else {
        key(user.uid) {
            AuthenticatedShell(
                uid = user.uid,
                userEmail = user.email.orEmpty(),
                currentLanguage = currentLanguage,
                onSelectLanguage = onSelectLanguage,
                onSignedOut = { currentUser = null },
            )
        }
    }
}

private data class NavTabSpec(
    val tab: MainTab,
    val labelRes: Int,
    val icon: ImageVector,
)

@Composable
private fun AuthenticatedShell(
    uid: String,
    userEmail: String,
    currentLanguage: AppLanguage,
    onSelectLanguage: (AppLanguage) -> Unit,
    onSignedOut: () -> Unit,
) {
    val mainViewModel: MainViewModel = viewModel(
        key = uid,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                MainViewModel(NotebookRepository(app), uid)
            }
        },
    )

    val isAdmin by mainViewModel.isAdmin.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableStateOf(MainTab.FREE) }
    var completedTaskMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val credentialManager = remember(context) { CredentialManager.create(context) }

    if (selectedTab == MainTab.ADMIN && !isAdmin) {
        selectedTab = MainTab.FREE
    }

    BackHandler(enabled = selectedTab != MainTab.FREE) {
        selectedTab = MainTab.FREE
    }

    val tabs = remember(isAdmin) {
        buildList {
            add(NavTabSpec(MainTab.FREE, R.string.tab_free, Icons.Outlined.LocalCafe))
            add(NavTabSpec(MainTab.ITEMS, R.string.tab_items, Icons.Outlined.Inventory2))
            add(NavTabSpec(MainTab.PEOPLE, R.string.tab_people, Icons.Outlined.Group))
            add(NavTabSpec(MainTab.REPORT, R.string.tab_report, Icons.Outlined.Assessment))
            add(NavTabSpec(MainTab.DELIVERY, R.string.tab_delivery, Icons.Outlined.LocalShipping))
            if (isAdmin) {
                add(NavTabSpec(MainTab.ADMIN, R.string.tab_admin, Icons.Outlined.AdminPanelSettings))
            }
        }
    }

    NotebookGradientBackground {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                NotebookHeaderBar(
                    userEmail = userEmail,
                    isAdmin = isAdmin,
                    currentLanguage = currentLanguage,
                    onSelectLanguage = onSelectLanguage,
                    onSignOut = {
                        scope.launch {
                            withContext(NonCancellable) {
                                try {
                                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                                } catch (_: Exception) {
                                } finally {
                                    Firebase.auth.signOut()
                                    onSignedOut()
                                }
                            }
                        }
                    },
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = GlassSurfaceElevated,
                    contentColor = TextPrimary,
                    tonalElevation = 0.dp,
                ) {
                    tabs.forEach { spec ->
                        val selected = selectedTab == spec.tab
                        NavigationBarItem(
                            selected = selected,
                            onClick = { selectedTab = spec.tab },
                            icon = {
                                Icon(
                                    imageVector = spec.icon,
                                    contentDescription = stringResource(spec.labelRes),
                                )
                            },
                            label = {
                                Text(
                                    text = stringResource(spec.labelRes),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = NavyDeep,
                                selectedTextColor = TealBright,
                                indicatorColor = TealPrimary,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted,
                            ),
                            modifier = Modifier.testTag("nav_tab_${spec.tab.name.lowercase()}"),
                        )
                    }
                }
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                val showMessage: (String) -> Unit = { msg ->
                    scope.launch { snackbarHostState.showSnackbar(msg) }
                }
                val showTaskSuccess: (String) -> Unit = { msg ->
                    completedTaskMessage = msg
                }

                Crossfade(
                    targetState = selectedTab,
                    animationSpec = tween(durationMillis = 220),
                    label = "tab_crossfade",
                ) { activeTab ->
                    when (activeTab) {
                        MainTab.FREE -> FreeTabScreen(mainViewModel, currentLanguage.code, showMessage, showTaskSuccess)
                        MainTab.ITEMS -> ItemsTabScreen(mainViewModel, currentLanguage.code, showMessage, showTaskSuccess)
                        MainTab.PEOPLE -> PeopleTabScreen(mainViewModel, showMessage, showTaskSuccess)
                        MainTab.REPORT -> ReportTabScreen(mainViewModel, currentLanguage.code, showMessage, showTaskSuccess)
                        MainTab.DELIVERY -> DeliveryTabScreen(mainViewModel, currentLanguage.code, showMessage, showTaskSuccess)
                        MainTab.ADMIN -> {
                            val adminViewModel: AdminViewModel = viewModel(
                                key = "admin_$uid",
                                factory = viewModelFactory {
                                    initializer {
                                        val app = checkNotNull(this[APPLICATION_KEY])
                                        AdminViewModel(NotebookRepository(app))
                                    }
                                },
                            )
                            AdminTabScreen(adminViewModel, uid, currentLanguage.code, showMessage, showTaskSuccess)
                        }
                    }
                }

                LottieTaskCompleteOverlay(
                    message = completedTaskMessage,
                    onFinished = { completedTaskMessage = null },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun NotebookHeaderBar(
    userEmail: String,
    isAdmin: Boolean,
    currentLanguage: AppLanguage,
    onSelectLanguage: (AppLanguage) -> Unit,
    onSignOut: () -> Unit,
) {
    var langExpanded by remember { mutableStateOf(false) }

    Surface(
        color = GlassSurfaceElevated,
        border = BorderStroke(1.dp, GlassBorder),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                    )
                    Surface(
                        shape = CircleShape,
                        color = TealTint,
                        border = BorderStroke(1.dp, TealPrimary),
                    ) {
                        Text(
                            text = stringResource(if (isAdmin) R.string.role_admin else R.string.role_staff),
                            style = MaterialTheme.typography.labelSmall,
                            color = TealBright,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
                if (userEmail.isNotBlank()) {
                    Text(
                        text = userEmail,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box {
                    Surface(
                        shape = CircleShape,
                        color = TealTint,
                        border = BorderStroke(1.dp, GlassBorder),
                        onClick = { langExpanded = true },
                        modifier = Modifier.testTag("header_language_button"),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Outlined.Language, contentDescription = null, tint = TealBright, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(currentLanguage.label, style = MaterialTheme.typography.labelSmall, color = TextPrimary)
                        }
                    }
                    DropdownMenu(
                        expanded = langExpanded,
                        onDismissRequest = { langExpanded = false },
                    ) {
                        AppLanguage.entries.forEach { lang ->
                            DropdownMenuItem(
                                text = { Text(lang.label) },
                                onClick = {
                                    langExpanded = false
                                    onSelectLanguage(lang)
                                },
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onSignOut,
                    modifier = Modifier.size(48.dp).testTag("sign_out_button"),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Logout,
                        contentDescription = stringResource(R.string.sign_out),
                        tint = TextSecondary,
                    )
                }
            }
        }
    }
}
