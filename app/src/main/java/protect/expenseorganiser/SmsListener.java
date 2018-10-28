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

        String value;
        String name;

        if(messageBody.startsWith("FNB :-) R")){
            value = messageBody.substring(messageBody.indexOf("R") + 1, messageBody.indexOf("R") + 7);
            name = messageBody.substring(messageBody.indexOf("@") + 2, messageBody.indexOf(" from"));
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