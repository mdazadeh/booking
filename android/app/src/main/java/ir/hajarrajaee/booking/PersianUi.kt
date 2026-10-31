package ir.hajarrajaee.booking

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp

/** Persian digits are presentation-only: storage, dates and API stay ASCII. */
fun String.faDigits():String=buildString(length) {
    for(ch in this@faDigits) append(if(ch in '0'..'9') ('۰'.code+ch.code-'0'.code).toChar() else ch)
}
fun String.asciiDigits():String=buildString(length) {
    for(ch in this@asciiDigits) append(when(ch) {
        in '۰'..'۹' -> ('0'.code+ch.code-'۰'.code).toChar()
        in '٠'..'٩' -> ('0'.code+ch.code-'٠'.code).toChar()
        else -> ch
    })
}
/** Paste-safe phone storage: remove Unicode direction marks, spaces and punctuation. */
fun phoneInput(text:String):String=buildString {
    for(ch in text.asciiDigits())if(ch in '0'..'9' || (ch=='+' && isEmpty()))append(ch)
}.take(20)
fun canonicalPhone(text:String):String {
    val raw=phoneInput(text)
    return when {
        raw.startsWith("+98") && raw.length>3 -> "0"+raw.drop(3)
        raw.startsWith("0098") && raw.length>4 -> "0"+raw.drop(4)
        raw.startsWith("98") && raw.length>=12 -> "0"+raw.drop(2)
        else -> raw
    }
}

val VaghFontFamily=FontFamily(
    Font(R.font.vazirmatn_regular,FontWeight.Normal),
    Font(R.font.vazirmatn_medium,FontWeight.Medium),
    Font(R.font.vazirmatn_bold,FontWeight.Bold),
    Font(R.font.vazirmatn_bold,FontWeight.SemiBold))
private val base=Typography()
val VaghTypography=Typography(
    displayLarge=base.displayLarge.copy(fontFamily=VaghFontFamily),
    displayMedium=base.displayMedium.copy(fontFamily=VaghFontFamily),
    displaySmall=base.displaySmall.copy(fontFamily=VaghFontFamily),
    headlineLarge=base.headlineLarge.copy(fontFamily=VaghFontFamily),
    headlineMedium=base.headlineMedium.copy(fontFamily=VaghFontFamily),
    headlineSmall=base.headlineSmall.copy(fontFamily=VaghFontFamily),
    titleLarge=base.titleLarge.copy(fontFamily=VaghFontFamily,fontWeight=FontWeight.Bold,fontSize=18.sp),
    titleMedium=base.titleMedium.copy(fontFamily=VaghFontFamily,fontWeight=FontWeight.Bold,fontSize=19.sp),
    titleSmall=base.titleSmall.copy(fontFamily=VaghFontFamily,fontWeight=FontWeight.Medium),
    bodyLarge=base.bodyLarge.copy(fontFamily=VaghFontFamily,fontWeight=FontWeight.Medium,fontSize=18.sp),
    bodyMedium=base.bodyMedium.copy(fontFamily=VaghFontFamily,fontWeight=FontWeight.Medium,fontSize=16.sp),
    bodySmall=base.bodySmall.copy(fontFamily=VaghFontFamily,fontSize=14.sp),
    labelLarge=base.labelLarge.copy(fontFamily=VaghFontFamily,fontWeight=FontWeight.Bold),
    labelMedium=base.labelMedium.copy(fontFamily=VaghFontFamily),
    labelSmall=base.labelSmall.copy(fontFamily=VaghFontFamily)
)

@Composable
fun PhoneNumberField(value:String,onValueChange:(String)->Unit,label:String="شماره موبایل") {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        OutlinedTextField(
            value=value.faDigits(),onValueChange={onValueChange(phoneInput(it))},
            label={Text(label)},modifier=Modifier.fillMaxWidth(),
            textStyle=TextStyle(fontFamily=VaghFontFamily,textDirection=TextDirection.Ltr,textAlign=TextAlign.Left),
            keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Phone),
            singleLine=true)
    }
}
