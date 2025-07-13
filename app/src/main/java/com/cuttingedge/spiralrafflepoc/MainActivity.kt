package com.cuttingedge.spiralrafflepoc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.cuttingedge.spiralrafflepoc.ui.compasables.SpiralRaffle
import com.cuttingedge.spiralrafflepoc.ui.data.Player
import com.cuttingedge.spiralrafflepoc.ui.theme.SpiralRafflePocTheme
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val playersList = mutableListOf<Player>()

        val numOfSpirals = 4

        with(playersList) {
            for (i in 0 until 80) {
                add(
                    Player(
                        id = UUID.randomUUID(),
                        drawableId = R.drawable.user,
                        spiralId = i % numOfSpirals,
                    )
                )
            }
        }

        setContent {
            SpiralRafflePocTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SpiralRaffle(
                        modifier = Modifier.padding(innerPadding),
                        numOfSpirals = numOfSpirals,
                        playersList = playersList
                    )
                }
            }
        }
    }
}