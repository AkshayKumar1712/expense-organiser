package halif.expenseorganiser;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

class TransactionCursorAdapter extends CursorAdapter
{
    public TransactionCursorAdapter(Context context, Cursor cursor)
    {
        super(context, cursor, 0);
    }

    private final DateFormat DATE_FORMATTER = SimpleDateFormat.getDateInstance();

    static class ViewHolder
    {
        TextView nameField;
        TextView valueField;
        TextView dateField;
        TextView budgetField;
        ImageView receiptIcon;
        TextView note;
        View noteLayout;
    }

    // The newView method is used to inflate a new view and return it,
    // you don't bind any data to the view at this point.
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent)
    {
        View view = LayoutInflater.from(context).inflate(R.layout.transaction_layout, parent, false);

        ViewHolder holder = new ViewHolder();
        holder.nameField = view.findViewById(R.id.name);
        holder.valueField = view.findViewById(R.id.value);
        holder.dateField = view.findViewById(R.id.date);
        holder.budgetField = view.findViewById(R.id.budget);
        holder.receiptIcon = view.findViewById(R.id.receiptIcon);
        holder.note = view.findViewById(R.id.note);
        holder.noteLayout = view.findViewById(R.id.noteLayout);
        view.setTag(holder);

        return view;
    }

    // The bindView method is used to bind all data to a given view
    // such as setting the text on a TextView.
    @Override
    public void bindView(View view, Context context, Cursor cursor)
    {
        ViewHolder holder = (ViewHolder)view.getTag();

        // Extract properties from cursor
        Transaction transaction = Transaction.toTransaction(cursor);

        // Populate fields with extracted properties
        holder.nameField.setText(transaction.description);
        holder.valueField.setText(String.format(Locale.US, "%.2f", transaction.value));
        holder.budgetField.setText(transaction.budget);

        holder.dateField.setText(DATE_FORMATTER.format(transaction.dateMs));

        if(transaction.receipt.isEmpty())
        {
            holder.receiptIcon.setVisibility(View.GONE);
        }
        else
        {
            holder.receiptIcon.setVisibility(View.VISIBLE);
        }

        if(transaction.note.isEmpty())
        {
            holder.noteLayout.setVisibility(View.GONE);
            holder.note.setText("");
        }
        else
        {
            holder.noteLayout.setVisibility(View.VISIBLE);
            holder.note.setText(transaction.note);
        }
    }
}
