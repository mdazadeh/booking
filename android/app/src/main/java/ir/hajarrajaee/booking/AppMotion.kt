package ir.hajarrajaee.booking

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Small real-operation loading state; no clock, hourglass or fake progress. */
@Composable
fun VaghLoadingLine(message:String){
    Surface(color=androidx.compose.ui.graphics.Color(0xFFF0EBFC),
        shape=androidx.compose.foundation.shape.RoundedCornerShape(15.dp)){
        Row(Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(12.dp)){
            CircularProgressIndicator(
                modifier=Modifier.size(20.dp),strokeWidth=2.5.dp,color=Lilac)
            Text(message,color=Forest,fontSize=14.sp)
        }
    }
}
