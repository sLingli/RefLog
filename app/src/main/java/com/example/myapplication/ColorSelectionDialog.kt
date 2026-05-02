package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

    0xFFFFFFFF.toInt(), // 白
    0xFF000000.toInt(), // 黑
)

@Composable
fun ColorSelectionDialog(
) {
    )




        Column(
        ) {
                Text(
                        fontWeight = FontWeight.Bold,
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                )
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                )
            }


            // 确认按钮
            Button(
                modifier = Modifier
            ) {
        }
    }
}

@Composable
    )
}

@Composable
}

@Composable
}
