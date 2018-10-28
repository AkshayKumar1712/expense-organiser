package protect.expenseorganiser;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PatternMatcher;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.widget.Toast;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SmsListener extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String messageBody = "";
        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) {
            for (SmsMessage smsMessage : Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
                messageBody = smsMessage.getMessageBody();
            }
        }

        String value = "";
        String name = "";

        Pattern pattern = Pattern.compile("([0-9.]+)");
        Matcher matcher;

        if (messageBody.contains("FNB :-)")) {
            if (messageBody.contains("reserved")) {
                matcher = pattern.matcher(messageBody.split("from")[0].trim());
                while (matcher.find()) {
                    value = matcher.group(0);
                }
                name = messageBody.split("@")[1].split("from")[0].trim();
            } else if (messageBody.contains("paid")) {
                matcher = pattern.matcher(messageBody);
                while (matcher.find()) {
                    value = matcher.group(0);
                }
                name = messageBody.split("Ref")[1].split(".")[0].trim();
            }else if (messageBody.contains("withdrawn")){
                matcher = pattern.matcher(messageBody.split("from")[0].trim());
                while (matcher.find()) {
                    value = matcher.group(0);
                }
                name = "Withdrawal @ " + messageBody.split("@")[1].split("Avail")[0].trim();
            }

            Intent i = new Intent(context, TransactionViewActivity.class);
            final Bundle b = new Bundle();
            b.putInt("type", 1);
            b.putString("value", value);
            b.putString("name", name);
            i.putExtras(b);
            context.startActivity(i);
        }
    }
}