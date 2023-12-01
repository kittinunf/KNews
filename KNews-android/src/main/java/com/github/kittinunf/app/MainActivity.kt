package com.github.kittinunf.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.github.kittinunf.app.theme.KNewsTheme
import com.github.kittinunf.hackernews.api.Dependency
import com.github.kittinunf.hackernews.repository.HackerNewsServiceImpl

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            KNewsTheme(windows = window) {
                Surface {
                    MainScaffold(service)
                }
            }
        }
    }
}

val service = HackerNewsServiceImpl(Dependency.networkModule)

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    KNewsTheme {}
}
