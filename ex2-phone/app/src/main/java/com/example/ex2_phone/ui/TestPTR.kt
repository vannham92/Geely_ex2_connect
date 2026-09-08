package com.example.ex2_phone.ui
import androidx.compose.runtime.Composable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun test() { PullToRefreshBox(isRefreshing = false, onRefresh = {}) {} }
