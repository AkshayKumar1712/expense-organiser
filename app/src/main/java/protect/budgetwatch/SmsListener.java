package protect.budgetwatch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.widget.Toast;

public class SmsListener extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String messageBody = "";
        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) {
            for (SmsMessage smsMessage : Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
               messageBody = smsMessage.getMessageBody();
            }
        }

//        Toast.makeText(context, "SMS received = "+ messageBody,Toast.LENGTH_LONG).show();
//        String SMS = "FNB :-) R320.56 reserved for purchase @ Pnp Fam Bright Water from cheq a/c..927486 using card..6517. Avail R2772. 21Oct 13:12";
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