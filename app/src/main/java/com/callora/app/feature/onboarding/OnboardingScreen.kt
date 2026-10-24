package com.callora.app.feature.onboarding

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.callora.app.R
import com.callora.app.core.designsystem.component.PrimaryCalloraButton
import com.callora.app.core.designsystem.component.SecondaryCalloraButton
import kotlinx.coroutines.launch

private data class OnboardingPage(val titleRes: Int, val bodyRes: Int)

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onFinished: () -> Unit,
) {
    val context = LocalContext.current
    val pages = remember {
        listOf(
            OnboardingPage(R.string.onboarding_welcome_title, R.string.onboarding_welcome_body),
            OnboardingPage(R.string.onboarding_principle_title, R.string.onboarding_principle_body),
            OnboardingPage(R.string.onboarding_sims_title, R.string.onboarding_sims_body),
            OnboardingPage(R.string.onboarding_permissions_title, R.string.onboarding_permissions_body),
            OnboardingPage(R.string.onboarding_privacy_title, R.string.onboarding_privacy_body),
            OnboardingPage(R.string.onboarding_finish_title, R.string.onboarding_finish_body),
        )
    }
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    var permissionRequestCount by remember { mutableIntStateOf(0) }

    val requiredPermissions = remember {
        buildList {
            add(Manifest.permission.READ_PHONE_STATE)
            add(Manifest.permission.CALL_PHONE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { /* result is re-checked on demand; onboarding never blocks completion */ }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Box(modifier = Modifier.fillMaxSize().weight(1f)) {
            HorizontalPager(state = pagerState) { pageIndex ->
                val page = pages[pageIndex]
                Column(
                    modifier = Modifier.fillMaxSize().padding(vertical = 24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.height(72.dp),
                    )
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = stringResource(page.titleRes),
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(page.bodyRes),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (pageIndex == 3) {
                        Spacer(Modifier.height(24.dp))
                        val allGranted = requiredPermissions.all {
                            ContextCompat.checkSelfPermission(context, it) ==
                                PackageManager.PERMISSION_GRANTED
                        }
                        SecondaryCalloraButton(
                            text = stringResource(R.string.action_grant_permission),
                            onClick = {
                                permissionRequestCount += 1
                                permissionLauncher.launch(requiredPermissions)
                            },
                            enabled = !allGranted,
                        )
                    }
                }
            }
        }

        Text(
            text = "${pagerState.currentPage + 1} / ${pages.size}",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Spacer(Modifier.height(8.dp))
        PrimaryCalloraButton(
            text = stringResource(
                if (pagerState.currentPage == pages.lastIndex) {
                    R.string.onboarding_button_finish
                } else {
                    R.string.onboarding_button_next
                },
            ),
            leadingIcon = Icons.AutoMirrored.Filled.ArrowForward,
            onClick = {
                if (pagerState.currentPage == pages.lastIndex) {
                    viewModel.completeOnboarding()
                    onFinished()
                } else {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        TextButton(
            onClick = {
                viewModel.completeOnboarding()
                onFinished()
            },
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
            Text(stringResource(R.string.onboarding_button_skip))
        }
        Spacer(Modifier.height(8.dp))
    }
}
