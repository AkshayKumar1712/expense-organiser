package protect.expenseorganiser;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsMessage;

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

        if (messageBody.contains("FNB")) {
            if (messageBody.contains("reserved")||messageBody.contains("purchase")) {
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

            if(!value.isEmpty() | !name.isEmpty()){
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
}