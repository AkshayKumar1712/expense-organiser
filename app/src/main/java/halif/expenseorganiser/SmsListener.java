package halif.expenseorganiser;

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
//Standard Bank: R00.00 purchased for CELC PREPD   0627137032 Acc. 7721. Acl bal R00.00 2018-10-12 Query? 0860123107.New T&Cs Apply
        //Standard Bank: R00.00 paid from Acc. 7721 to DE LUCIA PROPERTY   164588112. Acl bal R00.000 2018-10-31 Query? 0860123107.
        //Standard Bank: R00.00 withdrawn from Acc. 7721 at STDACD17 2018-10-26T14:21:57 44512122611.
        if (messageBody.contains("Standard")) {

            try {
                if (messageBody.contains("withdrawn")) {
                    name = "Withdrawal @ " + messageBody.split(" ")[8].trim();
                    value = messageBody.split("R")[1].split(" ")[0].trim();
                } else if (messageBody.contains("paid from")) {
                    value = messageBody.split("R")[1].split(" ")[0].trim();
                    name = messageBody.split("to")[1].split("[0-9]+")[0].trim();
                } else if (messageBody.contains("purchased")) {
                    value = messageBody.split("R")[1].split(" ")[0].trim();
                    name = messageBody.split("for")[1].split("[0-9]+")[0].trim();
                }
            } catch (Exception e) {
                Log.w(TAG, e.getMessage());
                Toast.makeText(context, "Something went wrong \n when extracting values from SMS", Toast.LENGTH_LONG).show();
            }
        }
//Capitec: Money Out -R857.00 from SAVINGS ACCOUNT; Ref FNB BANK; Avail R10.00; 29-Oct. Call 0860102043
        if (messageBody.contains("Capitec") && messageBody.contains("Money Out")) {
            try {
                name = messageBody.split(";")[1].trim();
                value = messageBody.split("from")[0].split("R")[1].trim();
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