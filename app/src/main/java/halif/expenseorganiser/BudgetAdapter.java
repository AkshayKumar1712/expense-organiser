package halif.expenseorganiser;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.List;

class BudgetAdapter extends ArrayAdapter<Budget>
{
    private final String FRACTION_FORMAT;

    public BudgetAdapter(Context context, List<Budget> items)
    {
        super(context, 0, items);

        FRACTION_FORMAT = context.getResources().getString(R.string.fraction);
    }

    static class ViewHolder
    {
        TextView budgetName;
        ProgressBar budgetBar;
        TextView budgetValue;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        // Get the data item for this position
        Budget item = getItem(position);

        ViewHolder holder;

        // Check if an existing view is being reused, otherwise inflate the view

        if (convertView == null)
        {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.budget_layout,
                    parent, false);

            holder = new ViewHolder();
            holder.budgetName = convertView.findViewById(R.id.budgetName);
            holder.budgetBar = convertView.findViewById(R.id.budgetBar);
            holder.budgetValue = convertView.findViewById(R.id.budgetValue);
            convertView.setTag(holder);
        }
        else
        {
            holder = (ViewHolder)convertView.getTag();
        }

        holder.budgetName.setText(item.name);

        holder.budgetBar.setMax(item.max);
        holder.budgetBar.setProgress(item.current);

        String fraction = String.format(FRACTION_FORMAT, item.current, item.max);

        holder.budgetValue.setText(fraction);

        if (item.current > item.max) {
            holder.budgetValue.setBackgroundColor(Color.parseColor("#cc0000"));
        } else if (item.current < item.max) {
            holder.budgetValue.setBackgroundColor(Color.GREEN);
        } else {
            holder.budgetValue.setBackgroundColor(Color.parseColor("#ffda00"));
        }

        return convertView;
    }
}
