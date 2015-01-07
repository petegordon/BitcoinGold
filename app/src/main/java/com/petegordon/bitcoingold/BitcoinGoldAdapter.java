package com.petegordon.bitcoingold;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.petegordon.bitcoingold.data.BitcoinGoldContract;

/**
 * Created by petegordon on 12/21/14.
 */
public class BitcoinGoldAdapter extends CursorAdapter {

    private static final int VIEW_TYPE_COUNT = 2;
    private static final int VIEW_TYPE_TODAY = 0;
    private static final int VIEW_TYPE_PAST_DAY = 1;

    // Flag to determine if we want to use a separate view for "today".
    private boolean mUseTodayLayout = true;


    /**
     * Cache of the children views for a BitcoinGold list item.
     */
    public static class ViewHolder {
        public final TextView bitcoinGoldRatioView;

        public final TextView bitcoinGoldDateView;

        public final ImageView bitcoinArrowView;
        public final ImageView goldArrowView;

        public final TextView goldPriceView;
        public final TextView bitcoinPriceView;

        public ViewHolder(View view) {
            bitcoinGoldRatioView = (TextView) view.findViewById(R.id.list_item_bitcoingold_ratio_textview);
            bitcoinGoldDateView = (TextView) view.findViewById(R.id.list_item_bitcoingold_date_textview);
            bitcoinArrowView = (ImageView) view.findViewById(R.id.list_item_bitcoin_direction_icon);
            goldArrowView = (ImageView) view.findViewById(R.id.list_item_gold_direction_icon);
            goldPriceView = (TextView) view.findViewById(R.id.list_item_gold_price_textview);
            bitcoinPriceView = (TextView) view.findViewById(R.id.list_item_bitcoin_price_textview);
        }
    }

    public BitcoinGoldAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        // Choose the layout type
        int viewType = getItemViewType(cursor.getPosition());
        int layoutId = -1;
        switch (viewType) {
            case VIEW_TYPE_TODAY: {
                layoutId = R.layout.bitcoingold_day_detail;
                break;
            }
            case VIEW_TYPE_PAST_DAY: {
                layoutId = R.layout.list_item_bitcoingold;
                break;
            }
        }

        View view = LayoutInflater.from(context).inflate(layoutId, parent, false);

        ViewHolder viewHolder = new ViewHolder(view);
        view.setTag(viewHolder);

        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        ViewHolder viewHolder = (ViewHolder) view.getTag();

        // Read date from cursor
        String dateString = Utility.getFriendlyDayString(context, cursor.getString(BitcoinGoldContract.BitcoinGoldEntry.COL_DATE));

        viewHolder.bitcoinGoldRatioView.setText(cursor.getString(BitcoinGoldContract.BitcoinGoldEntry.COL_PRICE_RATIO));
        if(viewHolder.bitcoinGoldDateView != null) {
            viewHolder.bitcoinGoldDateView.setText(dateString);
        }
        viewHolder.bitcoinPriceView.setText(Utility.getFormattedCurrencyValue(cursor.getDouble(BitcoinGoldContract.BitcoinGoldEntry.COL_BITCOIN_PRICE)));
        if(cursor.getString(BitcoinGoldContract.BitcoinGoldEntry.COL_GOLD_PRICE) != null) {
            viewHolder.goldPriceView.setText(Utility.getFormattedCurrencyValue(cursor.getDouble(BitcoinGoldContract.BitcoinGoldEntry.COL_GOLD_PRICE)));
        }else{
            viewHolder.goldPriceView.setText("");
        }

        if(Float.parseFloat(cursor.getString(BitcoinGoldContract.BitcoinGoldEntry.COL_BITCOIN_PERCENT_CHANGE)) > 0){
            viewHolder.bitcoinArrowView.setImageResource(R.drawable.uparrow);
        } else if(Float.parseFloat(cursor.getString(BitcoinGoldContract.BitcoinGoldEntry.COL_BITCOIN_PERCENT_CHANGE)) < 0){
            viewHolder.bitcoinArrowView.setImageResource(R.drawable.downarrow);
        } else {
            viewHolder.bitcoinArrowView.setImageResource(R.drawable.nochange);
        }

        boolean noChange = false;
        if(cursor.getString(BitcoinGoldContract.BitcoinGoldEntry.COL_GOLD_PERCENT_CHANGE) != null) {
            if (Float.parseFloat(cursor.getString(BitcoinGoldContract.BitcoinGoldEntry.COL_GOLD_PERCENT_CHANGE)) > 0) {
                viewHolder.goldArrowView.setImageResource(R.drawable.uparrow);
            } else if(Float.parseFloat(cursor.getString(BitcoinGoldContract.BitcoinGoldEntry.COL_GOLD_PERCENT_CHANGE)) < 0){
                viewHolder.goldArrowView.setImageResource(R.drawable.downarrow);
            } else {
                noChange = true;
            }
        }else{
            noChange = true;
        }

        if(noChange) {
            viewHolder.goldArrowView.setImageResource(R.drawable.nochange);
        }


    }

    public void setUseTodayLayout(boolean useTodayLayout) {
        mUseTodayLayout = useTodayLayout;
    }

    @Override
    public int getItemViewType(int position) {
        return (position == 0 && mUseTodayLayout) ? VIEW_TYPE_TODAY : VIEW_TYPE_PAST_DAY;
    }

    @Override
    public int getViewTypeCount() {
        return VIEW_TYPE_COUNT;
    }

}
