package protect.expenseorganiser;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

public class SmsListener extends BroadcastReceiver {

    private static final String TAG = "SmsReceiver";
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

        if (messageBody.contains("FNB")) {

            try {
                if (messageBody.contains("reserved") || messageBody.contains("purchase")) {
                    name = messageBody.split("@")[1].split("from")[0].trim();
                    messageBody = messageBody.split("@")[0];
                    value = messageBody.split("R")[1].split(" ")[0];
                } else if (messageBody.contains("paid from")) {
                    name = messageBody.split("(Ref\\.)")[1].split("(\\. )")[0].trim();
                    value = messageBody.split("R")[1].split("paid")[0].trim();
                } else if (messageBody.contains("withdrawn")) {
                    name = "Withdrawal @ " + messageBody.split("@")[1].split("\\.")[0].trim();
                    value = messageBody.split("R")[1].split("withdrawn")[0].trim();
                }
            } catch (Exception e) {
                Log.w(TAG, e.getMessage());
                Toast.makeText(context, "Something went wrong \n when extracting values from SMS", Toast.LENGTH_LONG).show();
            }
        }

        if (!value.isEmpty() | !name.isEmpty()) {
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